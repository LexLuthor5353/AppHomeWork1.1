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
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.load.model.LazyHeaders
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import ru.netology.nmedia.R
import ru.netology.nmedia.databinding.CardPostBinding
import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.formatCount

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
            like.isChecked = post.likedByMe
            like.text = post.likes.formatCount()
            share.isChecked = post.shared
            share.text = post.share.formatCount()
            countview.text = post.view.formatCount()

            val att = post.attachment
            if (att != null && att.type.equals("IMAGE", ignoreCase = true) && att.url.isNotBlank()) {
                postAttachmentBlock.visibility = View.VISIBLE
                val imageUrl = attachmentImageUrl(att.url)
                loadAttachmentWithRetry(postAttachmentImage, imageUrl)
                if (att.description.isNotBlank()) {
                    postAttachmentDescription.visibility = View.VISIBLE
                    postAttachmentDescription.text = att.description
                    postAttachmentImage.contentDescription = att.description
                } else {
                    postAttachmentDescription.visibility = View.GONE
                    postAttachmentImage.contentDescription =
                        postAttachmentImage.context.getString(R.string.description_post_attachment)
                }
            } else {
                Glide.with(postAttachmentImage).clear(postAttachmentImage)
                postAttachmentBlock.visibility = View.GONE
                postAttachmentDescription.text = ""
            }

            videoContainer.visibility = if (post.videolink != null && post.videolink.isNotBlank()) {
                android.view.View.VISIBLE
            } else {
                android.view.View.GONE
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

            like.setOnClickListener { onInteractionListener.onLike(post) }
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

    private fun setAvatar(avatarView: ImageView, author: String, authorAvatar: String) {
        if (author == "Me" || author == "Netology") {
            Glide.with(avatarView)
                .load(R.drawable.post_avatar_drawable_inset)
                .circleCrop()
                .into(avatarView)
            return
        }
        val name = authorAvatar.trim()
        if (name.isBlank()) {
            Glide.with(avatarView).clear(avatarView)
            avatarView.setImageDrawable(null)
            return
        }
        if (name.startsWith("@")) {
            Glide.with(avatarView).clear(avatarView)
            avatarView.setImageDrawable(null)
            return
        }
        if (name.startsWith("http://") || name.startsWith("https://")) {
            val normalized = normalizeHost(name)
            Glide.with(avatarView)
                .clear(avatarView)
            loadAvatarWithRetry(avatarView, normalized)
            return
        }
        val url = if (name.startsWith("/")) {
            BASE_URL + name
        } else {
            "$BASE_URL/avatars/$name"
        }
        Glide.with(avatarView).clear(avatarView)
        loadAvatarWithRetry(avatarView, url)
    }

    private fun attachmentImageUrl(raw: String): String {
        val u = raw.trim()
        if (u.startsWith("http://") || u.startsWith("https://")) {
            return normalizeHost(u)
        }
        if (u.startsWith("/")) {
            return BASE_URL + u
        }
        return "$BASE_URL/images/$u"
    }

    private fun normalizeHost(url: String): String {
        return url
            .replace("://localhost:", "://10.0.2.2:")
            .replace("://127.0.0.1:", "://10.0.2.2:")
    }

    private fun loadAvatarWithRetry(imageView: ImageView, url: String, tryCount: Int = 0) {
        Glide.with(imageView)
            .load(toGlideUrl(url))
            .diskCacheStrategy(DiskCacheStrategy.NONE)
            .skipMemoryCache(true)
            .circleCrop()
            .listener(object : RequestListener<android.graphics.drawable.Drawable> {
                override fun onLoadFailed(
                    e: GlideException?,
                    model: Any?,
                    target: Target<android.graphics.drawable.Drawable>,
                    isFirstResource: Boolean
                ): Boolean {
                    if (tryCount < 6) {
                        imageView.post { loadAvatarWithRetry(imageView, url, tryCount + 1) }
                    } else {
                        imageView.setImageDrawable(null)
                    }
                    return true
                }

                override fun onResourceReady(
                    resource: android.graphics.drawable.Drawable,
                    model: Any,
                    target: Target<android.graphics.drawable.Drawable>?,
                    dataSource: DataSource,
                    isFirstResource: Boolean
                ): Boolean {
                    return false
                }
            })
            .into(imageView)
    }

    private fun loadAttachmentWithRetry(imageView: ImageView, url: String, tryCount: Int = 0) {
        Glide.with(imageView)
            .load(toGlideUrl(url))
            .diskCacheStrategy(DiskCacheStrategy.NONE)
            .skipMemoryCache(true)
            .centerCrop()
            .listener(object : RequestListener<android.graphics.drawable.Drawable> {
                override fun onLoadFailed(
                    e: GlideException?,
                    model: Any?,
                    target: Target<android.graphics.drawable.Drawable>,
                    isFirstResource: Boolean
                ): Boolean {
                    if (tryCount < 6) {
                        imageView.post { loadAttachmentWithRetry(imageView, url, tryCount + 1) }
                    } else {
                        imageView.setImageDrawable(null)
                    }
                    return true
                }

                override fun onResourceReady(
                    resource: android.graphics.drawable.Drawable,
                    model: Any,
                    target: Target<android.graphics.drawable.Drawable>?,
                    dataSource: DataSource,
                    isFirstResource: Boolean
                ): Boolean {
                    return false
                }
            })
            .into(imageView)
    }

    private fun toGlideUrl(url: String): GlideUrl {
        val headers = LazyHeaders.Builder()
            .addHeader("Accept", "image/*")
            .addHeader("User-Agent", "Mozilla/5.0 (Android)")
            .build()
        return GlideUrl(url, headers)
    }

    companion object {
        private const val BASE_URL = "http://10.0.2.2:9999"
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
