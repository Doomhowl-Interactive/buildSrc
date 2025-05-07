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
import lombok.RequiredArgsConstructor;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public abstract class AutoconfPortTask extends PortTask {

    @Input
    public abstract Property<Consumer<AutoconfBuilder>> getAutoconf();

    public void autoconf(Consumer<AutoconfBuilder> block) {
        getAutoconf().set(block);
    }

    @Override
    protected void buildForAbi(
            Toolchain toolchain,
            File workingDirectory,
            File buildDirectory,
            File installDirectory) {
        buildDirectory.mkdirs();

        Consumer<AutoconfBuilder> autoconfBlock = getAutoconf().get();
        AutoconfBuilder builder = new AutoconfBuilder(
                toolchain,
                getPrefabGenerated().get().getAsFile().resolve(toolchain.getAbi().getTriple()));
        autoconfBlock.accept(builder);

        List<String> configureCmd = new ArrayList<>();
        configureCmd.add(getSourceDirectory().get().getAsFile().getAbsolutePath() + "/configure");
        configureCmd.add("--host=" + toolchain.getBinutilsTriple());
        configureCmd.add("--prefix=" + installDirectory.getAbsolutePath());
        configureCmd.addAll(builder.getCmd());

        Map<String, String> env = new HashMap<>();
        env.put("AR", toolchain.getAr().getAbsolutePath());
        env.put("CC", toolchain.getClang().getAbsolutePath());
        env.put("CXX", toolchain.getClangxx().getAbsolutePath());
        env.put("RANLIB", toolchain.getRanlib().getAbsolutePath());
        env.put("STRIP", toolchain.getStrip().getAbsolutePath());
        env.put("PATH", toolchain.getBinDir() + ":" + System.getenv("PATH"));
        env.putAll(builder.getEnv());

        executeSubprocess(configureCmd, buildDirectory, env);
        executeSubprocess(Arrays.asList("make", "-j" + getNcpus()), buildDirectory);
        executeSubprocess(Arrays.asList("make", "-j" + getNcpus(), "install"), buildDirectory);
    }

    @Getter
    @RequiredArgsConstructor
    public static class AutoconfBuilder {
        private final Toolchain toolchain;
        private final File sysroot;
        private final List<String> cmd = new ArrayList<>();
        private final Map<String, String> env = new HashMap<>();

        public boolean arg(String arg) {
            return cmd.add(arg);
        }

        public void args(String... args) {
            cmd.addAll(Arrays.asList(args));
        }

        public void env(String key, String value) {
            env.put(key, value);
        }
    }
}