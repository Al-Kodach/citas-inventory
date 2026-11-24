package com.citacomsys.shared.security

import kotlin.random.Random

object RecoveryCodeGenerator {
    private val recoveryChars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"

    fun generateEmergencyRecoveryCode(): String {
        return (1..4).joinToString("-") {
            (1..4).map { recoveryChars[Random.nextInt(recoveryChars.length)] }.joinToString("")
        }
    }
}
