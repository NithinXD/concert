package com.example.helloworld

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "bookings.db"
        private const val DATABASE_VERSION = 1
        private const val TABLE_BOOKINGS = "bookings"
        
        // Columns
        private const val COLUMN_ID = "id"
        private const val COLUMN_EVENT_NAME = "event_name"
        private const val COLUMN_EVENT_DATE = "event_date"
        private const val COLUMN_EVENT_TIME = "event_time"
        private const val COLUMN_VENUE = "venue"
        private const val COLUMN_PRICE = "price"
        private const val COLUMN_BOOKING_TIME = "booking_time"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createTableQuery = """
            CREATE TABLE $TABLE_BOOKINGS (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_EVENT_NAME TEXT,
                $COLUMN_EVENT_DATE TEXT,
                $COLUMN_EVENT_TIME TEXT,
                $COLUMN_VENUE TEXT,
                $COLUMN_PRICE REAL,
                $COLUMN_BOOKING_TIME TEXT
            )
        """.trimIndent()
        
        db.execSQL(createTableQuery)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_BOOKINGS")
        onCreate(db)
    }
    
    fun saveBooking(event: Event, bookingTime: String): Long {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_EVENT_NAME, event.name)
            put(COLUMN_EVENT_DATE, event.date)
            put(COLUMN_EVENT_TIME, event.time)
            put(COLUMN_VENUE, event.venue)
            put(COLUMN_PRICE, event.price)
            put(COLUMN_BOOKING_TIME, bookingTime)
        }
        
        val id = db.insert(TABLE_BOOKINGS, null, values)
        db.close()
        return id
    }
    
    fun getAllBookings(): List<BookingRecord> {
        val bookingsList = mutableListOf<BookingRecord>()
        val selectQuery = "SELECT * FROM $TABLE_BOOKINGS ORDER BY $COLUMN_BOOKING_TIME DESC"
        val db = this.readableDatabase
        
        val cursor = db.rawQuery(selectQuery, null)
        
        if (cursor.moveToFirst()) {
            do {
                val eventName = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_EVENT_NAME))
                val eventDate = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_EVENT_DATE))
                val eventTime = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_EVENT_TIME))
                val bookingTime = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_BOOKING_TIME))
                
                val booking = BookingRecord(
                    eventName = eventName,
                    date = eventDate,
                    time = eventTime,
                    bookingTime = bookingTime
                )
                
                bookingsList.add(booking)
            } while (cursor.moveToNext())
        }
        
        cursor.close()
        db.close()
        
        return bookingsList
    }
    
    fun clearAllBookings() {
        val db = this.writableDatabase
        db.delete(TABLE_BOOKINGS, null, null)
        db.close()
    }
}