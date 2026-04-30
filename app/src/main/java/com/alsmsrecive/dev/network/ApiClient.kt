// network/ApiClient.kt
package com.alsmsrecive.dev.network

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ApiClient {

    // আপনার VPS সার্ভারের IP অ্যাড্রেস এবং পোর্ট
    // নিশ্চিত করুন "http://" ব্যবহার করেছেন, "https://" নয়
    private const val BASE_URL = "http://156.67.104.253:5000" // আপনার ছবির IP

    // এটি ডিবাগিং-এর জন্য, API কল লগ-এ দেখাবে
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .build()

    // Retrofit ইন্সট্যান্স তৈরি করা
    val instance: ApiService by lazy {
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        retrofit.create(ApiService::class.java)
    }
}