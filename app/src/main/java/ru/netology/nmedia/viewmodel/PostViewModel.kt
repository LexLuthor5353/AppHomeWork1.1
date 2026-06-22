package ru.netology.nmedia.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import android.net.Uri
import dagger.hilt.android.lifecycle.HiltViewModel
import ru.netology.nmedia.auth.AppAuth
import ru.netology.nmedia.dto.MediaUpload
import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.model.FeedModel
import ru.netology.nmedia.model.FeedModelState
import ru.netology.nmedia.repository.PostRepository
import ru.netology.nmedia.util.SingleLiveEvent
import java.io.File
import javax.inject.Inject

private val empty = Post(
    id = 0L,
    author = "",
    content = "",
    published = 0,
    ownedByMe = false
)

@HiltViewModel
class PostViewModel @Inject constructor(
    private val appAuth: AppAuth,
    private val repository: PostRepository,
) : ViewModel() {

    val data: LiveData<FeedModel> = appAuth.authStateFlow
        .flatMapLatest { auth ->
            repository.data
                .map { posts ->
                    posts.map {
                        it.copy(ownedByMe = auth.id != 0L && auth.id == it.authorId)
                    }
                }
                .map(::FeedModel)
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

    private val _needAuth = SingleLiveEvent<Unit>()
    val needAuth: LiveData<Unit> = _needAuth

    private val _openNewPost = SingleLiveEvent<Unit>()
    val openNewPost: LiveData<Unit> = _openNewPost

    private val _photoUri = MutableLiveData<Uri?>(null)
    val photoUri: LiveData<Uri?> = _photoUri
    private var photoFile: File? = null

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
        if (appAuth.authStateFlow.value.id == 0L) {
            return
        }
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
                    val file = photoFile
                    if (file == null) {
                        repository.save(newPost)
                    } else {
                        repository.saveWithAttachment(newPost, MediaUpload(file))
                    }
                    _dataState.value = FeedModelState()
                } catch (e: Exception) {
                    e.printStackTrace()
                    _dataState.value = FeedModelState(error = true)
                }
            }
        }
        edited.value = empty
        _photoUri.value = null
        photoFile = null
    }

    fun changePhoto(uri: Uri?, file: File?) {
        _photoUri.value = uri
        photoFile = file
    }

    fun edit(post: Post) {
        if (!post.ownedByMe) {
            return
        }
        edited.value = post
        _photoUri.value = null
        photoFile = null
    }

    fun changeContent(content: String) {
        val text = content.trim()
        if (edited.value?.content == text) {
            return
        }
        edited.value = edited.value?.copy(content = text)
    }

    fun onFabClick() {
        if (!isLoggedIn()) {
            _needAuth.value = Unit
            return
        }
        _openNewPost.value = Unit
    }

    fun likeById(id: Long) {
        if (!isLoggedIn()) {
            _needAuth.value = Unit
            return
        }
        viewModelScope.launch {
            repository.likeById(id)
        }
    }

    private fun isLoggedIn(): Boolean {
        val auth = appAuth.authStateFlow.value
        if (auth.id == 0L) {
            return false
        }
        if (auth.token.isNullOrEmpty()) {
            return false
        }
        return true
    }

    fun sharedById(id: Long) = repository.sharedById(id)

    fun removeById(id: Long) {
        val post = data.value?.posts?.find { it.id == id } ?: return
        if (!post.ownedByMe) {
            return
        }
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
