import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.composeHotReload)
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.kotlinSerialization)
}

dependencies {
    implementation(project(":admin:uiShared"))
    implementation(project(":core-client:spi"))

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
    implementation(libs.kotlin.logging.jvm)

    implementation(libs.napier)
    implementation(libs.filekit.core)
    implementation(libs.ksafe.compose)
}

compose.desktop {
    application {
        mainClass = "com.citacomsys.inventory.admin.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "com.citacomsys.inventory.admin"
            packageVersion = "1.0.0"
        }
    }
}
