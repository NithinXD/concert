package com.example.helloworld

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import java.text.SimpleDateFormat
import java.util.*

class FirestoreManager {
    private val db: FirebaseFirestore = com.google.firebase.ktx.Firebase.firestore
    private val COLLECTION_BOOKINGS = "bookings"
    
    fun saveBooking(event: Event, userId: String, callback: (Boolean) -> Unit) {
        val currentTime = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            .format(Date())
            
        val booking = hashMapOf(
            "userId" to userId,
            "eventName" to event.name,
            "eventDate" to event.date,
            "eventTime" to event.time,
            "venue" to event.venue,
            "price" to event.price,
            "artist" to event.artist,
            "category" to event.category,
            "bookingTime" to currentTime,
            "timestamp" to com.google.firebase.Timestamp.now()
        )
        
        db.collection(COLLECTION_BOOKINGS)
            .add(booking)
            .addOnSuccessListener { documentReference ->
                Log.d(TAG, "Booking saved with ID: ${documentReference.id}")
                callback(true)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error saving booking", e)
                callback(false)
            }
    }
    
    fun getUserBookings(userId: String, callback: (List<Map<String, Any>>) -> Unit) {
        db.collection(COLLECTION_BOOKINGS)
            .whereEqualTo("userId", userId)
            .orderBy("timestamp")
            .get()
            .addOnSuccessListener { documents ->
                val bookings = mutableListOf<Map<String, Any>>()
                for (document in documents) {
                    bookings.add(document.data)
                }
                callback(bookings)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error getting user bookings", e)
                callback(emptyList())
            }
    }
    
    companion object {
        private const val TAG = "FirestoreManager"
    }
}