package com.android.ndkports;

import org.gradle.api.provider.Property;

import java.io.File;

/**
 * Extension class for NDK Ports plugin.
 */
public abstract class NdkPortsExtension {
    public abstract Property<File> getSourceTar();

    public abstract Property<GitSourceArgs> getSourceGit();

    public abstract Property<File> getRawSource();

    public abstract Property<Integer> getMinSdkVersion();
}