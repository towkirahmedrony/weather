package com.example.util

import android.content.Context
import android.content.res.Configuration
import com.example.domain.model.AppLanguage
import java.util.Locale

object LocaleHelper {
    private const val PREF_NAME = "atmos_locale_sync"
    private const val KEY_LANG = "lang_code"

    fun saveLanguageSync(context: Context, language: AppLanguage) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_LANG, language.code)
            .apply()
    }

    fun getSavedLanguageCode(context: Context): String {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getString(KEY_LANG, AppLanguage.ENGLISH.code) ?: AppLanguage.ENGLISH.code
    }

    fun wrapContext(base: Context): Context {
        val code = getSavedLanguageCode(base)
        val locale = if (code == AppLanguage.BANGLA.code) Locale("bn", "BD") else Locale.ENGLISH
        Locale.setDefault(locale)
        val config = Configuration(base.resources.configuration)
        config.setLocale(locale)
        return base.createConfigurationContext(config)
    }
}
