package com.android.ndkports;

import com.google.prefab.api.BuildSystemInterface;
import com.google.prefab.api.Module;
import com.google.prefab.api.Package;
import com.google.prefab.api.PlatformDataInterface;
import java.io.File;
import java.io.FileAlreadyExistsException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class PrefabSysrootPlugin implements BuildSystemInterface {
    private final File outputDirectory;
    private final List<Package> packages;

    public PrefabSysrootPlugin(File outputDirectory, List<Package> packages) {
        this.outputDirectory = outputDirectory;
        this.packages = packages;
    }

    @Override
    public File getOutputDirectory() {
        return outputDirectory;
    }

    @Override
    public List<Package> getPackages() {
        return packages;
    }

    @Override
    public void generate(Collection<PlatformDataInterface> requirements) {
        prepareOutputDirectory(outputDirectory);

        for (Package pkg : packages) {
            for (Module module : pkg.getModules()) {
                for (PlatformDataInterface requirement : requirements) {
                    installModule(module, requirement);
                }
            }
        }
    }

    private void installModule(Module module, PlatformDataInterface requirement) {
        File installDir = new File(outputDirectory, requirement.getTargetTriple());
        File includeDir = new File(installDir, "include");

        if (module.isHeaderOnly()) {
            installHeaders(module.getIncludePath().toFile(), includeDir);
            return;
        }

        Module.Library library = module.getLibraryFor(requirement);
        installHeaders(module.getIncludePath().toFile(), includeDir);

        File libDir = new File(installDir, "lib");
        libDir.mkdirs();

        File srcFile = library.getPath().toFile();
        File destFile = new File(libDir, srcFile.getName());
        try {
            java.nio.file.Files.copy(srcFile.toPath(), destFile.toPath());
        } catch (java.io.IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void installHeaders(File src, File dest) {
        try {
            org.apache.commons.io.FileUtils.copyDirectory(src, dest, file -> {
                if (file.isFile() && file.exists() && new File(dest, file.getName()).exists()) {
                    File existingFile = new File(dest, file.getName());
                    try {
                        byte[] srcBytes = java.nio.file.Files.readAllBytes(file.toPath());
                        byte[] destBytes = java.nio.file.Files.readAllBytes(existingFile.toPath());

                        if (!Arrays.equals(srcBytes, destBytes)) {
                            String path = file.getAbsolutePath().replace(dest.getAbsolutePath(), "");
                            throw new RuntimeException(
                                    "Found duplicate headers with non-equal contents: " + path);
                        }
                        return false; // Skip this file
                    } catch (java.io.IOException e) {
                        throw new RuntimeException(e);
                    }
                }
                return true; // Include this file
            });
        } catch (java.io.IOException e) {
            throw new RuntimeException(e);
        }
    }
}