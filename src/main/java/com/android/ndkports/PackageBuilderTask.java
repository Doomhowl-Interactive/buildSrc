package com.android.ndkports;

import lombok.Getter;
import org.gradle.api.DefaultTask;
import org.gradle.api.NamedDomainObjectContainer;
import org.gradle.api.file.Directory;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputDirectory;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.Nested;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction;

import javax.inject.Inject;
import java.util.List;
import java.util.stream.Collectors;

public abstract class PackageBuilderTask extends DefaultTask {

    /**
     * The name of the port. Will be used as the package name in prefab.json.
     */
    @Getter
    @Input
    private final Property<String> packageName;

    /**
     * The version to encode in the prefab.json.
     *
     * This version must be compatible with CMake's `find_package` for
     * config-style packages. This means that it must be one to four decimal
     * separated integers. No other format is allowed.
     *
     * If not set, the default is Project.getVersion as interpreted by
     * CMakeCompatibleVersion.parse.
     *
     * For example, OpenSSL 1.1.1g will set this value to
     * `CMakeCompatibleVersion(1, 1, 1, 7)`.
     */
    @Getter
    @Input
    abstract public Property<CMakeCompatibleVersion> getVersion();

    @Getter
    @Input
    abstract public Property<Integer> getMinSdkVersion();

    @Getter
    @Nested
    abstract public NamedDomainObjectContainer<ModuleProperty> getModules();

    @Getter
    @Input
    private final Property<String> licensePath;

    @Getter
    @Input
    abstract public MapProperty<String, String> getDependencies();

    @Getter
    @InputDirectory
    abstract public DirectoryProperty getSourceDirectory();

    @Getter
    @InputDirectory
    abstract public DirectoryProperty getInstallDirectory();

    @Getter
    @Internal
    abstract public DirectoryProperty getOutDir();

    @Getter
    @InputDirectory
    abstract public DirectoryProperty getNdkPath();

    @Inject
    public PackageBuilderTask(ObjectFactory objectFactory) {
        packageName = objectFactory.property(String.class).convention(getProject().getName());
        licensePath = objectFactory.property(String.class).convention("LICENSE");
    }

    @OutputDirectory
    public Provider<Directory> getIntermediatesDirectory() {
        return getOutDir().dir("aar");
    }

    private Ndk getNdk() {
        return new Ndk(getNdkPath().getAsFile().get());
    }

    @TaskAction
    public void run() {
        List<ModuleDescription> modules = getModules().getAsMap().values().stream()
                .map(it -> new ModuleDescription(
                        it.getName(),
                        it.getStatic_().get(),
                        it.getHeaderOnly().get(),
                        it.getIncludesPerAbi().get(),
                        it.getDependencies().get()))
                .collect(Collectors.toList());

        new PrefabPackageBuilder(
                new PackageData(
                        getPackageName().get(),
                        getProject().getVersion().toString(),
                        getVersion().get(),
                        getMinSdkVersion().get(),
                        getLicensePath().get(),
                        modules,
                        getDependencies().get()),
                getIntermediatesDirectory().get().getAsFile(),
                getInstallDirectory().get().getAsFile(),
                getSourceDirectory().get().getAsFile(),
                getNdk()).build();
    }
}