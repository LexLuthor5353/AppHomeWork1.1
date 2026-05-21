package ru.netology.nmedia.repository

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import ru.netology.nmedia.api.PostsApi
import ru.netology.nmedia.dao.PostDao
import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.entity.PostEntity

class PostRepositoryImpl(
    private val dao: PostDao,
) : PostRepository {
    private val _error = MutableLiveData(false)
    private val scope = CoroutineScope(Dispatchers.IO)

    override val data: LiveData<List<Post>> = dao.getAll().map { list ->
        list.map { it.toDto() }
    }

    init {
        scope.launch {
            load()
        }
    }

    override fun getError(): LiveData<Boolean> = _error

    override fun retry() {
        _error.value = false
        scope.launch {
            load()
            syncNotSynced()
        }
    }

    private suspend fun load() {
        try {
            val response = PostsApi.service.getAll()
            if (!response.isSuccessful) {
                _error.postValue(true)
                return
            }
            _error.postValue(false)
            val posts = response.body() ?: return
            dao.removeAllSynced()
            dao.insert(posts.map(PostEntity::fromDto))
        } catch (e: Exception) {
            e.printStackTrace()
            _error.postValue(true)
        }
    }

    override fun save(post: Post) {
        scope.launch {
            if (post.id == 0L) {
                val localId = dao.insert(PostEntity.fromNewPost(post))
                val entity = dao.getById(localId) ?: return@launch
                upload(entity)
            } else {
                val entity = dao.getById(post.id) ?: return@launch
                val updated = entity.copy(
                    content = post.content,
                    videolink = post.videolink,
                    synced = false,
                    syncFailed = false,
                )
                dao.insert(updated)
                upload(updated)
            }
        }
    }

    private fun PostEntity.toSaveRequest(): Post {
        val requestId = if (serverId != 0L) serverId else 0L
        return toDto().copy(id = requestId)
    }

    private suspend fun upload(entity: PostEntity) {
        try {
            val response = PostsApi.service.save(entity.toSaveRequest())
            if (!response.isSuccessful) {
                dao.insert(entity.copy(syncFailed = true))
                _error.postValue(true)
                return
            }
            val body = response.body()
            if (body == null) {
                dao.insert(entity.copy(syncFailed = true))
                _error.postValue(true)
                return
            }
            _error.postValue(false)
            dao.removeById(entity.id)
            dao.insert(PostEntity.fromDto(body))
        } catch (e: Exception) {
            e.printStackTrace()
            dao.insert(entity.copy(syncFailed = true))
            _error.postValue(true)
        }
    }

    private suspend fun syncNotSynced() {
        val list = dao.getNotSynced()
        for (entity in list) {
            upload(entity)
        }
    }

    override suspend fun likeById(id: Long) {
        val post = dao.getById(id) ?: return
        if (!post.synced) {
            return
        }
        dao.likeById(post.id)
        try {
            val response = if (post.likedByMe) {
                PostsApi.service.dislikeById(post.serverId)
            } else {
                PostsApi.service.likeById(post.serverId)
            }
            if (!response.isSuccessful) {
                _error.postValue(true)
                return
            }
            val body = response.body()
            if (body != null) {
                dao.insert(PostEntity.fromDto(body))
            }
        } catch (e: Exception) {
            e.printStackTrace()
            _error.postValue(true)
        }
    }

    override suspend fun removeById(id: Long) {
        val post = dao.getById(id) ?: return
        dao.removeById(post.id)
        if (!post.synced) {
            return
        }
        try {
            val response = PostsApi.service.removeById(post.serverId)
            if (!response.isSuccessful) {
                _error.postValue(true)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            _error.postValue(true)
        }
    }

    override fun sharedById(id: Long) {
        val post = dao.getById(id) ?: return
        if (!post.synced) {
            return
        }
        dao.share(post.id)
    }
}
