package ru.netology.nmedia.dto

data class Post(
    val id: Long,
    val author: String,
    val content: String,
    val published: String,
    val likes: Long = 0,
    val likedByMe: Boolean = false,
    val share: Long = 0,
    val shared: Boolean = false,
    val view: Long = 0
)