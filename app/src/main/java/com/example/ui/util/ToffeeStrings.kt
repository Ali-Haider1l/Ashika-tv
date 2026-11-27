package com.example.ui.util

import com.example.data.model.AppLanguage
import com.example.data.model.ChannelFilterTab

object ToffeeStrings {

    private fun pick(
        lang: AppLanguage,
        bn: String,
        en: String,
        hi: String,
        ur: String,
        ar: String
    ): String = when (lang) {
        AppLanguage.BANGLA -> bn
        AppLanguage.ENGLISH -> en
        AppLanguage.HINDI -> hi
        AppLanguage.URDU -> ur
        AppLanguage.ARABIC -> ar
    }

    fun navHome(lang: AppLanguage) = pick(lang, "হোম", "Home", "होम", "ہوم", "الرئيسية")
    fun navGuide(lang: AppLanguage) = pick(lang, "টিভি গাইড", "TV Guide", "टीवी गाइड", "ٹی وی گائیڈ", "دليل البرامج")
    fun navFavorites(lang: AppLanguage) = pick(lang, "পছন্দ", "Favorites", "पसंदीदा", "پسندیدہ", "المفضلة")
    fun navPlaylists(lang: AppLanguage) = pick(lang, "প্লেলিস্ট", "Playlists", "प्लेलिस्ट", "پلے لسٹس", "القوائم")
    fun navMenu(lang: AppLanguage) = pick(lang, "মেনু", "Menu", "मेनू", "مینو", "القائمة")

    fun watchNow(lang: AppLanguage) = pick(lang, "এখনই দেখুন", "Watch Now", "अभी देखें", "ابھی دیکھیں", "شاهد الآن")
    fun seeAll(lang: AppLanguage) = pick(lang, "সব দেখুন", "See All", "सभी देखें", "سب دیکھیں", "عرض الكل")
    fun searchHint(lang: AppLanguage) = pick(
        lang,
        "চ্যানেল বা অনুষ্ঠান খুঁজুন…",
        "Search channels or shows…",
        "चैनल या शो खोजें…",
        "چینل یا پروگرام تلاش کریں…",
        "ابحث عن قناة أو برنامج…"
    )
    fun popularLiveTv(lang: AppLanguage) = pick(lang, "জনপ্রিয় লাইভ টিভি", "Popular Live TV", "लोकप्रिय लाइव टीवी", "مقبول لائیو ٹی وی", "قنوات البث الشائعة")
    fun allChannelsTitle(lang: AppLanguage) = pick(lang, "সকল লাইভ চ্যানেল", "All Live Channels", "सभी लाइव चैनल", "تمام لائیو چینلز", "جميع القنوات المباشرة")
    fun starredQuickAccess(lang: AppLanguage) = pick(lang, "পছন্দের চ্যানেল", "Starred Channels", "पसंदीदा चैनल", "پسندیدہ چینلز", "القنوات المميزة")
    fun continueWatching(lang: AppLanguage) = pick(lang, "সাম্প্রতিক দেখা", "Continue Watching", "देखना जारी रखें", "دیکھنا جاری رکھیں", "متابعة المشاهدة")
    fun onNowLabel(lang: AppLanguage) = pick(lang, "এখন:", "On Now:", "अभी:", "ابھی:", "الآن:")
    fun upNextLabel(lang: AppLanguage) = pick(lang, "পরবর্তী:", "Up Next:", "आगे:", "اگلا:", "التالي:")
    fun channelsCount(count: Int, lang: AppLanguage) = when (lang) {
        AppLanguage.BANGLA -> "$count টি চ্যানেল"
        AppLanguage.ENGLISH -> "$count channels"
        AppLanguage.HINDI -> "$count चैनल"
        AppLanguage.URDU -> "$count چینلز"
        AppLanguage.ARABIC -> "$count قناة"
    }

    fun tabLabel(tab: ChannelFilterTab, lang: AppLanguage): String = when (tab) {
        ChannelFilterTab.ALL -> pick(lang, "সব টিভি", "All TV", "सभी टीवी", "تمام ٹی وی", "كل القنوات")
        ChannelFilterTab.FAVORITES -> pick(lang, "পছন্দ", "Favorites", "पसंदीदा", "پسندیدہ", "المفضلة")
        ChannelFilterTab.RECENT -> pick(lang, "সাম্প্রতিক", "Recent", "हाल ही में", "حالیہ", "الأخيرة")
    }

    fun categoryLabel(raw: String, lang: AppLanguage): String {
        if (lang == AppLanguage.ENGLISH) return raw
        val key = raw.trim().lowercase()
        return when (lang) {
            AppLanguage.BANGLA -> when (key) {
                "all" -> "সব"
                "news" -> "নিউজ"
                "sports", "sport" -> "স্পোর্টস"
                "entertainment" -> "বিনোদন"
                "music" -> "মিউজিক"
                "drama", "natok" -> "নাটক"
                "movies", "movie", "cinema" -> "সিনেমা"
                "kids", "cartoon" -> "কিডস"
                "documentary", "infotainment" -> "ডকুমেন্টারি"
                "islamic", "religion", "religious" -> "ধর্মীয়"
                "bangla", "bengali", "bangladesh" -> "বাংলা"
                "indian", "india", "hindi", "kolkata" -> "ভারতীয়"
                "english", "international" -> "আন্তর্জাতিক"
                "other" -> "অন্যান্য"
                else -> raw
            }
            AppLanguage.HINDI -> when (key) {
                "all" -> "सभी"
                "news" -> "समाचार"
                "sports", "sport" -> "खेल"
                "entertainment" -> "मनोरंजन"
                "music" -> "संगीत"
                "drama", "natok" -> "नाटक"
                "movies", "movie", "cinema" -> "फ़िल्में"
                "kids", "cartoon" -> "बच्चों के लिए"
                "documentary", "infotainment" -> "डॉक्यूमेंट्री"
                "islamic", "religion", "religious" -> "धार्मिक"
                "bangla", "bengali", "bangladesh" -> "बांग्ला"
                "indian", "india", "hindi", "kolkata" -> "भारतीय"
                "english", "international" -> "अंतर्राष्ट्रीय"
                "other" -> "अन्य"
                else -> raw
            }
            AppLanguage.URDU -> when (key) {
                "all" -> "سب"
                "news" -> "خبریں"
                "sports", "sport" -> "کھیل"
                "entertainment" -> "تفریح"
                "music" -> "موسیقی"
                "drama", "natok" -> "ڈرامہ"
                "movies", "movie", "cinema" -> "فلمیں"
                "kids", "cartoon" -> "بچے"
                "documentary", "infotainment" -> "دستاویزی"
                "islamic", "religion", "religious" -> "مذہبی"
                "bangla", "bengali", "bangladesh" -> "بنگلہ"
                "indian", "india", "hindi", "kolkata" -> "بھارتی"
                "english", "international" -> "بین الاقوامی"
                "other" -> "دیگر"
                else -> raw
            }
            AppLanguage.ARABIC -> when (key) {
                "all" -> "الكل"
                "news" -> "أخبار"
                "sports", "sport" -> "رياضة"
                "entertainment" -> "ترفيه"
                "music" -> "موسيقى"
                "drama", "natok" -> "دراما"
                "movies", "movie", "cinema" -> "أفلام"
                "kids", "cartoon" -> "أطفال"
                "documentary", "infotainment" -> "وثائقي"
                "islamic", "religion", "religious" -> "ديني"
                "bangla", "bengali", "bangladesh" -> "بنغالي"
                "indian", "india", "hindi", "kolkata" -> "هندي"
                "english", "international" -> "دولي"
                "other" -> "أخرى"
                else -> raw
            }
            AppLanguage.ENGLISH -> raw
        }
    }

    fun bannerSportsTitle(lang: AppLanguage) = pick(
        lang,
        "লাইভ স্পোর্টস ও ম্যাচ অ্যাকশন",
        "Live Sports & Match Action",
        "लाइव स्पोर्ट्स और मैच एक्शन",
        "لائیو اسپورٹس اور میچ ایکشن",
        "الرياضة المباشرة والمباريات"
    )
    fun bannerSportsSub(lang: AppLanguage) = pick(
        lang,
        "ক্রিকেট, ফুটবল ও স্পোর্টস চ্যানেল স্ট্রিমিং",
        "Live cricket, football & sports streams",
        "क्रिकेट, फुटबॉल और लाइव स्पोर्ट्स स्ट्रीम",
        "کرکٹ، فٹبال اور اسپورٹس اسٹریمنگ",
        "بث مباشر للكريكيت وكرة القدم والرياضة"
    )

    fun bannerEntertainmentTitle(lang: AppLanguage) = pick(
        lang,
        "বাংলা বিনোদন ও নাটক লাইভ",
        "Entertainment & Drama Live",
        "मनोरंजन और ड्रामा लाइव",
        "تفریح اور ڈرامہ لائیو",
        "الترفيه والدراما المباشرة"
    )
    fun bannerEntertainmentSub(lang: AppLanguage) = pick(
        lang,
        "জনপ্রিয় টিভি চ্যানেল ও সিনেমা শো",
        "Top TV channels & live entertainment",
        "लोकप्रिय टीवी चैनल और लाइव शो",
        "مشہور ٹی وی چینلز اور شوز",
        "أشهر القنوات التلفزيونية والعروض المباشرة"
    )

    fun bannerNewsTitle(lang: AppLanguage) = pick(
        lang,
        "২৪/৭ লাইভ নিউজ ও টিভি গাইড",
        "24/7 Live News & TV Guide",
        "24/7 लाइव न्यूज़ और टीवी गाइड",
        "24/7 لائیو نیوز اور ٹی وی گائیڈ",
        "الأخبار المباشرة ودليل البرامج"
    )
    fun bannerNewsSub(lang: AppLanguage) = pick(
        lang,
        "ব্রেকিং নিউজ এবং রিয়েল-টাইম EPG সূচি",
        "Breaking news and live XMLTV schedule",
        "ब्रेकिंग न्यूज़ और लाइव EPG समय-सारणी",
        "تازہ ترین خبریں اور لائیو EPG شیڈول",
        "الأخبار العاجلة وجدول البرامج المباشر"
    )

    fun epgTitle(lang: AppLanguage) = pick(lang, "টিভি গাইড (EPG)", "TV Guide (EPG)", "टीवी गाइड (EPG)", "ٹی وی گائیڈ (EPG)", "دليل البرامج (EPG)")
    fun epgOnNowAcross(lang: AppLanguage) = pick(lang, "এখন চলছে", "On Now Across Channels", "अभी चल रहा है", "ابھی چل رہا ہے", "يعرض الآن")
    fun epgEmptyTitle(lang: AppLanguage) = pick(lang, "কোনো EPG সূচি লোড করা হয়নি", "No EPG schedule loaded", "कोई EPG शेड्यूल लोड नहीं है", "کوئی EPG شیڈول لوڈ نہیں ہے", "لا يوجد دليل برامج محمّل")
    fun epgEmptySub(lang: AppLanguage) = pick(
        lang,
        "লাইভ ও আসন্ন অনুষ্ঠানের সময়সূচি দেখতে XMLTV (.xml বা .xml.gz) লিংক বা ফাইল লোড করুন।",
        "Load an XMLTV EPG feed URL (.xml or .xml.gz) or import a local file to see live & upcoming schedules.",
        "लाइव और आगामी शो देखने के लिए XMLTV (.xml या .xml.gz) URL या फ़ाइल लोड करें।",
        "لائیو اور آنے والے پروگرام دیکھنے کے لیے XMLTV لنک یا فائل لوڈ کریں۔",
        "حمّل رابط XMLTV أو ملفاً محلياً لعرض جدول البرامج المباشر."
    )

    fun favoritesTitle(lang: AppLanguage) = pick(lang, "পছন্দের চ্যানেলসমূহ", "Starred Channels", "पसंदीदा चैनल", "پسندیدہ چینلز", "القنوات المفضلة")
    fun favoritesEmptyTitle(lang: AppLanguage) = pick(lang, "এখনো কোনো পছন্দের চ্যানেল নেই", "No starred channels yet", "अभी कोई पसंदीदा चैनल नहीं है", "ابھی کوئی پسندیدہ چینل نہیں", "لا توجد قنوات مفضلة بعد")
    fun favoritesEmptySub(lang: AppLanguage) = pick(
        lang,
        "যেকোনো চ্যানেল কার্ড বা প্লেয়ার বারে স্টার আইকনে ট্যাপ করে এখানে পিন করুন।",
        "Tap the star icon on any channel card or player bar to pin it here for instant access.",
        "तुरंत एक्सेस के लिए किसी भी चैनल कार्ड या प्लेयर पर स्टार आइकन टैप करें।",
        "فوری رسائی کے لیے کسی بھی چینل کارڈ یا پلیئر پر اسٹار آئیکن دبائیں۔",
        "اضغط على أيقونة النجمة في أي قناة لتثبيتها هنا."
    )
    fun browseChannels(lang: AppLanguage) = pick(lang, "চ্যানেল দেখুন", "Browse Channels", "चैनल ब्राउज़ करें", "چینلز دیکھیں", "تصفح القنوات")

    fun menuStreamingSection(lang: AppLanguage) = pick(lang, "স্ট্রিমিং ও গাইড", "Streaming & Guide", "स्ट्रीमिंग और गाइड", "اسٹریمنگ اور گائیڈ", "البث والدليل")
    fun menuToolsSection(lang: AppLanguage) = pick(lang, "টুলস ও ইউটিলিটি", "Tools & Utilities", "टूल्स और उपयोगिताएँ", "ٹولز اور سہولیات", "الأدوات والمرافق")
    fun menuSystemSection(lang: AppLanguage) = pick(lang, "সিস্টেম ও পোর্টাল", "System & Portal", "सिस्टम और पोर्टल", "سسٹم اور پورٹل", "النظام والبوابة")

    fun languageTitle(lang: AppLanguage) = pick(lang, "অ্যাপের ভাষা (Language)", "App Language", "ऐप की भाषा (Language)", "ایپ کی زبان (Language)", "لغة التطبيق (Language)")
    fun languageSub(lang: AppLanguage) = "${pick(lang, "বর্তমান:", "Current:", "वर्तमान:", "موجودہ:", "الحالية:")} ${lang.nativeName} (${lang.englishName})"
    fun chooseLanguageTitle(lang: AppLanguage) = pick(lang, "ভাষা নির্বাচন করুন", "Choose Language", "भाषा चुनें", "زبان منتخب کریں", "اختر اللغة")

    fun pipMiniLabel(lang: AppLanguage) = pick(lang, "মিনি প্লেয়ার", "Mini PiP", "मिनी PiP", "منی PiP", "مشغل مصغر")
    fun pipSystemLabel(lang: AppLanguage) = pick(lang, "PiP মোড", "System PiP", "सिस्टम PiP", "سسٹم PiP", "صورة داخل صورة")
    fun pipUnavailableMsg(lang: AppLanguage) = pick(
        lang,
        "এই ডিভাইসে সিস্টেম PiP সমর্থিত নয়",
        "System Picture-in-Picture is not supported on this device.",
        "इस डिवाइस पर सिस्टम PiP समर्थित नहीं है।",
        "اس ڈیوائس پر سسٹم PiP دستیاب نہیں ہے۔",
        "وضع صورة داخل صورة غير مدعوم على هذا الجهاز."
    )

    fun toffeeSyncTitle(lang: AppLanguage) = pick(
        lang,
        "টফি ও বাংলাদেশি লাইভ টিভি সিঙ্ক",
        "Toffee & Bangladesh Live TV Sync",
        "टॉफ़ी और बांग्लादेश लाइव टीवी सिंक",
        "ٹافی اور بنگلہ دیش لائیو ٹی وی سنک",
        "مزامنة قنوات توفي وبنغلاديش المباشرة"
    )
    fun toffeeSyncSub(lang: AppLanguage) = pick(
        lang,
        "toffeelive.com, FreeHunter HDTV এবং বাংলা চ্যানেলগুলো একসাথে যুক্ত করুন।",
        "Extract channels from toffeelive.com & sync FreeHunter HDTV + BD/Bengali live TV feeds.",
        "toffeelive.com, FreeHunter HDTV और बंगाली लाइव टीवी फ़ीड सिंक करें।",
        "toffeelive.com، FreeHunter HDTV اور بنگالی لائیو ٹی وی چینلز سنک کریں۔",
        "استيراد القنوات من toffeelive.com وقوائم البث المباشر."
    )
    fun toffeeSyncButton(lang: AppLanguage) = pick(
        lang,
        "সব টফি ও BD চ্যানেল আনুন",
        "Sync All Channels",
        "सभी चैनल सिंक करें",
        "تمام چینلز سنک کریں",
        "مزامنة جميع القنوات"
    )

    // Real-time stream health diagnostics labels
    fun diagnosticsTitle(lang: AppLanguage) = pick(lang, "স্ট্রিম হেলথ", "Stream Health", "स्ट्रीम हेल्थ", "اسٹریم ہیلتھ", "تشخيص البث")
    fun diagBitrateLabel(lang: AppLanguage) = pick(lang, "বিটরেট", "Bitrate", "बिटरेट", "بٹ ریٹ", "معدل البت")
    fun diagResolutionLabel(lang: AppLanguage) = pick(lang, "রেজোলিউশন", "Resolution", "रिज़ॉल्यूशन", "ریزولوشن", "الدقة")
    fun diagFrameRateLabel(lang: AppLanguage) = pick(lang, "ফ্রেম রেট", "Frame Rate", "फ़्रेम रेट", "فریم ریٹ", "معدل الإطارات")
    fun diagBufferLabel(lang: AppLanguage) = pick(lang, "বাফার", "Buffer", "बफ़र", "بفر", "التخزين المؤقت")
    fun diagCodecLabel(lang: AppLanguage) = pick(lang, "কোডেক", "Codec", "कोडेक", "کوڈیک", "الترميز")
    fun diagDroppedLabel(lang: AppLanguage) = pick(lang, "ড্রপড ফ্রেম", "Dropped", "ड्रॉप्ड", "ڈراپڈ", "الإطارات المفقودة")
    fun cdDiagnosticsToggle(lang: AppLanguage) = pick(
        lang,
        "রিয়েল-টাইম স্ট্রিম হেলথ প্যানেল",
        "Toggle real-time stream health diagnostics",
        "रियल-टाइम स्ट्रीम हेल्थ पैनल टॉगल करें",
        "ریئل ٹائم اسٹریم ہیلتھ پینل دکھائیں",
        "تبديل لوحة تشخيص البث المباشر"
    )
}
