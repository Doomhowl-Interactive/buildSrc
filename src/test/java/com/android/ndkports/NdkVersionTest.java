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

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class NdkVersionTest {
    @Test
    public void canParseSourceProperties() {
        assertEquals(
            new NdkVersion(20, 0, 5594570, null),
            NdkVersion.fromSourcePropertiesText(
                "Pkg.Desc = Android NDK\n" +
                "Pkg.Revision = 20.0.5594570"
            )
        );

        assertEquals(
            new NdkVersion(20, 0, 5594570, "canary"),
            NdkVersion.fromSourcePropertiesText(
                "Pkg.Revision = 20.0.5594570-canary\n" +
                "Pkg.Desc = Android NDK"
            )
        );

        assertEquals(
            new NdkVersion(20, 0, 5594570, "beta2"),
            NdkVersion.fromSourcePropertiesText(
                "\n" +
                "    Pkg.Revision     =     20.0.5594570-beta2    \n" +
                "Pkg.Desc = Android NDK\n"
            )
        );
        
        assertEquals(
            new NdkVersion(20, 0, 5594570, "rc1"),
            NdkVersion.fromSourcePropertiesText(
                "Pkg.Desc = Android NDK\n" +
                "\n" +
                "\n" +
                "Pkg.Revision = 20.0.5594570-rc1"
            )
        );
    }

    @Test
    public void failsIfNotFound() {
        assertThrows(RuntimeException.class, () -> {
            NdkVersion.fromSourcePropertiesText("Pkg.Desc = Android NDK");
        });
    }
}