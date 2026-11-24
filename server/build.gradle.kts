plugins {
    alias(libs.plugins.citacomsysKtorApi)
    alias(libs.plugins.shadow)
    application
}

group = "com.citacomsys.inventory"
version = "1.0.0"

application {
    mainClass.set("com.citacomsys.inventory.server.ApplicationKt")
}

tasks.withType<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar> {
    manifest {
        attributes["Main-Class"] = "com.citacomsys.inventory.server.ApplicationKt"
    }
}

tasks.shadowJar {
    mergeServiceFiles()
    isZip64 = true
}

tasks.withType<Tar> {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

tasks.withType<Zip> {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

dependencies {
    implementation(project(":pharmacy-product-ext:server"))
    implementation(project(":admin:server"))
    implementation(project(":retail-product-ext:server"))

    implementation(project(":module:inventory:server"))
    implementation(project(":module:sales:server"))
    implementation(project(":module:device:server"))
    implementation(project(":feature:customer:server"))

    implementation(project(":feature:home:server"))
    implementation(project(":feature:user:api"))
    implementation(project(":feature:telemetry:server"))
    implementation(project(":feature:auth:server"))
    implementation(project(":feature:user:server"))
    implementation(project(":feature:organization:server"))
    implementation(project(":feature:platform:server"))
    implementation(project(":kernel:server"))
    implementation(project(":feature:billing:server"))
    implementation(project(":feature:workspace:server"))

    implementation(project(":core-server:domain"))
    implementation(project(":core-server:data"))
    implementation(project(":core-server:security"))
    implementation(project(":core-server:notification"))
    implementation(project(":core-server:storage"))
    implementation(project(":core-server:persistence"))

    implementation(project(":shared"))

    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.config.yaml)
    implementation(libs.ktor.server.forwarded.header)
    implementation(libs.ktor.server.cors)
    implementation(libs.ktor.server.status.pages)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.ktor.server.rate.limit)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.ktor.server.auth)
    implementation(libs.ktor.server.validation)
    implementation(libs.ktor.server.calllogging)
    implementation(libs.ktor.server.sse)
    implementation(libs.ktor.server.doublereceive)

    implementation(libs.koin.ktor)
    implementation(libs.logback)

}

abstract class DeployTask : DefaultTask() {

    @get:Inject
    abstract val execOperations: ExecOperations

    @get:Input
    abstract val env: Property<String>

    @get:Input
    abstract val imageTag: Property<String>

    @TaskAction
    fun execute() {

        val registry = "citacomsys"
        val imageName = "citas-server-kernel"

        val tag = imageTag.get()
        val fullImage = "$registry/$imageName:$tag"

        val latestAlias = "$registry/$imageName:latest"

        val vpsCommand = when (env.get()) {
            "staging" -> "staging"
            "production" -> "production"
            else -> throw GradleException("Invalid env: ${env.get()}")
        }

        logger.lifecycle("🚀 Deploying [$env] with image: $fullImage")

        val rootDir = project.rootDir.absolutePath
        val dockerfile = "$rootDir/citas-server-kernel/Dockerfile"

        // -------------------------------------------------
        // STEP 1: BUILD IMAGE
        // -------------------------------------------------
        logger.lifecycle("STEP 1: Building Docker image...")

        execOperations.exec {
            commandLine(
                "docker",
                "build",
                "-f", dockerfile,
                "-t", fullImage,
                "-t", latestAlias,
                rootDir
            )
        }

        // -------------------------------------------------
        // STEP 2: PUSH IMAGE
        // -------------------------------------------------
        logger.lifecycle("STEP 2: Pushing image...")

        execOperations.exec {
            commandLine("docker", "push", fullImage)
        }

        execOperations.exec {
            commandLine("docker", "push", latestAlias)
        }

        // -------------------------------------------------
        // STEP 3: DEPLOY VIA VPS SCRIPT (CLEAN MODEL)
        // -------------------------------------------------
        logger.lifecycle("STEP 3: Deploying to VPS...")

        execOperations.exec {
            commandLine(
                "ssh",
                "app",
                "~/update-citas.sh",
                vpsCommand,
                tag
            )
        }

        logger.lifecycle("✅ DEPLOYMENT SUCCESSFUL")
    }
}