package com.flame.recorder.data.ai

import com.flame.recorder.data.preference.AppPreferences
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody // Import standard OkHttp 4 extension function
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class AiManager(private val preferences: AppPreferences) {

    // Configure OkHttpClient with robust 90-second timeouts to sustain large Base64 uploads
    private val client = OkHttpClient.Builder()
        .connectTimeout(90, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .writeTimeout(90, TimeUnit.SECONDS)
        .build()

    private val sttProxyUrl = "https://api-stt.flameproject.net"
    private val summaryProxyUrl = "https://api-summary.flameproject.net"

    suspend fun transcribeAndSummarize(audioFile: File): String {
        val transcription = transcribeAudio(audioFile)
        if (transcription.isBlank()) {
            throw IOException("Transcription returned empty result.")
        }
        return getSummaryFromChat(transcription)
    }

    private suspend fun transcribeAudio(file: File): String = suspendCancellableCoroutine { continuation ->
        val fileBytes = file.readBytes()
        val base64Data = android.util.Base64.encodeToString(fileBytes, android.util.Base64.NO_WRAP)
        val mimeType = if (file.extension.lowercase() == "m4a") "audio/mp4" else "audio/3gpp"

        val jsonBody = JSONObject().apply {
            put("audio_base64", base64Data)
            put("mime_type", mimeType)
        }

        val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
        // Using standard OkHttp 4.x extension function to prevent compile errors
        val requestBody = jsonBody.toString().toRequestBody(mediaType)
        val request = Request.Builder()
            .url(sttProxyUrl)
            .post(requestBody)
            .build()

        val call = client.newCall(request)
        continuation.invokeOnCancellation { call.cancel() }

        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                continuation.resumeWithException(e)
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (!response.isSuccessful) {
                        val errorBody = response.body?.string() ?: ""
                        val errorMessage = if (errorBody.isNotBlank()) errorBody else "HTTP ${response.code}"
                        continuation.resumeWithException(IOException("Proxy STT failed: $errorMessage"))
                        return
                    }
                    val bodyString = response.body?.string() ?: ""
                    try {
                        val json = JSONObject(bodyString)
                        val text = json.optString("text", "")
                        continuation.resume(text)
                    } catch (e: Exception) {
                        continuation.resumeWithException(e)
                    }
                }
            }
        })
    }

    private suspend fun getSummaryFromChat(text: String): String = suspendCancellableCoroutine { continuation ->
        val prompt = preferences.aiPrompt

        val jsonBody = JSONObject().apply {
            put("text", text)
            put("prompt", prompt)
        }

        val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
        // Using standard OkHttp 4.x extension function to prevent compile errors
        val requestBody = jsonBody.toString().toRequestBody(mediaType)
        val request = Request.Builder()
            .url(summaryProxyUrl)
            .post(requestBody)
            .build()

        val call = client.newCall(request)
        continuation.invokeOnCancellation { call.cancel() }

        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                continuation.resumeWithException(e)
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (!response.isSuccessful) {
                        val errorBody = response.body?.string() ?: ""
                        val errorMessage = if (errorBody.isNotBlank()) errorBody else "HTTP ${response.code}"
                        continuation.resumeWithException(IOException("Proxy Summary failed: $errorMessage"))
                        return
                    }
                    val bodyString = response.body?.string() ?: ""
                    try {
                        val json = JSONObject(bodyString)
                        val summary = json.optString("summary", "")
                        continuation.resume(summary)
                    } catch (e: Exception) {
                        continuation.resumeWithException(e)
                    }
                }
            }
        })
    }
}