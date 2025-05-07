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
import java.util.Arrays;
import java.util.Collections;

@Getter
@RequiredArgsConstructor
public enum Abi {
    Arm("arm", "armeabi-v7a", "arm-linux-androideabi", 21),
    Arm64("arm64", "arm64-v8a", "aarch64-linux-android", 21),
    X86("x86", "x86", "i686-linux-android", 21),
    X86_64("x86_64", "x86_64", "x86_64-linux-android", 21);

    private final String archName;
    private final String abiName;
    private final String triple;
    private final int minSupportedVersion;

    public int adjustMinSdkVersion(int minSdkVersion) {
        return Collections.max(Arrays.asList(minSdkVersion, minSupportedVersion));
    }

    public static Abi fromAbiName(String name) {
        return Arrays.stream(values())
                .filter(abi -> abi.getAbiName().equals(name))
                .findFirst()
                .orElse(null);
    }
}