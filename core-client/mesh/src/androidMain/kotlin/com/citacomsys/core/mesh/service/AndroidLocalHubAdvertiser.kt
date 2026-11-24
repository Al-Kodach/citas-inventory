package com.citacomsys.core.mesh.service

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import com.citacomsys.core.mesh.security.MeshCryptoManager
import com.citacomsys.core.mesh.security.toChunks
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlin.time.Duration.Companion.milliseconds

/**
 * Android Implementation of NSD Mother Hub Advertising.
 */
class AndroidLocalHubAdvertiser(
    private val context: Context,
    private val scope: CoroutineScope
) : LocalHubAdvertiser {
    private var nsdManager: NsdManager? = null
    private var activeJob: Job? = null
    private var registrationListener: NsdManager.RegistrationListener? = null

    private val json = Json {
        encodeDefaults = true
    }

    override fun start(identity: HubIdentity, secret: ByteArray) {
        activeJob?.cancel()
        activeJob = scope.launch {
            stopInternal()

            // SECURITY HYGIENE: Short delay to ensure server is ready
            delay(1000.milliseconds)

            try {
                // 1. Serialize HubIdentity
                val identityJson = json.encodeToString(identity)

                // 2. Encrypt using Organization Secret
                val encryptedBlob =
                    MeshCryptoManager.encrypt(identityJson.encodeToByteArray(), secret)

                // 3. Chunked Delivery: Split encrypted blob into 240-byte strings
                val chunks = encryptedBlob.toChunks(240)

                val instanceName = identity.hubName.replace(" ", "").ifBlank { "CitasHub" }
                val serviceInfo = NsdServiceInfo().apply {
                    serviceName = instanceName
                    serviceType = "_citas-mesh._tcp"
                    port = identity.port

                    chunks.forEachIndexed { index, data ->
                        setAttribute("d${index + 1}", data)
                    }

                    setAttribute("id", "citas_v1")
                }

                registrationListener = object : NsdManager.RegistrationListener {
                    override fun onServiceRegistered(info: NsdServiceInfo) {
                        Napier.i(tag = "Mesh-Hub") { "Mother Hub registered via Android NSD: ${info.serviceName}" }
                    }

                    override fun onRegistrationFailed(info: NsdServiceInfo, errorCode: Int) {
                        Napier.e(tag = "Mesh-Hub") { "Android NSD registration failed: $errorCode" }
                    }

                    override fun onServiceUnregistered(info: NsdServiceInfo) {
                        Napier.v(tag = "Mesh-Hub") { "Android NSD service unregistered" }
                    }

                    override fun onUnregistrationFailed(info: NsdServiceInfo, errorCode: Int) {
                        Napier.e(tag = "Mesh-Hub") { "Android NSD unregistration failed: $errorCode" }
                    }
                }

                nsdManager = (context.getSystemService(Context.NSD_SERVICE) as NsdManager).apply {
                    registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, registrationListener)
                }
            } catch (e: Exception) {
                Napier.e(
                    tag = "Mesh-Hub",
                    throwable = e
                ) { "Failed to start Android Mother Hub advertiser" }
            }
        }
    }

    override fun stop() {
        activeJob?.cancel()
        scope.launch { stopInternal() }
    }

    private fun stopInternal() {
        registrationListener?.let {
            nsdManager?.unregisterService(it)
        }
        registrationListener = null
        nsdManager = null
        Napier.i(tag = "Mesh-Hub") { "Mother Hub advertisement stopped" }
    }
}
