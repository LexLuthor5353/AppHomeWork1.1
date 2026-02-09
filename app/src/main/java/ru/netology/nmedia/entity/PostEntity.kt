package ru.netology.nmedia.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import ru.netology.nmedia.dto.Post

@Entity
class PostEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val author: String = "",
    val content: String = "",
    val published: String = "",
    val likes: Long = 0L,
    val likedByMe: Boolean = false,
    val share: Long = 0L,
    val shared: Boolean = false,
    val view: Long = 0L,
    val videolink: String? = null
) {
    fun toDto() = Post(
        id, author, content, published, likes, likedByMe, share, shared, view, videolink
    )

    companion object {
        fun fromDto(post : Post) = PostEntity(
            post.id, post.author, post.content, post.published, post.likes, post.likedByMe, post.share, post.shared, post.view, post.videolink
        )
    }
}