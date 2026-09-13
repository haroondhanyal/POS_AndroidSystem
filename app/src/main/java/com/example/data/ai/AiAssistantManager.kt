package com.example.data.ai

import com.example.BuildConfig
import com.example.data.model.BundleDealEntity
import com.example.data.model.ChatMessage
import com.example.data.model.ProductEntity
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.Locale
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class GeminiTextPart(val text: String)

@JsonClass(generateAdapter = true)
data class GeminiContent(val parts: List<GeminiTextPart>, val role: String? = null)

@JsonClass(generateAdapter = true)
data class GeminiRequest(
    val contents: List<GeminiContent>,
    val systemInstruction: GeminiContent? = null
)

@JsonClass(generateAdapter = true)
data class GeminiCandidate(val content: GeminiContent?)

@JsonClass(generateAdapter = true)
data class GeminiResponse(val candidates: List<GeminiCandidate>?)

class AiAssistantManager {

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .build()

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val requestAdapter = moshi.adapter(GeminiRequest::class.java)
    private val responseAdapter = moshi.adapter(GeminiResponse::class.java)

    suspend fun generateResponse(
        userQuery: String,
        activeProducts: List<ProductEntity>,
        activeDeals: List<BundleDealEntity>
    ): ChatMessage = withContext(Dispatchers.IO) {
        val apiKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (_: Exception) {
            ""
        }

        // Try Gemini API if key is available
        if (!apiKey.isNullOrBlank() && apiKey != "MY_GEMINI_API_KEY") {
            try {
                val geminiAnswer = callGeminiApi(apiKey, userQuery, activeProducts, activeDeals)
                if (!geminiAnswer.isNullOrBlank()) {
                    val matchedDeals = findRelevantDeals(userQuery, activeDeals)
                    val matchedProducts = findRelevantProducts(userQuery, activeProducts)
                    return@withContext ChatMessage(
                        isUser = false,
                        text = geminiAnswer,
                        bundleDeals = matchedDeals,
                        discountedProducts = matchedProducts
                    )
                }
            } catch (e: Exception) {
                // Graceful fallback to smart local engine
                e.printStackTrace()
            }
        }

        // Smart Local AI Engine (Fast, 100% Offline, Context-Aware)
        return@withContext generateSmartLocalResponse(userQuery, activeProducts, activeDeals)
    }

    private fun callGeminiApi(
        apiKey: String,
        userQuery: String,
        products: List<ProductEntity>,
        deals: List<BundleDealEntity>
    ): String? {
        val dealsSummary = deals.joinToString("\n") {
            "- ${it.title} (${it.category}): Price $${it.bundlePrice} (Original $${it.originalPrice}, Save ${it.discountPercent}%). Items SKUs: ${it.productSkus}. Pitch: ${it.pitchLine}"
        }

        val prodsSummary = products.take(20).joinToString("\n") {
            "- ${it.name} (SKU: ${it.sku}, Category: ${it.category}): Price $${it.sellingPrice}, Stock: ${it.currentStock}, Discount: ${it.discountPercent}%"
        }

        val systemPrompt = """
            You are 'POS Genius', an AI Retail & Sales Assistant inside an Android Point of Sale system.
            You help cashiers and store managers instantly find active offers, bundle deals, discounts, and product combos without manual catalog searching.
            
            Current Store Bundle Deals:
            $dealsSummary
            
            Key Store Products:
            $prodsSummary
            
            Instructions:
            1. If the user asks in Urdu, Roman Urdu, or English, reply warmly in the same language.
            2. Be direct, clear, and highlight savings, prices, and pitch lines to help them close sales.
            3. Mention the bundle deal names and discounted prices clearly so they can be added directly to the cart.
            4. Keep answers concise, formatted with bullet points and emojis.
        """.trimIndent()

        val requestPayload = GeminiRequest(
            contents = listOf(
                GeminiContent(
                    parts = listOf(GeminiTextPart(text = userQuery)),
                    role = "user"
                )
            ),
            systemInstruction = GeminiContent(
                parts = listOf(GeminiTextPart(text = systemPrompt))
            )
        )

        val jsonBody = requestAdapter.toJson(requestPayload)
        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$apiKey"

        val request = Request.Builder()
            .url(url)
            .post(jsonBody.toRequestBody("application/json".toMediaType()))
            .build()

        val response = okHttpClient.newCall(request).execute()
        if (response.isSuccessful) {
            val responseString = response.body?.string() ?: return null
            val geminiResponse = responseAdapter.fromJson(responseString)
            return geminiResponse?.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
        }
        return null
    }

    private fun generateSmartLocalResponse(
        query: String,
        products: List<ProductEntity>,
        deals: List<BundleDealEntity>
    ): ChatMessage {
        val q = query.lowercase(Locale.ROOT).trim()

        val matchedDeals = mutableListOf<BundleDealEntity>()
        val matchedProducts = mutableListOf<ProductEntity>()
        val replyText = StringBuilder()

        val isOffersQuery = q.contains("offer") || q.contains("deal") || q.contains("bundle") ||
                q.contains("discount") || q.contains("combo") || q.contains("bachat") ||
                q.contains("sasta") || q.contains("sale") || q.contains("special")

        val isClearanceQuery = q.contains("clearance") || q.contains("expired") ||
                q.contains("expiry") || q.contains("expire") || q.contains("short")

        val isCoffeeQuery = q.contains("coffee") || q.contains("beverage") || q.contains("tea") || q.contains("drink")
        val isSnackQuery = q.contains("snack") || q.contains("chocolate") || q.contains("yogurt") || q.contains("food")
        val isTechQuery = q.contains("tech") || q.contains("electronic") || q.contains("headphone") || q.contains("audio")

        when {
            isClearanceQuery -> {
                val now = System.currentTimeMillis()
                val thirtyDays = 30L * 24 * 60 * 60 * 1000
                val nearExpiry = products.filter {
                    it.expiryDate != null && it.expiryDate > now && (it.expiryDate - now) < thirtyDays
                }
                matchedProducts.addAll(nearExpiry)
                replyText.append("⚡ **Near-Expiry Clearance & Flash Deals:**\n\n")
                if (nearExpiry.isNotEmpty()) {
                    replyText.append("We have items nearing expiration date that you can offer with special clearance discounts to prevent write-offs:\n\n")
                    nearExpiry.forEach { p ->
                        replyText.append("• **${p.name}** — Selling Price: $${p.sellingPrice} (Stock: ${p.currentStock})\n")
                    }
                    replyText.append("\n💡 *Tip: Bundle these with fast-moving items or apply a 20-30% clearance discount at checkout!*")
                } else {
                    replyText.append("Good news! No products are currently nearing urgent expiry in the next 30 days.")
                }
            }

            isCoffeeQuery -> {
                val coffeeDeals = deals.filter { it.title.contains("Coffee", ignoreCase = true) || it.category.contains("Combos", ignoreCase = true) }
                matchedDeals.addAll(coffeeDeals.ifEmpty { deals.take(2) })
                replyText.append("☕ **Active Coffee & Beverage Deals:**\n\n")
                coffeeDeals.forEach { d ->
                    replyText.append("✨ **${d.title}**\n")
                    replyText.append("• **Bundle Price**: $${d.bundlePrice} *(Original $${d.originalPrice} - Save ${d.discountPercent.toInt()}%)*\n")
                    replyText.append("• **Pitch**: ${d.pitchLine}\n\n")
                }
                replyText.append("👉 *Tap 'Add to Cart' below to immediately load this deal into the POS register!*")
            }

            isTechQuery -> {
                val techDeals = deals.filter { it.category.contains("Electronics", ignoreCase = true) || it.title.contains("Tech", ignoreCase = true) }
                matchedDeals.addAll(techDeals.ifEmpty { deals.take(1) })
                replyText.append("🎧 **Electronics & Tech Bundles:**\n\n")
                techDeals.forEach { d ->
                    replyText.append("✨ **${d.title}**\n")
                    replyText.append("• **Bundle Price**: $${d.bundlePrice} *(Original $${d.originalPrice} - Save ${d.discountPercent.toInt()}%)*\n")
                    replyText.append("• **Pitch**: ${d.pitchLine}\n\n")
                }
            }

            isSnackQuery -> {
                val snackDeals = deals.filter { it.category.contains("Snacks", ignoreCase = true) }
                matchedDeals.addAll(snackDeals.ifEmpty { deals.take(2) })
                replyText.append("🍫 **Snack & Food Combos:**\n\n")
                snackDeals.forEach { d ->
                    replyText.append("✨ **${d.title}**\n")
                    replyText.append("• **Bundle Price**: $${d.bundlePrice} *(Save ${d.discountPercent.toInt()}%)*\n")
                    replyText.append("• **Pitch**: ${d.pitchLine}\n\n")
                }
            }

            isOffersQuery || q.isBlank() || q == "hi" || q == "hello" || q.contains("kya chal raha") -> {
                matchedDeals.addAll(deals)
                replyText.append("🎉 **Store Active Offers & Bundle Deals:**\n\n")
                deals.forEach { d ->
                    replyText.append("🌟 **${d.title}** (${d.badge})\n")
                    replyText.append("• **Bundle Price**: $${d.bundlePrice} (Save ${d.discountPercent.toInt()}% off $${d.originalPrice})\n")
                    replyText.append("• **Items**: ${d.description}\n")
                    replyText.append("• *Pitch to customer:* \"${d.pitchLine}\"\n\n")
                }
                replyText.append("💡 *Cashiers can tap 'Add to Cart' directly on any deal card below to automatically apply the bundle deal.*")
            }

            else -> {
                // Search in products and deals
                val searchDeals = deals.filter {
                    it.title.contains(q, ignoreCase = true) ||
                            it.description.contains(q, ignoreCase = true) ||
                            it.category.contains(q, ignoreCase = true)
                }
                val searchProds = products.filter {
                    it.name.contains(q, ignoreCase = true) ||
                            it.category.contains(q, ignoreCase = true) ||
                            it.brand.contains(q, ignoreCase = true)
                }

                if (searchDeals.isNotEmpty() || searchProds.isNotEmpty()) {
                    matchedDeals.addAll(searchDeals)
                    matchedProducts.addAll(searchProds.take(3))
                    replyText.append("🔍 Found matching deals & products for **\"$query\"**:\n\n")
                    if (searchDeals.isNotEmpty()) {
                        replyText.append("**Active Bundle Deals:**\n")
                        searchDeals.forEach { d ->
                            replyText.append("• **${d.title}**: $${d.bundlePrice} (Save ${d.discountPercent.toInt()}%)\n")
                        }
                        replyText.append("\n")
                    }
                    if (searchProds.isNotEmpty()) {
                        replyText.append("**Products:**\n")
                        searchProds.take(3).forEach { p ->
                            replyText.append("• **${p.name}** — $${p.sellingPrice} (In Stock: ${p.currentStock})\n")
                        }
                    }
                } else {
                    matchedDeals.addAll(deals.take(3))
                    replyText.append("Here are our top recommended bundle deals and store offers running today:\n\n")
                    deals.take(3).forEach { d ->
                        replyText.append("• **${d.title}** — Only $${d.bundlePrice} (Save ${d.discountPercent.toInt()}%)\n")
                    }
                    replyText.append("\nAsk me about any specific item like 'Coffee deals', 'Snack bundles', or 'Clearance items'!")
                }
            }
        }

        return ChatMessage(
            isUser = false,
            text = replyText.toString(),
            bundleDeals = matchedDeals,
            discountedProducts = matchedProducts
        )
    }

    private fun findRelevantDeals(query: String, allDeals: List<BundleDealEntity>): List<BundleDealEntity> {
        val q = query.lowercase(Locale.ROOT)
        val filtered = allDeals.filter {
            it.title.lowercase(Locale.ROOT).contains(q) ||
                    it.category.lowercase(Locale.ROOT).contains(q) ||
                    it.description.lowercase(Locale.ROOT).contains(q)
        }
        return filtered.ifEmpty { allDeals.take(2) }
    }

    private fun findRelevantProducts(query: String, allProducts: List<ProductEntity>): List<ProductEntity> {
        val q = query.lowercase(Locale.ROOT)
        return allProducts.filter {
            it.name.lowercase(Locale.ROOT).contains(q) ||
                    it.category.lowercase(Locale.ROOT).contains(q)
        }.take(3)
    }
}
