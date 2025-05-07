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

import lombok.Value;

import java.io.File;
import java.nio.file.Files;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Represents the version of an NDK installation.
 */
@Value
public class NdkVersion {
    int major;
    int minor;
    int build;
    String qualifier;

    private static final Pattern PKG_REVISION_REGEX = Pattern.compile("^Pkg\\.Revision\\s*=\\s*(\\S+)$");
    private static final Pattern VERSION_REGEX = Pattern.compile("^(\\d+).(\\d+).(\\d+)(?:-(\\S+))?$");

    /**
     * Parse a version string into an NdkVersion object.
     *
     * @param versionString The version string to parse
     * @return A new NdkVersion object
     */
    private static NdkVersion fromString(String versionString) {
        Matcher matcher = VERSION_REGEX.matcher(versionString);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Invalid version string");
        }

        int major = Integer.parseInt(matcher.group(1));
        int minor = Integer.parseInt(matcher.group(2));
        int build = Integer.parseInt(matcher.group(3));
        String qualifier = matcher.group(4);

        return new NdkVersion(major, minor, build, qualifier);
    }

    /**
     * Parse the contents of an NDK source.properties file to extract the version.
     *
     * @param text The contents of source.properties
     * @return A new NdkVersion object
     */
    public static NdkVersion fromSourcePropertiesText(String text) {
        for (String line : text.split("\\r?\\n")) {
            line = line.trim();
            Matcher matcher = PKG_REVISION_REGEX.matcher(line);
            if (matcher.matches()) {
                return fromString(matcher.group(1));
            }
        }
        throw new RuntimeException("Did not find Pkg.Revision in source.properties");
    }

    /**
     * Get the NDK version from an NDK installation directory.
     *
     * @param ndk The NDK installation directory
     * @return A new NdkVersion object
     */
    public static NdkVersion fromNdk(File ndk) {
        try {
            File sourceProperties = new File(ndk, "source.properties");
            String content = new String(Files.readAllBytes(sourceProperties.toPath()));
            return fromSourcePropertiesText(content);
        } catch (Exception e) {
            throw new RuntimeException("Failed to read source.properties", e);
        }
    }
}