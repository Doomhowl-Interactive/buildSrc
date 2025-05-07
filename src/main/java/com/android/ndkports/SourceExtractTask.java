package com.android.ndkports;

import lombok.Getter;
import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.TextProgressMonitor;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;

public abstract class SourceExtractTask extends DefaultTask {

    @Optional
    @InputFile
    public abstract Property<File> getTarSource();

    @Optional
    @Input
    public abstract Property<GitSourceArgs> getGitSource();

    @Optional
    @Input
    public abstract Property<File> getRawSource();

    @OutputDirectory
    public abstract DirectoryProperty getOutDir();

    @TaskAction
    public void run() {
        int counter = 0;
        if (getTarSource().isPresent())
            counter++;
        if (getGitSource().isPresent())
            counter++;
        if (getRawSource().isPresent())
            counter++;

        if (counter > 1) {
            throw new RuntimeException("Only one source can be specified, either tar or git");
        }

        // skip if output directory already exists
        if (getOutDir().get().getAsFile().exists() &&
                getOutDir().get().getAsFileTree().getFiles().size() > 0) {
            getLogger().lifecycle("Output directory already exists, skipping extraction.");
            return;
        }

        if (getTarSource().isPresent()) {
            extractTar(getTarSource().get().getAbsolutePath());
        } else if (getGitSource().isPresent()) {
            cloneGitRepo(getGitSource().get());
        } else if (getRawSource().isPresent()) {
            File rawSourceFile = getRawSource().get();
            if (!rawSourceFile.isDirectory()) {
                throw new RuntimeException("Raw source must be a directory");
            }
            if (rawSourceFile.exists()) {
                try {
                    FileUtils.copyDirectory(rawSourceFile, getOutDir().get().getAsFile());
                } catch (java.io.IOException e) {
                    throw new RuntimeException(e);
                }
            } else {
                throw new RuntimeException("Raw source folder does not exist: " + rawSourceFile.getAbsolutePath());
            }
        } else {
            throw new RuntimeException(
                    "No source specified, must be either a tar file, a git URL or raw source folder");
        }
    }

    private void cloneGitRepo(GitSourceArgs args) {
        try {
            Git.cloneRepository()
                    .setURI(args.getUrl())
                    .setTimeout(60)
                    .setProgressMonitor(new TextProgressMonitor())
                    .setCloneSubmodules(true)
                    .setBranch(args.getBranch())
                    .setDepth(1)
                    .setDirectory(getOutDir().get().getAsFile())
                    .call();
        } catch (GitAPIException e) {
            throw new RuntimeException("Failed to clone git repository", e);
        }
    }

    private void extractTar(String tarFile) {
        // TODO: Cross-platform solution
        try {
            List<String> command = Arrays.asList(
                    "tar",
                    "xf",
                    tarFile,
                    "--strip-components=1");

            ProcessBuilder pb = new ProcessBuilder(command)
                    .redirectErrorStream(true)
                    .directory(getOutDir().get().getAsFile());

            Process process = pb.start();
            StringBuilder output = new StringBuilder();

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new RuntimeException("Subprocess failed with:\n" + output.toString());
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to extract tar file", e);
        }
    }
}