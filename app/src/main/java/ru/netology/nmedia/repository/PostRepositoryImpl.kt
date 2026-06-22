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
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import ru.netology.nmedia.api.ApiService
import ru.netology.nmedia.dao.PostDao
import ru.netology.nmedia.dto.Attachment
import ru.netology.nmedia.dto.Media
import ru.netology.nmedia.dto.MediaUpload
import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.entity.PostEntity
import ru.netology.nmedia.enumeration.AttachmentType
import ru.netology.nmedia.error.ApiError
import ru.netology.nmedia.error.AppError
import ru.netology.nmedia.error.NetworkError
import ru.netology.nmedia.error.UnknownError
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PostRepositoryImpl @Inject constructor(
    private val dao: PostDao,
    private val api: ApiService,
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
            val response = api.getNewer(maxId)
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
            val response = api.getAll()
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
                    attachmentUrl = post.attachment?.url,
                    attachmentDescription = post.attachment?.description,
                    attachmentType = post.attachment?.type?.name,
                    synced = false,
                    syncFailed = false,
                )
                dao.insert(updated)
                upload(updated)
            }
        }
    }

    override fun saveWithAttachment(post: Post, upload: MediaUpload) {
        scope.launch {
            try {
                val media = uploadMedia(upload)
                val postWithAttachment = post.copy(
                    attachment = Attachment(media.id, type = AttachmentType.IMAGE)
                )
                if (post.id == 0L) {
                    val localId = dao.insert(PostEntity.fromNewPost(postWithAttachment))
                    val entity = dao.getById(localId) ?: return@launch
                    upload(entity)
                } else {
                    val entity = dao.getById(post.id) ?: return@launch
                    val updated = entity.copy(
                        content = post.content,
                        videolink = post.videolink,
                        attachmentUrl = media.id,
                        attachmentType = AttachmentType.IMAGE.name,
                        synced = false,
                        syncFailed = false,
                    )
                    dao.insert(updated)
                    upload(updated)
                }
            } catch (e: AppError) {
                _error.value = true
            } catch (e: IOException) {
                _error.value = true
            } catch (e: Exception) {
                e.printStackTrace()
                _error.value = true
            }
        }
    }

    private suspend fun uploadMedia(upload: MediaUpload): Media {
        try {
            val media = MultipartBody.Part.createFormData(
                "file",
                upload.file.name,
                upload.file.asRequestBody()
            )
            val response = api.upload(media)
            if (!response.isSuccessful) {
                throw ApiError(response.code(), response.message())
            }
            return response.body() ?: throw ApiError(response.code(), response.message())
        } catch (e: IOException) {
            throw NetworkError
        } catch (e: AppError) {
            throw e
        } catch (e: Exception) {
            throw UnknownError
        }
    }

    private fun PostEntity.toSaveRequest(): Post {
        val requestId = if (serverId != 0L) serverId else 0L
        return toDto().copy(id = requestId)
    }

    private suspend fun upload(entity: PostEntity) {
        try {
            val response = api.save(entity.toSaveRequest())
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
                api.dislikeById(post.serverId)
            } else {
                api.likeById(post.serverId)
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
            val response = api.removeById(post.serverId)
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