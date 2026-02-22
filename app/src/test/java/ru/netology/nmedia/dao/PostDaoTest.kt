package ru.netology.nmedia.dao

import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import org.junit.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import ru.netology.nmedia.dto.Post

class PostDaoTest {

    private lateinit var postDao: PostDao
    private val testPost = Post(
        id = 1,
        content = "Test post",
        author = "Author",
        likedByMe = false,
        published = "10 минут назад"
    )

    @BeforeEach
    fun setup() {
        postDao = RoomPostDao()
        postDao.removeAll()
    }

    @Test
    fun getAll_shouldReturnEmptyList_whenNoPostsAdded() {
        assertTrue(postDao.getAll().isEmpty())
    }

    @Test
    fun insert_shouldAddPostToStorage() {
        postDao.insert(testPost)
        val posts = postDao.getAll()
        assertEquals(1, posts.size)
        assertEquals(testPost.content, posts[0].content)
    }

    @Test
    fun updateContentById_shouldUpdateExistingPostContent() {
        postDao.insert(testPost)
        val updatedContent = "Updated content"
        postDao.updateContentById(testPost.id, updatedContent)
        val updatedPost = postDao.getById(testPost.id)
        assertEquals(updatedContent, updatedPost?.content)
    }

    @Test
    fun save_shouldInsertNewPost_whenIdNotExists() {
        postDao.save(testPost)
        val posts = postDao.getAll()
        assertEquals(1, posts.size)
        assertEquals(testPost.content, posts[0].content)
    }

    @Test
    fun save_shouldUpdatePost_whenIdExists() {
        postDao.save(testPost)
        val updatedPost = testPost.copy(content = "Updated via save")
        postDao.save(updatedPost)
        val posts = postDao.getAll()
        assertEquals(1, posts.size)
        assertEquals("Updated via save", posts[0].content)
    }

    @Test
    fun likeById_shouldToggleLikeStatus() {
        postDao.insert(testPost)
        assertFalse(postDao.getById(testPost.id)?.likedByMe ?: true)

        postDao.likeById(testPost.id)
        assertTrue(postDao.getById(testPost.id)?.likedByMe ?: false)

        postDao.likeById(testPost.id)
        assertFalse(postDao.getById(testPost.id)?.likedByMe ?: true)
    }

    @Test
    fun removeById_shouldRemovePostFromStorage() {
        postDao.insert(testPost)
        assertEquals(1, postDao.getAll().size)

        postDao.removeById(testPost.id)
        assertTrue(postDao.getAll().isEmpty())
    }

    @Test
    fun share_shouldIncrementShareCount() {
        postDao.insert(testPost)
        val initialShares = postDao.getById(testPost.id)?.sharesCount ?: 0

        postDao.share(testPost.id)
        val updatedShares = postDao.getById(testPost.id)?.sharesCount ?: 0

        assertEquals(initialShares + 1, updatedShares)
    }
}