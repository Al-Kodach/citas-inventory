import com.citacomsys.plugin.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.internal.Actions
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

internal class KmpFeatureConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            Actions.with(pluginManager) {
                apply(libs.findPlugin("citacomsysKmpLibrary").get().get().pluginId)
                apply(libs.findPlugin("citacomsysComposeMultiplatform").get().get().pluginId)
            }

            extensions.configure<KotlinMultiplatformExtension> {
                sourceSets.apply {
                    all {
                        languageSettings.optIn("kotlin.test.EmbeddedTestCompiler")
                    }

                    commonMain.dependencies {
                        implementation(project(":shared"))
                        implementation(project(":core-client:design"))
                        implementation(project(":core-client:sync"))
                        implementation(project(":core-client:ui"))
                        implementation(project(":core-client:notification"))
                        implementation(project(":core-client:remote"))
                        implementation(project(":core-client:spi"))
                        implementation(libs.findLibrary("koin.compose").get())
                        implementation(libs.findLibrary("koin.compose.viewmodel").get())
                        implementation(libs.findLibrary("koin.compose.navigation").get())
                        implementation(libs.findLibrary("navigation.compose").get())
                        implementation(libs.findLibrary("kotlinx.serialization.json").get())
                        implementation(libs.findLibrary("androidx.nav3.ui").get())
                    }

                    commonTest.dependencies {
                        implementation(project(":test-suite:client"))
                    }
                }
            }
        }
    }
}
