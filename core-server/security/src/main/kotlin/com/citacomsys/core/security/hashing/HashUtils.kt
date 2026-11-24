package com.citacomsys.core.security.hashing

import java.security.MessageDigest
import java.util.Base64

object HashUtils {

    fun sha256(data: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(data.toByteArray())
        return Base64.getEncoder().encodeToString(hash)
    }
}
