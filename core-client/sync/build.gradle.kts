plugins {
    alias(libs.plugins.citacomsysKmpLibrary)
}

kotlin {
    sourceSets {
        androidMain.dependencies {
            api(libs.slf4j.android)
            api(libs.work.runtime)
        }

        commonMain.dependencies {
            implementation(project(":feature:workspace:api"))
            implementation(project(":feature:telemetry:api"))

            implementation(project(":core-client:domain"))
            implementation(project(":core-client:database"))
            implementation(project(":core-client:mesh"))
            implementation(project(":core-client:notification"))
            implementation(project(":core-client:remote"))
            implementation(project(":core-client:spi"))
            implementation(project(":shared"))
            implementation(project(":device-service"))


            implementation(libs.kotlinx.serialization.json)
        }

        jvmTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutinesTest)
            implementation(libs.mockk)
        }
    }
}
