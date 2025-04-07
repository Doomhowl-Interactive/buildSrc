val kotlinVersion = "1.9.20"

plugins {
    id("org.jetbrains.kotlin.jvm") version "1.9.20"
    id("com.google.cloud.artifactregistry.gradle-plugin") version "2.2.0"
    id("java-gradle-plugin")
    id("maven-publish")
}

group = "com.android.ndkports"
version = "1.0.1-SNAPSHOT"

tasks {
    compileKotlin {
    }
}

repositories {
    mavenCentral()
    google()
    maven {
        url = uri("artifactregistry://europe-west4-maven.pkg.dev/doomhowl-interactive/ndkports")
    }
}

dependencies {
    implementation(kotlin("stdlib", kotlinVersion))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.3")

    implementation("com.google.prefab:api:1.1.2")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.3.3")
    implementation("org.redundent:kotlin-xml-builder:1.6.1")
    implementation("com.github.sya-ri:kgit:1.1.0")

    testImplementation(kotlin("test", kotlinVersion))
    testImplementation(kotlin("test-junit", kotlinVersion))
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.7.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.7.0")
}

gradlePlugin {
    plugins {
        create("ndkports") {
            id = "com.android.ndkports.NdkPorts"
            implementationClass = "com.android.ndkports.NdkPortsPlugin"
        }
    }
}

publishing {
    repositories {
        maven {
            url = uri("${project.rootDir}/build/docs")
        }
        maven {
            url = uri("artifactregistry://europe-west4-maven.pkg.dev/doomhowl-interactive/ndkports")
        }
    }
}
