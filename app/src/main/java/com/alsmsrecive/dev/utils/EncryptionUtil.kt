package com.alsmsrecive.dev.utils

import android.util.Base64
import javax.crypto.spec.PBEKeySpec
import javax.crypto.SecretKeyFactory
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

object EncryptionUtil {

    private const val ALGORITHM = "AES"
    private const val CIPHER_TRANSFORMATION = "AES/CBC/PKCS5Padding"
    private const val IV_SIZE = 16
    private const val ITERATIONS = 100000 // PBKDF2 Iterations
    private const val KEY_LENGTH = 256
    
    // একটি শক্তিশালী ফিক্সড Salt ব্যবহার করা হচ্ছে, যাতে সব ফোনে একই চাবি তৈরি হয়
    private val STATIC_SALT = "ALSMSRECIVE_MILITARY_GRADE_SALT".toByteArray(Charsets.UTF_8)
    
    // Key Caching to prevent ANR (Application Not Responding)
    private var cachedPassword = ""
    private var cachedKey: SecretKeySpec? = null

    /**
     * পাসওয়ার্ড থেকে PBKDF2 ব্যবহার করে একটি শক্তিশালী 32-byte (256-bit) চাবি তৈরি করে
     */
    private fun generateKey(password: String): SecretKeySpec {
        if (password == cachedPassword && cachedKey != null) {
            return cachedKey!!
        }
        val spec = PBEKeySpec(password.toCharArray(), STATIC_SALT, ITERATIONS, KEY_LENGTH)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val keyBytes = factory.generateSecret(spec).encoded
        val keySpec = SecretKeySpec(keyBytes, ALGORITHM)
        
        cachedPassword = password
        cachedKey = keySpec
        return keySpec
    }

    /**
     * ডাটা এনক্রিপ্ট করে এবং Base64 স্ট্রিং রিটার্ন করে (IV সহ)
     */
    fun encrypt(data: String?, password: String?): String {
        if (password.isNullOrEmpty() || data.isNullOrEmpty()) return data ?: "" // পাসওয়ার্ড না থাকলে যেমন আছে তেমন পাঠাবে
        try {
            val key = generateKey(password)
            val cipher = Cipher.getInstance(CIPHER_TRANSFORMATION)
            
            // Random IV (Initialization Vector) তৈরি করা
            val ivBytes = ByteArray(IV_SIZE)
            java.security.SecureRandom().nextBytes(ivBytes)
            val ivSpec = IvParameterSpec(ivBytes)
            
            cipher.init(Cipher.ENCRYPT_MODE, key, ivSpec)
            val encryptedBytes = cipher.doFinal(data.toByteArray(Charsets.UTF_8))
            
            // IV এবং Encrypted Data একসাথে যুক্ত করে Base64 করা
            val combinedBytes = ByteArray(IV_SIZE + encryptedBytes.size)
            System.arraycopy(ivBytes, 0, combinedBytes, 0, IV_SIZE)
            System.arraycopy(encryptedBytes, 0, combinedBytes, IV_SIZE, encryptedBytes.size)
            
            return Base64.encodeToString(combinedBytes, Base64.NO_WRAP)
        } catch (e: Exception) {
            e.printStackTrace()
            return data // এরর হলে অন্তত ডাটা হারাবে না
        }
    }

    /**
     * Base64 এনক্রিপ্টেড স্ট্রিং থেকে ডিক্রিপ্ট করে আসল ডাটা বের করে
     */
    fun decrypt(encryptedBase64Data: String?, password: String?): String {
        if (password.isNullOrEmpty() || encryptedBase64Data.isNullOrEmpty()) return encryptedBase64Data ?: ""
        try {
            val combinedBytes = Base64.decode(encryptedBase64Data, Base64.NO_WRAP)
            
            if (combinedBytes.size <= IV_SIZE) return encryptedBase64Data ?: "" // ভ্যালিড নয়
            
            // IV এবং Encrypted Data আলাদা করা
            val ivBytes = ByteArray(IV_SIZE)
            System.arraycopy(combinedBytes, 0, ivBytes, 0, IV_SIZE)
            val encryptedBytes = ByteArray(combinedBytes.size - IV_SIZE)
            System.arraycopy(combinedBytes, IV_SIZE, encryptedBytes, 0, encryptedBytes.size)
            
            val key = generateKey(password)
            val cipher = Cipher.getInstance(CIPHER_TRANSFORMATION)
            val ivSpec = IvParameterSpec(ivBytes)
            
            cipher.init(Cipher.DECRYPT_MODE, key, ivSpec)
            val decryptedBytes = cipher.doFinal(encryptedBytes)
            
            return String(decryptedBytes, Charsets.UTF_8)
        } catch (e: Exception) {
            // e.printStackTrace()
            return encryptedBase64Data ?: "" // ডিক্রিপ্ট করতে না পারলে বা সাধারণ লেখা হলে আগেরটাই দেখাবে
        }
    }
}
