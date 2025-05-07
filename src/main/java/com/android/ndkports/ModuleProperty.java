package com.android.ndkports;

import lombok.Getter;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;

import javax.inject.Inject;

public abstract class ModuleProperty {
    @Getter
    @Input
    private final String name;

    @Getter
    @Input
    private final Property<Boolean> static_;

    @Getter
    @Input
    private final Property<Boolean> headerOnly;

    @Getter
    @Input
    private final Property<Boolean> includesPerAbi;

    @Getter
    @Input
    private final ListProperty<String> dependencies;

    @Inject
    public ModuleProperty(ObjectFactory objectFactory, String name) {
        this.name = name;
        this.static_ = objectFactory.property(Boolean.class).convention(false);
        this.headerOnly = objectFactory.property(Boolean.class).convention(false);
        this.includesPerAbi = objectFactory.property(Boolean.class).convention(false);
        this.dependencies = objectFactory.listProperty(String.class).convention(java.util.Collections.emptyList());
    }
}