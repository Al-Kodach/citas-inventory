package com.citacomsys.core.security.mfa


enum class OtpPurpose {
    USER_VERIFICATION,
    ACCOUNT_VERIFICATION,
    MFA_CHALLENGE,
    RESET_PASSWORD,
    ENABLE_MFA,
    WORKSPACE_BINDING,
}