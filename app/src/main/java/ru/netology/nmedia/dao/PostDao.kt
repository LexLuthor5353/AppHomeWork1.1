package ru.netology.nmedia.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import ru.netology.nmedia.entity.PostEntity

@Dao
interface PostDao {
    @Query("SELECT * FROM PostEntity ORDER BY id DESC")
    fun getAll(): LiveData<List<PostEntity>>

    @Query("SELECT * FROM PostEntity WHERE id = :id OR serverId = :id")
    fun getById(id: Long): PostEntity?

    @Query("SELECT * FROM PostEntity WHERE synced = 0")
    suspend fun getNotSynced(): List<PostEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(post: PostEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(posts: List<PostEntity>)

    @Query("UPDATE PostEntity SET content = :content WHERE id = :id")
    suspend fun updateContentById(id: Long, content: String)

    suspend fun save(post: PostEntity) =
        if (post.id == 0L) insert(post) else updateContentById(post.id, post.content)

    @Query(
        """
    UPDATE PostEntity SET
               likes = likes + CASE WHEN likedByMe THEN -1 ELSE 1 END,
               likedByMe = CASE WHEN likedByMe THEN 0 ELSE 1 END
           WHERE id = :id;
           """
    )
    fun likeById(id: Long)
    @Query("DELETE FROM PostEntity WHERE id = :id")
    suspend fun removeById(id: Long)

    @Query("DELETE FROM PostEntity WHERE synced = 1")
    suspend fun removeAllSynced()
    @Query(
        """
    UPDATE PostEntity SET
               share = share + CASE WHEN shared THEN -1 ELSE 1 END,
               shared = CASE WHEN shared THEN 0 ELSE 1 END
           WHERE id = :id;
           """
    )
    fun share(id: Long)
}