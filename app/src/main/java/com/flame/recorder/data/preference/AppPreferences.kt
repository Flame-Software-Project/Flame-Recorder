package com.flame.recorder.data.preference

import android.content.Context
import android.content.SharedPreferences

class AppPreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("FlamePrefs", Context.MODE_PRIVATE)

    var storageDirectoryUri: String
        get() = prefs.getString("storage_directory_uri", "") ?: ""
        set(value) = prefs.edit().putString("storage_directory_uri", value).apply()

    var aiPrompt: String
        get() = prefs.getString("ai_prompt", "Please provide a concise and clear bulleted summary of this audio recording.") ?: "Please provide a concise and clear bulleted summary of this audio recording."
        set(value) = prefs.edit().putString("ai_prompt", value).apply()

    var audioQuality: String
        get() = prefs.getString("audio_quality", "HIGH") ?: "HIGH"
        set(value) = prefs.edit().putString("audio_quality", value).apply()

    var audioFormat: String
        get() = prefs.getString("audio_format", "M4A") ?: "M4A"
        set(value) = prefs.edit().putString("audio_format", value).apply()
}