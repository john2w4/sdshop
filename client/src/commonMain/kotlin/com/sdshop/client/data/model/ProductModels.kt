package com.sdshop.client.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Price(
    val amount: Double,
    val currency: String = "CNY"
) {
    val formatted: String get() = if (currency.isBlank()) amount.toString() else "$currency ${"%.2f".format(amount)}"
}

@Serializable
data class ProductParameter(
    val name: String,
    val value: String
)

@Serializable
data class LogisticsInfo(
    val deliveryTime: String,
    val freightPolicy: String,
    val supportRegions: List<String>
)

@Serializable
data class AfterSalePolicy(
    val warranty: String,
    val returnPolicy: String,
    val serviceChannels: List<String>
)

@Serializable
data class ReviewSummary(
    val score: Double,
    val reviewCount: Int,
    val highlights: List<String>
)

@Serializable
data class ProductQuestion(
    val question: String,
    val answer: String,
    val upVotes: Int = 0
)

@Serializable
data class ShopInfo(
    val name: String,
    val rating: Double,
    val badges: List<String>
)

@Serializable
data class Product(
    val id: String,
    val heroImages: List<String>,
    val title: String,
    val price: Price,
    val parameters: List<ProductParameter> = emptyList(),
    val logistics: LogisticsInfo? = null,
    val rankings: List<String> = emptyList(),
    val afterSale: AfterSalePolicy? = null,
    val reviews: ReviewSummary? = null,
    val questions: List<ProductQuestion> = emptyList(),
    val shop: ShopInfo? = null,
    val description: String = ""
)

@Serializable
data class ProductTag(
    val id: String,
    val label: String
)

@Serializable
data class ThemeProduct(
    val product: Product,
    val tags: List<ProductTag> = emptyList(),
    val addedAtMillis: Long
)

@Serializable
sealed interface ProductPayload {
    val product: Product

    @Serializable
    data class Single(override val product: Product) : ProductPayload

    @Serializable
    data class Multiple(val items: List<Product>) : ProductPayload {
        override val product: Product get() = items.first()
    }
}
