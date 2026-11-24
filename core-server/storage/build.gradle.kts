plugins {
    alias(libs.plugins.citacomsysKtorApi)
}

dependencies {
    implementation(project(":feature:user:api"))
    implementation(project(":core-server:domain"))

    implementation(libs.ktor.server.resources)
    implementation(libs.kotlinx.serialization.json)
    
    implementation("software.amazon.awssdk:s3:2.25.10")
}