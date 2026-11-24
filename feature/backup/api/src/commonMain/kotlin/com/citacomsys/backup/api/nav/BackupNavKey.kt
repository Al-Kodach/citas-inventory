package com.citacomsys.backup.api.nav

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

//@Serializable
//sealed interface BackupNav : NavKey
@Serializable
data class RestoreBackupNav(val initPwd: String? = null): NavKey
@Serializable
data object CreateBackupNav : NavKey
