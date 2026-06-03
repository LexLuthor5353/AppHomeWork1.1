package ru.netology.nmedia.repository

import kotlinx.coroutines.flow.Flow
import ru.netology.nmedia.dto.MediaUpload
import ru.netology.nmedia.dto.Post

interface PostRepository {
    val data: Flow<List<Post>>
    val newerCount: Flow<Int>
    fun getError(): Flow<Boolean>
    suspend fun loadPosts()
    fun getNewer(): Flow<Unit>
    suspend fun showNewer()
    suspend fun likeById(id: Long)
    fun sharedById(id: Long)
    suspend fun removeById(id: Long)
    fun save(post: Post)
    fun saveWithAttachment(post: Post, upload: MediaUpload)
    fun retry()
}
