package com.citacomsys.core.security.hashing

import com.citacomsys.shared.security.PasswordHasher
import io.github.aakira.napier.Napier
import org.mindrot.jbcrypt.BCrypt


internal class JvmBCryptPasswordHasher : PasswordHasher {
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
            Napier.e(e) { "BCrypt Verification Error: ${e.message}" }
            false
        }
    }
}
