package com.android.ndkports;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import lombok.Getter;

public class DeviceFleet {
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
            String output = adb(Arrays.asList("devices"), null);
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