package ru.netology.nmedia.dto

data class Post(
    val id: Long = 0L,
    val author: String = "",
    val authorAvatar: String = "",
    val content: String = "",
    val published: Long = 0L,
    val likes: Long = 0L,
    val likedByMe: Boolean = false,
    val share: Long = 0L,
    val shared: Boolean = false,
    val view: Long = 0L,
    val videolink: String? = null
)