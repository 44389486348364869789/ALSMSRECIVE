package com.alsmsrecive.dev.network.models

import com.google.gson.annotations.SerializedName

data class UserProfileStats(
    @SerializedName("totalMessages") val totalMessages: Int,
    @SerializedName("totalCallLogs") val totalCallLogs: Int,
    @SerializedName("trashedMessages") val trashedMessages: Int
)

data class ActiveDeviceInfo(
    @SerializedName("deviceName") val deviceName: String,
    @SerializedName("loginTime") val loginTime: String
)

data class UserProfileResponse(
    @SerializedName("email") val email: String?,
    @SerializedName("phone") val phone: String?,
    @SerializedName("role") val role: String,
    @SerializedName("createdAt") val createdAt: String,
    @SerializedName("planExpiresAt") val planExpiresAt: String?,
    @SerializedName("deviceLimit") val deviceLimit: Int,
    @SerializedName("activeSessions") val activeSessions: Int,
    @SerializedName("activeDevices") val activeDevices: List<ActiveDeviceInfo>,
    @SerializedName("hasTelegram") val hasTelegram: Boolean,
    @SerializedName("stats") val stats: UserProfileStats
)
