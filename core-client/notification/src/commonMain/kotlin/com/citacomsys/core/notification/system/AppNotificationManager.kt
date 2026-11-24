package com.citacomsys.core.notification.system

import com.mmk.kmpnotifier.KMPNotifier
import com.mmk.kmpnotifier.local.localNotifier
import com.mmk.kmpnotifier.notification.NotificationAction as KmpNotificationAction
import io.github.aakira.napier.Napier
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlin.time.Duration.Companion.milliseconds

data class AppNotificationAction(
    val id: String,
    val title: String
)

data class AppNotification(
    val id: String,
    val title: String,
    val message: String,
    val isCritical: Boolean = false,
    val timestamp: Long = 0L,
    val playSound: Boolean = true,
    val autoDismiss: Boolean = true,
    val durationMillis: Long = 4000L,
    val actions: List<AppNotificationAction> = emptyList(),
    val payload: Map<String, String> = emptyMap()
)

object AppNotificationManager {
    private val _notifications = MutableStateFlow<List<AppNotification>>(emptyList())
    val notifications = _notifications.asStateFlow()

    private val _currentBanner = MutableStateFlow<AppNotification?>(null)
    val currentBanner = _currentBanner.asStateFlow()

    private val _actionEvents = MutableSharedFlow<Pair<String, AppNotification>>(extraBufferCapacity = 10)
    val actionEvents = _actionEvents.asSharedFlow()

    private var isAlertSoundEnabled: () -> Boolean = { true }
    private var playNormalSound: (() -> Unit)? = null
    private var playAlertSound: (() -> Unit)? = null

    var isAppInForeground: Boolean = false

    private val sideEffectScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var bannerJob: Job? = null

    fun init(
        soundEnabledProvider: () -> Boolean = { true },
        normalSoundPlayer: (() -> Unit)? = null,
        alertSoundPlayer: (() -> Unit)? = null
    ) {
        isAlertSoundEnabled = soundEnabledProvider
        playNormalSound = normalSoundPlayer
        playAlertSound = alertSoundPlayer

        try {
            KMPNotifier.addListener(object : KMPNotifier.Listener {
                override fun onNotificationClicked(data: Map<String, Any?>) {}

                override fun onAction(
                    actionId: String,
                    notificationId: Int,
                    payload: Map<String, Any?>
                ) {
                    val notification = _notifications.value.find { it.id.hashCode() == notificationId }
                    if (notification != null) {
                        _actionEvents.tryEmit(actionId to notification)
                    }
                }
            })
        } catch (_: Exception) {}
    }

    fun addNotification(notification: AppNotification) {
        _notifications.update { current ->
            val filtered = current.filterNot { it.id == notification.id }

            sideEffectScope.launch {
                try {
                    if (notification.playSound && isAlertSoundEnabled()) {
                        if (notification.isCritical) {
                            playAlertSound?.invoke()
                        } else {
                            playNormalSound?.invoke()
                        }
                    }

                    if (!isAppInForeground) {
                        try {
                            KMPNotifier.localNotifier.notify {
                                this.id = notification.id.hashCode()
                                this.title = notification.title
                                this.body = notification.message
                                this.payloadData = notification.payload
                                this.actions = notification.actions.map { KmpNotificationAction(it.id, it.title) }
                            }
                        } catch (_: Exception) {}
                    }
                } catch (e: Exception) {
                    Napier.e(e) { "Failed to process notification side-effects" }
                }
            }

            if (notification.autoDismiss) {
                bannerJob?.cancel()
                bannerJob = sideEffectScope.launch {
                    _currentBanner.value = notification
                    delay(notification.durationMillis.milliseconds)
                    if (_currentBanner.value?.id == notification.id) {
                        _currentBanner.value = null
                    }
                }
            }

            filtered + notification
        }
    }

    fun dismissBanner() {
        bannerJob?.cancel()
        _currentBanner.value = null
    }

    fun removeNotification(id: String) {
        try {
            KMPNotifier.localNotifier.remove(id.hashCode())
        } catch (_: Exception) {}
        _notifications.update { it.filterNot { n -> n.id == id } }
        if (_currentBanner.value?.id == id) {
            dismissBanner()
        }
    }

    fun clearAll() {
        try {
            KMPNotifier.localNotifier.removeAll()
        } catch (_: Exception) {}
        _notifications.value = emptyList()
        dismissBanner()
    }

    fun triggerAction(actionId: String, notification: AppNotification) {
        sideEffectScope.launch {
            _actionEvents.emit(actionId to notification)
        }
        removeNotification(notification.id)
    }
}
