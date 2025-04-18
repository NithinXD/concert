package com.example.helloworld

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.*

class SearchHistoryManager(context: Context) {
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )
    private val gson = Gson()
    
    fun addSearchQuery(query: String) {
        if (query.isBlank()) return
        
        val searches = getSearchHistory().toMutableList()
        
        // Remove if already exists (to avoid duplicates)
        searches.remove(query)
        
        // Add to the beginning of the list
        searches.add(0, query)
        
        // Keep only the most recent MAX_HISTORY_SIZE searches
        val trimmedSearches = if (searches.size > MAX_HISTORY_SIZE) {
            searches.take(MAX_HISTORY_SIZE)
        } else {
            searches
        }
        
        // Save to SharedPreferences
        sharedPreferences.edit().apply {
            putString(KEY_SEARCH_HISTORY, gson.toJson(trimmedSearches))
            apply()
        }
    }
    
    fun getSearchHistory(): List<String> {
        val json = sharedPreferences.getString(KEY_SEARCH_HISTORY, "[]")
        val type = object : TypeToken<List<String>>() {}.type
        return gson.fromJson(json, type) ?: emptyList()
    }
    
    fun removeSearchQuery(query: String) {
        val searches = getSearchHistory().toMutableList()
        if (searches.remove(query)) {
            sharedPreferences.edit().apply {
                putString(KEY_SEARCH_HISTORY, gson.toJson(searches))
                apply()
            }
        }
    }
    
    fun clearSearchHistory() {
        sharedPreferences.edit().apply {
            remove(KEY_SEARCH_HISTORY)
            apply()
        }
    }
    
    companion object {
        private const val PREFS_NAME = "search_history_prefs"
        private const val KEY_SEARCH_HISTORY = "search_history"
        private const val MAX_HISTORY_SIZE = 10
    }
}