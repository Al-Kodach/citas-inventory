package com.citacomsys.core.mesh.security

expect object ActiveRoutingChainHolder {
    fun get(): String?
    fun set(chainJson: String?)
}
