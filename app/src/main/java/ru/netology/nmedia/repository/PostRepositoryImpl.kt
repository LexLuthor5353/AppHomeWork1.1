package ru.netology.nmedia.repository

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import ru.netology.nmedia.api.PostsApi
import ru.netology.nmedia.dao.PostDao
import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.entity.PostEntity

class PostRepositoryImpl(
    private val dao: PostDao,
) : PostRepository {
    private val _error = MutableStateFlow(false)
    private val scope = CoroutineScope(Dispatchers.IO)

    override val data: Flow<List<Post>> = dao.getAll()
        .map { list -> list.map { it.toDto() } }
        .flowOn(Dispatchers.Default)

    override val newerCount: Flow<Int> = dao.countHidden()
        .flowOn(Dispatchers.Default)

    override fun getError(): Flow<Boolean> = _error

    override suspend fun loadPosts() {
        load()
    }

    override fun getNewer(): Flow<Unit> = flow<Unit> {
        while (true) {
            delay(10_000L)
            val maxId = dao.getMaxId() ?: 0L
            val response = PostsApi.service.getNewer(maxId)
            if (!response.isSuccessful) {
                _error.value = true
                continue
            }
            val body = response.body() ?: continue
            if (body.isNotEmpty()) {
                dao.insert(body.map { PostEntity.fromDto(it, visible = false) })
            }
            emit(Unit)
        }
    }.flowOn(Dispatchers.Default)

    override suspend fun showNewer() {
        dao.showAllHidden()
    }

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
                _error.value = true
                throw java.io.IOException()
            }
            _error.value = false
            val posts = response.body() ?: throw java.io.IOException()
            dao.removeAllSynced()
            dao.insert(posts.map(PostEntity::fromDto))
        } catch (e: Exception) {
            _error.value = true
            throw e
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
                _error.value = true
                return
            }
            val body = response.body()
            if (body == null) {
                dao.insert(entity.copy(syncFailed = true))
                _error.value = true
                return
            }
            _error.value = false
            dao.removeById(entity.id)
            dao.insert(PostEntity.fromDto(body))
        } catch (e: Exception) {
            e.printStackTrace()
            dao.insert(entity.copy(syncFailed = true))
            _error.value = true
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
                _error.value = true
                return
            }
            val body = response.body()
            if (body != null) {
                dao.insert(PostEntity.fromDto(body))
            }
        } catch (e: Exception) {
            e.printStackTrace()
            _error.value = true
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
                _error.value = true
            }
        } catch (e: Exception) {
            e.printStackTrace()
            _error.value = true
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
