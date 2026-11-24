plugins {
    `kotlin-dsl`
}

group = "com.citacomsys.buildlogic"

dependencies {
    implementation(libs.android.gradlePlugin)
    compileOnly(libs.android.tools.common)
    compileOnly(libs.kotlin.gradlePlugin)
    compileOnly(libs.compose.gradlePlugin)
}

gradlePlugin {
    plugins {
        register("kotlinMultiplatformLibrary") {
            id = libs.plugins.citacomsysKmpLibrary.get().pluginId
            implementationClass = "KmpLibraryConventionPlugin"
        }
        register("composeMultiplatform") {
            id = libs.plugins.citacomsysComposeMultiplatform.get().pluginId
            implementationClass = "ComposeMultiplatformConventionPlugin"
        }
        register("kmpComposeApi") {
            id = "citacomsys.kmp.compose.api"
            implementationClass = "ComposeKmpApiConventionPlugin"
        }

        register("kmpClientFeatureConvention") {
            id = libs.plugins.citacomsysKmpFeature.get().pluginId
            implementationClass = "KmpFeatureConventionPlugin"
        }
        register("JvmServerFeatureConvention") {
            id = libs.plugins.citacomsysKtorFeature.get().pluginId
            implementationClass = "KtorFeatureConventionPlugin"
        }
        register("jvmServerApiConvention") {
            id = libs.plugins.citacomsysKtorApi.get().pluginId
            implementationClass = "KtorApiConventionPlugin"
        }
        register("KotlinJvmApiConventionPlugin") {
            id = libs.plugins.citacomsysKotlinJvmApi.get().pluginId
            implementationClass = "KotlinJvmApiConventionPlugin"
        }
    }
}
