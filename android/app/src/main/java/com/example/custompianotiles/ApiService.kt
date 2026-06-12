package com.example.custompianotiles

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

data class SongData(
    val onsets: List<Double>,
    val stream_url: String
)

interface ApiService {
    @GET("analyze")
    suspend fun analyze(@Query("url") url: String): SongData
}

object RetrofitClient {
    private const val BASE_URL = "http://10.0.2.2:8000/"

    val api: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}