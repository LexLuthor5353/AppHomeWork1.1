package ru.netology.nmedia.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import ru.netology.nmedia.db.AppDb
import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.model.FeedModel
import ru.netology.nmedia.model.FeedModelState
import ru.netology.nmedia.repository.PostRepository
import ru.netology.nmedia.repository.PostRepositoryImpl
import ru.netology.nmedia.util.SingleLiveEvent

private val empty = Post(
    id = 0L,
    author = "",
    content = "",
    published = 0L
)

class PostViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: PostRepository =
        PostRepositoryImpl(AppDb.getInstance(application).postDao())

    val data: LiveData<FeedModel> = repository.data
        .map { posts ->
            FeedModel(
                posts = posts,
                empty = posts.isEmpty(),
            )
        }
        .asLiveData(Dispatchers.Default)

    private val _dataState = MutableLiveData(FeedModelState())
    val dataState: LiveData<FeedModelState> = _dataState

    val newerCount: LiveData<Int> = repository.newerCount
        .asLiveData(Dispatchers.Default)

    private val edited = MutableLiveData(empty)
    val editedPost: LiveData<Post> = edited

    private val _postCreated = SingleLiveEvent<Unit>()
    val postCreated: LiveData<Unit> = _postCreated

    init {
        loadPosts()
        viewModelScope.launch {
            repository.getNewer()
                .catch { e -> e.printStackTrace() }
                .collect { }
        }
    }

    fun loadPosts() = viewModelScope.launch {
        _dataState.value = FeedModelState(loading = true)
        try {
            repository.loadPosts()
            _dataState.value = FeedModelState()
        } catch (e: Exception) {
            e.printStackTrace()
            _dataState.value = FeedModelState(error = true)
        }
    }

    fun refreshPosts() = viewModelScope.launch {
        _dataState.value = FeedModelState(refreshing = true)
        try {
            repository.loadPosts()
            _dataState.value = FeedModelState()
        } catch (e: Exception) {
            e.printStackTrace()
            _dataState.value = FeedModelState(error = true)
        }
    }

    fun save() {
        edited.value?.let { post ->
            _postCreated.value = Unit
            viewModelScope.launch {
                try {
                    val trimmedContent = post.content.trim()
                    val newPost = post.copy(
                        content = trimmedContent,
                        videolink = extractVideoLink(trimmedContent),
                        author = if (post.id == 0L) "Me" else post.author,
                        published = if (post.id == 0L) System.currentTimeMillis() / 1000 else post.published,
                    )
                    repository.save(newPost)
                    _dataState.value = FeedModelState()
                } catch (e: Exception) {
                    e.printStackTrace()
                    _dataState.value = FeedModelState(error = true)
                }
            }
        }
        edited.value = empty
    }

    fun edit(post: Post) {
        edited.value = post
    }

    fun changeContent(content: String) {
        val text = content.trim()
        if (edited.value?.content == text) {
            return
        }
        edited.value = edited.value?.copy(content = text)
    }

    fun likeById(id: Long) {
        viewModelScope.launch {
            repository.likeById(id)
        }
    }

    fun sharedById(id: Long) = repository.sharedById(id)

    fun removeById(id: Long) {
        viewModelScope.launch {
            repository.removeById(id)
        }
    }

    fun retry() {
        repository.retry()
    }

    suspend fun showNewer() {
        repository.showNewer()
    }

    private fun extractVideoLink(content: String): String? {
        val urlRegex = """https?://[^\s]+""".toRegex()
        return urlRegex.findAll(content)
            .map { it.value }
            .find { url -> url.contains("rutube.ru", ignoreCase = true) }
    }
}
