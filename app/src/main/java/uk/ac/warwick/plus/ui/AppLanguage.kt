package uk.ac.warwick.plus.ui

import android.app.Activity
import android.app.LocaleManager
import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build
import android.os.LocaleList
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.core.content.edit
import java.util.Locale
import uk.ac.warwick.plus.R

enum class AppLanguage(val tag: String, val labelRes: Int) {
    SYSTEM("", R.string.language_system), ENGLISH("en", R.string.language_english),
    CHINESE("zh-CN", R.string.language_chinese);

    companion object {
        fun fromTag(tag: String?): AppLanguage = when (tag?.let(Locale::forLanguageTag)?.language) {
            "en" -> ENGLISH
            "zh" -> CHINESE
            else -> SYSTEM
        }
    }
}

val LocalLanguage = staticCompositionLocalOf<LanguagePreference?> { null }

/** Android 13+ keeps the per-app language itself (shared with Android settings); older versions store it here. */
class LanguagePreference(private val context: Context) {
    fun current(): AppLanguage = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
        AppLanguage.fromTag(context.getSystemService(LocaleManager::class.java).applicationLocales.toLanguageTags())
    else AppLanguage.fromTag(stored(context))

    fun select(language: AppLanguage, activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.getSystemService(LocaleManager::class.java).applicationLocales = LocaleList.forLanguageTags(language.tag)
        } else {
            preferences(context).edit { putString(KEY, language.tag) }
            activity.recreate()
        }
    }

    companion object {
        private const val KEY = "tag"
        private fun preferences(context: Context) = context.getSharedPreferences("language", Context.MODE_PRIVATE)
        private fun stored(context: Context) = preferences(context).getString(KEY, "").orEmpty()

        /** Before Android 13, applies the stored choice to an Application or Activity base context. */
        fun wrap(base: Context): Context {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) return base
            val tag = stored(base)
            val locale = if (tag.isBlank()) Resources.getSystem().configuration.locales[0] else Locale.forLanguageTag(tag)
            Locale.setDefault(locale)
            if (tag.isBlank()) return base
            return base.createConfigurationContext(Configuration(base.resources.configuration).apply { setLocale(locale) })
        }
    }
}
