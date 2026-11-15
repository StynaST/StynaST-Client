import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

/** Writes compile-time feature constants, so disabled features compile out entirely. */
@CacheableTask
abstract class GenerateBuildFlags extends DefaultTask {
    @Input abstract Property<Boolean> getFreecam()
    @OutputDirectory abstract DirectoryProperty getOutputDir()

    @TaskAction
    void generate() {
        def file = outputDir.file('net/styna/stynastmc/BuildFlags.java').get().asFile
        file.parentFile.mkdirs()
        file.text = """package net.styna.stynastmc;

public final class BuildFlags {
    public static final boolean FREECAM = ${freecam.get()};

    private BuildFlags() {
    }
}
"""
    }
}
