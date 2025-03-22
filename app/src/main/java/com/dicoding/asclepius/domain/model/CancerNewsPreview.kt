package com.dicoding.asclepius.domain.model

data class CancerNewsPreview(
    val title: String,
    val imageUrl: String,
    val description: String,
    val url: String,
    val publishedDate: String,
    val author: String
)