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
    fun searchJson(query: String, jsonString: String): List<Map<String, Any>> {
        val jsonMapType = object : TypeToken<Map<String, Any>>() {}.type
        val jsonData = gson.fromJson<Map<String, Any>>(jsonString, jsonMapType)

        val results = mutableListOf<Map<String, Any>>()

        jsonData.forEach { (_, value) ->
            when (value) {
                is List<*> -> {
                    value.forEach { item ->
                        if (item is Map<*, *>) {
                            // Safe transformation with null handling
                            val itemMap = item.entries
                                .mapNotNull { entry ->
                                    val key = entry.key?.toString() ?: return@mapNotNull null
                                    val value = entry.value ?: return@mapNotNull null
                                    key to value
                                }
                                .toMap()

                            if (itemMap.values.any { it.toString().contains(query, ignoreCase = true) }) {
                                results.add(itemMap)
                            }
                        }
                    }
                }
                is Map<*, *> -> {
                    // Same safe transformation for top-level maps
                    val map = value.entries
                        .mapNotNull { entry ->
                            val key = entry.key?.toString() ?: return@mapNotNull null
                            val value = entry.value ?: return@mapNotNull null
                            key to value
                        }
                        .toMap()

                    if (map.values.any { it.toString().contains(query, ignoreCase = true) }) {
                        results.add(map)
                    }
                }
            }
        }

        return results
    }}