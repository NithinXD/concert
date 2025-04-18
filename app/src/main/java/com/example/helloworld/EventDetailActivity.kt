package com.example.helloworld

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat

class EventDetailActivity : AppCompatActivity() {
    private lateinit var smsManager: SmsManager
    private lateinit var bookingReceiver: BookingBroadcastReceiver

    companion object {
        private const val SMS_PERMISSION_REQUEST_CODE = 101
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_event_detail)

        // Initialize SMS Manager
        smsManager = SmsManager(this)

        // Register booking broadcast receiver
        bookingReceiver = BookingBroadcastReceiver.register(this)

        // Show progress bar
        val progressBar = findViewById<ProgressBar>(R.id.progressBar)
        val mainContent = findViewById<ScrollView>(R.id.scrollView)
        progressBar.visibility = View.VISIBLE
        mainContent.visibility = View.GONE

        // Get event details from intent
        val event = intent.getSerializableExtra("event") as Event
        val selectedTimeSlot = intent.getStringExtra("timeSlot") ?: ""
        val selectedPrice = intent.getDoubleExtra("price", 0.0)

        // Find views
        val imageView = findViewById<ImageView>(R.id.detail_event_image)
        val titleText = findViewById<TextView>(R.id.detail_event_title)
        val artistText = findViewById<TextView>(R.id.detail_artist)
        val dateText = findViewById<TextView>(R.id.detail_date)
        val timeText = findViewById<TextView>(R.id.detail_time)
        val venueText = findViewById<TextView>(R.id.detail_venue)
        val categoryText = findViewById<TextView>(R.id.detail_category)
        val timeSlotText = findViewById<TextView>(R.id.detail_time_slot)
        val priceText = findViewById<TextView>(R.id.detail_price)
        val seatsText = findViewById<TextView>(R.id.detail_available_seats)
        val descriptionText = findViewById<TextView>(R.id.detail_description)

        // Set values
        titleText.text = event.name
        artistText.text = "Artist: ${event.artist}"
        dateText.text = "Date: ${event.date}"
        timeText.text = "Time: ${event.time}"
        venueText.text = "Venue: ${event.venue}"
        categoryText.text = "Category: ${event.category}"
        timeSlotText.text = "Selected Time Slot: $selectedTimeSlot"
        priceText.text = "Price: $${String.format("%.2f", selectedPrice)}"
        seatsText.text = "Available Seats: ${event.availableSeats}"
        descriptionText.text = event.description

        // Set image using local drawable based on event position
        try {
            // Get event position from intent
            val eventPosition = intent.getIntExtra("position", 0)
            val imageResourceName = "event_${eventPosition + 1}" // Add 1 to match image names
            val imageResourceId = resources.getIdentifier(imageResourceName, "drawable", packageName)
            if (imageResourceId != 0) {
                imageView.setImageResource(imageResourceId)
            } else {
                // If specific event image not found, use placeholder
                imageView.setImageResource(R.drawable.placeholder_image)
            }
        } catch (e: Exception) {
            // Handle image loading error
            imageView.setImageResource(R.drawable.placeholder_image)
        } finally {
            // Hide progress bar and show content
            progressBar.visibility = View.GONE
            mainContent.visibility = View.VISIBLE
        }

        // Set up booking button
        val bookButton = findViewById<Button>(R.id.book_button)
        bookButton.setOnClickListener {
            // Check if event was recently booked before showing confirmation dialog
            bookingReceiver.checkAndConfirmRebooking(this, event) {
                showBookingConfirmationDialog(event)
            }
        }

        // Set up WhatsApp share button
        val shareWhatsAppButton = findViewById<Button>(R.id.share_whatsapp_button)
        shareWhatsAppButton.setOnClickListener {
            shareEventOnWhatsApp(event)
        }

        // Set up show location button
        val showLocationButton = findViewById<Button>(R.id.show_location_button)
        showLocationButton.setOnClickListener {
            val intent = Intent(this, EventMapActivity::class.java).apply {
                putExtra("eventName", event.name)
                putExtra("eventVenue", event.venue)
                // For demo purposes, using example coordinates - in real app these would come from the event data
                putExtra("eventLatitude", 48.8583) // Example: Eiffel Tower latitude
                putExtra("eventLongitude", 2.2944) // Example: Eiffel Tower longitude
            }
            startActivity(intent)
        }
    }

    private fun showBookingConfirmationDialog(event: Event) {
        AlertDialog.Builder(this)
            .setTitle("Confirm Booking")
            .setMessage("Would you like to book ${event.name}?")
            .setPositiveButton("Yes") { _, _ ->
                // Handle booking confirmation
                processBooking()
            }
            .setNegativeButton("No", null)
            .show()
    }

    /**
     * Share event details via WhatsApp
     */
    private fun shareEventOnWhatsApp(event: Event) {
        try {
            // Create event details message
            val message = """
                Check out this event!

                ${event.name}
                Artist: ${event.artist}
                Date: ${event.date}
                Time: ${event.time}
                Venue: ${event.venue}
                Category: ${event.category}
                Price: $${String.format("%.2f", event.price)}

                ${event.description}
            """.trimIndent()

            // Create WhatsApp intent
            val intent = Intent(Intent.ACTION_SEND)
            intent.type = "text/plain"
            intent.setPackage("com.whatsapp")
            intent.putExtra(Intent.EXTRA_TEXT, message)

            // Start WhatsApp or show chooser if multiple WhatsApp versions are installed
            startActivity(Intent.createChooser(intent, "Share via"))
        } catch (e: Exception) {
            // WhatsApp not installed or other error
            Toast.makeText(
                this,
                "WhatsApp not installed or something went wrong",
                Toast.LENGTH_SHORT
            ).show()

            // Fallback to regular share if WhatsApp is not available
            val fallbackIntent = Intent(Intent.ACTION_SEND)
            fallbackIntent.type = "text/plain"
            fallbackIntent.putExtra(Intent.EXTRA_TEXT,
                "Check out ${event.name} on ${event.date} at ${event.venue}!")
            startActivity(Intent.createChooser(fallbackIntent, "Share via"))
        }
    }

    private fun processBooking() {
        // Show progress bar and disable book button
        val progressBar = findViewById<ProgressBar>(R.id.progressBar)
        val bookButton = findViewById<Button>(R.id.book_button)
        progressBar.visibility = View.VISIBLE
        bookButton.isEnabled = false

        // Simulate network delay (remove in production)
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            confirmBooking()
            progressBar.visibility = View.GONE
            bookButton.isEnabled = true
        }, 2000) // 2 second delay
    }

    private fun confirmBooking() {
        val event = intent.getSerializableExtra("event") as Event
        val currentTime = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
            .format(java.util.Date())

        // Save booking to SharedPreferences history (legacy)
        val bookingHistory = BookingHistory(this)
        bookingHistory.addBooking(event)

        // Save booking to SQLite database
        val dbHelper = DatabaseHelper(this)
        dbHelper.saveBooking(event, currentTime)

        // Save booking to Firestore
        val firestoreManager = FirestoreManager()
        // For demo purposes, using a fixed user ID - in a real app, this would come from authentication
        val userId = "user_" + android.provider.Settings.Secure.getString(
            contentResolver,
            android.provider.Settings.Secure.ANDROID_ID
        )

        firestoreManager.saveBooking(event, userId) { success ->
            runOnUiThread {
                if (success) {
                    // Show booking confirmation notification using NotificationManager
                    val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

                    val notification = NotificationCompat.Builder(this, MainActivity.CHANNEL_ID)
                        .setContentTitle("Booking Confirmed")
                        .setContentText("Your booking for ${event.name} on ${event.date} at ${event.time} has been confirmed!")
                        .setStyle(NotificationCompat.BigTextStyle()
                            .bigText("Your booking for ${event.name}\nDate: ${event.date}\nTime: ${event.time}\nVenue: ${event.venue}\nhas been successfully confirmed!"))
                        .setSmallIcon(android.R.drawable.ic_dialog_info)
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setAutoCancel(true)
                        .build()

                    notificationManager.notify(MainActivity.NOTIFICATION_ID, notification)

                    // Send SMS confirmation
                    if (smsManager.hasSmsPermission()) {
                        // Send SMS to the predefined number (+918667488608)
                        smsManager.sendBookingConfirmationSms(event)
                    } else {
                        // Request SMS permission if not granted
                        smsManager.requestSmsPermission(this@EventDetailActivity)
                        Toast.makeText(
                            this@EventDetailActivity,
                            "SMS permission required to send booking confirmation",
                            Toast.LENGTH_LONG
                        ).show()
                    }

                    // Broadcast that this event has been booked
                    val bookingIntent = Intent(BookingBroadcastReceiver.ACTION_EVENT_BOOKED).apply {
                        putExtra(BookingBroadcastReceiver.EXTRA_EVENT_ID, event.id)
                        putExtra(BookingBroadcastReceiver.EXTRA_EVENT_NAME, event.name)
                    }
                    sendBroadcast(bookingIntent)

                    // Show success message
                    Toast.makeText(this, "Booking confirmed and saved to cloud!", Toast.LENGTH_SHORT).show()
                } else {
                    // Show error message for Firestore, but the booking is still saved locally
                    Toast.makeText(this, "Booking saved locally, but failed to sync to cloud", Toast.LENGTH_LONG).show()
                }

                // Close the detail activity
                finish()
            }
        }
    }

    /**
     * Handle permission request results
     */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == SMS_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, try to send SMS again
                val event = intent.getSerializableExtra("event") as? Event
                if (event != null) {
                    smsManager.sendBookingConfirmationSms(event)
                }
            } else {
                // Permission denied
                Toast.makeText(
                    this,
                    "SMS permission denied. Booking confirmation SMS will not be sent.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Unregister the broadcast receiver
        try {
            unregisterReceiver(bookingReceiver)
        } catch (e: Exception) {
            // Receiver might not be registered
        }
    }
}