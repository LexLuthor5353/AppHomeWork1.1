package ru.netology.nmedia.repository

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import ru.netology.nmedia.dto.Post
import java.io.File
import java.io.IOException

class PostRepositoryFileImpl(
    private val context: Context
) : PostRepository {

    private val file: File = context.filesDir.resolve("posts.json")
    private val gson = Gson()
    private val type = object : TypeToken<List<Post>>() {}.type

    private var posts: List<Post> = emptyList()
        set(value) {
            field = value
            saveToFile()
        }

    private val data = MutableLiveData(posts)

    init {
        loadFromFile()
    }

    override fun getAll(): LiveData<List<Post>> = data

    override fun likeById(id: Long) {
        posts = posts.map {
            if (it.id == id) {
                it.copy(
                    likedByMe = !it.likedByMe,
                    likes = if (it.likedByMe) it.likes - 1 else it.likes + 1
                )
            } else it
        }
        data.value = posts
    }

    override fun sharedById(id: Long) {
        posts = posts.map {
            if (it.id == id) {
                it.copy(
                    shared = !it.shared,
                    share = if (it.shared) it.share - 1 else it.share + 1
                )
            } else it
        }
        data.value = posts
    }

    override fun removeById(id: Long) {
        posts = posts.filter { it.id != id }
        data.value = posts
    }

    override fun save(post: Post) {
        if (post.id == 0L) {
            val newPost = post.copy(
                id = generateId(),
                author = "Me",
                published = "Сейчас"
            )
            posts = listOf(newPost) + posts
        } else {
            posts = posts.map { if (it.id == post.id) post else it }
        }
        data.value = posts
    }

    private fun generateId(): Long = posts.maxByOrNull { it.id }?.id?.inc() ?: 1L

    private fun loadFromFile() {
        try {
            if (file.exists()) {
                val content = file.readText()
                if (content.isNotEmpty()) {
                    posts = gson.fromJson(content, type)
                } else {
                    posts = emptyList()
                }
            } else {
                file.createNewFile()
                file.writeText("[]")
                posts = emptyList()
            }
            data.value = posts
        } catch (e: IOException) {
            posts = emptyList()
            data.value = posts
        } catch (e: Exception) {
            posts = emptyList()
            data.value = posts
        }
    }

    private fun saveToFile() {
        try {
            file.writeText(gson.toJson(posts))
        } catch (e: IOException) {
        }
    }
}