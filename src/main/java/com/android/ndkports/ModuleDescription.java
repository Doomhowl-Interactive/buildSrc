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

import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * A module exported by the package.
 *
 * As currently implemented by ndkports, one module is exactly one library.
 * Prefab supports header-only libraries, but ndkports does not support these
 * yet.
 *
 * Static libraries are not currently supported by ndkports.
 *
 * @property name The name of the module. Note that currently the name of the
 *           installed library file must be exactly `lib$name.so`.
 * @property includesPerAbi Set to true if a different set of headers should be
 *           exposed per-ABI. Not currently implemented.
 * @property dependencies A list of other modules required by this module, in
 *           the format described by https://google.github.io/prefab/.
 */
@Data
@AllArgsConstructor
public class ModuleDescription implements Serializable {
    private String name;
    private boolean static_;
    private boolean headerOnly;
    private boolean includesPerAbi;
    private List<String> dependencies;
}