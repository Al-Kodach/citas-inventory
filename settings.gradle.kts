rootProject.name = "CitasInventorySaaS"
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    includeBuild("build-logic")
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        mavenLocal()
        google()
    }
}


plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

// Apps
include(
    "admin:api",
    "admin:android",
    "admin:desktop",
    "admin:server",
    "admin:uiShared",

    "pharmacy-product-ext:api",
    "pharmacy-product-ext:server",
    "pharmacy-product-ext:client",

    "retail-product-ext:api",
    "retail-product-ext:server",
    "retail-product-ext:client",

    ":server",
    "devApp:uiShared",
    "devApp:desktopApp",
    "devApp:androidApp",

    ":app:shared",
    ":app:desktop",
    ":app:android",
)

// Client core
include(
    ":core-client:domain",
    "core-client:data",
    "core-client:design",
    "core-client:ui",
    "core-client:notification",
    "core-client:sync",
    "core-client:database",
    "core-client:security",
    "core-client:spi",
    "core-client:remote",
    "core-client:mesh",
    ":core-client:barcode",
)

// Server core
include(
    "core-server:domain",
    "core-server:persistence",
    "core-server:security",
    "core-server:notification",
    "core-server:storage",
    "core-server:data",
    "core-server:logging",
    "core-server:common",
)


// Features
include(
    "feature:user:api",
    "feature:user:client",
    "feature:user:server",

    "feature:auth:api",
    "feature:auth:client",
    "feature:auth:server",

    "feature:platform:api",
    "feature:platform:client",
    "feature:platform:server",

    "feature:settings:api",
    "feature:settings:client",
    "feature:settings:server",

    "feature:home:api",
    "feature:home:client",
    "feature:home:server",

    "feature:backup:api",
    "feature:backup:client",
    "feature:backup:server",

    "feature:billing:api",
    "feature:billing:client",
    "feature:billing:server",

    "feature:organization:api",
    "feature:organization:client",
    "feature:organization:server",

    "feature:workspace:api",
    "feature:workspace:client",
    "feature:workspace:server",

    "feature:support:api",
    "feature:support:client",
    "feature:support:server",

    "feature:platform:api",
    "feature:platform:client",
    "feature:platform:server",

    "feature:telemetry:api",
    "feature:telemetry:client",
    "feature:telemetry:server",

    "feature:branding:api",
    "feature:branding:impl",
    "feature:branding:shared",

    "feature:customer:api",
    "feature:customer:client",
    "feature:customer:server",
)

// Module
include(
    "module:inventory:api",
    "module:inventory:client",
    "module:inventory:server",

    "module:sales:api",
    "module:sales:client",
    "module:sales:server",

    "module:device:api",
    "module:device:client",
    "module:device:server",
)

// Shared
include(
    ":types",
    ":types-jvm",
    ":device-service",
    ":shared",
    ":client-support",
)

// Kernel
include(
    "kernel:api",
    "kernel:client",
    "kernel:server",
)

include(
    ":test-suite:common",
    ":test-suite:client",
    ":test-suite:server",
)
include(":core-client:printer")
