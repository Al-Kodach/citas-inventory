import org.gradle.kotlin.dsl.dependencies

plugins {
    alias(libs.plugins.citacomsysKtorFeature)
}

dependencies {
    implementation(project(":admin:api"))
    implementation(project(":core-server:notification"))
    implementation(project(":core-server:storage"))

    implementation(project(":feature:platform:api"))
    implementation(project(":feature:billing:api"))
    implementation(project(":feature:billing:server"))
    implementation(project(":feature:organization:api"))
    implementation(project(":feature:user:api"))
    implementation(project(":feature:user:server"))
    implementation(project(":feature:auth:api"))
    implementation(project(":feature:auth:server"))
    implementation(project(":feature:telemetry:server"))
    implementation(project(":feature:workspace:api"))

    implementation(project(":module:device:api"))

    implementation(project(":core-server:persistence"))
    implementation(project(":core-server:security"))
}
