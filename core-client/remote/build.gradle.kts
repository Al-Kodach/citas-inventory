import java.util.Properties
import org.gradle.kotlin.dsl.kotlin


plugins {
    alias(libs.plugins.citacomsysKmpLibrary)
//    alias(libs.plugins.buildConfig)
}

//val localProperties = Properties()
//val localPropertiesFile = project.rootProject.file("local.properties")
//if (localPropertiesFile.exists()) {
//    localPropertiesFile.inputStream().use { localProperties.load(it) }
//}

//fun getSecret(key: String, default: String = ""): String =
//    (project.findProperty(key) ?: localProperties.getProperty(key))?.toString() ?: default

//buildConfig {
//    useJavaOutput()
//    packageName.set("com.citacomsys.core.remote")
//    val baseUrl = getSecret("base.url", "http://localhost:8080/")
//    buildConfigField("String", "BASE_URL", "\"$baseUrl\"")
//}

//compose.resources {
//    publicResClass = true
//    packageOfResClass = "com.citacomsys.core.remote"
//}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(project(":core-client:domain"))
            implementation(project(":shared"))
            implementation(project(":kernel:api"))
            implementation(project(":core-client:mesh"))
            implementation(project(":device-service"))
            implementation(project(":core-client:security"))

            api(libs.ktor.client.core)
            api(libs.ktor.client.auth)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(libs.ktor.client.logging)
            implementation(libs.napier)
            implementation(libs.ktor.client.cio)
        }
    }
}
