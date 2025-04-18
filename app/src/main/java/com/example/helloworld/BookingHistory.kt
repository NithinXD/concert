package com.example.helloworld

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.text.SimpleDateFormat
import java.util.*

data class BookingRecord(
    val eventName: String,
    val date: String,
    val time: String,
    val bookingTime: String
)

class BookingHistory(context: Context) {
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences(
        "booking_history",
        Context.MODE_PRIVATE
    )
    private val gson = Gson()

    fun addBooking(event: Event) {
        val bookings = getBookings().toMutableList()
        val currentTime = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            .format(Date())
        
        val newBooking = BookingRecord(
            eventName = event.name,
            date = event.date,
            time = event.time,
            bookingTime = currentTime
        )
        
        bookings.add(0, newBooking) // Add to the beginning of the list
        
        // Keep only the last 10 bookings
        val trimmedBookings = if (bookings.size > 10) bookings.take(10) else bookings
        
        sharedPreferences.edit().apply {
            putString("bookings", gson.toJson(trimmedBookings))
            apply()
        }
    }

    fun getBookings(): List<BookingRecord> {
        val json = sharedPreferences.getString("bookings", "[]")
        val type = object : TypeToken<List<BookingRecord>>() {}.type
        return gson.fromJson(json, type) ?: emptyList()
    }

    fun clearBookings() {
        sharedPreferences.edit().apply {
            remove("bookings")
            apply()
        }
    }
}