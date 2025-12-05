package ru.netology.nmedia

import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import ru.netology.nmedia.databinding.ActivityMainBinding
import ru.netology.nmedia.dto.Post

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val post = Post(
            id = 1,
            author = "Нетология. Университет интернет-профессий будущего",
            content = "Привет, это новая Нетология! Когда-то Нетология начиналась с интенсивов по онлайн-маркетингу. Затем появились курсы по дизайну, разработке, аналитике и управлению. Мы растём сами и помогаем расти студентам: от новичков до уверенных профессионалов. Но самое важное остаётся с нами: мы верим, что в каждом уже есть сила, которая заставляет хотеть больше, целиться выше, бежать быстрее. Наша миссия — помочь встать на путь роста и начать цепочку перемен → http://netolo.gy/fyb",
            published = "21 мая в 18:36",
            likes = 999,
            shares = 567,
            views = 1240,
            likedByMe = false
        )

        fun Long.formatCount(): String {
            return when {
                this < 1_000 -> this.toString()

                this < 10_000 -> {
                    val thousands = this / 1_000
                    val hundreds = (this % 1_000) / 100
                    if (hundreds == 0L) "${thousands}K" else "${thousands}.${hundreds}K"
                }

                this < 1_000_000 -> {
                    "${this / 1_000}K"
                }

                this < 10_000_000 -> {
                    val millions = this / 1_000_000
                    val hundredThousands = (this % 1_000_000) / 100_000
                    if (hundredThousands == 0L) "${millions}M" else "${millions}.${hundredThousands}M"
                }

                else -> {
                    "${this / 1_000_000}M"
                }
            }
        }

        fun updateCounters() {
            with(binding) {
                countlikes.text = post.likes.formatCount()
                countshare.text = post.shares.formatCount()
                countview.text = post.views.formatCount()

                like.setImageResource(
                    if (post.likedByMe) R.drawable.baseline_favorite_24
                    else R.drawable.outline_favorite_24
                )
            }
        }

        with(binding) {
            author.text = post.author
            published.text = post.published
            content.text = post.content
            avatar.setImageResource(R.drawable.post_avatar_drawable)

            updateCounters()

            like.setOnClickListener {
                post.likedByMe = !post.likedByMe
                post.likes += if (post.likedByMe) 1 else -1
                updateCounters()
            }

            share.setOnClickListener {
                post.shares += 1
                updateCounters()
            }

        }
    }
}