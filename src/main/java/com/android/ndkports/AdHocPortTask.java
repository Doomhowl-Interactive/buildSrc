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

public abstract class AdHocPortTask extends PortTask {
    @Input
    public abstract Property<Consumer<AdHocBuilder>> getBuilder();

    public void builder(Consumer<AdHocBuilder> block) {
        getBuilder().set(block);
    }

    @Override
    protected void buildForAbi(
            Toolchain toolchain,
            File workingDirectory,
            File buildDirectory,
            File installDirectory
    ) {
        buildDirectory.mkdirs();

        Consumer<AdHocBuilder> builderBlock = getBuilder().get();
        AdHocBuilder builder = new AdHocBuilder(
                getSourceDirectory().get().getAsFile(),
                buildDirectory,
                installDirectory,
                toolchain,
                getPrefabGenerated().get().getAsFile(),
                getNcpus()
        );
        builderBlock.accept(builder);

        for (RunBuilder run : builder.getRuns()) {
            executeSubprocess(
                    run.getCmd(), 
                    getSourceDirectory().get().getAsFile(), 
                    run.getEnv()
            );
        }
    }

    @Getter
    public static class RunBuilder {
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

    @Getter
    @RequiredArgsConstructor
    public static class AdHocBuilder {
        private final File sourceDirectory;
        private final File buildDirectory;
        private final File installDirectory;
        private final Toolchain toolchain;
        private final File sysroot;
        private final int ncpus;
        private final List<RunBuilder> runs = new ArrayList<>();

        public void run(Consumer<RunBuilder> block) {
            RunBuilder runBuilder = new RunBuilder();
            block.accept(runBuilder);
            runs.add(runBuilder);
        }
    }
}