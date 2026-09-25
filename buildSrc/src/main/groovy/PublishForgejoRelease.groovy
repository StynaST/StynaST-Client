import groovy.json.JsonOutput
import groovy.json.JsonSlurper
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

import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

@UntrackedTask(because = 'Uploads to a remote server')
abstract class PublishForgejoRelease extends DefaultTask {
    @Input abstract Property<String> getServerUrl()
    @Input abstract Property<String> getRepo()
    @Input abstract Property<String> getTag()
    @InputFiles abstract ConfigurableFileCollection getJars()
    @Internal abstract DirectoryProperty getRepoDir()

    private transient HttpClient http

    @TaskAction
    void publish() {
        http = HttpClient.newHttpClient()
        String token = readToken()
        String sha = requireCleanPushedHead()
        String api = "${serverUrl.get().replaceAll('/+$', '')}/api/v1/repos/${repo.get()}"
        String tagName = tag.get()

        // Replace an existing release and its tag so the tag moves to HEAD
        def existing = call(token, 'GET', "$api/releases/tags/$tagName", null, 200, 404)
        if (existing.statusCode() == 200) {
            def id = new JsonSlurper().parseText(existing.body()).id
            call(token, 'DELETE', "$api/releases/$id", null, 204)
            logger.lifecycle("Deleted existing release $tagName")
        }
        call(token, 'DELETE', "$api/tags/$tagName", null, 204, 404)

        String notes = "[Built from commit $sha](${serverUrl.get().replaceAll('/+$', '')}/${repo.get()}/commit/$sha)"
        def created = call(token, 'POST', "$api/releases",
                JsonOutput.toJson([tag_name: tagName, target_commitish: sha, name: tagName, body: notes]), 201)
        def releaseId = new JsonSlurper().parseText(created.body()).id
        logger.lifecycle("Created release $tagName at $sha")

        jars.files.each { jar ->
            if (!jar.exists()) throw new GradleException("Missing jar: $jar")
            uploadAsset(token, "$api/releases/$releaseId/assets?name=${URLEncoder.encode(jar.name, 'UTF-8')}", jar)
            logger.lifecycle("Uploaded ${jar.name}")
        }
    }

    private String readToken() {
        def env = new File(System.getProperty('user.home'), '.env')
        if (env.isFile()) {
            def line = env.readLines().find { it.trim() ==~ /(export\s+)?RELEASE_TOKEN\s*=.*/ }
            if (line) {
                def value = line.substring(line.indexOf('=') + 1).trim().replaceAll(/^["']|["']$/, '')
                if (value) return value
            }
        }
        def fromEnv = System.getenv('RELEASE_TOKEN')
        if (fromEnv) return fromEnv
        throw new GradleException('RELEASE_TOKEN not found in ~/.env or environment')
    }

    private String requireCleanPushedHead() {
        if (git('status', '--porcelain')) {
            throw new GradleException('Working tree has uncommitted changes')
        }
        git('fetch', '--quiet')
        if (git('rev-list', '@{u}..HEAD')) {
            throw new GradleException('HEAD has commits not pushed to upstream')
        }
        return git('rev-parse', 'HEAD')
    }

    private String git(String... args) {
        def proc = new ProcessBuilder(['git', *args]).directory(repoDir.get().asFile).redirectErrorStream(true).start()
        def out = proc.inputStream.text.trim()
        if (proc.waitFor() != 0) throw new GradleException("git ${args.join(' ')} failed: $out")
        return out
    }

    private HttpResponse<String> call(String token, String method, String url, String json, int... ok) {
        def req = HttpRequest.newBuilder(URI.create(url))
                .header('Authorization', "token $token")
                .header('Accept', 'application/json')
        if (json != null) {
            req.header('Content-Type', 'application/json').method(method, HttpRequest.BodyPublishers.ofString(json))
        } else {
            req.method(method, HttpRequest.BodyPublishers.noBody())
        }
        return check(http.send(req.build(), HttpResponse.BodyHandlers.ofString()), method, url, ok)
    }

    private void uploadAsset(String token, String url, File jar) {
        String boundary = "----stynast${UUID.randomUUID()}"
        def head = "--$boundary\r\nContent-Disposition: form-data; name=\"attachment\"; filename=\"${jar.name}\"\r\n" +
                "Content-Type: application/java-archive\r\n\r\n"
        def body = HttpRequest.BodyPublishers.concat(
                HttpRequest.BodyPublishers.ofString(head),
                HttpRequest.BodyPublishers.ofFile(jar.toPath()),
                HttpRequest.BodyPublishers.ofString("\r\n--$boundary--\r\n"))
        def req = HttpRequest.newBuilder(URI.create(url))
                .header('Authorization', "token $token")
                .header('Content-Type', "multipart/form-data; boundary=$boundary")
                .POST(body).build()
        check(http.send(req, HttpResponse.BodyHandlers.ofString()), 'POST', url, 201)
    }

    private static HttpResponse<String> check(HttpResponse<String> res, String method, String url, int... ok) {
        if (!(res.statusCode() in ok)) {
            throw new GradleException("$method $url -> ${res.statusCode()}: ${res.body()}")
        }
        return res
    }
}
