package com.android.ndkports;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.io.File;

@RequiredArgsConstructor
public class CMakeBuilder extends RunBuilder {
    @Getter
    private final Toolchain toolchain;

    @Getter
    private final File sysroot;
}
