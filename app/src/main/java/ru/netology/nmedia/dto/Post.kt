package ru.netology.nmedia.dto

class Post (
    val id: Long,
    val author: String,
    val published: String,
    val content: String,
    var likes: Long = 0,
    var shares: Long = 0,
    var views: Long = 0,
    var likedByMe: Boolean
)
