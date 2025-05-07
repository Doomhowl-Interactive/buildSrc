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
import java.io.File;

public class Toolchain {
    @Getter
    public final Ndk ndk;
    @Getter
    public final Abi abi;
    @Getter
    public final int api;
    @Getter
    public final String binutilsTriple;
    @Getter
    public final File sysrootLibs;
    @Getter
    public final File binDir;
    @Getter
    public final File ar;
    @Getter
    public final File clang;
    @Getter
    public final File clangxx;
    @Getter
    public final File nm;
    @Getter
    public final File objdump;
    @Getter
    public final File ranlib;
    @Getter
    public final File readelf;
    @Getter
    public final File strip;

    public Toolchain(Ndk ndk, Abi abi, int api) {
        this.ndk = ndk;
        this.abi = abi;
        this.api = api;

        switch (abi) {
            case Arm:
                binutilsTriple = "arm-linux-androideabi";
                break;
            case Arm64:
                binutilsTriple = "aarch64-linux-android";
                break;
            case X86:
                binutilsTriple = "i686-linux-android";
                break;
            case X86_64:
                binutilsTriple = "x86_64-linux-android";
                break;
            default:
                throw new IllegalArgumentException("Unknown ABI: " + abi);
        }

        String clangTriple;
        if (abi == Abi.Arm) {
            clangTriple = "armv7a-linux-androideabi" + api;
        } else {
            clangTriple = binutilsTriple + api;
        }

        sysrootLibs = new File(ndk.getSysrootDirectory(), "usr/lib/" + binutilsTriple);
        binDir = ndk.getToolchainBinDirectory();
        ar = new File(binDir, "llvm-ar");
        clang = new File(binDir, clangTriple + "-clang");
        clangxx = new File(binDir, clangTriple + "-clang++");
        nm = new File(binDir, "llvm-nm");
        objdump = new File(binDir, "llvm-objdump");
        ranlib = new File(binDir, "llvm-ranlib");
        readelf = new File(binDir, "llvm-readelf");
        strip = new File(binDir, "llvm-strip");
    }
}