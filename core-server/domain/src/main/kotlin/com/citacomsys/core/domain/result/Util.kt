package com.citacomsys.core.domain.result

import java.util.UUID

fun String.toUuid(): UUID = UUID.fromString(this)
