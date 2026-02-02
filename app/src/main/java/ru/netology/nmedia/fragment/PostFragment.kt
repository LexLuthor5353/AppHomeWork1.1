package ru.netology.nmedia.fragment

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.PopupMenu
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import ru.netology.nmedia.R
import ru.netology.nmedia.databinding.FragmentPostBinding
import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.formatCount
import ru.netology.nmedia.viewmodel.PostViewModel

class PostFragment : Fragment() {
    private lateinit var binding: FragmentPostBinding
    private val viewModel: PostViewModel by viewModels(ownerProducer = ::requireActivity)

    private val sharePostLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {}

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentPostBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val postId = arguments?.getLong("postId", 0L) ?: 0L
        
        viewModel.data.observe(viewLifecycleOwner) { posts ->
            var foundPost: Post? = null
            for (p in posts) {
                if (p.id == postId) {
                    foundPost = p
                    break
                }
            }
            if (foundPost != null) {
                bindPost(foundPost)
            }
        }
    }

    private fun bindPost(post: Post) {
        binding.apply {
            author.text = post.author
            published.text = post.published
            content.text = post.content
            like.isChecked = post.likedByMe
            like.text = post.likes.formatCount()
            share.isChecked = post.shared
            share.text = post.share.formatCount()
            countview.text = post.view.formatCount()

            if (post.videolink != null && post.videolink.isNotBlank()) {
                videoContainer.visibility = View.VISIBLE
            } else {
                videoContainer.visibility = View.GONE
            }

            videoContainer.setOnClickListener {
                val url = post.videolink
                if (url != null) {
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
                viewModel.likeById(post.id)
            }

            share.setOnClickListener {
                viewModel.sharedById(post.id)
                val shareIntent = Intent().apply {
                    action = Intent.ACTION_SEND
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, post.content)
                }
                try {
                    sharePostLauncher.launch(Intent.createChooser(shareIntent, null))
                } catch (e: Exception) {
                }
            }

            menu.setOnClickListener { view ->
                PopupMenu(view.context, view).apply {
                    inflate(R.menu.post_menu)
                    setOnMenuItemClickListener { item ->
                        when (item.itemId) {
                            R.id.remove -> {
                                viewModel.removeById(post.id)
                                findNavController().popBackStack()
                                true
                            }
                            R.id.edit -> {
                                findNavController().navigate(
                                    R.id.action_postFragment_to_newPostFragment,
                                    Bundle().apply {
                                        putString("content", post.content)
                                        putLong("postId", post.id)
                                    }
                                )
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
}

