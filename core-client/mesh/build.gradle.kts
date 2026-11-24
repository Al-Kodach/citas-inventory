@file:OptIn(ExperimentalKotlinGradlePluginApi::class)

import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi

plugins {
    alias(libs.plugins.citacomsysKmpLibrary)
}

kotlin {
    sourceSets {
        val jvmAndroidMain = maybeCreate("jvmAndroidMain").apply {
            dependsOn(commonMain.get())
            kotlin.srcDir("src/jvmAndroidMain/kotlin")
            dependencies {
                implementation(libs.jmdns)
                implementation(libs.ktor.server.netty)
                implementation(libs.ktor.server.calllogging)
                implementation(libs.ktor.client.cio)
                implementation("io.ktor:ktor-network-tls-certificates:${libs.versions.ktor.get()}")
            }
        }

        androidMain.get().dependsOn(jvmAndroidMain)
        jvmMain.get().dependsOn(jvmAndroidMain)

        commonMain.dependencies {
            implementation(project(":core-client:domain"))
            implementation(project(":core-client:security"))
            implementation(project(":core-client:database"))
            implementation(project(":core-client:notification"))
            implementation(project(":device-service"))
            implementation(project(":kernel:api"))
            implementation(project(":shared"))

            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.datetime)
            implementation(libs.napier)
            implementation(libs.koin.core)
            implementation(libs.koin.ktor)

            // Ktor Client
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(libs.ktor.client.logging)

            // Ktor Server Core & Plugins (Multiplatform)
            api(libs.ktor.server.core)
            api(libs.ktor.server.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(libs.ktor.server.auth)
            implementation(libs.ktor.server.sse)
            implementation(libs.ktor.server.status.pages)
            implementation(libs.ktor.server.doublereceive)
        }

        jvmTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutinesTest)
            implementation("io.ktor:ktor-client-mock:${libs.versions.ktor.get()}")
            implementation(libs.ktor.server.test.host)
            implementation(libs.mockk)
        }
    }
}
