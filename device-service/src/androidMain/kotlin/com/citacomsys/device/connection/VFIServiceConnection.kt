//package com.citacomsys.device.connection
//
//import android.content.ComponentName
//import android.content.Context
//import android.content.Intent
//import android.content.ServiceConnection
//import android.os.IBinder
//import com.citacomsys.device.connection.DeviceServiceConnection
//import com.citacomsys.device.connection.DeviceServiceConnectionState
//import com.vfi.smartpos.deviceservice.aidl.IDeviceService
//import io.github.aakira.napier.Napier
//import kotlinx.coroutines.flow.MutableStateFlow
//import kotlinx.coroutines.flow.StateFlow
//import kotlinx.coroutines.flow.asStateFlow
//import org.koin.core.Koin
//import org.koin.core.module.Module
//import org.koin.dsl.module
//
//internal class VFIServiceConnection(
//    private val context: Context,
//    private val koin: Koin
//): DeviceServiceConnection, ServiceConnection {
//
//    private var iDeviceService: IDeviceService? = null
//
//    val iDeviceModule: Module
//        get () = module {
//            single { iDeviceService }
//        }
//
//    private val _connectionState = MutableStateFlow(DeviceServiceConnectionState.DISCONNECTED)
//
//    override val connectionState: StateFlow<DeviceServiceConnectionState> = _connectionState.asStateFlow()
//
//    override fun bind() {
//        if (connectionState.value != DeviceServiceConnectionState.DISCONNECTED || iDeviceService != null) {
//            Napier.d { "Service is already connected or connecting. Ignoring bind request." }
//            return
//        }
//
//        Napier.d { "Attempting to bind to the device service..." }
//        _connectionState.value = DeviceServiceConnectionState.CONNECTING
//
//        val intent = Intent().apply {
//            action = "com.vfi.smartpos.device_service"
//            setPackage("com.vfi.smartpos.deviceservice")
//        }
//
//        val isSuccess = context.bindService(intent, this, Context.BIND_AUTO_CREATE)
//
//        if (!isSuccess) {
//            Napier.e("bindService returned false. The service may not be installed or available.")
//            _connectionState.value = DeviceServiceConnectionState.DISCONNECTED
//        } else {
//            Napier.d("bindService returned true. Service bound successfully.")
//        }
//    }
//
//    override fun unbind() {
//        if (connectionState.value != DeviceServiceConnectionState.DISCONNECTED) {
//            Napier.d { "Unbinding from the device service." }
//            context.unbindService(this)
//            iDeviceService = null
//            _connectionState.value = DeviceServiceConnectionState.DISCONNECTED
//            koin.unloadModules(listOf(iDeviceModule))
//        }
//    }
//
//    override fun onBindingDied(ID: ComponentName?) {
//        Napier.e("Device service binding died. Reconnecting...")
//        _connectionState.value = DeviceServiceConnectionState.DISCONNECTED
//        bind()
//    }
//
//    override fun onServiceConnected(ID: ComponentName?, service: IBinder?) {
//        iDeviceService = IDeviceService.Stub.asInterface(service)
//        _connectionState.value = DeviceServiceConnectionState.CONNECTED
//        Napier.i("Device service connected successfully.")
//        koin.loadModules(listOf(iDeviceModule))
//    }
//
//    override fun onServiceDisconnected(ID: ComponentName?) {
//        Napier.w("Device service disconnected unexpectedly.")
//        iDeviceService = null
//        _connectionState.value = DeviceServiceConnectionState.DISCONNECTED
//        koin.unloadModules(listOf(iDeviceModule))
//    }
//}