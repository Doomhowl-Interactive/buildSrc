package com.android.ndkports;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.component.SoftwareComponentFactory;
import org.gradle.api.model.ObjectFactory;

import javax.inject.Inject;

/**
 * Main plugin class that applies NDK Ports functionality.
 */
@SuppressWarnings({ "UnstableApiUsage", "unused" })
public class NdkPortsPlugin implements Plugin<Project> {
    private final ObjectFactory objects;
    private final SoftwareComponentFactory softwareComponentFactory;

    @Inject
    public NdkPortsPlugin(
            ObjectFactory objects,
            SoftwareComponentFactory softwareComponentFactory) {
        this.objects = objects;
        this.softwareComponentFactory = softwareComponentFactory;
    }

    @Override
    public void apply(Project project) {
        new NdkPortsPluginImpl(project, softwareComponentFactory, objects).apply();
    }
}