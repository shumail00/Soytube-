package com.example.data.local

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Secure storage for YouTube authentication cookies (SAPISID, SSID, HSID, LOGIN_INFO)
 * using AndroidX EncryptedSharedPreferences.
 */
class SecurePreferences(context: Context) {

    private val sharedPreferences: SharedPreferences = try {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        EncryptedSharedPreferences.create(
            context,
            "soytube_secure_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    } catch (e: Exception) {
        Log.e("SecurePreferences", "Failed to init EncryptedSharedPreferences, falling back to standard mode", e)
        context.getSharedPreferences("soytube_secure_prefs_fallback", Context.MODE_PRIVATE)
    }

    fun saveCookies(cookies: Map<String, String>) {
        sharedPreferences.edit().apply {
            cookies.forEach { (key, value) ->
                putString("cookie_$key", value)
            }
            putBoolean(KEY_IS_LOGGED_IN, true)
            apply()
        }
    }

    fun getCookie(key: String): String? {
        return sharedPreferences.getString("cookie_$key", null)
    }

    fun getCookieHeader(): String {
        val sapisid = getCookie("SAPISID") ?: ""
        val ssid = getCookie("SSID") ?: ""
        val hsid = getCookie("HSID") ?: ""
        val loginInfo = getCookie("LOGIN_INFO") ?: ""

        val list = mutableListOf<String>()
        if (sapisid.isNotEmpty()) list.add("SAPISID=$sapisid")
        if (ssid.isNotEmpty()) list.add("SSID=$ssid")
        if (hsid.isNotEmpty()) list.add("HSID=$hsid")
        if (loginInfo.isNotEmpty()) list.add("LOGIN_INFO=$loginInfo")

        return list.joinToString("; ")
    }

    fun isLoggedIn(): Boolean {
        return sharedPreferences.getBoolean(KEY_IS_LOGGED_IN, false) &&
                (!getCookie("SAPISID").isNullOrEmpty() || !getCookie("LOGIN_INFO").isNullOrEmpty())
    }

    fun clearAuth() {
        sharedPreferences.edit().clear().apply()
    }

    companion object {
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
    }
}
