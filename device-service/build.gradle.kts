
plugins {
    alias(libs.plugins.citacomsysKmpLibrary)
}

kotlin {
    sourceSets {
        androidMain.dependencies {
            implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))
            implementation(libs.printer.core)
            implementation(libs.printer.compose)
        }

        commonMain.dependencies {
            implementation(project(":core-client:domain"))
            implementation(project(":shared"))
            implementation(libs.compose.foundation)
            implementation(libs.compose.runtime)
            implementation(libs.ksafe)
        }

        jvmMain.dependencies {
            implementation(libs.pdfbox)
            implementation(libs.printer.core)
            implementation(libs.printer.compose)
        }
    }
}

