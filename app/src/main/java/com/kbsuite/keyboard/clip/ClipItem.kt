package com.kbsuite.keyboard.clip

data class ClipItem(
    val id: Long,
    val text: String,
    val title: String,
    val pinned: Boolean,
    val createdAt: Long,
    val isImage: Boolean,
    val imagePath: String
)
