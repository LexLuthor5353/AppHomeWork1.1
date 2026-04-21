package ru.netology.nmedia.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import ru.netology.nmedia.db.AppDb
import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.repository.PostRepository
import ru.netology.nmedia.repository.PostRepositoryImpl

private val empty = Post(
    id = 0L,
    author = "",
    content = "",
    published = 0L
)

class PostViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDb.getInstance(application)
    private val dao = db.postDao()
    private val repository: PostRepository = PostRepositoryImpl(dao)
    val data = repository.getAll()
    val error = repository.getError()
    val editor = MutableLiveData(empty)
    fun likeById(id: Long) = repository.likeById(id)
    fun sharedById(id: Long) = repository.sharedById(id)
    fun removeById(id: Long) = repository.removeById(id)

    fun save(content: String) {
        val post = editor.value ?: empty
        val trimmedContent = content.trim()
        val videoLink = extractVideoLink(trimmedContent)
        val currentUnixTime = System.currentTimeMillis() / 1000

        val newPost = post.copy(
            content = trimmedContent,
            videolink = videoLink,
            author = if (post.id == 0L) "Me" else post.author,
            published = if (post.id == 0L) currentUnixTime else post.published
        )

        repository.save(newPost)
        editor.value = empty
    }

    fun editById(id: Long) {
        val postToEdit = data.value?.find { it.id == id } ?: return
        editor.value = postToEdit
    }
    fun clearEditor() {
        editor.value = empty
    }

    fun retry() {
        repository.retry()
    }

    private fun extractVideoLink(content: String): String? {
        val urlRegex = """https?://[^\s]+""".toRegex()
        val matches = urlRegex.findAll(content)
        return matches
            .map { it.value }
            .find { url ->
                url.contains("rutube.ru", ignoreCase = true)
            }
    }
}