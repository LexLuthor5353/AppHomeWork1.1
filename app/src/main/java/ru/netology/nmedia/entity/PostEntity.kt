package ru.netology.nmedia.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import ru.netology.nmedia.dto.Attachment
import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.enumeration.AttachmentType

@Entity
data class PostEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val serverId: Long = 0L,
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
    val synced: Boolean = true,
    val syncFailed: Boolean = false,
    val visible: Boolean = true,
) {
    fun toDto(): Post {
        val attachment = if (!attachmentUrl.isNullOrBlank()) {
            Attachment(
                url = attachmentUrl,
                description = attachmentDescription,
                type = attachmentType?.let { runCatching { AttachmentType.valueOf(it) }.getOrNull() }
                    ?: AttachmentType.IMAGE,
            )
        } else {
            null
        }
        val displayId = if (serverId != 0L) serverId else id
        return Post(
            displayId,
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
            synced,
            syncFailed,
        )
    }

    companion object {
        fun fromDto(post: Post, visible: Boolean = true): PostEntity {
            val a = post.attachment
            val serverId = if (post.id != 0L) post.id else 0L
            return PostEntity(
                id = serverId,
                serverId = serverId,
                post.author,
                post.authorAvatar.orEmpty(),
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
                a?.type?.name,
                synced = true,
                syncFailed = false,
                visible = visible,
            )
        }

        fun fromNewPost(post: Post): PostEntity {
            val a = post.attachment
            return PostEntity(
                id = 0L,
                serverId = 0L,
                post.author,
                post.authorAvatar.orEmpty(),
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
                a?.type?.name,
                synced = false,
                syncFailed = false,
            )
        }
    }
}

fun List<PostEntity>.toDto(): List<Post> = map { it.toDto() }

fun List<Post>.toEntity(): List<PostEntity> = map { PostEntity.fromDto(it) }
