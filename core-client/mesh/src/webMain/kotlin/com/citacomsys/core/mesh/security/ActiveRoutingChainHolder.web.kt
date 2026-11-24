package com.citacomsys.core.mesh.security

actual object ActiveRoutingChainHolder {
    private var activeChain: String? = null

    actual fun get(): String? = activeChain
    actual fun set(chainJson: String?) {
        activeChain = chainJson
    }
}
