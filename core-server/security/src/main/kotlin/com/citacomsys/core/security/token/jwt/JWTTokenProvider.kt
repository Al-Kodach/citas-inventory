package com.citacomsys.core.security.token.jwt

import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.interfaces.DecodedJWT
import java.util.Date


/**
 * A generic, low-level JWT provider. Its only job is to sign and verify tokens
 * based on a specific configuration. It is the reusable engine.
 */
class JWTTokenProvider(val config: TokenConfig) {
    private val algorithm = Algorithm.HMAC512(config.secret)
    val verifier: JWTVerifier = JWT.require(algorithm)
        .withAudience(config.audience)
        .withIssuer(config.issuer)
        .acceptLeeway(5L)
        .build()

    /**
     * Creates and signs a new JWT with the given claims.
     * @param claims clams to generate token with
     * @param durationMillis a option to modify the token exp time, useful for generating act
     */
    fun generate(
        claims: Map<String, Any>,
        durationMillis: Long? = null
    ): String {

        val expireAt = System.currentTimeMillis() +
                (durationMillis ?: config.durationMillis)

        val builder = JWT.create()
            .withAudience(config.audience)
            .withIssuer(config.issuer)
            .withExpiresAt(Date(expireAt))

        claims.forEach { (key, value) ->
            when (value) {
                is String -> builder.withClaim(key, value)
                is Boolean -> builder.withClaim(key, value)
                is Int -> builder.withClaim(key, value)
                is Long -> builder.withClaim(key, value)
            }
        }

        return builder.sign(algorithm)
    }

    /**
     * Verifies the signature and claims of a token, returning the decoded JWT.
     * Throws an exception if the token is invalid.
     */
    fun verify(token: String): DecodedJWT {
        return verifier.verify(token)
    }
}