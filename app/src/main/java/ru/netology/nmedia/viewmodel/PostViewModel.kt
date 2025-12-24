package ru.netology.nmedia.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.room.util.copy
import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.repository.PostRepository
import ru.netology.nmedia.repository.PostRepositoryInMemoryImpl

private val empty = Post(
    id = 0L,
    author = "",
    content = "",
    published = ""
)

class PostViewModel : ViewModel() {
    private val repository: PostRepository = PostRepositoryInMemoryImpl()
    val data = repository.getAll()
    val editer = MutableLiveData(empty)
    fun likeById(id: Long) = repository.likeById(id)
    fun sharedById(id: Long) = repository.sharedById(id)
    fun removeById(id: Long) = repository.removeById(id)
    fun save(content: String) {
        editer.value?.let { post ->
            val trim = content.trim()
            if (trim != post.content) {
                repository.save(post.copy(content = trim))
            }
        }
    }
    fun cancelEditing() {
        editer.value = Post()
    }

    fun edit(post: Post) {
        editer.value = post

    }
}