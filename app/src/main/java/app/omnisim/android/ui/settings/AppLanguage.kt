package app.omnisim.android.ui.settings

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

enum class AppLanguage(val languageTag: String) {
    System(""),
    SimplifiedChinese("zh-CN"),
    English("en"),
    ;

    companion object {
        fun fromLanguageTag(languageTag: String?): AppLanguage = when {
            languageTag.isNullOrBlank() -> System
            languageTag.startsWith("zh", ignoreCase = true) -> SimplifiedChinese
            languageTag.startsWith("en", ignoreCase = true) -> English
            else -> System
        }
    }
}

object AppLanguageController {
    fun current(): AppLanguage = AppLanguage.fromLanguageTag(
        AppCompatDelegate.getApplicationLocales()[0]?.toLanguageTag(),
    )

    fun set(language: AppLanguage) {
        val locales = if (language == AppLanguage.System) {
            LocaleListCompat.getEmptyLocaleList()
        } else {
            LocaleListCompat.forLanguageTags(language.languageTag)
        }
        AppCompatDelegate.setApplicationLocales(locales)
    }
}
