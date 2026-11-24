package com.citacomsys.core.notification.sms.notification

import com.citacomsys.types.User


internal class SmsMessageBuilder(
//    private val appointmentRepository: AppointmentRepository
) {
    suspend fun build(type: SmsMessageType, user: User, context: MessageContext): String {
        return when (type) {
            SmsMessageType.USER_WELCOME -> {
                "Welcome to the platform, ${user.name}!"
            }
            SmsMessageType.APPOINTMENT_REMINDER -> {
                // Get additional context needed for this specific message category
                val appointmentId = context["appointmentId"] ?: throw IllegalArgumentException("Missing appointmentId")
//                val appointment = appointmentRepository.find(appointmentId).getOrThrow()
                "Hi ${user.name}, this is a reminder for your appointment at $ {appointment.startTime}."
            }

            SmsMessageType.PASSWORD_RESET_CONFIRMATION -> "Password reset succesful"
        }
    }
}