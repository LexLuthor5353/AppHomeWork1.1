package ru.netology.nmedia.repository

import androidx.lifecycle.LiveData
import androidx.lifecycle.map
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.IOException
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
        refresh()
    }

    override fun getAll(): LiveData<List<Post>> = data

    private fun refresh() {
        val request = Request.Builder()
            .url("${BASE_URL}/api/slow/posts")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
            }

            override fun onResponse(call: Call, response: Response) {
                response.use { resp ->
                    if (!resp.isSuccessful) return
                    val body = resp.body.string()
                    val posts = gson.fromJson<List<Post>>(body, typeToken.type)
                    dao.insert(posts.map(PostEntity::fromDto))
                }
            }
        })
    }

    override fun likeById(id: Long) {
        val post = dao.getById(id) ?: return

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

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
            }

            override fun onResponse(call: Call, response: Response) {
                response.use { resp ->
                    if (!resp.isSuccessful) return
                    val body = resp.body.string()
                    val updatedPost = gson.fromJson(body, Post::class.java)
                    dao.insert(PostEntity.fromDto(updatedPost))
                }
            }
        })
    }

    override fun save(post: Post) {
        val request = Request.Builder()
            .post(gson.toJson(post).toRequestBody(jsonType))
            .url("${BASE_URL}/api/slow/posts")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
            }

            override fun onResponse(call: Call, response: Response) {
                response.use { resp ->
                    if (!resp.isSuccessful) return
                    val body = resp.body.string()
                    val savedPost = gson.fromJson(body, Post::class.java)
                    dao.insert(PostEntity.fromDto(savedPost))
                }
            }
        })
    }

    override fun removeById(id: Long) {
        val request = Request.Builder()
            .delete()
            .url("${BASE_URL}/api/slow/posts/$id")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
            }

            override fun onResponse(call: Call, response: Response) {
                response.use { resp ->
                    if (!resp.isSuccessful) return
                    dao.removeById(id)
                }
            }
        })
    }

    override fun sharedById(id: Long) {
        dao.share(id)
    }
}