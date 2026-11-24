plugins {
    alias(libs.plugins.citacomsysKtorFeature)
}

dependencies {
    implementation(project(":feature:organization:api"))
    implementation(project(":feature:workspace:api"))
    implementation(project(":kernel:api"))
    implementation(project(":kernel:server"))
    implementation(project(":module:device:api"))
    implementation(project(":feature:user:api"))
    implementation(project(":feature:billing:api"))
    implementation(project(":feature:platform:api"))

    implementation(project(":core-server:persistence"))

    implementation(project(":core-server:notification"))
}