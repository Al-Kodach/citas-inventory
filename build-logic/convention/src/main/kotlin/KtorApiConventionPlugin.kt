import com.citacomsys.plugin.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

internal class KtorApiConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply(libs.findPlugin("citacomsysKotlinJvmApi").get().get().pluginId)
                apply(libs.findPlugin("ktor").get().get().pluginId)
            }

            dependencies {
                "implementation"(libs.findLibrary("ktor.server.core").get())
                "implementation"(libs.findLibrary("kotlin.logging.jvm").get())
                "implementation"(project(":core-server:domain"))
                "implementation"(project(":core-server:common"))
                "implementation"(project(":core-server:logging"))
                "implementation"(project(":types-jvm"))

                "testImplementation"("io.mockk:mockk:1.14.7")
                "testImplementation"("io.strikt:strikt-core:0.35.1")
            }
        }
    }
}
