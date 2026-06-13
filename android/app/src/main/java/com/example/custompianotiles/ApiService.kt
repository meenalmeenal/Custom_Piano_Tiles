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
    suspend fun analyze(@Query("url") url: String, @Query("start_time") startTime: Int): SongData
}

object RetrofitClient {
    private const val BASE_URL = "http://192.168.29.140:8000/"

    val api: ApiService by lazy {
        val client = okhttp3.OkHttpClient.Builder()
            .connectTimeout(120, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(120, java.util.concurrent.TimeUnit.SECONDS)
            .writeTimeout(120, java.util.concurrent.TimeUnit.SECONDS)
            .build()

        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}
