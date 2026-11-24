package com.citacomsys.auth.usecase

import com.citacomsys.core.security.service.ActionTokenIssuer
import com.citacomsys.core.security.token.jwt.UserPrincipal
import com.citacomsys.auth.service.UserAuthenticationService
import com.citacomsys.types.ActionConfirmationPurpose
import com.citacomsys.types.ActionToken
import mu.KotlinLogging

internal class GenerateActUseCase(
    private val actionTokenIssuer: ActionTokenIssuer,
    private val authenticationService: UserAuthenticationService
) {

    private val logger = KotlinLogging.logger { }

    suspend fun execute(
        command: Command,
    ): Result<ActionToken> = runCatching {

        logger.debug { "Generating Action token..." }

       val user = authenticationService.execute(
            identity = command.identity,
            command.password
        )

        val token = actionTokenIssuer.issue(
            userId = user.id,
            organizationId = user.organizationId,
            workspaceId = command.workspaceId,
            username = user.username,
            role = user.role,
            sessionId = command.sessionId,
            purpose = command.purpose
        )

        ActionToken(token)
    }

    data class Command(
        val password: String,
        val identity: String,
        val workspaceId: String?,
        val sessionId: String,
        val purpose: ActionConfirmationPurpose
    )
}