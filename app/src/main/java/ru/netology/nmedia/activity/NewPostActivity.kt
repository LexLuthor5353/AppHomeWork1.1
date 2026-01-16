package ru.netology.nmedia.activity

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import ru.netology.nmedia.databinding.ActivityNewPostBinding

class NewPostActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        enableEdgeToEdge()
        val binding: ActivityNewPostBinding = ActivityNewPostBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val postId = intent.getLongExtra("postId", 0L)
        val content = intent.getStringExtra("content") ?: ""
        binding.content.setText(content)
        binding.content.setSelection(content.length)

        binding.save.setOnClickListener {
            val text = binding.content.text.toString().trim()
            if (text.isBlank()) {
                setResult(RESULT_CANCELED)
            } else {
                Intent().apply {
                    putExtra("postId", intent.getLongExtra("postId", 0L))
                    putExtra(Intent.EXTRA_TEXT, text)
                }.also { result ->
                    setResult(RESULT_OK, result)
                }
            }
            finish()
        }

        binding.close.setOnClickListener {
            setResult(RESULT_CANCELED)
            finish()
        }
    }
}