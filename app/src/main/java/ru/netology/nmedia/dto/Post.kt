package ru.netology.nmedia.dto

data class Post(
    val id: Long = 0L,
    val authorId: Long = 0L,
    val author: String = "",
    val authorAvatar: String? = null,
    val content: String = "",
    val published: Long = 0L,
    val likes: Long = 0L,
    val likedByMe: Boolean = false,
    val share: Long = 0L,
    val shared: Boolean = false,
    val view: Long = 0L,
    val videolink: String? = null,
    val attachment: Attachment? = null,
    val synced: Boolean = true,
    val syncFailed: Boolean = false,
    val ownedByMe: Boolean = false,
)