package com.citacomsys.core.security.crypto

actual object CryptoManager {
    actual fun encrypt(data: ByteArray, secretKey: ByteArray): String {
        TODO("Not yet implemented")
    }

    actual fun decrypt(
        encryptedData: String,
        secretKey: ByteArray
    ): ByteArray {
        TODO("Not yet implemented")
    }

    actual fun generateRandomKey(): ByteArray {
        TODO("Not yet implemented")
    }

    actual fun hmac(data: ByteArray, secretKey: ByteArray): ByteArray {
        TODO("Not yet implemented")
    }

    actual fun sha256(data: String): ByteArray {
        TODO("Not yet implemented")
    }

    actual fun sha256(data: ByteArray): ByteArray {
        TODO("Not yet implemented")
    }

    actual fun deriveKeys(
        masterKey: ByteArray,
        dnaSecret: ByteArray?
    ): DerivedKeys {
        TODO("Not yet implemented")
    }

    actual fun deriveDatabaseKey(masterKey: ByteArray): ByteArray {
        TODO("Not yet implemented")
    }

    actual fun generateRequestId(): String {
        TODO("Not yet implemented")
    }

    actual fun generateUUID(): String {
        TODO("Not yet implemented")
    }
}

actual object Base64Helper {
    actual fun encode(bytes: ByteArray): String {
        TODO("Not yet implemented")
    }

    actual fun decode(base64: String): ByteArray {
        TODO("Not yet implemented")
    }
}