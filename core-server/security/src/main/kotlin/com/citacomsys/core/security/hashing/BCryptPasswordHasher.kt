package com.citacomsys.core.security.hashing

import com.citacomsys.shared.security.PasswordHasher
import mu.KotlinLogging
import org.mindrot.jbcrypt.BCrypt

private val logger = KotlinLogging.logger {}

internal class BCryptPasswordHasher : PasswordHasher {
    private val workload = 12

    override fun hash(raw: String): String {
        return BCrypt.hashpw(raw, BCrypt.gensalt(workload))
    }

    override fun verify(raw: String, hash: String): Boolean {
        if (hash.isBlank()) return false
        return try {
            // Trim the hash to handle DB padding (e.g. from CHAR columns)
            BCrypt.checkpw(raw, hash)
        } catch (e: Exception) {
            logger.error(e) { "BCrypt Verification Error: ${e.message}" }
            false
        }
    }
}

fun main() {
    val password123Hasher = BCryptPasswordHasher().hash("password123")
    println("Raw-Password: password123")
    println("Hashed-Password: $password123Hasher")
}