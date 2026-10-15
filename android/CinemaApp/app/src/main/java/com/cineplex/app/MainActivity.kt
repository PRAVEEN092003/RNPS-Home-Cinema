package com.cineplex.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.compose.rememberNavController
import com.cineplex.app.navigation.CinemaNavGraph
import com.cineplex.app.ui.theme.CinemaTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import com.cineplex.app.util.LocaleHelper
import com.cineplex.app.util.SessionManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val initialLang = remember { sessionManager.getLanguageSync() }
            val currentLanguage by sessionManager.languageFlow.collectAsState(initial = initialLang)
            val baseContext = LocalContext.current
            val localizedContext = remember(currentLanguage) {
                LocaleHelper.createLocalizedContext(baseContext, currentLanguage)
            }

            CompositionLocalProvider(
                LocalContext provides localizedContext,
                LocalConfiguration provides localizedContext.resources.configuration
            ) {
                CinemaTheme {
                    val navController = rememberNavController()
                    CinemaNavGraph(navController = navController)
                }
            }
        }
    }
}
