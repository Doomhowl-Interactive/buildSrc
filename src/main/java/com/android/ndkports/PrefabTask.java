package com.android.ndkports;

import com.google.prefab.api.Android;
import com.google.prefab.api.BuildSystemInterface;
import com.google.prefab.api.Package;
import lombok.Getter;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.Directory;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.FileCollection;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputDirectory;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public abstract class PrefabTask extends DefaultTask {
    @InputFiles
    private FileCollection aars;

    @Getter
    @OutputDirectory
    public abstract DirectoryProperty getOutputDirectory();

    @OutputDirectory
    public Provider<Directory> getGeneratedDirectory() {
        return getOutputDirectory().dir("generated");
    }

    @Getter
    @Optional
    @Input
    public abstract Property<Class<? extends BuildSystemInterface>> getGenerator();

    @Getter
    @InputDirectory
    public abstract DirectoryProperty getNdkPath();

    @Getter
    @Input
    public abstract Property<Integer> getMinSdkVersion();

    private Ndk getNdk() {
        return new Ndk(getNdkPath().getAsFile().get());
    }

    public void setAars(FileCollection aars) {
        this.aars = aars;
    }

    @TaskAction
    public void run() {
        if (!getGenerator().isPresent()) {
            // Creating the generated directory even if we have no generator
            // makes it easier to write tasks that *might* consume prefab
            // packages.
            getGeneratedDirectory().get().getAsFile().mkdirs();
            return;
        }

        File outDir = getOutputDirectory().get().getAsFile();
        List<Package> packages = new ArrayList<>();
        for (File aar : aars) {
            File packagePath = new File(outDir, aar.getName().replace(".aar", ""));
            extract(aar, packagePath);
            packages.add(new Package(packagePath.toPath().resolve("prefab")));
        }
        generateSysroot(packages, getMinSdkVersion().get(), getNdk().getVersion().getMajor());
    }

    private void extract(File aar, File extractDir) {
        extractDir.mkdirs();
        try (ZipFile zip = new ZipFile(aar)) {
            for (ZipEntry entry : java.util.Collections.list(zip.entries())) {
                try {
                    File outFile = new File(extractDir, entry.getName());
                    if (entry.isDirectory()) {
                        outFile.mkdirs();
                    } else {
                        outFile.getParentFile().mkdirs();
                        try (java.io.InputStream input = zip.getInputStream(entry);
                                java.io.OutputStream output = new java.io.FileOutputStream(outFile)) {
                            byte[] buffer = new byte[1024];
                            int length;
                            while ((length = input.read(buffer)) > 0) {
                                output.write(buffer, 0, length);
                            }
                        }
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void generateSysroot(List<Package> packages, int osVersion, int ndkVersion) {
        Class<? extends BuildSystemInterface> generatorType = getGenerator().get();
        try {
            Constructor<? extends BuildSystemInterface> constructor = generatorType.getConstructor(
                    File.class, List.class);
            BuildSystemInterface buildSystemIntegration = constructor.newInstance(
                    getGeneratedDirectory().get().getAsFile(), packages);

            List<Android> androidPlatforms = java.util.Arrays.stream(Android.Abi.values())
                    .map(abi -> new Android(abi, osVersion, Android.Stl.CxxShared, ndkVersion))
                    .collect(Collectors.toList());

            buildSystemIntegration.generate(androidPlatforms);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}