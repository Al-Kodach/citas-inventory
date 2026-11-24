package com.citacomsys.core.data.di

import com.citacomsys.core.data.file.PlatformFileSystemImpl
import com.citacomsys.core.data.local.AppInfoProviderImpl
import com.citacomsys.core.data.repository.GlobalSearchRepositoryImpl
import com.citacomsys.core.data.repository.PreferencesRepositoryImpl
import com.citacomsys.core.data.service.ClientSystemEventService
import com.citacomsys.core.data.service.NetworkConnectivityServiceImpl
import com.citacomsys.core.data.session.BranchSwitcherViewModel
import com.citacomsys.core.data.session.DefaultAppSessionManager
import com.citacomsys.core.domain.AppInfoProvider
import com.citacomsys.core.domain.file.PlatformFileSystem
import com.citacomsys.core.domain.repository.GlobalSearchRepository
import com.citacomsys.core.domain.repository.PreferencesRepository
import com.citacomsys.core.domain.repository.SyncPreferencesRepository
import com.citacomsys.core.data.repository.SyncPreferencesRepositoryImpl
import com.citacomsys.core.data.session.WorkspaceSessionManagerImpl
import com.citacomsys.core.domain.service.AppSessionManager
import com.citacomsys.core.domain.service.NetworkConnectivityService
import com.citacomsys.core.domain.service.SystemEventService
import com.citacomsys.core.domain.service.WorkspaceSessionManager
import eu.anifantakis.lib.ksafe.SecurityViolation
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.module
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

fun getViolationDescription(violation: SecurityViolation): String {
    return when (violation) {
        SecurityViolation.RootedDevice ->
            "The device is rooted (Android) or jailbroken (iOS). " +
                    "This allows apps to bypass sandboxing and potentially access encrypted data."

        SecurityViolation.DebuggerAttached ->
            "A debugger is attached to the process. " +
                    "This allows inspection of memory and runtime values including decrypted secrets."

        SecurityViolation.DebugBuild ->
            "The app is running in debug mode. " +
                    "Debug builds may have weaker security settings and expose more information."

        SecurityViolation.Emulator ->
            "The app is running on an emulator/simulator. " +
                    "Emulators don't have hardware-backed security like real devices."
    }
}

expect fun platformModule(): Module

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
expect fun ioDispatcher(): CoroutineDispatcher
expect fun defaultDispatcher(): CoroutineDispatcher

@OptIn(ExperimentalTime::class)
val coreDataModule = module {
    includes(platformModule())
    single<CoroutineScope> { CoroutineScope(Dispatchers.Default + SupervisorJob()) }

    single<CoroutineDispatcher>(named("IO")) {
        ioDispatcher()
    }

    single<CoroutineDispatcher> { defaultDispatcher() }

    single<NetworkConnectivityService> { NetworkConnectivityServiceImpl() }
    single<AppSessionManager> {
        DefaultAppSessionManager(
            store = get(),
            vault = get(named("vault")),
            syncPrefs = get(),
            deviceService = get(),
            workspaceSessionManager = get(),
            scope = get(),
            appManifest = get()
        )
    }

    single<WorkspaceSessionManager> {
        WorkspaceSessionManagerImpl(get())
    }

    single<PreferencesRepository> {
        PreferencesRepositoryImpl(
            prefs = get(),
            scope = get(),
            appConfig = get()
        )
    }

    single<SyncPreferencesRepository> {
        SyncPreferencesRepositoryImpl(
            prefs = get(),
            workspaceIdProvider = { get<AppSessionManager>().activeWorkspaceId },
            scope = get()
        )
    }

    single<SystemEventService> {
        ClientSystemEventService(
            client = get(),
            networkService = get(),
            scope = get(),
            prefs = get()
        )
    }

    single<Clock> { Clock.System }

    single<PlatformFileSystem> {
        PlatformFileSystemImpl(
            support = get(),
            ioDispatcher = get(named("IO"))
        )
    }

    single<AppInfoProvider> {
        AppInfoProviderImpl()
    }

    single<GlobalSearchRepository> {
        GlobalSearchRepositoryImpl(get(), get())
    }

    factory {
        BranchSwitcherViewModel(
            get(),
            get(),
            get()
        )
    }
}
