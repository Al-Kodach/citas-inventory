package com.citacomsys.citread.security

/**
 * SECURITY COMPLIANCE: SC-13
 * Constant-time comparison to prevent timing side-channel attacks.
 */
fun constantTimeEquals(a: String, b: String): Boolean {
    if (a.length != b.length) return false
    var result = 0
    for (i in a.indices) {
        result = result or (a[i].code xor b[i].code)
    }
    return result == 0
}

/**
 * Safely clears a byte array from memory.
 */
fun ByteArray.wipe() {
    fill(0)
}
