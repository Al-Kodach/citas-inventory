plugins {
    alias(libs.plugins.citacomsysKotlinJvmApi)
}

dependencies {
    implementation(libs.libphonenumber)
    api(project(":types"))
}