package com.citacomsys.core.persistence.config

data class DbConfig(
    val url: String,
    val user: String,
    val pass: String,
    val poolSize: Int,
    val isProduction: Boolean
)