package ru.netology.nmedia

import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.RecyclerView
import ru.netology.nmedia.adapter.OnInteractionListener
import ru.netology.nmedia.adapter.PostsAdapter
import ru.netology.nmedia.adapter.onRemoveListener
import ru.netology.nmedia.databinding.ActivityMainBinding
import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.utils.AndroidUtils
import ru.netology.nmedia.viewmodel.PostViewModel


class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(
                v.paddingLeft + systemBars.left,
                v.paddingTop + systemBars.top,
                v.paddingRight + systemBars.right,
                v.paddingBottom + systemBars.bottom
            )
            insets
        }

        val viewModel: PostViewModel by viewModels()
        val adapter = PostsAdapter(
            object: OnInteractionListener {
                override fun onLike(post: Post) {
                    viewModel.likeById(post.id)
                }
                override fun onShare(post: Post) {
                    viewModel.sharedById(post.id)
                }
                override fun onEdit(post: Post) {
                    viewModel.edit(post)
                }
                override fun onRemove(post: Post) {
                    viewModel.removeById(post.id)
                }
            }
        )

        adapter.registerAdapterDataObserver(
            object : RecyclerView.AdapterDataObserver() {
                override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                    if (positionStart == 0) {
                        binding.list.smoothScrollToPosition(0)
                    }
                }
            }
        )
        viewModel.editer.observe(this) { post ->
            if (post.id != 0L) {
                binding.inputtext.setText(post.content)
                binding.inputtext.setSelection(post.content.length)
                binding.inputtext.requestFocus()
                binding.editorBar.visibility = android.view.View.VISIBLE
                AndroidUtils.showKeyboard(binding.inputtext)
            } else {
                binding.editorBar.visibility = android.view.View.GONE
            }
        }

        binding.close.setOnClickListener {
            viewModel.cancelEditing()
            binding.inputtext.setText("")
            binding.inputtext.clearFocus()
            binding.close.visibility = android.view.View.GONE
            AndroidUtils.hideKeyboard(binding.inputtext)
        }

        binding.save.setOnClickListener {
            with(binding.inputtext) {
                val currentText: String = text.toString()
                if (text.isBlank()) {
                    Toast.makeText(context, R.string.empty_text, Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                viewModel.save(currentText)
                setText("")
                clearFocus()
                AndroidUtils.hideKeyboard(this)
            }

        }
        binding.list.adapter = adapter
        viewModel.data.observe(this) { posts ->
            adapter.submitList(posts)
        }


    }
}