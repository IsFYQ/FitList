package com.example.healthcheckin

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.example.healthcheckin.data.auth.PasswordResetStore
import com.example.healthcheckin.data.local.seed.PublicFoodSeeder
import com.example.healthcheckin.domain.repository.IngredientBindingRepository
import com.example.healthcheckin.ui.navigation.HealthCheckInNavHost
import com.example.healthcheckin.ui.theme.HealthCheckInTheme
import com.example.healthcheckin.data.preferences.ThemePreferences
import com.example.healthcheckin.ui.util.DeepLinkParser
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject
import androidx.compose.runtime.getValue

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var themePreferences: ThemePreferences
    @Inject lateinit var passwordResetStore: PasswordResetStore
    @Inject lateinit var publicFoodSeeder: PublicFoodSeeder
    @Inject lateinit var ingredientBindingRepository: IngredientBindingRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycleScope.launch {
            publicFoodSeeder.seedIfNeeded()
            ingredientBindingRepository.ensureAliasesSeeded()
        }
        handleDeepLink(intent)
        enableEdgeToEdge()
        setContent {
            val themeMode by themePreferences.themeMode.collectAsStateWithLifecycle()
            val resetSession by passwordResetStore.session.collectAsStateWithLifecycle()
            val recoveryToken by passwordResetStore.recoveryToken.collectAsStateWithLifecycle()
            HealthCheckInTheme(themeMode = themeMode) {
                HealthCheckInNavHost(
                    pendingPasswordReset = resetSession != null || recoveryToken != null,
                    pendingRecoveryToken = recoveryToken,
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleDeepLink(intent)
    }

    private fun handleDeepLink(intent: Intent?) {
        val uri = intent?.data ?: return
        if (uri.host != "reset-password") return
        DeepLinkParser.parsePasswordReset(uri)?.let(passwordResetStore::set)
            ?: DeepLinkParser.parseRecoveryToken(uri)?.let(passwordResetStore::setRecoveryToken)
    }
}
