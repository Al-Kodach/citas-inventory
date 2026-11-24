import java.io.File

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeCompiler)
}

val appVersion = project.findProperty("APP_VERSION")?.toString()
    ?: System.getenv("APP_VERSION")
    ?: System.getenv("GITHUB_REF_NAME")?.removePrefix("v")?.takeIf { it.isNotBlank() }
    ?: "1.0.0"

android {
    namespace = "com.citacomsys.inventory"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.citacomsys.inventory.android"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = 37
        versionCode = 1
        versionName = appVersion
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        multiDexEnabled = true
    }

    signingConfigs {
        create("release") {
            val keystoreFile = project.findProperty("KEYSTORE_FILE")?.toString() ?: System.getenv("KEYSTORE_FILE")
            val keystorePassword = project.findProperty("KEYSTORE_PASSWORD")?.toString() ?: System.getenv("KEYSTORE_PASSWORD")
            val keyAlias = project.findProperty("KEY_ALIAS")?.toString() ?: System.getenv("KEY_ALIAS")
            val keyPassword = project.findProperty("KEY_PASSWORD")?.toString() ?: System.getenv("KEY_PASSWORD")

            if (!keystoreFile.isNullOrBlank() && File(keystoreFile).exists()) {
                storeFile = File(keystoreFile)
                storePassword = keystorePassword
                this.keyAlias = keyAlias
                this.keyPassword = keyPassword
            } else {
                val debugConfig = getByName("debug")
                storeFile = debugConfig.storeFile
                storePassword = debugConfig.storePassword
                this.keyAlias = debugConfig.keyAlias
                this.keyPassword = debugConfig.keyPassword
            }
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        compose = true
    }

    bundle {
        language {
            enableSplit = true
        }
        density {
            enableSplit = true
        }
        abi {
            enableSplit = true
        }
    }

    splits {
        abi {
            isEnable = false
        }
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "META-INF/io.netty.versions.properties"
            excludes += "META-INF/INDEX.LIST"
        }
    }
}

dependencies {
    implementation(project(":app:shared"))
    implementation(project(":pharmacy-product-ext:client"))
    implementation(project(":retail-product-ext:client"))

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.appcompat)
    implementation(libs.koin.android)
    implementation(libs.koin.androidx.compose)
}
