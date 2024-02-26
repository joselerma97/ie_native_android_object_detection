package com.ie.real_time_object_detection

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface TipsService {
    @GET("products/tips/")
    fun getTips(@Query("prompt") prompt: String): Call<TipsResponse>
}