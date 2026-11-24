import com.citacomsys.plugin.configureKotlinMultiplatform
import com.citacomsys.plugin.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.jvm.tasks.Jar
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

internal class KmpLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply(libs.findPlugin("androidMultiplatformLibrary").get().get().pluginId)
                apply(libs.findPlugin("kotlinMultiplatform").get().get().pluginId)
                apply(libs.findPlugin("kotlinSerialization").get().get().pluginId)
            }

            val sanitizedPath = path.split(":").filter { it.isNotEmpty() }.joinToString("_") { it.replace("-", "_") }
            group = if (sanitizedPath.isNotEmpty()) "com.citacomsys.$sanitizedPath" else "com.citacomsys"
            version = "1.0.0"

            if (sanitizedPath.isNotEmpty()) {
                tasks.withType<Jar>().configureEach {
                    archiveBaseName.set(sanitizedPath.replace("_", "-"))
                }
            }

            extensions.configure<KotlinMultiplatformExtension> {
                configureKotlinMultiplatform(this)
            }
        }
    }
}
