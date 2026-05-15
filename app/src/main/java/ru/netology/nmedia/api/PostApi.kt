package ru.netology.nmedia.api

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object PostApi {
    private const val BASE_URL = "http://10.0.2.2:9999/"

    val service: PostApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(PostApiService::class.java)
    }
}
