package com.example.helloworld

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.app.NotificationChannel
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import android.app.NotificationManager
import androidx.core.app.NotificationCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.widget.ImageView
import android.widget.TextView
import android.graphics.Color
import java.util.Calendar

class MainActivity : AppCompatActivity() {
    companion object {
        private const val MENU_DATE_FILTER = 100
        private const val MENU_TIME_FILTER = 101
        const val NOTIFICATION_ID = 1
        const val CHANNEL_ID = "booking_channel"
    }
    private lateinit var eventAdapter: EventAdapter
    private lateinit var bookingHistory: BookingHistory
    private lateinit var databaseHelper: DatabaseHelper
    private lateinit var searchHistoryManager: SearchHistoryManager
    private var allEvents = mutableListOf<Event>()
    private var selectedDate: String? = null
    private var selectedStartTime: String? = null
    private var selectedEndTime: String? = null

    override fun onBackPressed() {
        AlertDialog.Builder(this)
            .setTitle("Exit App")
            .setMessage("Are you sure you want to exit?")
            .setCancelable(true)
            .setPositiveButton("Yes") { _, _ ->
                super.onBackPressed()
            }
            .setNegativeButton("No", null)
            .show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        createNotificationChannel()

        // Initialize our managers and helpers
        bookingHistory = BookingHistory(this)
        databaseHelper = DatabaseHelper(this)
        searchHistoryManager = SearchHistoryManager(this)

        // Set up toolbar
        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        // Initialize sample data
        allEvents = createSampleEvents()

        // Set up RecyclerView
        val recyclerView = findViewById<RecyclerView>(R.id.recyclerView)
        eventAdapter = EventAdapter(allEvents)
        recyclerView.adapter = eventAdapter
        recyclerView.layoutManager = LinearLayoutManager(this)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.options_menu, menu)

        // Add date filter menu item
        menu.add(Menu.NONE, MENU_DATE_FILTER, Menu.NONE, "Filter by Date")
            .setIcon(android.R.drawable.ic_menu_my_calendar)
            .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)

        // Add time filter menu item
        menu.add(Menu.NONE, MENU_TIME_FILTER, Menu.NONE, "Filter by Time")
            .setIcon(android.R.drawable.ic_menu_recent_history)
            .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)

        val searchItem = menu.findItem(R.id.action_search)
        val searchView = searchItem.actionView as SearchView

        // Make search icon and text black
        val searchIcon = searchView.findViewById<ImageView>(androidx.appcompat.R.id.search_mag_icon)
        searchIcon.setColorFilter(Color.BLACK)

        val searchText = searchView.findViewById<TextView>(androidx.appcompat.R.id.search_src_text)
        searchText.setTextColor(Color.BLACK)
        searchText.setHintTextColor(Color.GRAY)

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                if (!query.isNullOrBlank()) {
                    // Save search query to history
                    searchHistoryManager.addSearchQuery(query)
                }
                filterEvents(query)
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                filterEvents(newText)
                return true
            }
        })

        // Show search history when search is clicked
        searchView.setOnSearchClickListener {
            showSearchHistory(searchView)
        }

        return true
    }

    private fun filterEvents(query: String?) {
        var filteredList = allEvents.toList()

        // Apply text search filter
        if (!query.isNullOrBlank()) {
            filteredList = filteredList.filter { event ->
                event.name.contains(query, ignoreCase = true) ||
                event.description.contains(query, ignoreCase = true) ||
                event.artist.contains(query, ignoreCase = true) ||
                event.category.contains(query, ignoreCase = true) ||
                event.venue.contains(query, ignoreCase = true)
            }
        }

        // Apply date filter
        if (selectedDate != null) {
            filteredList = filteredList.filter { event ->
                event.date == selectedDate
            }
        }

        // Apply time filter
        if (selectedStartTime != null && selectedEndTime != null) {
            filteredList = filteredList.filter { event ->
                val eventTime = event.time
                eventTime >= selectedStartTime!! && eventTime <= selectedEndTime!!
            }
        }
        android.util.Log.d("DateFilter", "Final filtered list size: ${filteredList.size}")
        eventAdapter.updateEvents(filteredList)
    }

    override  fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            MENU_DATE_FILTER -> showDatePicker()
            MENU_TIME_FILTER -> showTimeRangePicker()
            R.id.action_history -> showBookingHistory()
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }

    private fun showBookingHistory() {
        // Get bookings from SQLite instead of SharedPreferences
        val bookings = databaseHelper.getAllBookings()
        if (bookings.isEmpty()) {
            AlertDialog.Builder(this)
                .setTitle("Booking History")
                .setMessage("No bookings found")
                .setPositiveButton("OK", null)
                .show()
            return
        }

        val historyText = bookings.joinToString(separator = "\n\n") { booking ->
            """
            Event: ${booking.eventName}
            Date: ${booking.date}
            Time: ${booking.time}
            Booked on: ${booking.bookingTime}
            """.trimIndent()
        }

        AlertDialog.Builder(this)
            .setTitle("Booking History")
            .setMessage(historyText)
            .setPositiveButton("OK", null)
            .setNeutralButton("Clear History") { _, _ ->
                databaseHelper.clearAllBookings()
                bookingHistory.clearBookings() // Also clear the legacy storage
            }
            .show()
    }

    private fun showSearchHistory(searchView: SearchView) {
        val searchHistory = searchHistoryManager.getSearchHistory()
        if (searchHistory.isEmpty()) return

        val items = searchHistory.toTypedArray()

        val builder = AlertDialog.Builder(this)
        builder.setTitle("Recent Searches")
        builder.setItems(items) { dialog, which ->
            // When a search history item is clicked, set it as the search query
            val selectedQuery = items[which]
            searchView.setQuery(selectedQuery, true)
            dialog.dismiss()
        }

        // Create a custom view for each item with an X button
        val adapter = object : android.widget.ArrayAdapter<String>(
            this,
            android.R.layout.simple_list_item_1,
            items
        ) {
            override fun getView(position: Int, convertView: android.view.View?, parent: android.view.ViewGroup): android.view.View {
                val view = super.getView(position, convertView, parent)
                val textView = view.findViewById<TextView>(android.R.id.text1)

                // Add X button to the right of each item
                textView.setCompoundDrawablesWithIntrinsicBounds(0, 0, android.R.drawable.ic_menu_close_clear_cancel, 0)
                textView.compoundDrawablePadding = 16

                // Handle click on the X button
                textView.setOnTouchListener { v, event ->
                    if (event.action == android.view.MotionEvent.ACTION_UP) {
                        val drawableRight = textView.compoundDrawables[2]
                        if (drawableRight != null && event.rawX >= (textView.right - drawableRight.bounds.width())) {
                            // X button clicked, remove this item from history
                            searchHistoryManager.removeSearchQuery(items[position])
                            this.notifyDataSetChanged()
                            if (searchHistoryManager.getSearchHistory().isEmpty()) {
                                builder.create().dismiss()
                            }
                            return@setOnTouchListener true
                        }
                    }
                    false
                }

                return view
            }
        }

        builder.setAdapter(adapter) { dialog, which ->
            val selectedQuery = items[which]
            searchView.setQuery(selectedQuery, true)
        }

        builder.setNegativeButton("Clear All") { _, _ ->
            searchHistoryManager.clearSearchHistory()
        }

        builder.show()
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        DatePickerDialog(
            this,
            { _, year, month, day ->
                selectedDate = String.format("%04d-%02d-%02d", year, month + 1, day)
                filterEvents(null)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun showTimeRangePicker() {
        // Show start time picker
        TimePickerDialog(
            this,
            { _, hourOfDay, minute ->
                selectedStartTime = String.format("%02d:%02d", hourOfDay, minute)
                // After selecting start time, show end time picker
                showEndTimePicker()
            },
            0, 0, true
        ).show()
    }

    private fun showEndTimePicker() {
        TimePickerDialog(
            this,
            { _, hourOfDay, minute ->
                selectedEndTime = String.format("%02d:%02d", hourOfDay, minute)
                filterEvents(null)
            },
            23, 59, true
        ).show()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = getString(R.string.channel_name)
            val descriptionText = getString(R.string.channel_description)
            val importance = android.app.NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showBookingConfirmation(event: Event) {
        // Save the booking to history
        bookingHistory.addBooking(event)

        // Show notification
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Booking Confirmed")
            .setContentText("Your event booking has been successfully confirmed!")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun createSampleEvents(): MutableList<Event> {
        return mutableListOf(
            Event(
                name = "Rock Festival 2024",
                artist = "Various Artists",
                date = "2025-02-02",
                time = "4:00",
                venue = "Central Stadium",
                price = 89.99,
                imageUrl = "https://example.com/rock.jpg",
                category = "Rock",
                availableSeats = 5000,
                description = "Annual rock music festival featuring top bands"
            ),
            Event(
                name = "Classical Night",
                artist = "City Symphony Orchestra",
                date = "2025-03-03",
                time = "19:30",
                venue = "Symphony Hall",
                price = 59.99,
                imageUrl = "https://example.com/classical.jpg",
                category = "Classical",
                availableSeats = 2000,
                description = "An evening of classical masterpieces"
            ),
            Event(
                name = "Jazz in the Park",
                artist = "Jazz Ensemble",
                date = "2025-02-25",
                time = "17:00",
                venue = "City Park",
                price = 29.99,
                imageUrl = "https://example.com/jazz.jpg",
                category = "Jazz",
                availableSeats = 3000,
                description = "Open-air jazz concert"
            )
        )
    }
}