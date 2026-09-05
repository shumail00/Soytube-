package com.example.data.model

data class ChannelItem(
    val channelId: String,
    val name: String,
    val handle: String,
    val avatarUrl: String,
    val subscribersText: String,
    val hasNewUpload: Boolean = false,
    val isSubscribed: Boolean = true
)
