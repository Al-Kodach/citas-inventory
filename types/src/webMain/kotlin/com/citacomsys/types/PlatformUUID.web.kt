package com.citacomsys.types

import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.js

private const val RANDOM_UIID = "crypto.randomUUID().toString()"

@OptIn(ExperimentalWasmJsInterop::class)
actual fun generateUUID(): String = js(RANDOM_UIID)