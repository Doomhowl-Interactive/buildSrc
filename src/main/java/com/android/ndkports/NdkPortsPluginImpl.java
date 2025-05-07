package com.android.ndkports;

import org.gradle.api.InvalidUserDataException;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.attributes.Attribute;
import org.gradle.api.component.SoftwareComponentFactory;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.bundling.Zip;

import java.io.File;
import java.util.Properties;

/**
 * Implementation class for the NDK Ports plugin.
 */
class NdkPortsPluginImpl {
    private final Project project;
    private final SoftwareComponentFactory softwareComponentFactory;
    private final ObjectFactory objects;
    private final File topBuildDir;

    private final NdkPortsExtension extension;

    private boolean portTaskAdded = false;
    private final Property<PortTask> portTask;

    private DirectoryProperty ndkPathProp;
    private Provider<PrefabTask> prefabTask;
    private Provider<SourceExtractTask> extractTask;
    private Provider<PackageBuilderTask> packageTask;
    private Provider<Zip> aarTask;

    private Configuration implementation;
    private Configuration exportedAars;
    private Configuration consumedAars;

    private final Attribute<String> artifactType = Attribute.of("artifactType", String.class);

    public NdkPortsPluginImpl(
            Project project,
            SoftwareComponentFactory softwareComponentFactory,
            ObjectFactory objects) {
        this.project = project;
        this.softwareComponentFactory = softwareComponentFactory;
        this.objects = objects;
        this.topBuildDir = new File(project.getBuildDir(), "port");
        this.extension = project.getExtensions().create("ndkPorts", NdkPortsExtension.class);
        this.portTask = objects.property(PortTask.class);
    }

    /**
     * Find the NDK path from various sources: gradle properties, project
     * properties,
     * or local.properties file.
     */
    private void findNdkPath() {
        final String prop = "ndkPath";
        try {
            String ndkPathStr = project.getProviders().gradleProperty(prop).getOrElse("");
            if (ndkPathStr.isEmpty()) {
                Object propValue = project.findProperty(prop);
                ndkPathStr = propValue != null ? propValue.toString() : "";
            }
            if (ndkPathStr.isEmpty()) {
                Properties localProperties = new Properties();
                File localPropertiesFile = project.getRootProject().file("local.properties");

                if (localPropertiesFile.exists()) {
                    localProperties.load(localPropertiesFile.inputStream());
                }

                ndkPathStr = localProperties.getProperty(prop, "");
            }

            if (ndkPathStr.isEmpty()) {
                throw new IllegalStateException("No " + prop + " found in gradle.properties or local.properties!");
            }

            ndkPathProp = project.getObjects().directoryProperty();
            ndkPathProp.set(project.getLayout().getProjectDirectory().dir(ndkPathStr));
        } catch (Exception e) {
            throw new IllegalArgumentException("No " + prop + " passed to Gradle or set in local.properties!", e);
        }
    }

    /**
     * Create the necessary Gradle configurations.
     */
    private void createConfigurations() {
        implementation = project.getConfigurations().create("implementation", config -> {
            config.setCanBeResolved(false);
            config.setCanBeConsumed(false);
        });

        exportedAars = project.getConfigurations().create("exportedAars", config -> {
            config.setCanBeResolved(false);
            config.setCanBeConsumed(true);
            config.extendsFrom(implementation);
            config.getAttributes().attribute(artifactType, "aar");
        });

        consumedAars = project.getConfigurations().create("consumedAars", config -> {
            config.setCanBeResolved(true);
            config.setCanBeConsumed(false);
            config.extendsFrom(implementation);
            config.getAttributes().attribute(artifactType, "aar");
        });
    }

    /**
     * Create all necessary tasks for the plugin.
     */
    private void createTasks() {
        prefabTask = project.getTasks().register("prefab", PrefabTask.class, task -> {
            task.setAars(consumedAars.getIncoming().getArtifacts().getArtifactFiles());
            task.getOutputDirectory().set(new File(topBuildDir, "dependencies"));
            task.getNdkPath().set(ndkPathProp);
            task.getMinSdkVersion().set(extension.getMinSdkVersion());
        });

        extractTask = project.getTasks().register("extractSrc", SourceExtractTask.class, task -> {
            task.getTarSource().set(extension.getSourceTar());
            task.getGitSource().set(extension.getSourceGit());
            task.getRawSource().set(extension.getRawSource());
            task.getOutDir().set(new File(topBuildDir, "src"));
        });

        packageTask = project.getTasks().register("prefabPackage", PackageBuilderTask.class, task -> {
            if (!portTask.isPresent()) {
                throw new InvalidUserDataException(
                        "The ndkports plugin was applied but no port task was " +
                                "registered. A task deriving from NdkPortsTask " +
                                "must be registered.");
            }
            task.getSourceDirectory().set(extractTask.get().getOutDir());
            task.getOutDir().set(topBuildDir);
            task.getNdkPath().set(ndkPathProp);
            task.getInstallDirectory().set(portTask.get().getInstallDir());
            task.getMinSdkVersion().set(extension.getMinSdkVersion());
        });

        aarTask = project.getTasks().register("packageAar", Zip.class, task -> {
            task.from(packageTask.get().getIntermediatesDirectory());
            task.getArchiveExtension().set("aar");
            task.dependsOn(packageTask);
        });

        project.getArtifacts().add(exportedAars.getName(), aarTask);

        project.getTasks().withType(PortTask.class).configureEach(portTask -> {
            if (portTaskAdded) {
                throw new InvalidUserDataException(
                        "Cannot define multiple port tasks for a single module");
            }
            portTaskAdded = true;
            this.portTask.set(portTask);

            portTask.getSourceDirectory().set(extractTask.get().getOutDir());
            portTask.getNdkPath().set(ndkPathProp);
            portTask.getBuildDir().set(topBuildDir);
            portTask.getMinSdkVersion().set(extension.getMinSdkVersion());
            portTask.getPrefabGenerated().set(prefabTask.get().getGeneratedDirectory());
        });

        project.getTasks().withType(AndroidExecutableTestTask.class).configureEach(testTask -> {
            testTask.dependsOn(aarTask);
            testTask.getMinSdkVersion().set(extension.getMinSdkVersion());
            testTask.getNdkPath().set(ndkPathProp);

            project.getTasks().named("check").configure(checkTask -> checkTask.dependsOn(testTask));
        });
    }

    /**
     * Create the software components for publishing.
     */
    private void createComponents() {
        org.gradle.api.component.AdhocComponentWithVariants adhocComponent = softwareComponentFactory.adhoc("prefab");
        project.getComponents().add(adhocComponent);
        adhocComponent.addVariantsFromConfiguration(exportedAars,
                variantDetails -> variantDetails.mapToMavenScope("runtime"));
    }

    /**
     * Apply the plugin by configuring everything needed.
     */
    public void apply() {
        project.getPluginManager().apply(org.gradle.api.plugins.BasePlugin.class);
        findNdkPath();
        createConfigurations();
        createTasks();
        createComponents();
    }
}