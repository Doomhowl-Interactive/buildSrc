/*
 * Copyright (C) 2019 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.ndkports;

import com.google.prefab.api.AndroidAbiMetadata;
import com.google.prefab.api.ModuleMetadataV1;
import com.google.prefab.api.PackageMetadataV1;
import lombok.Getter;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

public class PrefabPackageBuilder {
    private final PackageData packageData;
    private final File packageDirectory;
    private final File directory;
    private final File sourceDirectory;
    private final Ndk ndk;
    private final File prefabDirectory;
    private final File modulesDirectory;

    // TODO: Get from gradle.
    private final String packageName;

    public PrefabPackageBuilder(
            PackageData packageData,
            File packageDirectory,
            File directory,
            File sourceDirectory,
            Ndk ndk) {
        this.packageData = packageData;
        this.packageDirectory = packageDirectory;
        this.directory = directory;
        this.sourceDirectory = sourceDirectory;
        this.ndk = ndk;
        this.prefabDirectory = new File(packageDirectory, "prefab");
        this.modulesDirectory = new File(prefabDirectory, "modules");
        this.packageName = "com.android.ndk.thirdparty." + packageData.getName();
    }

    private void preparePackageDirectory() {
        if (packageDirectory.exists()) {
            deleteRecursively(packageDirectory);
        }
        modulesDirectory.mkdirs();
    }

    private void deleteRecursively(File file) {
        if (file.isDirectory()) {
            for (File child : file.listFiles()) {
                deleteRecursively(child);
            }
        }
        file.delete();
    }

    private void makePackageMetadata() {
        try {
            List<String> dependencies = new ArrayList<>(packageData.getDependencies().keySet());

            PackageMetadataV1 metadata = new PackageMetadataV1(
                    packageData.getName(),
                    1,
                    dependencies,
                    packageData.getPrefabVersion().toString());

            File metadataFile = new File(prefabDirectory, "prefab.json");
            Files.writeString(metadataFile.toPath(), serializeToJson(metadata));
        } catch (IOException e) {
            throw new RuntimeException("Failed to create package metadata", e);
        }
    }

    private <T> String serializeToJson(T object) {
        try {
            return com.fasterxml.jackson.databind.json.JsonMapper.builder()
                    .build()
                    .writeValueAsString(object);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize to JSON", e);
        }
    }

    private void makeModuleMetadata(ModuleDescription module, File moduleDirectory) {
        try {
            ModuleMetadataV1 metadata = new ModuleMetadataV1(module.getDependencies());

            File metadataFile = new File(moduleDirectory, "module.json");
            Files.writeString(metadataFile.toPath(), serializeToJson(metadata));
        } catch (IOException e) {
            throw new RuntimeException("Failed to create module metadata", e);
        }
    }

    private void installLibForAbi(ModuleDescription module, Abi abi, File libsDir) {
        String extension = module.isStatic_() ? "a" : "so";
        String libName = "lib" + module.getName() + "." + extension;
        File installDirectory = new File(libsDir, "android." + abi.getAbiName());
        installDirectory.mkdirs();

        try {
            File src = new File(directory, abi + "/lib/" + libName);
            File dest = new File(installDirectory, libName);
            Files.copy(src.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);

            AndroidAbiMetadata metadata = new AndroidAbiMetadata(
                    abi.getAbiName(),
                    abi.adjustMinSdkVersion(packageData.getMinSdkVersion()),
                    ndk.getVersion().getMajor(),
                    "c++_shared");

            File metadataFile = new File(installDirectory, "abi.json");
            Files.writeString(metadataFile.toPath(), serializeToJson(metadata));
        } catch (IOException e) {
            throw new RuntimeException("Failed to install library for ABI: " + abi, e);
        }
    }

    private void installLicense() {
        try {
            File src = new File(sourceDirectory, packageData.getLicensePath());
            File metaInfDir = new File(packageDirectory, "META-INF");
            metaInfDir.mkdirs();

            File dest = new File(metaInfDir, new File(packageData.getLicensePath()).getName());
            Files.copy(src.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException("Failed to install license", e);
        }
    }

    private void createAndroidManifest() {
        try {
            String manifest = createXml();
            File manifestFile = new File(packageDirectory, "AndroidManifest.xml");
            Files.writeString(manifestFile.toPath(), manifest);
        } catch (IOException e) {
            throw new RuntimeException("Failed to create Android manifest", e);
        }
    }

    private String createXml() {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n");
        sb.append("<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\"\n");
        sb.append("    package=\"").append(packageName).append("\"\n");
        sb.append("    android:versionCode=\"1\"\n");
        sb.append("    android:versionName=\"1.0\">\n");
        sb.append("    <uses-sdk\n");
        sb.append("        android:minSdkVersion=\"").append(packageData.getMinSdkVersion()).append("\"\n");
        sb.append("        android:targetSdkVersion=\"29\" />\n");
        sb.append("</manifest>\n");
        return sb.toString();
    }

    public void build() {
        preparePackageDirectory();
        makePackageMetadata();

        for (ModuleDescription module : packageData.getModules()) {
            File moduleDirectory = new File(modulesDirectory, module.getName());
            moduleDirectory.mkdirs();

            makeModuleMetadata(module, moduleDirectory);

            if (module.isIncludesPerAbi()) {
                throw new UnsupportedOperationException("includesPerAbi is not implemented");
            } else {
                // TODO: Check that headers are actually identical across ABIs.
                File includeDir = new File(directory, Abi.Arm + "/include");
                File destIncludeDir = new File(moduleDirectory, "include");
                try {
                    org.apache.commons.io.FileUtils.copyDirectory(includeDir, destIncludeDir);
                } catch (IOException e) {
                    throw new RuntimeException("Failed to copy include directory", e);
                }
            }

            if (!module.isHeaderOnly()) {
                File libsDir = new File(moduleDirectory, "libs");
                libsDir.mkdirs();

                for (Abi abi : Abi.values()) {
                    installLibForAbi(module, abi, libsDir);
                }
            }
        }

        installLicense();
        createAndroidManifest();
    }
}