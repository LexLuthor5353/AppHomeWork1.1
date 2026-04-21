package ru.netology.nmedia.repository

import androidx.lifecycle.LiveData
import ru.netology.nmedia.dto.Post

interface PostRepository {
    fun getAll(): LiveData<List<Post>>
    fun getError(): LiveData<Boolean>
    fun likeById(id: Long)
    fun sharedById(id: Long)
    fun removeById(id: Long)
    fun save(post: Post)
    fun retry()
}