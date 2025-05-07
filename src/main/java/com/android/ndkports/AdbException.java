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
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public class AdbException extends RuntimeException {
    private final Iterable<String> args;
    private final String output;

    @Getter(lazy = true)
    private final String cmd = formatCmd(args);

    public AdbException(Iterable<String> args, String output) {
        super(formatCmd(args) + ":\n" + output);
        this.args = args;
        this.output = output;
    }

    public static String formatCmd(Iterable<String> args) {
        StringBuilder builder = new StringBuilder();
        boolean first = true;
        for (String arg : args) {
            if (!first) {
                builder.append(" ");
            }
            builder.append(arg);
            first = false;
        }
        return builder.toString();
    }
}

