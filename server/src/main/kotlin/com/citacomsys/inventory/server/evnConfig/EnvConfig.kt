package com.citacomsys.inventory.server.evnConfig

import mu.KotlinLogging
import java.io.File

private val logger = KotlinLogging.logger {}

/**
 * A robust, self-contained object for loading and accessing environment configuration.
 * It loads variables from a .env file for local development and falls back to
 * system environment variables (which is what Docker and production environments use).
 */
object EnvConfig {
    private val logger = KotlinLogging.logger {}
    private var isLoaded = false
    private val config = mutableMapOf<String, String>()

    /**
     * The main entry point. Loads the .env file if found.
     * This should be the VERY first thing called in your main() function.
     */
    fun load() {
        if (isLoaded) return

        val projectDir = System.getProperty("user.dir")
        val potentialFiles = listOfNotNull(
            File(projectDir, "./.env"),
            File(projectDir).parentFile?.resolve(".env")
        )

        logger.info { "Searching for environment variables file in the following locations: ${potentialFiles.map { it.absolutePath }}" }

        val envFile = potentialFiles.firstOrNull { it.exists() }

        if (envFile != null) {
            logger.info { "Loading environment variables from: ${envFile.absolutePath}" }
            try {
                envFile.forEachLine { line ->
                    // Ignore comments and empty lines
                    if (line.isNotBlank() && !line.trim().startsWith("#")) {
                        val parts = line.split("=", limit = 2)
                        if (parts.size == 2) {
                            val key = parts[0].trim()
                            val value = parts[1].trim()
                            config[key] = value
                            System.setProperty(key, value)
                        }
                    }
                }
                isLoaded = true
                logger.info { "Successfully loaded ${config.size} variables from .env file." }
            } catch (e: Exception) {
                logger.error(e) { "Failed to readTransaction .env file at ${envFile.absolutePath}" }
            }
        } else {
            logger.warn { ".env file not found. Relying solely on system environment variables." }
        }
    }

    /**
     * Gets a configuration value. It first checks system properties (loaded from .env),
     * then falls back to system environment variables.
     */
    fun get(key: String, default: String? = null): String {
        return System.getProperty(key) // For Docker/OS provided vars
            ?: System.getenv(key) // For .env loaded vars
            ?: default
            ?: throw IllegalStateException("Configuration variable '$key' not found and no default value provided.")
    }
}
