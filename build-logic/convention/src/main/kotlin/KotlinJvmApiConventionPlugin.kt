import com.citacomsys.plugin.configureKotlinJvm
import com.citacomsys.plugin.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.jvm.tasks.Jar
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.withType

internal class KotlinJvmApiConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply(libs.findPlugin("kotlinJvm").get().get().pluginId)
                apply(libs.findPlugin("kotlinSerialization").get().get().pluginId)
            }

            configureKotlinJvm()

            val moduleName = path.split(":").drop(1).joinToString(".")
            group = if (moduleName.isNotEmpty()) "com.citacomsys.inventory.$moduleName" else "com.citacomsys.inventory"
            version = "1.0.0"

            if (moduleName.isNotEmpty()) {
                tasks.withType<Jar>().configureEach {
                    archiveBaseName.set(moduleName.replace(".", "-"))
                }
            }

            dependencies {
                "implementation"(libs.findLibrary("koin.core").get())
                "implementation"(libs.findLibrary("kotlinx.serialization.core").get())
                "implementation"(libs.findLibrary("kotlinx.coroutines.core").get())

                "testImplementation"(libs.findLibrary("kotlin.test").get())
                "testImplementation"(libs.findLibrary("koin.test").get())
            }
        }
    }
}
