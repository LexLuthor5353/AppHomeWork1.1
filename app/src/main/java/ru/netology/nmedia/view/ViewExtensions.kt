package ru.netology.nmedia.view

import android.graphics.Outline
import android.view.View
import android.view.ViewOutlineProvider
import android.widget.ImageView
import androidx.core.view.doOnLayout
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit
import android.graphics.BitmapFactory

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

fun ImageView.clearImage() {
    tag = null
    setImageDrawable(null)
}
