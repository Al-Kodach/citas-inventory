import com.citacomsys.plugin.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

internal class KtorFeatureConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply(libs.findPlugin("citacomsysKtorApi").get().get().pluginId)
                apply(libs.findPlugin("shadow").get().get().pluginId)
            }

            dependencies {
                "implementation"(libs.findLibrary("koin.ktor").get())
                "implementation"(project(":feature:telemetry:api"))
                "implementation"(project(":types-jvm"))
                "implementation"(project(":shared"))
                "implementation"(project(":core-server:domain"))
                "implementation"(project(":core-server:security"))

                "implementation"(libs.findLibrary("ktor.server.core").get())
                "implementation"(libs.findLibrary("ktor.server.netty").get())
                "implementation"(libs.findLibrary("ktor.server.config.yaml").get())
                "implementation"(libs.findLibrary("ktor.server.auth").get())
                "implementation"(libs.findLibrary("ktor.server.validation").get())
                "implementation"(libs.findLibrary("ktor.server.content.negotiation").get())
                "implementation"(libs.findLibrary("ktor.serialization.kotlinx.json").get())
                "implementation"(libs.findLibrary("kotlinx.serialization.json").get())

                "testImplementation"(project(":test-suite:server"))
            }
        }
    }
}
