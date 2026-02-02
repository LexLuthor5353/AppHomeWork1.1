package ru.netology.nmedia.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.repository.PostRepository
import ru.netology.nmedia.repository.PostRepositoryFileImpl
import ru.netology.nmedia.repository.PostRepositoryInMemoryImpl

private val empty = Post(
    id = 0L,
    author = "",
    content = "",
    published = ""
)

class PostViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: PostRepository = PostRepositoryFileImpl(application)
    val data = repository.getAll()
    val editer = MutableLiveData(empty)
    fun likeById(id: Long) = repository.likeById(id)
    fun sharedById(id: Long) = repository.sharedById(id)
    fun removeById(id: Long) = repository.removeById(id)

    fun save(content: String) {
        val post = editer.value ?: empty
        val trimmedContent = content.trim()
        val videoLink = extractVideoLink(trimmedContent)

        val newPost = post.copy(
            content = trimmedContent,
            videolink = videoLink,
            author = if (post.id == 0L) "Me" else post.author,
            published = if (post.id == 0L) "Сейчас" else post.published
        )

        repository.save(newPost)
        editer.value = empty
    }

    fun editById(id: Long) {
        val postToEdit = data.value?.find { it.id == id } ?: return
        editer.value = postToEdit
    }
    fun clearEditor() {
        editer.value = empty
    }

    private fun extractVideoLink(content: String): String? {
        val urlRegex = """https?://[^\s]+""".toRegex()
        val matches = urlRegex.findAll(content)
        var result: String? = null
        for (match in matches) {
            val url = match.value
            if (url.contains("rutube.ru", ignoreCase = true)) {
                result = url
                break
            }
        }
        return result
    }
}