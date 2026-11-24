package com.citacomsys.core.mesh.security

actual object ActiveRoutingChainHolder {
    private val activeChain = ThreadLocal<String?>()

    actual fun get(): String? = activeChain.get()
    actual fun set(chainJson: String?) {
        if (chainJson != null) activeChain.set(chainJson)
        else activeChain.remove()
    }
}
