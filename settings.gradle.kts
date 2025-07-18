rootProject.name = "thomas-cache"

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

dependencyResolutionManagement {
    pluginManagement {
        repositories {
            gradlePluginPortal()
            mavenCentral()
            mavenLocal()
            maven(url = "https://plugins.gradle.org/m2/")
            google()
        }
    }
}
include("cache-handler")
include("cache-handler-caffeine")