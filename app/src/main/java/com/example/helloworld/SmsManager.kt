package com.example.helloworld

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.telephony.SmsManager
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

/**
 * Utility class to handle SMS operations
 */
class SmsManager(private val context: Context) {

    companion object {
        private const val SMS_PERMISSION_REQUEST_CODE = 101
        private const val DEFAULT_PHONE_NUMBER = "+918667488608" // Default phone number for booking notifications
    }

    /**
     * Check if the app has SMS permission
     * @return true if permission is granted, false otherwise
     */
    fun hasSmsPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.SEND_SMS
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Request SMS permission
     * @param activity The activity requesting the permission
     */
    fun requestSmsPermission(activity: Activity) {
        ActivityCompat.requestPermissions(
            activity,
            arrayOf(Manifest.permission.SEND_SMS),
            SMS_PERMISSION_REQUEST_CODE
        )
    }

    /**
     * Send booking confirmation SMS
     * @param event The event that was booked
     * @param phoneNumber The phone number to send the SMS to (optional, uses default if not provided)
     * @return true if SMS was sent successfully, false otherwise
     */
    fun sendBookingConfirmationSms(event: Event, phoneNumber: String = DEFAULT_PHONE_NUMBER): Boolean {
        if (!hasSmsPermission()) {
            Toast.makeText(
                context,
                "SMS permission not granted. SMS notification not sent.",
                Toast.LENGTH_SHORT
            ).show()
            return false
        }

        try {
            val message = buildBookingConfirmationMessage(event)
            val smsManager = android.telephony.SmsManager.getDefault()
            smsManager.sendTextMessage(phoneNumber, null, message, null, null)
            
            Toast.makeText(
                context,
                "Booking confirmation SMS sent successfully!",
                Toast.LENGTH_SHORT
            ).show()
            return true
        } catch (e: Exception) {
            Toast.makeText(
                context,
                "Failed to send SMS: ${e.message}",
                Toast.LENGTH_SHORT
            ).show()
            return false
        }
    }

    /**
     * Build the booking confirmation message
     * @param event The event that was booked
     * @return The formatted message
     */
    private fun buildBookingConfirmationMessage(event: Event): String {
        return """
            Booking Confirmed!
            Event: ${event.name}
            Date: ${event.date}
            Time: ${event.time}
            Venue: ${event.venue}
            Thank you for your booking!
        """.trimIndent()
    }
}