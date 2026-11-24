plugins {
    alias(libs.plugins.citacomsysKmpLibrary)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(project(":test-suite:common"))
            api(project(":core-client:domain"))

            api(kotlin("test"))
            api(libs.koin.test)
            api("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")
        }
        
        jvmMain.dependencies {
            api(libs.mockk)
        }

        androidMain.dependencies {
            api(libs.mockk)
        }
    }
}
