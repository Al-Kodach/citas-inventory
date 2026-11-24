import org.gradle.kotlin.dsl.dependencies

plugins {
    alias(libs.plugins.citacomsysKtorFeature)
}

dependencies {
    api(
        project(":feature:organization:api")
    )
    implementation(project(":feature:auth:api"))
    implementation(project(":feature:user:api"))
    implementation(project(":feature:platform:api"))
    implementation(project(":feature:billing:api"))
    implementation(project(":feature:telemetry:server"))
    implementation(project(":core-server:persistence"))
    implementation(project(":core-server:notification"))
    implementation(project(":core-server:security"))
}
