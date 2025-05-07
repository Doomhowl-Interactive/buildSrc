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

import lombok.Getter;

import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.inject.Inject;

public abstract class MesonPortTask extends PortTask {

    public enum DefaultLibraryType {
        Both("both"),
        Shared("shared"),
        Static("static");

        @Getter
        private final String argument;

        DefaultLibraryType(String argument) {
            this.argument = argument;
        }
    }

    @Input
    public abstract Property<DefaultLibraryType> getDefaultLibraryType();

    @Inject
    public MesonPortTask(ObjectFactory objects) {
        getDefaultLibraryType().convention(DefaultLibraryType.Shared);
    }

    @Override
    protected void buildForAbi(
            Toolchain toolchain,
            File workingDirectory,
            File buildDirectory,
            File installDirectory) {
        configure(toolchain, workingDirectory, buildDirectory, installDirectory);
        build(buildDirectory);
        install(buildDirectory);
    }

    private void configure(
            Toolchain toolchain,
            File workingDirectory,
            File buildDirectory,
            File installDirectory) {
        String cpuFamily;
        switch (toolchain.getAbi()) {
            case Arm:
                cpuFamily = "arm";
                break;
            case Arm64:
                cpuFamily = "aarch64";
                break;
            case X86:
                cpuFamily = "x86";
                break;
            case X86_64:
                cpuFamily = "x86_64";
                break;
            default:
                throw new IllegalStateException("Unknown ABI: " + toolchain.getAbi());
        }

        String cpu;
        switch (toolchain.getAbi()) {
            case Arm:
                cpu = "armv7a";
                break;
            case Arm64:
                cpu = "armv8a";
                break;
            case X86:
                cpu = "i686";
                break;
            case X86_64:
                cpu = "x86_64";
                break;
            default:
                throw new IllegalStateException("Unknown ABI: " + toolchain.getAbi());
        }

        File crossFile = new File(workingDirectory, "cross_file.txt");
        try {
            StringBuilder contentBuilder = new StringBuilder();
            contentBuilder.append("[binaries]\n");
            contentBuilder.append("ar = '").append(toolchain.getAr()).append("'\n");
            contentBuilder.append("c = '").append(toolchain.getClang()).append("'\n");
            contentBuilder.append("cpp = '").append(toolchain.getClangxx()).append("'\n");
            contentBuilder.append("strip = '").append(toolchain.getStrip()).append("'\n\n");
            contentBuilder.append("[host_machine]\n");
            contentBuilder.append("system = 'android'\n");
            contentBuilder.append("cpu_family = '").append(cpuFamily).append("'\n");
            contentBuilder.append("cpu = '").append(cpu).append("'\n");
            contentBuilder.append("endian = 'little'");

            Files.write(crossFile.toPath(), contentBuilder.toString().getBytes());
        } catch (IOException e) {
            throw new RuntimeException("Failed to write cross file", e);
        }

        List<String> command = new ArrayList<>();
        command.add("meson");
        command.add("--cross-file");
        command.add(crossFile.getAbsolutePath());
        command.add("--buildtype");
        command.add("release");
        command.add("--prefix");
        command.add(installDirectory.getAbsolutePath());
        command.add("--default-library");
        command.add(getDefaultLibraryType().get().getArgument());
        command.add(getSourceDirectory().get().getAsFile().getAbsolutePath());
        command.add(buildDirectory.getAbsolutePath());

        executeSubprocess(command, workingDirectory);
    }

    private void build(File buildDirectory) {
        executeSubprocess(Arrays.asList("ninja", "-v"), buildDirectory);
    }

    private void install(File buildDirectory) {
        executeSubprocess(Arrays.asList("ninja", "-v", "install"), buildDirectory);
    }
}
