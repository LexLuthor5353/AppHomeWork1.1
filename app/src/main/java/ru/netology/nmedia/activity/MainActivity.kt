package ru.netology.nmedia.activity

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import ru.netology.nmedia.adapter.OnInteractionListener
import ru.netology.nmedia.adapter.PostsAdapter
import ru.netology.nmedia.databinding.ActivityMainBinding
import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.viewmodel.PostViewModel

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val viewModel: PostViewModel by viewModels {
            object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    PostViewModel(application) as T
            }
        }

        val newPostLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK && result.data != null) {
                val postId = result.data?.getLongExtra("postId", 0L)
                val content = result.data?.getStringExtra(Intent.EXTRA_TEXT)

                content?.let {
                    if (postId == 0L || postId == null) {
                        viewModel.save(it)
                    } else {
                        viewModel.editById(postId)
                        viewModel.save(it)
                    }
                }
            }
        }

        val adapter = PostsAdapter(object : OnInteractionListener {
            override fun onLike(post: Post) = viewModel.likeById(post.id)

            override fun onShare(post: Post) {
                val intent = Intent().apply {
                    putExtra(Intent.EXTRA_TEXT, post.content)
                    action = Intent.ACTION_SEND
                    type = "text/plain"
                }
                viewModel.sharedById(post.id)
                try {
                    startActivity(Intent.createChooser(intent, null))
                } catch (e: ActivityNotFoundException) {
                    Toast.makeText(this@MainActivity, "Apps not found", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onEdit(post: Post) {
                val intent = Intent(this@MainActivity, NewPostActivity::class.java).apply {
                    putExtra("postId", post.id)
                    putExtra("content", post.content)
                }
                newPostLauncher.launch(intent)
            }

            override fun onRemove(post: Post) = viewModel.removeById(post.id)
        })

        adapter.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                if (positionStart == 0) {
                    binding.list.smoothScrollToPosition(0)
                }
            }
        })

        binding.list.adapter = adapter
        viewModel.data.observe(this) { posts ->
            adapter.submitList(posts)
        }

        binding.fab.setOnClickListener {
            val intent = Intent(this, NewPostActivity::class.java)
            newPostLauncher.launch(intent)
        }
    }
}