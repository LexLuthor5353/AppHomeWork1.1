package ru.netology.nmedia.api

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import ru.netology.nmedia.dto.Post

interface PostApiService {
    fun getAll(): Call<List<Post>>



}