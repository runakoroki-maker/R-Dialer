package com.example.ai

import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class SmartSmsInfo(
    val isAutomated: Boolean,
    val brand: String?,
    val messageType: String?,
    val isVerified: Boolean = false,
    val logoUrl: String? = null,
    val description: String? = null
)

object SmartSmsRecognizer {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val trustedBrands = mapOf(
        "zomato" to Triple("Zomato", "https://logo.clearbit.com/zomato.com", "Online food ordering and restaurant discovery platform."),
        "swiggy" to Triple("Swiggy", "https://logo.clearbit.com/swiggy.com", "Food delivery and quick-commerce service."),
        "hdfc" to Triple("HDFC", "https://logo.clearbit.com/hdfcbank.com", "Banking and financial services provider."),
        "sbi" to Triple("SBI", "https://logo.clearbit.com/sbi.co.in", "Banking and financial services provider."),
        "icici" to Triple("ICICI", "https://logo.clearbit.com/icicibank.com", "Banking and financial services provider."),
        "paytm" to Triple("Paytm", "https://logo.clearbit.com/paytm.com", "Digital payments and financial services."),
        "uber" to Triple("Uber", "https://logo.clearbit.com/uber.com", "Ride-hailing and transportation service."),
        "amazon" to Triple("Amazon", "https://logo.clearbit.com/amazon.com", "E-commerce and cloud services platform."),
        "flipkart" to Triple("Flipkart", "https://logo.clearbit.com/flipkart.com", "E-commerce marketplace."),
        "google" to Triple("Google", "https://logo.clearbit.com/google.com", "Technology and internet services provider."),
        "netflix" to Triple("Netflix", "https://logo.clearbit.com/netflix.com", "Streaming entertainment service.")
    )

    suspend fun analyzeSms(sender: String, body: String): SmartSmsInfo = withContext(Dispatchers.IO) {
        val bodyLower = body.lowercase()
        val senderLower = sender.lowercase()
        val isNumericAddress = sender.all { it.isDigit() || it == '+' || it == '-' || it == ' ' }

        val hasOtpKeyword = bodyLower.contains("otp") || bodyLower.contains("verification") || bodyLower.contains("code") || bodyLower.contains("verify") || bodyLower.contains("log in") || bodyLower.contains("login")
        val hasTransactionKeyword = bodyLower.contains("debited") || bodyLower.contains("credited") || bodyLower.contains("rs.") || bodyLower.contains("₹") || bodyLower.contains("paid") || bodyLower.contains("spent")
        val hasDeliveryKeyword = bodyLower.contains("delivered") || bodyLower.contains("shipped") || bodyLower.contains("order") || bodyLower.contains("out for delivery") || bodyLower.contains("courier")

        // Human messages check: if numeric address and no automated/OTP/transaction/delivery keywords, treat as human message
        if (isNumericAddress && !hasOtpKeyword && !hasTransactionKeyword && !hasDeliveryKeyword) {
            return@withContext SmartSmsInfo(isAutomated = false, brand = null, messageType = null, isVerified = false)
        }

        // Local extraction of brand
        var detectedBrand: String? = null
        var matchedKey: String? = null

        if (!isNumericAddress) {
            val cleanSender = sender.replace(Regex("^(AD-|VM-|JD-|TX-|BP-|BK-|RZ-|IN-)"), "").trim()
            detectedBrand = cleanSender
            for ((key, value) in trustedBrands) {
                if (cleanSender.lowercase().contains(key)) {
                    detectedBrand = value.first
                    matchedKey = key
                    break
                }
            }
        } else {
            for ((key, value) in trustedBrands) {
                if (bodyLower.contains(key) || senderLower.contains(key)) {
                    detectedBrand = value.first
                    matchedKey = key
                    break
                }
            }
        }

        var messageType = "Promotional/System notifications"
        if (hasOtpKeyword || bodyLower.contains("auth") || bodyLower.contains("password")) {
            messageType = "OTP / Authentication"
        } else if (hasTransactionKeyword) {
            messageType = "Transaction"
        } else if (hasDeliveryKeyword) {
            messageType = "Delivery/Service"
        } else if (bodyLower.contains("login") || bodyLower.contains("log in")) {
            messageType = "Login/Security"
        }

        val isAutomated = true
        val isVerified = matchedKey != null || (!isNumericAddress && detectedBrand != null)
        val brandMeta = matchedKey?.let { trustedBrands[it] }
        val logoUrl = brandMeta?.second
        val description = brandMeta?.third ?: (detectedBrand?.let { "Official automated notification sender for $it." })

        // Use Gemini Flash-Lite (gemini-3.1-flash-lite-preview) for precise classification when needed
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (!apiKey.isNullOrBlank() && apiKey != "YOUR_API_KEY") {
            try {
                val prompt = """
                    Analyze this SMS message.
                    Sender: $sender
                    Body: $body
                    
                    Determine if it is automated/system-generated (true/false).
                    If automated, identify:
                    1. Brand/Service name (e.g., Zomato, HDFC, Uber)
                    2. Message type (Choose strictly one: OTP / Authentication, Login/Security, Transaction, Delivery/Service, Promotional/System notifications).
                    
                    Return ONLY valid JSON format:
                    {"isAutomated": true, "brand": "Name", "messageType": "Category"}
                """.trimIndent()

                val jsonBody = JSONObject().apply {
                    put("contents", org.json.JSONArray().put(
                        JSONObject().put("parts", org.json.JSONArray().put(
                            JSONObject().put("text", prompt)
                        ))
                    ))
                }

                val requestBody = jsonBody.toString().toRequestBody("application/json".toMediaType())
                val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.1-flash-lite-preview:generateContent?key=$apiKey"
                val request = Request.Builder().url(url).post(requestBody).build()

                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val responseString = response.body?.string() ?: ""
                        val root = JSONObject(responseString)
                        val candidates = root.optJSONArray("candidates")
                        if (candidates != null && candidates.length() > 0) {
                            val candidate = candidates.getJSONObject(0)
                            val content = candidate.optJSONObject("content")
                            val parts = content?.optJSONArray("parts")
                            if (parts != null && parts.length() > 0) {
                                val text = parts.getJSONObject(0).optString("text", "")
                                val cleanJson = text.replace("```json", "").replace("```", "").trim()
                                val jsonResult = JSONObject(cleanJson)
                                val aiAutomated = jsonResult.optBoolean("isAutomated", isAutomated)
                                val aiBrand = jsonResult.optString("brand", detectedBrand)
                                val aiType = jsonResult.optString("messageType", messageType)

                                val aiLower = aiBrand.lowercase()
                                var aiMatchedKey: String? = null
                                for ((k, v) in trustedBrands) {
                                    if (aiLower.contains(k)) {
                                        aiMatchedKey = k
                                        break
                                    }
                                }
                                val aiVerified = aiMatchedKey != null || !aiBrand.isNullOrBlank()
                                val aiMeta = aiMatchedKey?.let { trustedBrands[it] }
                                val aiLogo = aiMeta?.second ?: logoUrl
                                val aiDesc = aiMeta?.third ?: "Official automated notification sender for $aiBrand."

                                return@withContext SmartSmsInfo(
                                    isAutomated = aiAutomated,
                                    brand = aiBrand.ifBlank { detectedBrand },
                                    messageType = aiType,
                                    isVerified = aiVerified,
                                    logoUrl = aiLogo,
                                    description = aiDesc
                                )
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("SmartSmsRecognizer", "Gemini classification error: ${e.message}")
            }
        }

        return@withContext SmartSmsInfo(
            isAutomated = isAutomated,
            brand = detectedBrand,
            messageType = messageType,
            isVerified = isVerified,
            logoUrl = logoUrl,
            description = description
        )
    }
}

