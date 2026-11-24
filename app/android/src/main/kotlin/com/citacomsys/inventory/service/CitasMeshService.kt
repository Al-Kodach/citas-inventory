package com.citacomsys.inventory.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.citacomsys.core.mesh.server.EngineState
import com.citacomsys.core.mesh.service.MeshManager
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

class CitasMeshService : Service() {
    private val TAG = "HUB_SERVICE"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val meshManager: MeshManager by inject()

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        Napier.i(tag = TAG) { "CitasMeshService created. Observing Mother Hub engine state..." }

        scope.launch {
            meshManager.state.collect { state ->
                when (state) {
                    EngineState.Running, EngineState.Starting -> {
                        Napier.i(tag = TAG) { "Mother Hub Engine active. Promoting to Foreground Service..." }
                        startForeground(NOTIFICATION_ID, createNotification())
                    }
                    EngineState.Stopped -> {
                        Napier.i(tag = TAG) { "Mother Hub Engine stopped. Terminating Foreground Service..." }
                        stopServiceMeshSilently()
                    }
                    else -> {}
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP_MESH) {
            Napier.i(tag = TAG) { "Stop intent received. Terminating Mesh Service." }
            stopServiceMeshSilently()
            return START_NOT_STICKY
        }
        return START_STICKY
    }

    private fun stopServiceMeshSilently() {
        if (meshManager.state.value == EngineState.Running) {
            meshManager.stopMesh()
        }
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        Napier.i(tag = TAG) { "CitasMeshService destroyed." }
        scope.cancel()
    }

    private fun createNotification(): Notification {
        val channelId = "citas_mesh_service_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Citas Mother Hub Mesh Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Hosts local LAN mesh network and gateway services."
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("Citas Mother Hub Active")
            .setContentText("Local LAN mesh gateway running...")
            .setOngoing(true)
            .build()
    }

    companion object {
        const val ACTION_STOP_MESH = "com.citacomsys.inventory.ACTION_STOP_MESH"
        private const val NOTIFICATION_ID = 1002

        fun startService(context: Context) {
            val intent = Intent(context, CitasMeshService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, CitasMeshService::class.java).apply {
                action = ACTION_STOP_MESH
            }
            context.startService(intent)
        }
    }
}
