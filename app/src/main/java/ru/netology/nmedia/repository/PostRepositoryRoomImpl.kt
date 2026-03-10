package ru.netology.nmedia.repository

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import ru.netology.nmedia.dao.PostDao
import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.entity.PostEntity

class PostRepositoryRoomImpl(
    private val dao: PostDao
) : PostRepository {
    private var posts = emptyList<Post>()
    private val data = MutableLiveData(posts)

    override fun getAll() = dao.getAll().map { list-> list.map { it.toDto() } }

    override fun save(post: Post) = dao.save(PostEntity.fromDto(post))

    override fun likeById(id: Long) = dao.likeById(id)

    override fun sharedById(id: Long) = dao.share(id)

    override fun removeById(id: Long) = dao.removeById(id)
}