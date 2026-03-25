package ru.netology.nmedia.repository

import androidx.lifecycle.LiveData
import androidx.lifecycle.map
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import ru.netology.nmedia.dao.PostDao
import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.entity.PostEntity
import java.util.concurrent.TimeUnit


class PostRepositoryImpl(
    private val dao: PostDao
) : PostRepository {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .build()
    private val gson = Gson()
    private val typeToken = object : TypeToken<List<Post>>() {}

    companion object {
        private const val BASE_URL = "http://10.0.2.2:9999"
        private val jsonType = "application/json".toMediaType()
    }

    private val data: LiveData<List<Post>> = dao.getAll().map { entities ->
        entities.map { it.toDto() }
    }

    init {
        Thread {
            refresh()
        }.start()
    }

    override fun getAll(): LiveData<List<Post>> = data

    private fun refresh() {
        val request = Request.Builder()
            .url("${BASE_URL}/api/slow/posts")
            .build()

        client.newCall(request)
            .execute()
            .use { response ->
                val body = response.body?.string() ?: throw RuntimeException("body is null")
                val posts = gson.fromJson<List<Post>>(body, typeToken.type)
                dao.insert(posts.map(PostEntity::fromDto))
            }
    }

    override fun likeById(id: Long) {
        Thread {
            val post = dao.getById(id) ?: return@Thread

            val request = if (post.likedByMe) {
                Request.Builder()
                    .delete()
                    .url("${BASE_URL}/api/posts/$id/likes")
                    .build()
            } else {
                Request.Builder()
                    .post("".toRequestBody(jsonType))
                    .url("${BASE_URL}/api/posts/$id/likes")
                    .build()
            }

            client.newCall(request)
                .execute()
                .use { response ->
                    val body = response.body?.string() ?: throw RuntimeException("body is null")
                    val updatedPost = gson.fromJson(body, Post::class.java)
                    dao.insert(PostEntity.fromDto(updatedPost))
                }
        }.start()
    }

    override fun save(post: Post) {
        Thread {
            val request = Request.Builder()
                .post(gson.toJson(post).toRequestBody(jsonType))
                .url("${BASE_URL}/api/slow/posts")
                .build()

            client.newCall(request)
                .execute()
                .use { response ->
                    val body = response.body?.string() ?: throw RuntimeException("body is null")
                    val savedPost = gson.fromJson(body, Post::class.java)
                    dao.insert(PostEntity.fromDto(savedPost))
                }
        }.start()
    }

    override fun removeById(id: Long) {
        Thread {
            val request = Request.Builder()
                .delete()
                .url("${BASE_URL}/api/slow/posts/$id")
                .build()

            client.newCall(request)
                .execute()
                .close()

            dao.removeById(id)
        }.start()
    }

    override fun sharedById(id: Long) {
        dao.share(id)
    }
}