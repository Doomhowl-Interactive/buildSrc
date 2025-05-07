package com.android.ndkports;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AdbUtils {
    public static String adb(Iterable<String> args, String serial) throws IOException, InterruptedException {
        List<String> adbCmd = new ArrayList<>();
        if (serial == null) {
            adbCmd.add("adb");
        } else {
            adbCmd.addAll(Arrays.asList("adb", "-s", serial));
        }

        for (String arg : args) {
            adbCmd.add(arg);
        }

        Process result = new ProcessBuilder(adbCmd)
                .redirectErrorStream(true)
                .start();

        BufferedReader reader = new BufferedReader(new InputStreamReader(result.getInputStream()));
        StringBuilder output = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            output.append(line).append("\n");
        }

        if (result.waitFor() != 0) {
            throw new AdbException(args, output.toString());
        }

        return output.toString();
    }

}
