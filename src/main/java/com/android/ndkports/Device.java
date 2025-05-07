package com.android.ndkports;

import lombok.Getter;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Getter
public class Device {
    private final String serial;

    @Getter(lazy = true)
    private final List<Abi> abis = initAbis();

    @Getter(lazy = true)
    private final int version = initVersion();

    public Device(String serial) {
        this.serial = serial;
    }

    private List<Abi> initAbis() {
        List<String> abiProps = Arrays.asList(
                "ro.product.cpu.abi",
                "ro.product.cpu.abi2",
                "ro.product.cpu.abilist");

        Set<Abi> abiSet = new HashSet<>();
        for (String abiProp : abiProps) {
            try {
                String propValue = getProp(abiProp).trim();
                for (String abiName : propValue.split(",")) {
                    Abi abi = Abi.fromAbiName(abiName);
                    if (abi != null) {
                        abiSet.add(abi);
                    }
                }
            } catch (Exception e) {
                // Ignore exceptions when getting properties
            }
        }

        return abiSet.stream()
                .sorted(Comparator.comparing(a -> a.abiName))
                .collect(Collectors.toList());
    }

    private int initVersion() {
        try {
            return Integer.parseInt(getProp("ro.build.version.sdk").trim());
        } catch (Exception e) {
            return 0;
        }
    }

    public boolean compatibleWith(Abi abi, int minSdkVersion) {
        return getAbis().contains(abi) && minSdkVersion <= getVersion();
    }

    public String push(File src, File dest) throws IOException, InterruptedException {
        List<String> args = Arrays.asList("push", src.toString(), dest.toString());
        return run(args);
    }

    public String shell(Iterable<String> cmd) throws IOException, InterruptedException {
        List<String> args = new ArrayList<>();
        args.add("shell");
        for (String arg : cmd) {
            args.add(arg);
        }
        return run(args);
    }

    private String getProp(String name) throws IOException, InterruptedException {
        return shell(Arrays.asList("getprop", name));
    }

    private String run(Iterable<String> args) throws IOException, InterruptedException {
        return adb(args, serial);
    }
}
