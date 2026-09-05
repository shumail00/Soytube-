package com.example.data.model

data class VideoComment(
    val commentId: String,
    val author: String,
    val authorAvatarUrl: String = "",
    val text: String,
    val likeCount: String = "",
    val publishedTime: String = ""
)
