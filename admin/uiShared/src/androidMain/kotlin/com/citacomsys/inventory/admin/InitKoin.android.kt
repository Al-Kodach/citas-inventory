package com.citacomsys.inventory.admin

import io.github.aakira.napier.DebugAntilog
import io.github.aakira.napier.Napier

actual fun initNapier(context: Any?) {
    Napier.base(DebugAntilog())
}