package com.me.gemini.data

// DataLoader.kt
import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.InputStream
import javax.inject.Inject

class DataLoader @Inject constructor(private val context: Context) {
    private val gson = Gson()

    // Load JSON from assets
    fun loadJsonFromAssets(fileName: String): String {
        return context.assets.open(fileName).bufferedReader().use { it.readText() }
    }

    // Search JSON for relevant content
    fun searchJson(query: String, jsonString: String): String {
        val jsonMapType = object : TypeToken<Map<String, Any>>() {}.type
        val jsonData = gson.fromJson<Map<String, Any>>(jsonString, jsonMapType)

        // Simple keyword search - you can enhance this
        val results = mutableListOf<String>()

        jsonData.forEach { (key, value) ->
            when (value) {
                is List<*> -> {
                    value.forEach { item ->
                        if (item is Map<*, *>) {
                            item.values.forEach { field ->
                                if (field.toString().contains(query, ignoreCase = true)) {
                                    results.add(gson.toJson(item))
                                }
                            }
                        }
                    }
                }

                else -> {
                    if (value.toString().contains(query, ignoreCase = true)) {
                        results.add("$key: $value")
                    }
                }
            }
        }

        return if (results.isEmpty()) {
            "No direct matches found in JSON data."
        } else {
            "Found ${results.size} relevant items:\n\n${results.joinToString("\n\n")}"
        }
    }
}