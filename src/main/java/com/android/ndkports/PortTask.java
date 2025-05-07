package com.android.ndkports;

import org.gradle.api.DefaultTask;
import org.gradle.api.file.Directory;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputDirectory;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public abstract class PortTask extends DefaultTask {

    @InputDirectory
    public abstract DirectoryProperty getSourceDirectory();

    @OutputDirectory
    public abstract DirectoryProperty getBuildDir();

    @OutputDirectory
    public Provider<Directory> getInstallDir() {
        return getBuildDir().dir("install");
    }

    @InputDirectory
    public abstract DirectoryProperty getPrefabGenerated();

    @Input
    public abstract Property<Integer> getMinSdkVersion();

    @InputDirectory
    public abstract DirectoryProperty getNdkPath();

    private Ndk getNdk() {
        return new Ndk(getNdkPath().getAsFile().get());
    }

    /**
     * The number of CPUs available for building.
     *
     * May be passed to the build system if required.
     */
    @Internal
    protected int ncpus = Runtime.getRuntime().availableProcessors();

    protected void executeSubprocess(
            List<String> args,
            File workingDirectory,
            Map<String, String> additionalEnvironment) throws Exception {
        ProcessBuilder pb = new ProcessBuilder(args).redirectErrorStream(true)
                .directory(workingDirectory);

        if (additionalEnvironment != null) {
            pb.environment().putAll(additionalEnvironment);
        }

        Process result = pb.start();
        String output = new BufferedReader(new InputStreamReader(result.getInputStream())).lines()
                .collect(Collectors.joining("\n"));
        if (result.waitFor() != 0) {
            throw new Exception("Subprocess failed with:\n" + output);
        }
    }

    public File buildDirectoryFor(Abi abi) {
        return new File(getBuildDir().getAsFile().get(), "build/" + abi);
    }

    public File installDirectoryFor(Abi abi) {
        return new File(getInstallDir().get().getAsFile(), abi.toString());
    }

    @TaskAction
    public void run() {
        for (Abi abi : Abi.values()) {
            int api = abi.adjustMinSdkVersion(getMinSdkVersion().get());
            buildForAbi(
                    new Toolchain(getNdk(), abi, api),
                    getBuildDir().getAsFile().get(),
                    buildDirectoryFor(abi),
                    installDirectoryFor(abi));
        }
    }

    protected abstract void buildForAbi(
            Toolchain toolchain,
            File workingDirectory,
            File buildDirectory,
            File installDirectory);
}
