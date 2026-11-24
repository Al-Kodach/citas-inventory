import org.gradle.kotlin.dsl.dependencies

plugins {
    alias(libs.plugins.citacomsysKtorFeature)
}

dependencies {
    api(project(":feature:billing:api"))
    implementation(project(":feature:home:api"))
    implementation(project(":core-server:persistence"))
    implementation(project(":feature:workspace:api"))
    implementation(project(":module:sales:api"))
    implementation(project(":module:inventory:api"))
    implementation(project(":feature:organization:api",
))
    implementation(project(":feature:platform:api"))
    implementation(project(":feature:user:api"))

    implementation(project(":core-server:notification"))
    implementation(project(":feature:organization:server",
))
}