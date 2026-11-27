package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
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
    }

    val activeProfileFlow: Flow<String> = context.dataStore.data
        .map { prefs -> prefs[Keys.ACTIVE_PROFILE] ?: DEFAULT_PROFILE }

    val hasAdminPinFlow: Flow<Boolean> = context.dataStore.data
        .map { prefs -> !prefs[Keys.ADMIN_PIN_HASH].isNullOrBlank() }

    val lanSyncUrlFlow: Flow<String> = context.dataStore.data
        .map { prefs -> prefs[Keys.LAN_SYNC_URL] ?: "" }

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
