// network/TelegramService.kt (সম্পূর্ণ সংশোধিত)
package com.alsmsrecive.dev.network

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

// এটি টেলিগ্রাম এপিআই-এর সাথে কথা বলবে
interface TelegramApiService {

    @GET("/{token}/sendMessage")
    suspend fun sendMessage(
        @Path("token") token: String, // এখানে এখন "bot" সহ পুরো টোকেনটি আসবে
        @Query("chat_id") chatId: String,
        @Query("text") text: String,

        // --- !!! এই নতুন লাইনটি যোগ করা হয়েছে !!! ---
        @Query("parse_mode") parseMode: String // "Markdown" পাঠানোর জন্য

    ): Response<Void>

}

// টেলিগ্রাম এপিআই-এর জন্য আলাদা রেট্রোফিট অবজেক্ট
object TelegramApiClient {

    // টেলিগ্রামের মূল এপিআই ইউআরএল
    private const val BASE_URL = "https://api.telegram.org/"

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .build()

    val instance: TelegramApiService by lazy {
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        retrofit.create(TelegramApiService::class.java)
    }
}