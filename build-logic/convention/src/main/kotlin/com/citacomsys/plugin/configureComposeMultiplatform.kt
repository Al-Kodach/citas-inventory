package com.citacomsys.plugin

import org.gradle.api.Project
import org.gradle.kotlin.dsl.getByType
import org.jetbrains.compose.ComposePlugin
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

internal fun Project.configureComposeMultiplatform(
    extension: KotlinMultiplatformExtension,
) = extension.apply {
    val compose = extensions.getByType<ComposePlugin.Dependencies>()
    sourceSets.apply {
        androidMain {
            dependencies {
                implementation(libs.findLibrary("compose.uiToolingPreview").get())
                implementation(libs.findLibrary("compose.uiTooling").get())
                implementation(libs.findLibrary("androidx.activity.compose").get())
                implementation(libs.findLibrary("koin.androidx.compose").get())
            }
        }

        commonMain {
            dependencies {
                implementation(libs.findLibrary("compose.runtime").get())
                implementation(libs.findLibrary("compose.foundation").get())
                implementation(libs.findLibrary("compose.material3").get())
                implementation(libs.findLibrary("compose.ui").get())
                implementation(libs.findLibrary("compose.components.resources").get())
                implementation(libs.findLibrary("compose.uiToolingPreview").get())
                implementation(libs.findLibrary("androidx.lifecycle.viewmodelCompose").get())
                implementation(libs.findLibrary("androidx.lifecycle.viewmodel").get())
                implementation(libs.findLibrary("androidx.lifecycle.runtimeCompose").get())
            }
        }

        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)
        }
    }
}

