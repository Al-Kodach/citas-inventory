package com.citacomsys.client.support.di

import io.github.aakira.napier.DebugAntilog
import io.github.aakira.napier.Napier

actual fun initNapier(context: Any?) {
    Napier.base(DebugAntilog())
}