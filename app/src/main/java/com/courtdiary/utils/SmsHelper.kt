package com.courtdiary.utils

import android.content.Context
import android.os.Build
import android.telephony.SmsManager

object SmsHelper {

    /**
     * Sends a hearing reminder SMS to [phone].
     * Returns true if the send call succeeded (delivery is not guaranteed —
     * network/SIM issues are handled by the system).
     */
    fun sendHearingReminder(
        context: Context,
        phone: String,
        clientName: String,
        caseNumber: String,
        courtName: String
    ): Boolean {
        return try {
            // Clean the number — keep digits and leading +
            val cleaned = phone.trim().replace(Regex("[\\s\\-()]"), "")
            if (cleaned.length < 7) return false   // sanity check

            val name  = clientName.ifBlank { "Client" }
            val court = if (courtName.isNotBlank()) " at $courtName" else ""
            val message = "Dear $name, this is a reminder that your court hearing for " +
                    "Case $caseNumber$court is scheduled for tomorrow. " +
                    "Please be prepared. - Court Diary"

            val smsManager: SmsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                context.getSystemService(SmsManager::class.java)
            } else {
                @Suppress("DEPRECATION")
                SmsManager.getDefault()
            }

            // divideMessage handles messages longer than 160 characters automatically
            val parts = smsManager.divideMessage(message)
            smsManager.sendMultipartTextMessage(cleaned, null, parts, null, null)
            true
        } catch (e: Exception) {
            false
        }
    }
}
