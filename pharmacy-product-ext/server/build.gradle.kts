import org.gradle.kotlin.dsl.dependencies

plugins {
    alias(libs.plugins.citacomsysKtorFeature)
}

dependencies {
    implementation(project(":core-client:design"))
    implementation(project(":core-server:notification"))
    implementation(project(":feature:workspace:api"))
    implementation(project(":feature:organization:api"))
    implementation(project(":feature:user:api"))
    implementation(project(":feature:workspace:api"))
    implementation(project(":feature:customer:server"))
    implementation(project(":module:sales:server"))
    implementation(project(":pharmacy-product-ext:api"))
    implementation(project(":module:inventory:server"))

    implementation(project(":feature:platform:api"))
    implementation(project(":feature:billing:api"))

    implementation(project(":core-server:persistence"))
    implementation(project(":core-server:security"))

    testImplementation(project(":test-suite:server"))
}
