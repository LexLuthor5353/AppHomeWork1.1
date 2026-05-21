package ru.netology.nmedia.repository

import androidx.lifecycle.LiveData
import ru.netology.nmedia.dto.Post

interface PostRepository {
    val data: LiveData<List<Post>>
    fun getError(): LiveData<Boolean>
    suspend fun likeById(id: Long)
    fun sharedById(id: Long)
    suspend fun removeById(id: Long)
    fun save(post: Post)
    fun retry()
}