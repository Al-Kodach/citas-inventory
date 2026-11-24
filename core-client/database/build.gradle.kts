@file:OptIn(ExperimentalWasmDsl::class)

import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    alias(libs.plugins.citacomsysKmpLibrary)
    alias(libs.plugins.sqlDelight)
}


sqldelight {
    databases {
        create("InventoryDatabase") {
            packageName.set("com.citacomsys.database")
            srcDirs.setFrom("src/commonMain/sqldelight/inventory")
            generateAsync.set(true)
            verifyMigrations.set(true)
        }
        create("TelemetryDatabase") {
            packageName.set("com.citacomsys.telemetry.database")
            srcDirs.setFrom("src/commonMain/sqldelight/telemetry")
            generateAsync.set(true)
        }
        create("CustomerDatabase") {
            packageName.set("com.citacomsys.customer.database")
            srcDirs.setFrom("src/commonMain/sqldelight/customer")
            generateAsync.set(true)
        }
    }
}


kotlin {
    android {
        namespace = "com.citacomsys.inventory.core.client.database"
    }
    sourceSets {
        androidMain.dependencies {
            implementation(libs.sqldelight.android.driver)
            implementation("net.zetetic:android-database-sqlcipher:4.5.4")
        }

        commonMain.dependencies {
            implementation(project(":feature:customer:api"))
            implementation(project(":module:inventory:api"))
            implementation(project(":module:sales:api"))
            implementation(project(":shared"))
            implementation(project(":core-client:domain"))
            implementation(libs.ksafe)
            api(libs.sqldelight.coroutines.extensions)
            api(libs.kotlinx.serialization.json)
            api(libs.store)
            api(libs.okio)
        }

        jvmMain.dependencies {
            implementation(libs.sqldelight.sqlite.driver)
        }

        jsMain.dependencies {
            implementation(libs.web.worker.driver)
            implementation(devNpm("copy-webpack-plugin", "9.1.0"))
            implementation(npm("@cashapp/sqldelight-sqljs-worker", "2.1.0"))
            implementation(npm("sql.js", "1.8.0"))
        }
    }
}
