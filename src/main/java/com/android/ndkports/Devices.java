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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class Devices {


    public static class DeviceFleet {
        @Getter(lazy = true)
        private final List<Device> devices = initDevices();

        private boolean lineHasUsableDevice(String line) {
            if (line == null || line.trim().isEmpty()) {
                return false;
            }
            if (line.equals("List of devices attached")) {
                return false;
            }
            if (line.contains("offline")) {
                return false;
            }
            if (line.contains("unauthorized")) {
                return false;
            }
            if (line.startsWith("* daemon")) {
                return false;
            }
            return true;
        }

        private List<Device> initDevices() {
            try {
                String output = AdbUtils.adb(Arrays.asList("devices"), null);
                List<Device> result = new ArrayList<>();

                for (String line : output.split("\\n")) {
                    if (lineHasUsableDevice(line)) {
                        String serial = line.split("\\s+")[0];
                        result.add(new Device(serial));
                    }
                }

                return result;
            } catch (Exception e) {
                return new ArrayList<>();
            }
        }

        public Device findDeviceFor(Abi abi, int minSdkVersion) {
            for (Device device : getDevices()) {
                if (device.compatibleWith(abi, minSdkVersion)) {
                    return device;
                }
            }
            return null;
        }
    }
}