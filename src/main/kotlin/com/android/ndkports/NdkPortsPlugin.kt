@file:Suppress("DEPRECATION")

package com.android.ndkports

import org.gradle.api.InvalidUserDataException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.attributes.Attribute
import org.gradle.api.component.SoftwareComponentFactory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.plugins.BasePlugin
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.bundling.Zip
import java.io.File
import java.util.Properties
import javax.inject.Inject

abstract class NdkPortsExtension {
    abstract val sourceTar: Property<File>
    abstract val sourceGit: Property<GitSourceArgs>
    abstract val rawSource: Property<File>
    abstract val minSdkVersion: Property<Int>
}

class NdkPortsPluginImpl(
    private val project: Project,
    private val softwareComponentFactory: SoftwareComponentFactory,
    objects: ObjectFactory,
) {
    private val topBuildDir = project.buildDir.resolve("port")

    private val extension =
        project.extensions.create("ndkPorts", NdkPortsExtension::class.java)

    private var portTaskAdded: Boolean = false
    private val portTask = objects.property(PortTask::class.java)

    private lateinit var ndkPathProp: DirectoryProperty;
    private lateinit var prefabTask: Provider<PrefabTask>
    private lateinit var extractTask: Provider<SourceExtractTask>
    private lateinit var packageTask: Provider<PackageBuilderTask>
    private lateinit var aarTask: Provider<Zip>

    private lateinit var implementation: Configuration
    private lateinit var exportedAars: Configuration
    private lateinit var consumedAars: Configuration

    private val artifactType = Attribute.of("artifactType", String::class.java)

    private fun findNdkPath() {
        val prop = "ndkPath"
        try {

            var ndkPathStr = project.providers.gradleProperty(prop).getOrElse("")
            if (ndkPathStr.isEmpty()) {
                ndkPathStr = project.findProperty(prop) as? String ?: ""
            }
            if (ndkPathStr.isEmpty()) {
                val localProperties = Properties()
                val localPropertiesFile = project.rootProject.file("local.properties")

                if (localPropertiesFile.exists()) {
                    localPropertiesFile.inputStream().use { localProperties.load(it) }
                }

                ndkPathStr = localProperties.getProperty(prop)
            }

            if (ndkPathStr.isEmpty()) {
                throw IllegalStateException("No $prop found in gradle.properties or local.properties!")
            }

            ndkPathProp = project.objects.directoryProperty().apply {
                set(project.layout.projectDirectory.dir(ndkPathStr))
            }
        } catch (e: Exception) {
            throw IllegalArgumentException("No $prop passed to Gradle or set in local.properties!", e)
        }
    }

    private fun createConfigurations() {
        implementation = project.configurations.create("implementation") {
            it.isCanBeResolved = false
            it.isCanBeConsumed = false
        }

        exportedAars = project.configurations.create("exportedAars") {
            it.isCanBeResolved = false
            it.isCanBeConsumed = true
            it.extendsFrom(implementation)
            it.attributes { attributes ->
                with(attributes) {
                    attribute(artifactType, "aar")
                }
            }
        }

        consumedAars = project.configurations.create("consumedAars") {
            it.isCanBeResolved = true
            it.isCanBeConsumed = false
            it.extendsFrom(implementation)
            it.attributes { attributes ->
                with(attributes) {
                    attribute(artifactType, "aar")
                }
            }
        }
    }

    private fun createTasks() {
        prefabTask = project.tasks.register("prefab", PrefabTask::class.java) {
            with(it) {
                aars = consumedAars.incoming.artifacts.artifactFiles
                outputDirectory.set(topBuildDir.resolve("dependencies"))
                ndkPath.set(ndkPathProp)
                minSdkVersion.set(extension.minSdkVersion)
            }
        }

        extractTask = project.tasks.register(
            "extractSrc", SourceExtractTask::class.java
        ) {
            with(it) {
                tarSource.set(extension.sourceTar)
                gitSource.set(extension.sourceGit)
                rawSource.set(extension.rawSource)
                outDir.set(topBuildDir.resolve("src"))
            }
        }

        packageTask = project.tasks.register(
            "prefabPackage", PackageBuilderTask::class.java
        ) {
            if (!portTask.isPresent) {
                throw InvalidUserDataException(
                    "The ndkports plugin was applied but no port task was " +
                            "registered. A task deriving from NdkPortsTask " +
                            "must be registered."
                )
            }
            with(it) {
                sourceDirectory.set(extractTask.get().outDir)
                outDir.set(topBuildDir)
                ndkPath.set(ndkPathProp)
                installDirectory.set(portTask.get().installDir)
                minSdkVersion.set(extension.minSdkVersion)
            }
        }

        aarTask = project.tasks.register("packageAar", Zip::class.java) {
            it.from(packageTask.get().intermediatesDirectory)
            it.archiveExtension.set("aar")
            it.dependsOn(packageTask)
        }

        project.artifacts.add(exportedAars.name, aarTask)

        val portTasks = project.tasks.withType(PortTask::class.java)
        portTasks.whenTaskAdded { portTask ->
            if (portTaskAdded) {
                throw InvalidUserDataException(
                    "Cannot define multiple port tasks for a single module"
                )
            }
            portTaskAdded = true
            this.portTask.set(portTask)

            with(portTask) {
                sourceDirectory.set(extractTask.get().outDir)
                ndkPath.set(ndkPathProp)
                buildDir.set(topBuildDir)
                minSdkVersion.set(extension.minSdkVersion)
                prefabGenerated.set(prefabTask.get().generatedDirectory)
            }
        }

        val testTasks =
            project.tasks.withType(AndroidExecutableTestTask::class.java)
        testTasks.whenTaskAdded { testTask ->
            with(testTask) {
                dependsOn(aarTask)
                minSdkVersion.set(extension.minSdkVersion)
                ndkPath.set(ndkPathProp)
            }
            project.tasks.getByName("check").dependsOn(testTask)
        }
    }

    private fun createComponents() {
        val adhocComponent = softwareComponentFactory.adhoc("prefab")
        project.components.add(adhocComponent)
        adhocComponent.addVariantsFromConfiguration(exportedAars) {
            it.mapToMavenScope("runtime")
        }
    }

    fun apply() {
        project.pluginManager.apply(BasePlugin::class.java)
        findNdkPath()
        createConfigurations()
        createTasks()
        createComponents()
    }
}

@Suppress("UnstableApiUsage", "Unused")
class NdkPortsPlugin @Inject constructor(
    private val objects: ObjectFactory,
    private val softwareComponentFactory: SoftwareComponentFactory,
) : Plugin<Project> {
    override fun apply(project: Project) {
        NdkPortsPluginImpl(project, softwareComponentFactory, objects).apply()
    }
}