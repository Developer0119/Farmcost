package com.example.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

object GeminiAdvisor {
    private const val MODEL_NAME = "gemini-3.5-flash"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models"

    suspend fun getAdvice(
        apiKey: String,
        language: String, // "en", "hi", "mr"
        farmsList: List<Farm>,
        cropsList: List<Crop>,
        expensesList: List<Expense>,
        incomeList: List<Income>
    ): String = withContext(Dispatchers.IO) {
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            val defaultTips = when (language) {
                "hi" -> "नमस्ते। सलाह पाने के लिए कृपया सेटिंग्स में अपना जेमिनी एपीआई की (API Key) दर्ज करें। ऑफलाइन रहने पर भी आप अपने बुनियादी खर्च और आय के आंकड़े देख सकते हैं।"
                "mr" -> "नमस्कार. सल्ला मिळवण्यासाठी कृपया सेटिंग्जमध्ये तुमचा जेमिनी एपीआय की (API Key) प्रविष्ट करा. ऑफलाइन असताना देखील तुम्ही तुमचे मूलभूत खर्च आणि उत्पन्नाचे आकडे पाहू शकता."
                else -> "Hello. Please set your Gemini API Key in the settings to load dynamic AI insights. You can continue managing your expenses fully offline."
            }
            return@withContext defaultTips
        }

        try {
            val urlString = "$BASE_URL/$MODEL_NAME:generateContent?key=$apiKey"
            val url = URL(urlString)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.doOutput = true

            // Formulate data status to let the AI analyze
            val prompt = StringBuilder()
            prompt.append("You are FarmCost AI, a highly skilled agricultural accountant and profitability advisor.\n")
            prompt.append("Analyze this farmer's business state and provide 3 action-oriented financial or farming suggestions in ${if (language == "hi") "Hindi" else if (language == "mr") "Marathi" else "English"}.\n\n")
            
            prompt.append("--- FARM LEDGER SUMMARY ---\n")
            prompt.append("Total Farms: ${farmsList.size}\n")
            farmsList.forEach { f ->
                prompt.append("- Farm: ${f.name} (${f.area} ${f.areaUnit}, Soil: ${f.soilType}, Irrigation: ${f.irrigationType})\n")
            }
            prompt.append("\nActive Crops: ${cropsList.size}\n")
            cropsList.forEach { c ->
                prompt.append("- Crop: ${c.name} (${c.variety}, Season: ${c.season})\n")
            }
            
            val totalExpense = expensesList.sumOf { it.amount }
            prompt.append("\nExpenses List (${expensesList.size} records, total: $totalExpense):\n")
            // Aggregate by category
            expensesList.groupBy { it.category }.forEach { (cat, list) ->
                prompt.append("- Category $cat: ${list.sumOf { it.amount }}\n")
            }

            val totalIncome = incomeList.sumOf { it.amount }
            prompt.append("\nIncome List (${incomeList.size} records, total: $totalIncome):\n")
            incomeList.forEach { i ->
                prompt.append("- Crop sold: ${i.quantity} ${i.unit} at rate ${i.rate} per unit, buyer ${i.buyerName}, total ${i.amount}\n")
            }
            
            prompt.append("\nProvide specific crop-wise and category-wise spending/saving analysis, irrigation or fertilizer optimization tips, and suggest which crops display maximum margins. Output exactly high-value, direct solutions. Use formatting or bullet points. No technical jargon.")

            // Construct Gemini Request JSON
            val requestJson = JSONObject()
            val contents = JSONArray()
            val contentObj = JSONObject()
            val parts = JSONArray()
            val partObj = JSONObject()
            partObj.put("text", prompt.toString())
            parts.put(partObj)
            contentObj.put("parts", parts)
            contents.put(contentObj)
            requestJson.put("contents", contents)

            val writer = OutputStreamWriter(connection.outputStream)
            writer.write(requestJson.toString())
            writer.flush()
            writer.close()

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val responseText = connection.inputStream.bufferedReader().use { it.readText() }
                connection.disconnect()

                val responseJson = JSONObject(responseText)
                val candidates = responseJson.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val firstCandidate = candidates.getJSONObject(0)
                    val outContent = firstCandidate.optJSONObject("content")
                    if (outContent != null) {
                        val outParts = outContent.optJSONArray("parts")
                        if (outParts != null && outParts.length() > 0) {
                            return@withContext outParts.getJSONObject(0).optString("text", "No generated text.")
                        }
                    }
                }
                return@withContext "Error: Unable to parse AI response."
            } else {
                val errorStream = connection.errorStream?.bufferedReader()?.use { it.readText() }
                connection.disconnect()
                return@withContext "API Error: $responseCode\n$errorStream"
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext "Network Error: ${e.message}. Are you online?"
        }
    }
}
