package com.dicoding.asclepius.domain.model

data class PredictionHistory(
    val id: Int = 0,
    val sessionName: String,
    val imageUri: String,
    val modelOutput: ModelOutput,
    val date: String,
    val note: String
)