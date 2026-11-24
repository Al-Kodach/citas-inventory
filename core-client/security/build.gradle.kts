plugins {
    alias(libs.plugins.citacomsysKmpLibrary)
}

kotlin {
    sourceSets {
        androidMain.dependencies {
            implementation(libs.mindrot.jbcrypt)
        }

        commonMain.dependencies {
            implementation(project(":feature:user:api"))
            implementation(project(":feature:organization:api"))
            implementation(project(":feature:workspace:api"))

            implementation(project(":device-service"))
            implementation(project(":core-client:domain"))
            implementation(project(":core-client:database"))
            implementation(project(":core-client:notification"))
            implementation(project(":shared"))

            implementation(libs.kotlinx.serialization.json)
            implementation(libs.ksafe)
            implementation(libs.ksafe.biometrics)
        }

        jvmMain.dependencies {
            implementation(libs.mindrot.jbcrypt)
        }
    }
}
