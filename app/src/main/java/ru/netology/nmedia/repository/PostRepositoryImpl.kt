package ru.netology.nmedia.repository

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import ru.netology.nmedia.api.PostApi
import ru.netology.nmedia.dao.PostDao
import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.entity.PostEntity

class PostRepositoryImpl(
    private val dao: PostDao
) : PostRepository {
    private val api = PostApi.service
    private val error = MutableLiveData(false)

    private val data: LiveData<List<Post>> = dao.getAll().map { entities ->
        entities.map { it.toDto() }
    }

    init {
        refresh()
    }

    override fun getAll(): LiveData<List<Post>> = data

    override fun getError(): LiveData<Boolean> = error

    private fun refresh() {
        api.getAll().enqueue(object : Callback<List<Post>> {
            override fun onFailure(call: Call<List<Post>>, t: Throwable) {
                error.postValue(true)
            }

            override fun onResponse(call: Call<List<Post>>, response: Response<List<Post>>) {
                if (response.isSuccessful) {
                    val posts = response.body().orEmpty()
                    dao.insert(posts.map { PostEntity.fromDto(it) })
                    error.postValue(false)
                } else {
                    error.postValue(true)
                }
            }
        })
    }

    override fun likeById(id: Long) {
        val post = dao.getById(id) ?: return
        val call = if (post.likedByMe) {
            api.dislikeById(id)
        } else {
            api.likeById(id)
        }
        call.enqueue(object : Callback<Post> {
            override fun onFailure(call: Call<Post>, t: Throwable) {
                error.postValue(true)
            }

            override fun onResponse(call: Call<Post>, response: Response<Post>) {
                if (response.isSuccessful) {
                    val updatedPost = response.body() ?: return
                    dao.insert(PostEntity.fromDto(updatedPost))
                    error.postValue(false)
                } else {
                    error.postValue(true)
                }
            }
        })
    }

    override fun save(post: Post) {
        api.save(post).enqueue(object : Callback<Post> {
            override fun onFailure(call: Call<Post>, t: Throwable) {
                error.postValue(true)
            }

            override fun onResponse(call: Call<Post>, response: Response<Post>) {
                if (response.isSuccessful) {
                    val savedPost = response.body() ?: return
                    dao.insert(PostEntity.fromDto(savedPost))
                    error.postValue(false)
                } else {
                    error.postValue(true)
                }
            }
        })
    }

    override fun removeById(id: Long) {
        api.removeById(id).enqueue(object : Callback<Unit> {
            override fun onFailure(call: Call<Unit>, t: Throwable) {
                error.postValue(true)
            }

            override fun onResponse(call: Call<Unit>, response: Response<Unit>) {
                if (response.isSuccessful) {
                    dao.removeById(id)
                    error.postValue(false)
                } else {
                    error.postValue(true)
                }
            }
        })
    }

    override fun sharedById(id: Long) {
        dao.share(id)
    }

    override fun retry() {
        refresh()
    }
}