package com.citacomsys.test.server

import com.citacomsys.core.security.plugin.TailscaleSecurity
import com.citacomsys.core.security.plugin.isTailscaleIp
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse

class TailscaleSecurityTest {

    @Test
    fun `public endpoints are always allowed`() = testApplication {
        application {
            install(TailscaleSecurity)
            routing {
                get("/auth/test") { call.respond(HttpStatusCode.OK, "public") }
                get("/onboarding/test") { call.respond(HttpStatusCode.OK, "public") }
            }
        }

        val client = createClient {}
        
        assertEquals(HttpStatusCode.OK, client.get("/auth/test").status)
        assertEquals(HttpStatusCode.OK, client.get("/onboarding/test").status)
    }

    @Test
    fun `isTailscaleIp logic correctly identifies ranges`() {
        assertTrue(isTailscaleIp("100.64.0.1"), "100.64.0.1 should be Tailscale")
        assertFalse(isTailscaleIp("192.168.1.1"), "192.168.1.1 should NOT be Tailscale")
        assertTrue(isTailscaleIp("127.0.0.1"), "127.0.0.1 should be allowed (loopback)")
        assertTrue(isTailscaleIp("100.64.0.0"), "100.64.0.0 is lower bound")
        assertTrue(isTailscaleIp("100.127.255.255"), "100.127.255.255 is upper bound")
        assertFalse(isTailscaleIp("100.128.0.0"), "100.128.0.0 is outside range")
    }
}
