plugins {
    alias(libs.plugins.citacomsysKtorFeature)
}

dependencies {
    implementation(project(":feature:user:api"))
    implementation(project(":feature:auth:api"))
    implementation(project(":core-server:persistence"))
    implementation(project(":module:device:api"))
    implementation(project(":feature:workspace:api"))
}