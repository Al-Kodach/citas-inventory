plugins {
    alias(libs.plugins.citacomsysKtorFeature)
}

dependencies {
    implementation(project(":core-server:notification"))
    implementation(project(":core-server:persistence"))
    implementation(project(":feature:workspace:api"))

    implementation(project(":module:inventory:api"))
    implementation(project(":module:sales:api"))
    implementation(project(":feature:customer:api"))
    implementation(project(":feature:user:api"))
    implementation(project(":feature:organization:api"))

    implementation(libs.caffeine)
}
