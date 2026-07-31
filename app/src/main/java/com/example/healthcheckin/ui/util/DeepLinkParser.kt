package com.example.healthcheckin.ui.util

import android.net.Uri
import com.example.healthcheckin.data.auth.PasswordResetSession

object DeepLinkParser {
    fun parsePasswordReset(uri: Uri): PasswordResetSession? {
        uri.fragment?.let { fragment ->
            parseParams(fragment)["access_token"]?.let { token ->
                return PasswordResetSession(
                    accessToken = token,
                    refreshToken = parseParams(fragment)["refresh_token"],
                )
            }
        }
        uri.getQueryParameter("access_token")?.let { token ->
            return PasswordResetSession(
                accessToken = token,
                refreshToken = uri.getQueryParameter("refresh_token"),
            )
        }
        return null
    }

    fun parseRecoveryToken(uri: Uri): String? =
        uri.getQueryParameter("token")
            ?: uri.getQueryParameter("token_hash")
            ?: parseParams(uri.fragment.orEmpty())["token"]

    private fun parseParams(raw: String): Map<String, String> =
        raw.split("&")
            .mapNotNull { part ->
                val idx = part.indexOf('=')
                if (idx <= 0) return@mapNotNull null
                part.substring(0, idx) to part.substring(idx + 1)
            }
            .toMap()
}
