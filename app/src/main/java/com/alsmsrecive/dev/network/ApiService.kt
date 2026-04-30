// ApiService.kt (Fixed for 404 and Data Mismatch)
package com.alsmsrecive.dev.network

import com.alsmsrecive.dev.network.models.LogoutRequest
import com.alsmsrecive.dev.network.models.*
import com.google.gson.annotations.SerializedName
import retrofit2.Response
import retrofit2.http.*

// !!! FIX: Added @SerializedName("ids") to match server's "req.body.ids" !!!
data class DeleteRequest(
    @SerializedName("ids") val messageIds: List<String>
)

data class RestoreRequest(
    @SerializedName("ids") val messageIds: List<String>
)

data class TrashRequest(
    @SerializedName("ids") val messageIds: List<String>
)

// Call Log Data Class
data class CallLog(
    val number: String,
    val type: String,
    val date: Long,
    val duration: String
)

// Call Log Response
data class CallLogResponse(
    @SerializedName("_id")
    val id: String,
    val userId: String,
    val number: String,
    val type: String,
    val date: String,
    val duration: String
)

interface ApiService {

    @POST("/api/register")
    suspend fun registerUser(@Body registerRequest: RegisterRequest): Response<Void>

    @POST("/api/login")
    suspend fun loginUser(@Body loginRequest: LoginRequest): Response<LoginResponse>

    @POST("/api/logout")
    suspend fun logoutUser(@Body request: LogoutRequest): Response<Void>

    @POST("/api/messages")
    suspend fun postMessage(
        @Header("x-auth-token") token: String,
        @Body messageRequest: MessageRequest
    ): Response<Void>

    @GET("/api/messages")
    suspend fun getMessages(@Header("x-auth-token") token: String): Response<List<MessageResponse>>

    // --- Fixed Delete Route ---
    @POST("/api/messages/delete")
    suspend fun deleteMessagesByIds(
        @Header("x-auth-token") token: String,
        @Body request: DeleteRequest
    ): Response<Void>

    // --- !!! FIX 1: Changed DELETE to POST to match Server !!! ---
    @POST("/api/messages/delete-all")
    suspend fun deleteAllMessages(
        @Header("x-auth-token") token: String
    ): Response<Void>

    @GET("/api/messages/trash")
    suspend fun getDeletedMessages(@Header("x-auth-token") token: String): Response<List<MessageResponse>>

    @POST("/api/messages/restore")
    suspend fun restoreMessages(
        @Header("x-auth-token") token: String,
        @Body request: RestoreRequest
    ): Response<Void>

    @POST("/api/messages/trash")
    suspend fun trashMessagesByIds(
        @Header("x-auth-token") token: String,
        @Body request: TrashRequest
    ): Response<Void>


    // --- !!! FIX 2: Changed URL from /api/call-logs to /api/call-logs/sync !!! ---
    @POST("/api/call-logs/sync")
    suspend fun syncCallLogs(
        @Header("x-auth-token") token: String,
        @Body callLogs: List<CallLog>
    ): Response<Void>

    @GET("/api/call-logs")
    suspend fun getCallLogs(
        @Header("x-auth-token") token: String
    ): Response<List<CallLogResponse>>

    @POST("/api/user/telegram")
    suspend fun saveTelegramSettings(
        @Header("x-auth-token") token: String,
        @Body request: TelegramSettingsRequest
    ): Response<Void>
}