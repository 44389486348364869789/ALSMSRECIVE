// network/ApiClient.kt
package com.alsmsrecive.dev.network

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ApiClient {

    private var currentDeviceId: String? = null

    fun setDeviceId(id: String) {
        currentDeviceId = id
    }

    // Obfuscated Base URL (Base64 + Reversed)
    // "http://156.67.104.253:5000" -> reversed -> "0005:352.401.76.651//:ptth" -> Base64 -> "MDAwNTozNTIuNDAxLjc2LjY1MS8vOnB0dGg="
    private fun getBaseUrl(): String {
        val obf = "MDAwNTozNTIuNDAxLjc2LjY1MS8vOnB0dGg="
        val dec = android.util.Base64.decode(obf, android.util.Base64.DEFAULT)
        return String(dec).reversed()
    }

    // এটি ডিবাগিং-এর জন্য, API কল লগ-এ দেখাবে (শুধুমাত্র ডিবাগ মোডে)
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = if (com.alsmsrecive.dev.BuildConfig.DEBUG) {
            HttpLoggingInterceptor.Level.BODY
        } else {
            HttpLoggingInterceptor.Level.NONE // রিলিজ বিল্ডে লগিং সম্পূর্ণ বন্ধ
        }
    }

    private val deviceInterceptor = okhttp3.Interceptor { chain ->
        val original = chain.request()
        val requestBuilder = original.newBuilder()
        currentDeviceId?.let { requestBuilder.header("x-device-id", it) }
        chain.proceed(requestBuilder.build())
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .addInterceptor(deviceInterceptor)
        .build()

    // Retrofit ইন্সট্যান্স তৈরি করা
    val instance: ApiService by lazy {
        val retrofit = Retrofit.Builder()
            .baseUrl(getBaseUrl())
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        retrofit.create(ApiService::class.java)
    }
}