import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.UntrackedTask

@UntrackedTask(because = 'Uploads to a remote server')
abstract class PublishForgejoRelease extends DefaultTask {
    @Input abstract Property<String> getServerUrl()
    @Input abstract Property<String> getRepo()
    @Input abstract Property<String> getTag()
    @InputFiles abstract ConfigurableFileCollection getJars()
    @Internal abstract DirectoryProperty getRepoDir()

    @TaskAction
    void publish() {
        String token = readToken()
        String sha = requireCleanPushedHead()
        def release = new ForgejoReleasePublisher(token).publish(serverUrl.get(), repo.get(), tag.get(), sha, jars.files)
        logger.lifecycle("Published ${jars.files*.name.join(', ')} to ${release.html_url}")
    }

    private String readToken() {
        def fromEnv = System.getenv('RELEASE_TOKEN')
        if (fromEnv) return fromEnv
        def env = new File(System.getProperty('user.home'), '.env')
        if (env.isFile()) {
            def line = env.readLines().find { it.trim() ==~ /(export\s+)?RELEASE_TOKEN\s*=.*/ }
            if (line) {
                def value = line.substring(line.indexOf('=') + 1).trim().replaceAll(/^["']|["']$/, '')
                if (value) return value
            }
        }
        throw new GradleException('RELEASE_TOKEN not found in ~/.env or environment')
    }

    private String requireCleanPushedHead() {
        if (git('status', '--porcelain')) {
            throw new GradleException('Working tree has uncommitted changes')
        }
        git('fetch', '--quiet', 'origin')
        if (!git('for-each-ref', '--contains=HEAD', '--format=%(refname)', 'refs/remotes/origin/')) {
            throw new GradleException('HEAD has not been pushed to origin')
        }
        return git('rev-parse', 'HEAD')
    }

    private String git(String... args) {
        def proc = new ProcessBuilder(['git', *args]).directory(repoDir.get().asFile).redirectErrorStream(true).start()
        def out = proc.inputStream.text.trim()
        if (proc.waitFor() != 0) throw new GradleException("git ${args.join(' ')} failed: $out")
        return out
    }
}
