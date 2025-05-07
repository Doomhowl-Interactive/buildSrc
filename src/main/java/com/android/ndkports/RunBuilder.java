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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Utility class for building commands to run in subprocesses.
 */
public class RunBuilder {
    @Getter
    private final List<String> cmd = new ArrayList<>();

    @Getter
    private final Map<String, String> env = new HashMap<>();

    public void arg(String arg) {
        cmd.add(arg);
    }

    public void args(String... args) {
        for (String arg : args) {
            cmd.add(arg);
        }
    }

    public void env(String key, String value) {
        env.put(key, value);
    }
}