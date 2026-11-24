package com.citacomsys.core.security

import com.citacomsys.core.security.plugin.isTailscaleIp
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class WorkspaceSecurityAuditTest {

    @Test
    fun testTailscaleIpValidationLogic() {
        assertTrue(isTailscaleIp("100.64.0.1"))
        assertTrue(isTailscaleIp("127.0.0.1"))
        assertTrue(isTailscaleIp("192.168.1.50"))
        assertFalse(isTailscaleIp("8.8.8.8"))
    }
}
