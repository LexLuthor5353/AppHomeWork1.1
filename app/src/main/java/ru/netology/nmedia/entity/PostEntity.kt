package ru.netology.nmedia.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import ru.netology.nmedia.dto.Attachment
import ru.netology.nmedia.dto.Post

@Entity
class PostEntity(
    @PrimaryKey(autoGenerate = true)
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
    val videolink: String? = null,
    val attachmentUrl: String? = null,
    val attachmentDescription: String? = null,
    val attachmentType: String? = null,
) {
    fun toDto(): Post {
        val attachment = if (!attachmentUrl.isNullOrBlank()) {
            Attachment(
                url = attachmentUrl,
                description = attachmentDescription ?: "",
                type = attachmentType ?: "",
            )
        } else {
            null
        }
        return Post(
            id,
            author,
            authorAvatar,
            content,
            published,
            likes,
            likedByMe,
            share,
            shared,
            view,
            videolink,
            attachment,
        )
    }

    companion object {
        fun fromDto(post: Post): PostEntity {
            val a = post.attachment
            return PostEntity(
                post.id,
                post.author,
                post.authorAvatar,
                post.content,
                post.published,
                post.likes,
                post.likedByMe,
                post.share,
                post.shared,
                post.view,
                post.videolink,
                a?.url?.ifBlank { null },
                a?.description,
                a?.type?.ifBlank { null },
            )
        }
    }
}