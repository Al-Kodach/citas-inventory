plugins {
    alias(libs.plugins.citacomsysKtorFeature)
}

dependencies {
    api(project(":kernel:api"))
    implementation(project(":core-server:notification"))
    implementation(project(":core-server:persistence"))
    implementation(project(":feature:workspace:api"))
    implementation(project(":feature:user:api"))
    implementation(project(":feature:organization:api"))
    implementation(project(":module:device:api"))
}
