package ru.netology.nmedia.repository

import android.content.Context
import android.util.Log
import androidx.core.content.edit
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.gson.reflect.TypeToken
import ru.netology.nmedia.dto.Post
import com.google.gson.Gson
import java.lang.reflect.Type

class PostRepositoryInMemoryImpl(
    context: Context
) : PostRepository {

    private val prefs = context.getSharedPreferences("data", Context.MODE_PRIVATE)

    private var posts = getPosts()
        set(value) {
            field = value
            sync()
        }
    private var nextId = getId()

    private val data = MutableLiveData(posts)

    override fun getAll(): LiveData<List<Post>> = data

    override fun likeById(id: Long) {
        posts = posts.map {
            if (it.id != id) it else it.copy(
                likedByMe = !it.likedByMe,
                likes = if (it.likedByMe) it.likes - 1 else it.likes + 1
            )
        }
        data.value = posts
    }

    override fun sharedById(id: Long) {
        posts = posts.map {
            if (it.id != id) it else it.copy(
                shared = !it.shared,
                share = if (it.shared) it.share - 1 else it.share + 1
            )
        }
        data.value = posts
    }

    override fun removeById(id: Long) {
        Log.d("PostRepository", "Удаление id = $id")
        posts = posts.filter { it.id != id }
        data.value = posts
    }

    override fun save(post: Post) {
        if (post.id == 0L) {
            val newPost = post.copy(id = nextId++)
            posts = listOf(newPost) + posts.filter { it.id != newPost.id }
        } else {
            posts = posts.map { if (it.id == post.id) post else it }
        }
        data.value = posts
    }

    private fun getPosts(): List<Post> = prefs.getString(POSTS_KEY, null)?.let {
        gson.fromJson(it, postsType)
    } ?: emptyList()

    private fun getId() = prefs.getLong(ID_KEY, 1L)

    private fun sync() {
        prefs.edit {
            putString(POSTS_KEY, gson.toJson(posts))
            putLong(ID_KEY, nextId)
        }
    }

    private companion object {
        const val POSTS_KEY = "posts"
        const val ID_KEY = "nextId"
        val gson = Gson()
        val postsType: Type = object : TypeToken<List<Post>>() {}.type
    }
}