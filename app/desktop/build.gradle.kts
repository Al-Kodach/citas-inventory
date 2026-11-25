import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.composeHotReload)
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.kotlinSerialization)
}

dependencies {
    implementation(project(":app:shared"))
    implementation(project(":core-client:notification"))
    implementation(project(":pharmacy-product-ext:client"))
    implementation(project(":retail-product-ext:client"))
    implementation(project(":module:inventory:client"))
    implementation(project(":module:sales:client"))

    implementation(project(":types"))
    implementation(libs.compose.runtime)
    implementation(libs.compose.ui)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
    implementation(libs.compose.components.resources)
    implementation(libs.compose.uiToolingPreview)
    implementation(libs.androidx.lifecycle.viewmodelCompose)
    implementation(libs.androidx.lifecycle.viewmodel)
    implementation(libs.jetbrains.lifecycle.viewmodelNavigation3)
    implementation(libs.androidx.lifecycle.runtimeCompose)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.androidx.nav3.ui)
    implementation(libs.jetbrains.material3.adaptiveNavigation3)
    implementation(compose.desktop.currentOs)
    implementation(libs.koin.compose)
    implementation(libs.koin.compose.viewmodel)
    implementation(libs.kotlinx.coroutinesSwing)
    implementation(libs.kotlinx.serialization.core)
    implementation(libs.napier)
    implementation(libs.kotlin.logging.jvm)
}


val appVersion = project.findProperty("APP_VERSION")?.toString()
    ?: System.getenv("APP_VERSION")
    ?: System.getenv("GITHUB_REF_NAME")?.removePrefix("v")?.takeIf { it.isNotBlank() && it.matches(Regex("^[0-9]+\\.[0-9]+\\.[0-9]+.*$")) }
    ?: "1.0.0"

compose.desktop {
    application {
        mainClass = "com.citacomsys.inventory.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Exe, TargetFormat.Deb)
            packageName = "citas-inventory"
            packageVersion = appVersion
        }
    }
}
