package com.cineplex.app.util

import android.content.Context
import android.content.ContextWrapper
import android.content.res.AssetManager
import android.content.res.Configuration
import android.content.res.Resources
import java.util.Locale

class LocalizedContext(
    base: Context,
    private val configContext: Context
) : ContextWrapper(base) {

    override fun getResources(): Resources = configContext.resources

    override fun getAssets(): AssetManager = configContext.assets
}

object LocaleHelper {
    const val LANGUAGE_ENGLISH = "en"
    const val LANGUAGE_TAMIL = "ta"

    fun createLocalizedContext(
        baseContext: Context,
        languageCode: String
    ): Context {
        val locale = Locale(languageCode)
        Locale.setDefault(locale)

        val config = Configuration(baseContext.resources.configuration)
        config.setLocale(locale)
        config.setLayoutDirection(locale)

        val configurationContext =
            baseContext.createConfigurationContext(config)

        return LocalizedContext(
            baseContext,
            configurationContext
        )
    }
}
