package ru.netology.nmedia.view

import android.graphics.BitmapFactory
import android.graphics.Outline
import android.view.View
import android.view.ViewOutlineProvider
import android.widget.ImageView
import androidx.annotation.DrawableRes
import androidx.core.view.doOnLayout
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
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
    val name = authorAvatar?.trim().orEmpty()
    if (author != "Me" && name.isEmpty()) {
        Glide.with(this).clear(this)
        tag = null
        setImageDrawable(null)
        return
    }
    val src = if (author == "Me") {
        "http://127.0.0.1:9/x"
    } else {
        "$baseUrl/avatars/$name"
    }
    val req = Glide.with(this).load(src).apply(RequestOptions().circleCrop())
    if (author == "Me") {
        req.placeholder(fallbackDrawable).error(fallbackDrawable).fallback(fallbackDrawable).into(this)
    } else {
        req.into(this)
    }
}

fun ImageView.clearImage() {
    Glide.with(this).clear(this)
    tag = null
    setImageDrawable(null)
}
