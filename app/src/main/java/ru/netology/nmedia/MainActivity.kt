package ru.netology.nmedia

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.util.TreeMap

class MainActivity : AppCompatActivity() {

    fun Int.toShortNum(): String {
        if(this < 1000){
            return this.toString()
        }
        val suffix = TreeMap<Long, String>()
        suffix[1000L] = "K"
        suffix[1000000L] = "M"
        val entry = suffix.floorEntry(this.toLong()) ?: return this.toString()

        val base = entry.key
        val suffixes = entry.value
        val formatted = this.toDouble() / base
        return String.format("%.1f%s", formatted, suffixes).replace(".0", "")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val countLikes: TextView = findViewById(R.id.countlikes)
        formatLikesFromResources(countLikes)
    }

    private fun formatLikesFromResources(textView: TextView) {
        val rawLikes = resources.getInteger(R.integer.post_likes)
        val formattedLikes = rawLikes.toShortNum()
        textView.text = formattedLikes
    }
}
