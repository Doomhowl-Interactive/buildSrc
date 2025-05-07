/*
 * Copyright (C) 2021 The Android Open Source Project
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

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.function.Consumer;


public abstract class CMakePortTask extends PortTask {

    @Input
    public abstract Property<Consumer<CMakeBuilder>> getCmake();

    public void cmake(Consumer<CMakeBuilder> block) {
        getCmake().set(block);
    }

    private final String cmakeBinary;

    {
        Properties localProperties = new Properties();
        File localPropertiesFile = getProject().getRootProject().file("local.properties");
        if (localPropertiesFile.exists()) {
            try {
                localProperties.load(localPropertiesFile.inputStream());
            } catch (Exception e) {
                throw new RuntimeException("Failed to load local.properties", e);
            }
        }
        cmakeBinary = localProperties.getProperty("cmakeBinary", "cmake");
    }

    @Override
    protected void buildForAbi(
            Toolchain toolchain,
            File workingDirectory,
            File buildDirectory,
            File installDirectory) {
        checkCMakeInstalled();
        configure(toolchain, buildDirectory, installDirectory);
        build(buildDirectory);
        install(buildDirectory);
    }

    private void checkCMakeInstalled() {
        try {
            executeSubprocess(
                    Arrays.asList(cmakeBinary, "--version"),
                    File.createTempFile("cmake", "check").getParentFile());
        } catch (Exception e) {
            throw new RuntimeException(
                    "CMake not found. Please install CMake and add it to your PATH.", e);
        }
    }

    private void configure(
            Toolchain toolchain, File buildDirectory, File installDirectory) {
        Consumer<CMakeBuilder> cmakeBlock = getCmake().get();
        CMakeBuilder builder = new CMakeBuilder(
                toolchain,
                getPrefabGenerated().get().getAsFile().resolve(toolchain.getAbi().getTriple()));
        cmakeBlock.accept(builder);

        File toolchainFile = toolchain.getNdk().getPath().resolve("build/cmake/android.toolchain.cmake");

        buildDirectory.mkdirs();

        List<String> command = new ArrayList<>();
        command.add(cmakeBinary);
        command.add("-DCMAKE_TOOLCHAIN_FILE=" + toolchainFile.getAbsolutePath());
        command.add("-DCMAKE_BUILD_TYPE=RelWithDebInfo");
        command.add("-DCMAKE_INSTALL_PREFIX=" + installDirectory.getAbsolutePath());
        command.add("-DANDROID_ABI=" + toolchain.getAbi().getAbiName());
        command.add("-DANDROID_API_LEVEL=" + toolchain.getApi());
        command.add("-GNinja");
        command.add(getSourceDirectory().get().getAsFile().getAbsolutePath());
        command.addAll(builder.getCmd());

        executeSubprocess(command, buildDirectory, builder.getEnv());
    }

    private void build(File buildDirectory) {
        executeSubprocess(Arrays.asList("ninja", "-v"), buildDirectory);
    }

    private void install(File buildDirectory) {
        executeSubprocess(Arrays.asList("ninja", "-v", "install"), buildDirectory);
    }
}