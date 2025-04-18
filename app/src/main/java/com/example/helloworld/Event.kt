package com.example.helloworld

import java.io.Serializable
data class Event(
    val id: String = "", // Added ID field with default value
    val name: String,
    val artist: String,
    val date: String,
    val time: String,
    val venue: String,
    val price: Double,
    val imageUrl: String,
    val category: String,
    val availableSeats: Int,
    val description: String
) : Serializable
