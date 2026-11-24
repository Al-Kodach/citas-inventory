plugins {
    alias(libs.plugins.citacomsysKtorFeature)
}

dependencies {
    implementation(project(":core-server:persistence"))

    api(project(":feature:platform:api"))
    implementation(project(":feature:organization:api"))
    implementation(project(":feature:workspace:api"))
    implementation(project(":feature:user:api"))
    implementation(project(":feature:billing:api"))

    implementation(project(":module:device:api"))

    implementation(libs.kotlinx.datetime)
}