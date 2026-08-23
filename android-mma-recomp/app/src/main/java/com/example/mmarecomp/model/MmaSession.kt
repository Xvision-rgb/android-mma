package com.example.mmarecomp.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MmaSession(
    val id: String,
    @SerialName("user_id") val userId: String,
    val date: String,
    @SerialName("wod_content") val wodContent: String,
    @SerialName("rounds_sets") val roundsSets: String? = null,
    val ressenti: Int? = null,
    @SerialName("notes_technique") val notesTechnique: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
)

@Serializable
data class NewMmaSession(
    val date: String,
    @SerialName("wod_content") val wodContent: String,
    @SerialName("rounds_sets") val roundsSets: String? = null,
    val ressenti: Int? = null,
    @SerialName("notes_technique") val notesTechnique: String? = null,
)
