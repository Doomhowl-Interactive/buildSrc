package com.android.ndkports

import com.github.syari.kgit.KGit.Companion.cloneRepository
import org.eclipse.jgit.lib.TextProgressMonitor
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import java.io.File

abstract class SourceExtractTask : DefaultTask() {

    @get:Optional
    @get:InputFile
    abstract val tarSource: Property<File>

    @get:Optional
    @get:Input
    abstract val gitSource: Property<GitSourceArgs>

    @get:OutputDirectory
    abstract val outDir: DirectoryProperty

    @TaskAction
    fun run() {
        if (tarSource.isPresent && gitSource.isPresent) {
            throw RuntimeException("Both source and gitSource are specified! Use one of the two.")
        }

        // skip if output directory already exists
        if (outDir.get().asFile.exists() && outDir.get().asFileTree.files.isNotEmpty()) {
            logger.lifecycle("Output directory already exists, skipping extraction.")
            return
        }

        if (tarSource.isPresent) {
            extractTar(tarSource.get().absolutePath);
        } else if (gitSource.isPresent) {
            cloneGitRepo(gitSource.get())
        } else {
            throw RuntimeException("No source specified, must be either a tar file or a git URL")
        }
    }

    private fun cloneGitRepo(args: GitSourceArgs) {
        cloneRepository {
            setURI(args.url)
            setTimeout(60)
            setProgressMonitor(TextProgressMonitor())
            setCloneSubmodules(true)
            setBranch(args.branch)
            setDepth(1)
            setDirectory(outDir.get().asFile)
        }
    }

    private fun extractTar(tarFile: String) {
        // TODO: Cross-platform solution
        val pb = ProcessBuilder(
            listOf(
                "tar",
                "xf",
                tarFile,
                "--strip-components=1"
            )
        ).redirectErrorStream(true).directory(outDir.get().asFile)

        val result = pb.start()
        val output = result.inputStream.bufferedReader().use { it.readText() }
        if (result.waitFor() != 0) {
            throw RuntimeException("Subprocess failed with:\n$output")
        }
    }
}