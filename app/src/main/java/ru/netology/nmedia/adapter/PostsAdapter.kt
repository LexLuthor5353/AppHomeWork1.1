package ru.netology.nmedia.adapter

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.widget.PopupMenu
import androidx.core.net.toUri
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import ru.netology.nmedia.BuildConfig
import ru.netology.nmedia.R
import ru.netology.nmedia.databinding.CardPostBinding
import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.enumeration.AttachmentType
import ru.netology.nmedia.formatCount
import ru.netology.nmedia.view.clearImage
import ru.netology.nmedia.view.loadAuthorAvatar
import ru.netology.nmedia.view.loadUrl

interface OnInteractionListener {
    fun onLike(post: Post)
    fun onShare(post: Post)
    fun onEdit(post: Post)
    fun onRemove(post: Post)
}

class PostsAdapter(
    private val onInteractionListener: OnInteractionListener
) : ListAdapter<Post, PostViewHolder>(PostDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val binding = CardPostBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PostViewHolder(binding, onInteractionListener)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        val post = getItem(position)
        holder.bind(post)
    }
}

class PostViewHolder(
    private val binding: CardPostBinding,
    private val onInteractionListener: OnInteractionListener,
) : RecyclerView.ViewHolder(binding.root) {

    fun bind(post: Post) {
        binding.apply {
            author.text = post.author
            setAvatar(avatar, post.author, post.authorAvatar)
            published.text = formatPublished(post.published)
            content.text = post.content

            if (!post.synced) {
                syncStatus.visibility = View.VISIBLE
                syncStatus.setImageResource(
                    if (post.syncFailed) R.drawable.ic_sync_error_24
                    else R.drawable.ic_sync_pending_24
                )
            } else {
                syncStatus.visibility = View.GONE
            }

            like.isEnabled = post.synced
            like.alpha = if (post.synced) 1f else 0.5f
            like.isChecked = post.likedByMe
            like.text = post.likes.formatCount()
            share.isChecked = post.shared
            share.text = post.share.formatCount()
            countview.text = post.view.formatCount()

            val att = post.attachment
            if (att != null && att.type == AttachmentType.IMAGE && att.url.isNotBlank()) {
                postAttachmentBlock.visibility = View.VISIBLE
                postAttachmentImage.scaleType = ImageView.ScaleType.CENTER_CROP
                postAttachmentImage.loadUrl("${BuildConfig.BASE_URL}/images/${att.url}")
                if (!att.description.isNullOrBlank()) {
                    postAttachmentDescription.visibility = View.VISIBLE
                    postAttachmentDescription.text = att.description
                    postAttachmentImage.contentDescription = att.description
                } else {
                    postAttachmentDescription.visibility = View.GONE
                    postAttachmentImage.contentDescription =
                        postAttachmentImage.context.getString(R.string.description_post_attachment)
                }
            } else {
                postAttachmentImage.clearImage()
                postAttachmentBlock.visibility = View.GONE
                postAttachmentDescription.text = ""
            }

            videoContainer.visibility = if (post.videolink != null && post.videolink.isNotBlank()) {
                View.VISIBLE
            } else {
                View.GONE
            }

            videoContainer.setOnClickListener {
                post.videolink?.let { url ->
                    val intent = Intent(Intent.ACTION_VIEW, url.toUri())
                    val chooser = Intent.createChooser(intent, "Открыть видео в ")
                    if (intent.resolveActivity(it.context.packageManager) != null) {
                        it.context.startActivity(chooser)
                    } else {
                        Toast.makeText(it.context, "Нет приложений для просмотра видео", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            like.setOnClickListener {
                if (post.synced) {
                    onInteractionListener.onLike(post)
                }
            }
            share.setOnClickListener { onInteractionListener.onShare(post) }

            menu.setOnClickListener { view ->
                PopupMenu(view.context, view).apply {
                    inflate(R.menu.post_menu)
                    setOnMenuItemClickListener { item ->
                        when (item.itemId) {
                            R.id.remove -> {
                                onInteractionListener.onRemove(post)
                                true
                            }
                            R.id.edit -> {
                                onInteractionListener.onEdit(post)
                                true
                            }
                            else -> false
                        }
                    }
                    show()
                }
            }
        }
    }

    private fun setAvatar(avatarView: ImageView, author: String, authorAvatar: String?) {
        avatarView.loadAuthorAvatar(
            author,
            authorAvatar,
            BuildConfig.BASE_URL,
            R.drawable.post_avatar_drawable_inset,
        )
    }

    private fun formatPublished(time: Long): String {
        if (time <= 0L) {
            return ""
        }
        val millis = if (time < 100000000000L) {
            time * 1000
        } else {
            time
        }
        return java.time.Instant.ofEpochMilli(millis)
            .atZone(java.time.ZoneId.systemDefault())
            .toLocalDateTime()
            .toString()
    }
}

class PostDiffCallback : DiffUtil.ItemCallback<Post>() {
    override fun areItemsTheSame(oldItem: Post, newItem: Post): Boolean = oldItem.id == newItem.id
    override fun areContentsTheSame(oldItem: Post, newItem: Post): Boolean = oldItem == newItem
}
