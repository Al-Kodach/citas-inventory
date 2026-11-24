plugins {
    alias(libs.plugins.citacomsysKtorFeature)
}

dependencies {
    implementation(project(":core-server:persistence"))
    implementation(project(":feature:customer:api"))
}
