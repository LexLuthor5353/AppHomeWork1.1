package ru.netology.nmedia.api

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import ru.netology.nmedia.dto.Post

interface PostApiService {
    @GET("api/slow/posts")
    fun getAll(): Call<List<Post>>

    @POST("api/slow/posts")
    fun save(@Body post: Post): Call<Post>

    @DELETE("api/slow/posts/{id}")
    fun removeById(@Path("id") id: Long): Call<Unit>

    @POST("api/posts/{id}/likes")
    fun likeById(@Path("id") id: Long): Call<Post>

    @DELETE("api/posts/{id}/likes")
    fun dislikeById(@Path("id") id: Long): Call<Post>
}
