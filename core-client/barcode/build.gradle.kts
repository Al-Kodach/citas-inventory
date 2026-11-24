plugins {
    alias(libs.plugins.citacomsysKmpLibrary)
    alias(libs.plugins.citacomsysComposeMultiplatform)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(project(":shared"))
            implementation(project(":types"))
            implementation(project(":core-client:notification"))
            implementation(project(":device-service"))
            implementation(libs.material.icons.extended)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.koin.core)
            implementation(libs.zxing.core)
        }
        androidMain.dependencies {
            implementation(libs.kscan)
        }
        jvmMain.dependencies {
            implementation(libs.kscan)
            implementation(libs.zxing.javase)
            implementation(libs.webcam.capture)
        }
    }
}
