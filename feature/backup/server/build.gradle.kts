plugins {
    alias(libs.plugins.citacomsysKtorFeature)
}

dependencies {
    implementation(project(":feature:home:api"))
    implementation(project(":core-server:persistence"))
    implementation(project(":module:sales:api"))
    implementation(project(":module:inventory:api"))
}