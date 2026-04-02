package ru.netology.nmedia.adapter

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.widget.PopupMenu
import androidx.core.net.toUri
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import ru.netology.nmedia.R
import ru.netology.nmedia.databinding.CardPostBinding
import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.formatCount
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import java.util.concurrent.TimeUnit

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
            setAvatar(avatar, post.authorAvatar)
            published.text = formatPublished(post.published)
            content.text = post.content
            like.isChecked = post.likedByMe
            like.text = post.likes.formatCount()
            share.isChecked = post.shared
            share.text = post.share.formatCount()
            countview.text = post.view.formatCount()

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

    private fun setAvatar(avatarView: ImageView, authorAvatar: String) {
        val avatarKey = authorAvatar.trim()
        avatarView.setTag(avatarKey)

        if (avatarKey.isBlank()) {
            avatarView.setImageResource(R.drawable.post_avatar_drawable)
            return
        }

        // Try local drawable first (e.g. "@sample/posts_avatars")
        if (avatarKey.startsWith("@")) {
            val localResId = resolveLocalAvatarResId(avatarView, avatarKey)
            if (localResId != 0) {
                avatarView.setImageResource(localResId)
            } else {
                avatarView.setImageResource(R.drawable.post_avatar_drawable)
            }
            return
        }

        bitmapCache[avatarKey]?.let { cached ->
            avatarView.setImageBitmap(cached)
            return
        }

        // Avoid showing a bitmap from a recycled row while the new image loads.
        avatarView.setImageResource(R.drawable.post_avatar_drawable)

        val urls = avatarUrlCandidates(avatarKey)
        loadAvatarFromUrlList(avatarView, avatarKey, urls, 0)
    }

    /**
     * Tries several URLs: NMedia server often stores files under `/avatars/`, while older code
     * requested `/filename` and always fell back to the same placeholder.
     */
    private fun loadAvatarFromUrlList(
        avatarView: ImageView,
        avatarKey: String,
        urls: List<String>,
        index: Int,
    ) {
        if (index >= urls.size) return
        val url = urls[index]
        val request = Request.Builder().url(url).build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                avatarView.post {
                    loadAvatarFromUrlList(avatarView, avatarKey, urls, index + 1)
                }
            }

            override fun onResponse(call: Call, response: Response) {
                response.use { resp ->
                    if (!resp.isSuccessful) {
                        avatarView.post {
                            loadAvatarFromUrlList(avatarView, avatarKey, urls, index + 1)
                        }
                        return
                    }
                    val bytes = resp.body.bytes()
                    val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    if (bitmap == null) {
                        avatarView.post {
                            loadAvatarFromUrlList(avatarView, avatarKey, urls, index + 1)
                        }
                        return
                    }
                    bitmapCache.put(avatarKey, bitmap)

                    avatarView.post {
                        if (avatarView.tag == avatarKey) {
                            avatarView.setImageBitmap(bitmap)
                        }
                    }
                }
            }
        })
    }

    private fun resolveLocalAvatarResId(avatarView: ImageView, authorAvatar: String): Int {
        var name = authorAvatar
        // Examples: "@sample/posts_avatars" or "@drawable/some_avatar"
        name = name.removePrefix("@sample/").removePrefix("@drawable/").removePrefix("@")
        name = name.substringAfterLast('/')
        name = name.substringBeforeLast('.')
        if (name.isBlank()) return 0

        val resources = avatarView.context.resources
        val pkg = avatarView.context.packageName
        var resId = resources.getIdentifier(name, "drawable", pkg)
        if (resId == 0) resId = resources.getIdentifier(name, "mipmap", pkg)
        return resId
    }

    private fun avatarUrlCandidates(authorAvatar: String): List<String> {
        return when {
            authorAvatar.startsWith("http://") || authorAvatar.startsWith("https://") ->
                listOf(authorAvatar)

            authorAvatar.startsWith("/") ->
                listOf("$BASE_URL$authorAvatar")

            else -> {
                val list = ArrayList<String>(3)
                // Typical for course server: only file name like "sber.jpg"
                if (!authorAvatar.contains("/")) {
                    list.add("$BASE_URL/avatars/$authorAvatar")
                    list.add("$BASE_URL/static/$authorAvatar")
                }
                list.add("$BASE_URL/$authorAvatar")
                list.distinct()
            }
        }
    }

    companion object {
        private const val BASE_URL = "http://10.0.2.2:9999"
        private val client: OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .build()

        // Simple in-memory cache to avoid re-downloading on scroll.
        private val bitmapCache = object : LruCache<String, Bitmap>(50) {}
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