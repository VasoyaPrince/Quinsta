package com.example.quinsta.model

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Header

interface ApiService {

        @GET("v1/apps/package/com.example.quinsta")
        fun getAds():Call<ApiResponse>

}