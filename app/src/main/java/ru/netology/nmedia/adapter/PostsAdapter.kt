package ru.netology.nmedia.adapter

import android.app.ProgressDialog.show
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.widget.PopupMenu
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import ru.netology.nmedia.R
import ru.netology.nmedia.databinding.CardPostBinding
import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.formatCount

typealias OnLikeListener = (post: Post) -> Unit
typealias onShareListener = (post: Post) -> Unit
typealias onRemoveListener = (post: Post) -> Unit

interface OnInteractionListener  {
    fun onEdit(post: Post) {}
    fun onRemove(post: Post) {}
    fun onLike(post: Post) {}
    fun onShare(post: Post) {}
}

class PostsAdapter(
    private val onInteractionListener: OnInteractionListener
) : ListAdapter<Post, PostViewHolder>(PostViewHolder.PostDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val binding = CardPostBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PostViewHolder(binding, onInteractionListener)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        val post = getItem(position)
        Log.d(
            "PostsAdapter",
            "Binding post ID: ${post.id}, likedByMe: ${post.likedByMe}, position: $position"
        )
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
            published.text = post.published
            content.text = post.content
//            countlikes.text = post.likes.toLong().formatCount()
//            countshare.text = post.share.toLong().formatCount()
            countview.text = post.view.toLong().formatCount()
            share.isChecked = post.shared
            share.text = post.share.toLong().formatCount()
            like.isChecked = post.likedByMe
            like.text = post.likes.toLong().formatCount()

//            like.setImageResource(
//                if (post.likedByMe) {
//                    R.drawable.baseline_favorite_24
//                } else {
//                    R.drawable.outline_favorite_24
//                }
//            )
            like.setOnClickListener {
                onInteractionListener.onLike(post)
            }
            share.setOnClickListener {
                onInteractionListener.onShare(post)
            }
            menu.setOnClickListener { menuItem ->
                PopupMenu(menuItem.context, menuItem).apply {
                    inflate(R.menu.post_menu)
                    setOnMenuItemClickListener {
                        when (it.itemId) {
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
                like.setOnClickListener { onInteractionListener.onLike(post) }
                share.setOnClickListener { onInteractionListener.onShare(post) }
            }
        }
    }

    class PostDiffCallback : DiffUtil.ItemCallback<Post>() {

        override fun areItemsTheSame(oldItem: Post, newItem: Post): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Post, newItem: Post): Boolean {
            return oldItem == newItem
        }
    }
}