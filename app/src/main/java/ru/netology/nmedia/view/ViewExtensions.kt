package ru.netology.nmedia.view

import android.graphics.BitmapFactory
import android.graphics.Outline
import android.view.View
import android.view.ViewOutlineProvider
import android.widget.ImageView
import androidx.annotation.DrawableRes
import androidx.core.view.doOnLayout
import com.bumptech.glide.Glide
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

private val imageClient = OkHttpClient.Builder()
    .connectTimeout(30, TimeUnit.SECONDS)
    .readTimeout(30, TimeUnit.SECONDS)
    .build()

fun ImageView.loadUrl(url: String) {
    tag = url
    Thread {
        try {
            val request = Request.Builder().url(url).build()
            imageClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@Thread
                }
                val bytes = response.body.bytes() ?: return@Thread
                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return@Thread
                post {
                    if (tag == url) {
                        setImageBitmap(bitmap)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }.start()
}

fun ImageView.loadCircleCrop(url: String) {
    scaleType = ImageView.ScaleType.CENTER_CROP
    doOnLayout {
        clipToOutline = true
        outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline) {
                outline.setOval(0, 0, view.width, view.height)
            }
        }
    }
    loadUrl(url)
}

private val authorAvatarByName = mapOf(
    "Game of Thrones" to "got.jpg",
    "Игра престолов" to "got.jpg",
    "Netology" to "netology.jpg",
    "Сбер" to "sber.jpg",
    "Тинькофф" to "tcs.jpg",
)

private fun avatarFileName(author: String, authorAvatar: String?): String? {
    val fromServer = authorAvatar?.trim().orEmpty()
    if (fromServer.isNotEmpty() && !fromServer.startsWith("@")) {
        return if (fromServer.contains('.')) fromServer else "$fromServer.jpg"
    }
    return authorAvatarByName[author.trim()]
}

fun ImageView.loadAuthorAvatar(
    author: String,
    authorAvatar: String?,
    baseUrl: String,
    @DrawableRes fallbackDrawable: Int,
) {
    scaleType = ImageView.ScaleType.CENTER_CROP
    doOnLayout {
        clipToOutline = true
        outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline) {
                outline.setOval(0, 0, view.width, view.height)
            }
        }
    }
    Glide.with(this).clear(this)
    setImageDrawable(null)
    if (author == "Me") {
        setImageResource(fallbackDrawable)
        return
    }
    val file = avatarFileName(author, authorAvatar) ?: run {
        setImageResource(fallbackDrawable)
        return
    }
    loadCircleCrop("$baseUrl/avatars/$file")
}

fun ImageView.clearImage() {
    Glide.with(this).clear(this)
    tag = null
    setImageDrawable(null)
}
