package com.sdshop.client.data.model

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
data class SingleProductSession(
    val product: Product,
    val inquiries: List<InquiryRecord> = emptyList(),
    val toolRecords: List<ToolRecord> = emptyList(),
    val lastUpdated: Instant = Instant.DISTANT_PAST
)
