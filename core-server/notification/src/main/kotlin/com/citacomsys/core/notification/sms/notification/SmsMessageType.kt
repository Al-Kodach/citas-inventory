package com.citacomsys.core.notification.sms.notification

enum class SmsMessageType {
    USER_WELCOME,
    APPOINTMENT_REMINDER,
    PASSWORD_RESET_CONFIRMATION
}

typealias MessageContext = Map<String, String>
