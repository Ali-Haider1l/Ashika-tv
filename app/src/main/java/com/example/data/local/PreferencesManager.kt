package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.model.AppLanguage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.security.MessageDigest

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "ashika_prefs")

class PreferencesManager(private val context: Context) {

    private object Keys {
        val ACTIVE_PROFILE = stringPreferencesKey("active_profile")
        val ADMIN_PIN_HASH = stringPreferencesKey("admin_pin_hash")
        val LAN_SYNC_URL = stringPreferencesKey("lan_sync_url")
        val HAS_SEEDED_DEFAULT = booleanPreferencesKey("has_seeded_default")
        val EPG_URL = stringPreferencesKey("epg_url")
        val EPG_LAST_UPDATED = longPreferencesKey("epg_last_updated")
        val IS_BANGLA_MODE = booleanPreferencesKey("is_bangla_mode")
        val APP_LANGUAGE = stringPreferencesKey("app_language")
    }

    val activeProfileFlow: Flow<String> = context.dataStore.data
        .map { prefs -> prefs[Keys.ACTIVE_PROFILE] ?: DEFAULT_PROFILE }

    val hasAdminPinFlow: Flow<Boolean> = context.dataStore.data
        .map { prefs -> !prefs[Keys.ADMIN_PIN_HASH].isNullOrBlank() }

    val lanSyncUrlFlow: Flow<String> = context.dataStore.data
        .map { prefs -> prefs[Keys.LAN_SYNC_URL] ?: "" }

    val epgUrlFlow: Flow<String> = context.dataStore.data
        .map { prefs -> prefs[Keys.EPG_URL] ?: "" }

    val epgLastUpdatedFlow: Flow<Long> = context.dataStore.data
        .map { prefs -> prefs[Keys.EPG_LAST_UPDATED] ?: 0L }

    val isBanglaModeFlow: Flow<Boolean> = context.dataStore.data
        .map { prefs ->
            val savedCode = prefs[Keys.APP_LANGUAGE]
            if (savedCode != null) savedCode == AppLanguage.BANGLA.code
            else prefs[Keys.IS_BANGLA_MODE] ?: true
        }

    val appLanguageFlow: Flow<AppLanguage> = context.dataStore.data
        .map { prefs ->
            val savedCode = prefs[Keys.APP_LANGUAGE]
            if (savedCode != null) {
                AppLanguage.fromCode(savedCode)
            } else {
                val bn = prefs[Keys.IS_BANGLA_MODE] ?: true
                if (bn) AppLanguage.BANGLA else AppLanguage.ENGLISH
            }
        }

    suspend fun setActiveProfile(name: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.ACTIVE_PROFILE] = name
        }
    }

    suspend fun setLanSyncUrl(url: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.LAN_SYNC_URL] = url.trim()
        }
    }

    suspend fun setEpgMetadata(url: String, updatedAt: Long) {
        context.dataStore.edit { prefs ->
            prefs[Keys.EPG_URL] = url.trim()
            prefs[Keys.EPG_LAST_UPDATED] = updatedAt
        }
    }

    suspend fun setBanglaMode(enabled: Boolean) {
        setAppLanguage(if (enabled) AppLanguage.BANGLA else AppLanguage.ENGLISH)
    }

    suspend fun setAppLanguage(language: AppLanguage) {
        context.dataStore.edit { prefs ->
            prefs[Keys.APP_LANGUAGE] = language.code
            prefs[Keys.IS_BANGLA_MODE] = (language == AppLanguage.BANGLA)
        }
    }

    suspend fun hasSeededDefaultPlaylist(): Boolean {
        return context.dataStore.data.first()[Keys.HAS_SEEDED_DEFAULT] ?: false
    }

    suspend fun markDefaultPlaylistSeeded() {
        context.dataStore.edit { prefs ->
            prefs[Keys.HAS_SEEDED_DEFAULT] = true
        }
    }

    suspend fun saveAdminPin(rawPin: String) {
        val hash = sha256(rawPin)
        context.dataStore.edit { prefs ->
            prefs[Keys.ADMIN_PIN_HASH] = hash
        }
    }

    suspend fun verifyAdminPin(rawPin: String): Boolean {
        val storedHash = context.dataStore.data.first()[Keys.ADMIN_PIN_HASH] ?: return false
        return storedHash == sha256(rawPin)
    }

    private fun sha256(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    companion object {
        const val DEFAULT_PROFILE = "Default"
    }
}
