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

public class CMakeCompatibleVersionTest {
    @Test
    public void canParseVersionNumber() {
        assertEquals(
            new CMakeCompatibleVersion(1, null, null, null),
            CMakeCompatibleVersion.parse("1")
        );
        assertEquals(
            new CMakeCompatibleVersion(2, 1, null, null),
            CMakeCompatibleVersion.parse("2.1")
        );
        assertEquals(
            new CMakeCompatibleVersion(3, 2, 1, null),
            CMakeCompatibleVersion.parse("3.2.1")
        );
        assertEquals(
            new CMakeCompatibleVersion(4, 3, 2, 1),
            CMakeCompatibleVersion.parse("4.3.2.1")
        );
    }

    @Test
    public void rejectInvalidVersions() {
        assertThrows(IllegalArgumentException.class, () -> {
            CMakeCompatibleVersion.parse("");
        });
        assertThrows(IllegalArgumentException.class, () -> {
            CMakeCompatibleVersion.parse(" ");
        });
        assertThrows(IllegalArgumentException.class, () -> {
            CMakeCompatibleVersion.parse("1.");
        });
        assertThrows(IllegalArgumentException.class, () -> {
            CMakeCompatibleVersion.parse(".1");
        });
        assertThrows(IllegalArgumentException.class, () -> {
            CMakeCompatibleVersion.parse(".1.");
        });
        assertThrows(IllegalArgumentException.class, () -> {
            CMakeCompatibleVersion.parse(" 1");
        });
        assertThrows(IllegalArgumentException.class, () -> {
            CMakeCompatibleVersion.parse("1 ");
        });
        assertThrows(IllegalArgumentException.class, () -> {
            CMakeCompatibleVersion.parse(" 1 ");
        });
        assertThrows(IllegalArgumentException.class, () -> {
            CMakeCompatibleVersion.parse("2.1.");
        });
        assertThrows(IllegalArgumentException.class, () -> {
            CMakeCompatibleVersion.parse(".2.1");
        });
        assertThrows(IllegalArgumentException.class, () -> {
            CMakeCompatibleVersion.parse(".2.1.");
        });
        assertThrows(IllegalArgumentException.class, () -> {
            CMakeCompatibleVersion.parse("1a");
        });
        assertThrows(IllegalArgumentException.class, () -> {
            CMakeCompatibleVersion.parse("2b.1");
        });
        assertThrows(IllegalArgumentException.class, () -> {
            CMakeCompatibleVersion.parse("5.4.3.2.1");
        });
        assertThrows(IllegalArgumentException.class, () -> {
            CMakeCompatibleVersion.parse("4.3.2.1a");
        });
        assertThrows(IllegalArgumentException.class, () -> {
            CMakeCompatibleVersion.parse("2.a.1");
        });
        assertThrows(IllegalArgumentException.class, () -> {
            CMakeCompatibleVersion.parse("3. .1");
        });
        assertThrows(IllegalArgumentException.class, () -> {
            CMakeCompatibleVersion.parse("1..2");
        });
        assertThrows(IllegalArgumentException.class, () -> {
            CMakeCompatibleVersion.parse(".");
        });
        assertThrows(IllegalArgumentException.class, () -> {
            CMakeCompatibleVersion.parse("...");
        });
    }

    @Test
    public void constructorRequiresThatNullsComeLast() {
        assertThrows(IllegalArgumentException.class, () -> {
            new CMakeCompatibleVersion(1, 2, null, 3);
        });
        assertThrows(IllegalArgumentException.class, () -> {
            new CMakeCompatibleVersion(1, null, 2, 3);
        });
        assertThrows(IllegalArgumentException.class, () -> {
            new CMakeCompatibleVersion(1, null, 2, null);
        });
        assertThrows(IllegalArgumentException.class, () -> {
            new CMakeCompatibleVersion(1, null, null, 2);
        });
    }
}