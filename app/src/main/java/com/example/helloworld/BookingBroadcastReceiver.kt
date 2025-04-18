package com.example.helloworld

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

/**
 * BroadcastReceiver to prevent multiple bookings of the same event
 */
class BookingBroadcastReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_EVENT_BOOKED = "com.example.helloworld.EVENT_BOOKED"
        const val EXTRA_EVENT_ID = "event_id"
        const val EXTRA_EVENT_NAME = "event_name"
        
        // Map to track booked events (eventId -> timestamp)
        private val bookedEvents = mutableMapOf<String, Long>()
        
        // Time window for considering a booking as "recent" (in milliseconds)
        private const val RECENT_BOOKING_WINDOW = 24 * 60 * 60 * 1000 // 24 hours
        
        /**
         * Register the receiver
         */
        fun register(context: Context): BookingBroadcastReceiver {
            val receiver = BookingBroadcastReceiver()
            context.registerReceiver(receiver, IntentFilter(ACTION_EVENT_BOOKED))
            return receiver
        }
        
        /**
         * Check if an event was recently booked
         */
        fun wasRecentlyBooked(eventId: String): Boolean {
            val bookingTime = bookedEvents[eventId] ?: return false
            val currentTime = System.currentTimeMillis()
            return (currentTime - bookingTime) < RECENT_BOOKING_WINDOW
        }
        
        /**
         * Record a booking
         */
        fun recordBooking(eventId: String) {
            bookedEvents[eventId] = System.currentTimeMillis()
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_EVENT_BOOKED) {
            val eventId = intent.getStringExtra(EXTRA_EVENT_ID) ?: return
            val eventName = intent.getStringExtra(EXTRA_EVENT_NAME) ?: "this event"
            
            // Record the booking
            recordBooking(eventId)
            
            // Show toast notification
            Toast.makeText(
                context,
                "You've booked $eventName. This will be remembered to prevent duplicate bookings.",
                Toast.LENGTH_LONG
            ).show()
        }
    }
    
    /**
     * Check if the event was recently booked and show confirmation dialog if needed
     * @return true if booking should proceed, false if it should be blocked
     */
    fun checkAndConfirmRebooking(activity: AppCompatActivity, event: Event, onProceed: () -> Unit) {
        if (wasRecentlyBooked(event.id)) {
            // Show confirmation dialog for rebooking
            AlertDialog.Builder(activity)
                .setTitle("Already Booked")
                .setMessage("You've already booked ${event.name} recently. Are you sure you want to book it again?")
                .setPositiveButton("Yes, Book Again") { _, _ ->
                    onProceed()
                }
                .setNegativeButton("Cancel", null)
                .show()
        } else {
            // Proceed with booking
            onProceed()
        }
    }
}