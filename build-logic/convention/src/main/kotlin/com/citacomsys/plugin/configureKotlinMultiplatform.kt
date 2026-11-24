package com.citacomsys.plugin

import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

@OptIn(ExperimentalKotlinGradlePluginApi::class)
internal fun Project.configureKotlinMultiplatform(
    extension: KotlinMultiplatformExtension
) {
    val sanitizedPath = path.split(":").filter { it.isNotEmpty() }.joinToString("_") { it.replace("-", "_") }
    val namespaceName = if (sanitizedPath.isNotEmpty()) "com.citacomsys.$sanitizedPath" else "com.citacomsys"

    extension.apply {
        // GLOBAL COMPILER OPTIONS FOR THE ENTIRE EXTENSION (All targets and source sets)
        compilerOptions {
            optIn.addAll(
                "kotlin.time.ExperimentalTime",
                "kotlinx.coroutines.ExperimentalCoroutinesApi",
                "kotlinx.coroutines.FlowPreview",
                "androidx.compose.material3.ExperimentalMaterial3Api"
            )
        }

        // Configure the modern AGP 9.2+ KMP Android target
        try {
            (this as ExtensionAware).extensions.configure<KotlinMultiplatformAndroidLibraryTarget>("android") {
                namespace = namespaceName
                compileSdk = 37
                minSdk = 26

                compilerOptions {
                    jvmTarget.set(JvmTarget.JVM_11)
                }

                androidResources {
                    enable = true
                }

                withHostTest {}
            }
        } catch (_: Exception) {
            // Fallback or ignore if "android" target is not applied
        }

        jvm {
            compilerOptions {
                jvmTarget.set(JvmTarget.JVM_17)
            }
        }

        js(IR) {
            browser()
        }

        @OptIn(ExperimentalWasmDsl::class)
        wasmJs {
            browser()
        }

        applyDefaultHierarchyTemplate {
            common {
                group("web") {
                    withJs()
                    withWasmJs()
                }
            }
        }

        sourceSets.apply {
            val webMain = getByName("webMain")

            androidMain.dependencies {
                implementation(libs.findLibrary("koin.android").get())
            }

            commonMain.dependencies {
                implementation(project(":types"))
                implementation(libs.findLibrary("koin.core").get())
                implementation(libs.findLibrary("kotlinx.coroutines.core").get())
                implementation(libs.findLibrary("kotlinx.serialization.core").get())
                implementation(libs.findLibrary("kotlinx.datetime").get())
                api(libs.findLibrary("napier").get())
                implementation(libs.findLibrary("androidx.nav3.ui").get())
            }

            jvmMain.dependencies {
                implementation(libs.findLibrary("kotlinx.coroutinesSwing").get())
                implementation("org.slf4j:slf4j-simple:2.0.12")
            }

            webMain.dependencies {  }

            commonTest.dependencies {
                implementation(libs.findLibrary("kotlin.test").get())
            }
        }
    }
}
