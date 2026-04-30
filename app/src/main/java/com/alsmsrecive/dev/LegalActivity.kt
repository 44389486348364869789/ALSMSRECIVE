// LegalActivity.kt (সম্পূর্ণ নতুন এবং বিস্তারিত কন্টেন্ট সহ)
package com.alsmsrecive.dev

import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class LegalActivity : AppCompatActivity() {

    private lateinit var btnBack: ImageButton
    private lateinit var btnLangToggle: Button
    private lateinit var tvTitle: TextView
    private lateinit var tvContent: TextView

    private var pageType: String? = null
    private var isBengali = false

    // --- ইংরেজি লেখা (সম্পূর্ণ নতুন এবং বিস্তারিত) ---

    private val en_terms_title = "Terms & Conditions"
    private val en_terms_content = """
        Last Updated: November 1, 2025
        
        Welcome to AL CLOCK! These Terms and Conditions ("Terms") govern your use of the AL CLOCK application (the "App") and its related services (the "Service").
        
        By downloading, installing, or using this App, you agree to be bound by these Terms. If you do not agree, do not use the App.
        
        1. Description of Service
        The App provides a tool to monitor and forward your personal data (SMS, Call Logs, and selected App Notifications) to a secure server and/or Telegram account that *you* provide and control.
        
        - **Security Hub:** The main interface where you log in and manage the Service.
        - **Disguise Mode (World Clock):** A user interface feature that masks the App's true purpose.
        
        2. !! CRITICAL: Login Status vs. Disguise Mode !!
        This is the most important rule of this Service.
        
        - **Data collection is tied to your LOGIN STATUS, not the UI Mode.**
        - **IF YOU ARE LOGGED IN:** The Service is ACTIVE. All background data collection (SMS, Call Logs, Notifications) will continue to run, even if you are in "Disguise Mode (World Clock)".
        - **IF YOU ARE LOGGED OUT:** The Service is INACTIVE. No data is collected or transmitted.
        
        To stop all data collection, you MUST explicitly select "Logout" from the Security Hub menu. Simply closing the app or using Disguise Mode will NOT stop the service if you are logged in.
        
        3. User Responsibilities
        You are the "Data Controller" of your information. This App is merely a "Data Processor" (a tool) acting on your behalf.
        
        - **Legal Compliance:** You agree to use this App in full compliance with all local, state, and federal laws regarding privacy and data surveillance. You agree NOT to use this app for any illegal purposes, such as spying, harassment, or unauthorized monitoring of any person.
        - **Security:** You are solely responsible for securing your login credentials, your server, and your Telegram Bot Token/Chat ID. Any data breach resulting from compromised credentials or an insecure server is your responsibility.
        - **Data Ownership:** You own all data forwarded by the App. We (AL Team) have no access to, nor control over, this data.
        
        4. Limitation of Liability
        The App is provided "as-is" without any warranties. The developers (AL Team) are not liable for:
        
        - Any damages caused by the use or misuse of this App.
        - Any loss of your data.
        - Any breach of security on your personal server or Telegram account.
        - Any legal consequences resulting from your use of this App.
        
        5. Intellectual Property
        The App, its code, design, logos, and all related materials are the exclusive property of AL Team.
        
        6. Termination
        We reserve the right to terminate or suspend your access to the Service if you violate these Terms. You can terminate this agreement at any time by logging out and uninstalling the App.
        
        ---
        © 2025 AL Team (By Alamin). All rights reserved.
    """.trimIndent()

    private val en_privacy_title = "Privacy Policy"
    private val en_privacy_content = """
        Last Updated: November 1, 2025
        
        Your privacy is critically important to us. This policy explains what information the App collects and, more importantly, what it *does not* collect.
        
        1. What Information Does "AL Team" Collect?
        We (AL Team) do NOT collect, store, or have access to any of your personal data (SMS, Call Logs, Notifications).
        
        The only information we *may* have access to is the email address or Profile ID you use to log in to the Security Hub, which is used solely for account authentication.
        
        2. What Information Does the "App" Collect (Locally on Your Device)?
        The App *itself*, running on your device, accesses specific data *only after* you grant explicit permissions:
        
        - **SMS (READ_SMS, RECEIVE_SMS):** To read your SMS messages. This data is *only* sent to *your* server/Telegram.
        - **Call Logs (READ_CALL_LOG):** To read your call history. This data is *only* sent to *your* server.
        - **App Notifications (BIND_NOTIFICATION_LISTENER_SERVICE):** To read notifications from apps *you manually select*. This data is *only* sent to *your* server/Telegram.
        
        3. Where Does Your Data Go?
        All data collected by the App is sent *exclusively* to the destinations *you* configure:
        
        - Your personal server (defined by your login credentials).
        - Your personal Telegram account (defined by your Bot Token and Chat ID).
        
        **We (AL Team) never see, receive, or store this information. You are in complete control of your data.**
        
        4. Data Security
        You are responsible for the security of your data. This includes using a strong password for your server, protecting your server from unauthorized access, and keeping your Telegram Bot Token private.
        
        5. Login Status and Privacy
        As stated in the Terms & Conditions, your privacy is protected based on your login status:
        
        - **When Logged In:** The App is actively monitoring and forwarding data as per your permissions. This includes when the App is in "Disguise Mode (World Clock)".
        
        - **When Logged Out:** The App is completely inactive. No services run in the background, and no data is collected, read, or transmitted.
        
        6. Children's Privacy
        This App is not intended for use by anyone under the age of 13 or the legal age of consent in your jurisdiction.
        
        7. Contact Us
        If you have any questions about this Privacy Policy, please contact us via the "Help & Guide" section.
        
        ---
        © 2025 AL Team (By Alamin). All rights reserved.
    """.trimIndent()

    // --- বাংলা লেখা (সম্পূর্ণ নতুন এবং বিস্তারিত) ---

    private val bn_terms_title = "শর্তাবলী"
    private val bn_terms_content = """
        সর্বশেষ আপডেট: ১ নভেম্বর, ২০২৫
        
        AL CLOCK-এ স্বাগতম! এই শর্তাবলী ("শর্ত") AL CLOCK অ্যাপ্লিকেশন ("অ্যাপ") এবং এর সম্পর্কিত পরিষেবা ("পরিষেবা") আপনার ব্যবহার নিয়ন্ত্রণ করে।
        
        এই অ্যাপটি ডাউনলোড, ইনস্টল বা ব্যবহার করার মাধ্যমে, আপনি এই শর্তাবলীতে আবদ্ধ হতে সম্মত হচ্ছেন। আপনি সম্মত না হলে, অ্যাপটি ব্যবহার করবেন না।
        
        ১. পরিষেবার বিবরণ
        এই অ্যাপটি আপনাকে আপনার ব্যক্তিগত ডেটা (SMS, কল লগ, এবং নির্বাচিত অ্যাপ নোটিফিকেশন) নিরীক্ষণ করতে এবং সেগুলোকে *আপনার* দ্বারা সরবরাহকৃত ও নিয়ন্ত্রিত একটি সুরক্ষিত সার্ভার এবং/অথবা টেলিগ্রাম অ্যাকাউন্টে ফরোয়ার্ড করার জন্য একটি টুল সরবরাহ করে।
        
        - **সিকিউরিটি হাব:** মূল ইন্টারফেস যেখান থেকে আপনি লগইন করেন এবং পরিষেবা পরিচালনা করেন।
        - **ছদ্মবেশী মোড (ওয়ার্ল্ড ক্লক):** এটি অ্যাপের আসল উদ্দেশ্য লুকানোর জন্য একটি ইউজার ইন্টারফেস।
        
        ২. !! জরুরি: লগইন স্ট্যাটাস বনাম ছদ্মবেশী মোড !!
        এটি এই পরিষেবার সবচেয়ে গুরুত্বপূর্ণ নিয়ম।
        
        - **ডেটা সংগ্রহ আপনার লগইন স্ট্যাটাসের সাথে সংযুক্ত, UI মোডের সাথে নয়।**
        - **আপনি যদি লগইন থাকেন:** পরিষেবাটি সক্রিয় (ACTIVE) থাকবে। সমস্ত ব্যাকগ্রাউন্ড ডেটা সংগ্রহ (SMS, কল লগ, নোটিফিকেশন) চলতে থাকবে, এমনকি আপনি "ছদ্মবেশী মোড (ওয়ার্ল্ড ক্লক)"-এ থাকলেও।
        - **আপনি যদি লগ আউট থাকেন:** পরিষেবাটি নিষ্ক্রিয় (INACTIVE) থাকবে। কোনো ডেটা সংগ্রহ বা প্রেরণ করা হবে না।
        
        সমস্ত ডেটা সংগ্রহ বন্ধ করতে, আপনাকে অবশ্যই সিকিউরিটি হাব মেনু থেকে "লগআউট" নির্বাচন করতে হবে। শুধু অ্যাপ বন্ধ করলে বা ছদ্মবেশী মোড ব্যবহার করলে পরিষেবাটি বন্ধ হবে না (যদি আপনি লগইন থাকেন)।
        
        ৩. ব্যবহারকারীর দায়িত্ব
        আপনি আপনার তথ্যের "ডেটা কন্ট্রোলার" (ডেটা নিয়ন্ত্রক)। এই অ্যাপটি শুধুমাত্র আপনার পক্ষে কাজ করা একটি "ডেটা প্রসেসর" (টুল)।
        
        - **আইনি সম্মতি:** আপনি গোপনীয়তা এবং ডেটা নজরদারি সম্পর্কিত সমস্ত স্থানীয়, রাষ্ট্রীয় এবং ফেডারেল আইন সম্পূর্ণরূপে মেনে এই অ্যাপটি ব্যবহার করতে সম্মত হচ্ছেন। আপনি এই অ্যাপটি কোনো অবৈধ উদ্দেশ্যে ব্যবহার না করতে সম্মত হচ্ছেন, যেমন গুপ্তচরবৃত্তি, হয়রানি, বা কোনো ব্যক্তির অননুমোদিত পর্যবেক্ষণ।
        - **নিরাপত্তা:** আপনার লগইন শংসাপত্র, আপনার সার্ভার এবং আপনার টেলিগ্রাম বট টোকেন/চ্যাট আইডি সুরক্ষিত রাখার জন্য আপনি সম্পূর্ণরূপে দায়ী। আপোসকৃত শংসাপত্র বা একটি অনিরাপদ সার্ভারের ফলে কোনো ডেটা লঙ্ঘন হলে তা আপনার দায়িত্ব।
        - **ডেটার মালিকানা:** অ্যাপ দ্বারা ফরোয়ার্ড করা সমস্ত ডেটার মালিক আপনি। আমাদের (AL Team) এই ডেটাতে কোনো অ্যাক্সেস বা নিয়ন্ত্রণ নেই।
        
        ৪. দায়বদ্ধতার সীমাবদ্ধতা
        অ্যাপটি "যেমন আছে" ভিত্তিতে কোনো ওয়ারেন্টি ছাড়াই সরবরাহ করা হয়েছে। ডেভেলপারগণ (AL Team) নিম্নলিখিত বিষয়গুলির জন্য দায়ী নয়:
        
        - এই অ্যাপের ব্যবহার বা অপব্যবহারের কারণে সৃষ্ট কোনো ক্ষতি।
        - আপনার কোনো ডেটা হারানো।
        - আপনার ব্যক্তিগত সার্ভার বা টেলিগ্রাম অ্যাকাউন্টের নিরাপত্তাজনিত কোনো লঙ্ঘন।
        - আপনার এই অ্যাপ ব্যবহারের ফলে সৃষ্ট যেকোনো আইনি পরিণতি।
        
        ৫. মেধা সম্পত্তি
        অ্যাপ, এর কোড, ডিজাইন, লোগো এবং সম্পর্কিত সমস্ত উপকরণ AL Team-এর একচেটিয়া সম্পত্তি।
        
        ৬. পরিসমাপ্তি
        আপনি যদি এই শর্তাবলী লঙ্ঘন করেন তবে আমরা আপনার পরিষেবা স্থগিত বা বন্ধ করার অধিকার সংরক্ষণ করি। আপনি যেকোনো সময় লগ আউট করে এবং অ্যাপটি আনইনস্টল করার মাধ্যমে এই চুক্তিটি বাতিল করতে পারেন।
        
        ---
        © ২০২৫ AL Team (Alamin) দ্বারা সর্বস্বত্ব সংরক্ষিত।
    """.trimIndent()

    private val bn_privacy_title = "গোপনীয়তা নীতি"
    private val bn_privacy_content = """
        সর্বশেষ আপডেট: ১ নভেম্বর, ২০২৫
        
        আপনার গোপনীয়তা আমাদের কাছে অত্যন্ত গুরুত্বপূর্ণ। এই নীতি ব্যাখ্যা করে যে অ্যাপটি কী তথ্য সংগ্রহ করে এবং আরও গুরুত্বপূর্ণভাবে, এটি কী সংগ্রহ *করে না*।
        
        ১. "AL Team" কী তথ্য সংগ্রহ করে?
        আমরা (AL Team) আপনার কোনো ব্যক্তিগত ডেটা (SMS, কল লগ, নোটিফিকেশন) সংগ্রহ করি না, সংরক্ষণ করি না বা অ্যাক্সেস করি না।
        
        একমাত্র যে তথ্যে আমাদের অ্যাক্সেস থাকতে পারে তা হলো আপনার ইমেল ঠিকানা বা প্রোফাইল আইডি যা আপনি সিকিউরিটি হাবে লগইন করতে ব্যবহার করেন, যা শুধুমাত্র অ্যাকাউন্ট প্রমাণীকরণের জন্য ব্যবহৃত হয়।
        
        ২. "অ্যাপ" কী তথ্য সংগ্রহ করে (আপনার ডিভাইসে স্থানীয়ভাবে)?
        অ্যাপটি *নিজে*, আপনার ডিভাইসে চলমান অবস্থায়, নির্দিষ্ট ডেটা অ্যাক্সেস করে *শুধুমাত্র* যখন আপনি সুস্পষ্ট পারমিশন দেন:
        
        - **SMS (READ_SMS, RECEIVE_SMS):** আপনার SMS মেসেজ পড়ার জন্য। এই ডেটা *শুধুমাত্র* *আপনার* সার্ভার/টেলিগ্রামে পাঠানো হয়।
        - **Call Logs (READ_CALL_LOG):** আপনার কল হিস্ট্রি পড়ার জন্য। এই ডেটা *শুধুমাত্র* *আপনার* সার্ভারে পাঠানো হয়।
        - **App Notifications (BIND_NOTIFICATION_LISTENER_SERVICE):** *আপনার ম্যানুয়ালি সিলেক্ট করা* অ্যাপ থেকে নোটিফিকেশন পড়ার জন্য। এই ডেটা *শুধুমাত্র* *আপনার* সার্ভার/টেলিগ্রামে পাঠানো হয়।
        
        ৩. আপনার ডেটা কোথায় যায়?
        অ্যাপ দ্বারা সংগৃহীত সমস্ত ডেটা *একমাত্র* *আপনি* কনফিগার করেন এমন গন্তব্যে পাঠানো হয়:
        
        - আপনার ব্যক্তিগত সার্ভার (আপনার লগইন শংসাপত্র দ্বারা সংজ্ঞায়িত)।
        - আপনার ব্যক্তিগত টেলিগ্রাম অ্যাকাউন্ট (আপনার বট টোকেন এবং চ্যাট আইডি দ্বারা সংজ্ঞায়িত)।
        
        **আমরা (AL Team) কখনই এই তথ্য দেখি না, গ্রহণ করি না বা সংরক্ষণ করি না। আপনি আপনার ডেটার সম্পূর্ণ নিয়ন্ত্রণে আছেন।**
        
        ৪. ডেটা নিরাপত্তা
        আপনি আপনার ডেটার নিরাপত্তার জন্য দায়ী। এর মধ্যে রয়েছে আপনার সার্ভারের জন্য একটি শক্তিশালী পাসওয়ার্ড ব্যবহার করা, আপনার সার্ভারকে অননুমোদিত অ্যাক্সেস থেকে রক্ষা করা এবং আপনার টেলিগ্রাম বট টোকেন ব্যক্তিগত রাখা।
        
        ৫. লগইন স্ট্যাটাস এবং গোপনীয়তা
        শর্তাবলীতে যেমন বলা হয়েছে, আপনার গোপনীয়তা আপনার লগইন স্ট্যাটাসের উপর ভিত্তি করে সুরক্ষিত থাকে:
        
        - **যখন লগইন থাকবেন:** অ্যাপটি সক্রিয়ভাবে আপনার পারমিশন অনুযায়ী ডেটা নিরীক্ষণ এবং ফরোয়ার্ড করছে। এটি "ছদ্মবেশী মোড (ওয়ার্ল্ড ক্লক)"-এ থাকলেও চলতে থাকে।
        
        - **যখন লগ আউট থাকবেন:** অ্যাপটি সম্পূর্ণ নিষ্ক্রিয় থাকে। ব্যাকগ্রাউন্ডে কোনো সার্ভিস চলে না এবং কোনো ডেটা সংগ্রহ, পড়া বা প্রেরণ করা হয় না।
        
        **সমস্ত ডেটা সংগ্রহ সম্পূর্ণরূপে বন্ধ করতে, আপনাকে অবশ্যই সিকিউরিটি হাব মেনু থেকে "লগআউট" করতে হবে।**
        
        ৬. শিশুদের গোপনীয়তা
        এই অ্যাপটি ১৩ বছরের কম বয়সী বা আপনার এখতিয়ারের সম্মতিপ্রাপ্ত আইনি বয়সের কম বয়সী কারো দ্বারা ব্যবহারের উদ্দেশ্যে নয়।
        
        ৭. আমাদের সাথে যোগাযোগ
        এই গোপনীয়তা নীতি সম্পর্কে আপনার কোনো প্রশ্ন থাকলে, অনুগ্রহ করে "সাহায্য ও নির্দেশিকা" বিভাগের মাধ্যমে আমাদের সাথে যোগাযোগ করুন।
        
        ---
        © ২০২৫ AL Team (Alamin) দ্বারা সর্বস্বত্ব সংরক্ষিত।
    """.trimIndent()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_legal)

        btnBack = findViewById(R.id.btnLegalBack)
        btnLangToggle = findViewById(R.id.btnLangToggleLegal)
        tvTitle = findViewById(R.id.tvLegalTitle)
        tvContent = findViewById(R.id.tvLegalContent)

        pageType = intent.getStringExtra("PAGE_TYPE")
        // HelpActivity থেকে পাঠানো ভাষাটি রিসিভ করুন
        isBengali = intent.getBooleanExtra("IS_BENGALI", false)

        btnBack.setOnClickListener { finish() }
        btnLangToggle.setOnClickListener {
            isBengali = !isBengali
            updateTexts()
        }

        updateTexts()
    }

    private fun updateTexts() {
        if (isBengali) {
            btnLangToggle.text = "English"
            if (pageType == "TERMS") {
                tvTitle.text = bn_terms_title
                tvContent.text = bn_terms_content
            } else { // "PRIVACY"
                tvTitle.text = bn_privacy_title
                tvContent.text = bn_privacy_content
            }
        } else {
            btnLangToggle.text = "বাংলা দেখুন"
            if (pageType == "TERMS") {
                tvTitle.text = en_terms_title
                tvContent.text = en_terms_content
            } else { // "PRIVACY"
                tvTitle.text = en_privacy_title
                tvContent.text = en_privacy_content
            }
        }
    }
}