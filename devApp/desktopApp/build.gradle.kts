import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinJvm)
}

dependencies {
    implementation(project(":app:shared"))
    implementation(project(":app:desktop"))
    implementation(project(":devApp:uiShared"))
    
    implementation(libs.compose.runtime)
    implementation(libs.compose.ui)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
    implementation(libs.androidx.lifecycle.viewmodelCompose)
    implementation(libs.koin.compose)
    implementation(compose.desktop.currentOs)
    implementation(libs.napier)
}

compose.desktop {
    application {
        mainClass = "com.citacomsys.dev.MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "com.citacomsys.dev.sandbox"
            packageVersion = "1.0.0"
        }
    }
}
