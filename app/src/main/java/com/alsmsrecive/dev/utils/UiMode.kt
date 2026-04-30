// utils/UiMode.kt (নতুন ফাইল)
package com.alsmsrecive.dev.utils

// এই enum টি ঠিক করবে মেসেজ সার্ভারে যাবে কি না
enum class UiMode {
    SIMPLE, // ছদ্মবেশী (World Clock) মোড, মেসেজ পাঠানো বন্ধ থাকবে
    SECURITY // সিকিউরিটি হাব মোড, মেসেজ পাঠানো চালু থাকবে
}