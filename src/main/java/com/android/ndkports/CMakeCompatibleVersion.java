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
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A version number that is compatible with CMake's package version format.
 *
 * https://cmake.org/cmake/help/latest/manual/cmake-packages.7.html#package-version-file
 *
 * CMake package versions *must* be numeric with a maximum of four dot separated
 * components.
 */
@Getter
@EqualsAndHashCode
public class CMakeCompatibleVersion implements Serializable {
    private final int major;
    private final Integer minor;
    private final Integer patch;
    private final Integer tweak;

    public CMakeCompatibleVersion(int major, Integer minor, Integer patch, Integer tweak) {
        this.major = major;
        this.minor = minor;
        this.patch = patch;
        this.tweak = tweak;

        if (tweak != null) {
            if (patch == null) {
                throw new IllegalArgumentException("patch must not be null if tweak is specified");
            }
        }

        if (patch != null) {
            if (minor == null) {
                throw new IllegalArgumentException("minor must not be null if patch is specified");
            }
        }
    }

    @Override
    public String toString() {
        List<String> components = new ArrayList<>();
        components.add(String.valueOf(major));

        if (minor != null) {
            components.add(String.valueOf(minor));

            if (patch != null) {
                components.add(String.valueOf(patch));

                if (tweak != null) {
                    components.add(String.valueOf(tweak));
                }
            }
        }

        return String.join(".", components);
    }

    private static final Pattern VERSION_REGEX = Pattern.compile(
            "^(\\d+)(?:\\.(\\d+)(?:\\.(\\d+)(?:\\.(\\d+))?)?)?$");

    public static CMakeCompatibleVersion parse(String versionString) {
        Matcher matcher = VERSION_REGEX.matcher(versionString);
        if (!matcher.matches()) {
            throw new IllegalArgumentException(
                    versionString + " is not in major[.minor[.patch[.tweak]]] format");
        }

        return new CMakeCompatibleVersion(
                Integer.parseInt(matcher.group(1)),
                matcher.group(2) != null ? Integer.parseInt(matcher.group(2)) : null,
                matcher.group(3) != null ? Integer.parseInt(matcher.group(3)) : null,
                matcher.group(4) != null ? Integer.parseInt(matcher.group(4)) : null);
    }
}