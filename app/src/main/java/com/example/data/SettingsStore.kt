package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.domain.UserSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.pocketlyDataStore: DataStore<Preferences> by preferencesDataStore(name = "pocketly_settings")

class SettingsStore(private val context: Context) {

    private object Keys {
        val CURRENCY = stringPreferencesKey("currency")
        val THEME = stringPreferencesKey("theme")
        val REDUCE_MOTION = booleanPreferencesKey("reduce_motion")
        val SOUND_EFFECTS = booleanPreferencesKey("sound_effects")
        val PIN_HASH = stringPreferencesKey("pin_hash")
        val PIN_SALT = stringPreferencesKey("pin_salt")
        val FAILED_PIN_ATTEMPTS = intPreferencesKey("failed_pin_attempts")
        val LOCKOUT_UNTIL_MS = longPreferencesKey("lockout_until_ms")
        val BIOMETRIC = booleanPreferencesKey("biometric")
        val LOCK_AFTER = stringPreferencesKey("lock_after")
        val ONBOARDED = booleanPreferencesKey("onboarded")
        val DEMO_LOADED = booleanPreferencesKey("demo_loaded")
        val PROFILE_NAME = stringPreferencesKey("profile_name")
        val AVATAR_COLOR = stringPreferencesKey("avatar_color")
        val LAST_ACCOUNT_ID = stringPreferencesKey("last_account_id")
        val LAST_CATEGORY_ID = stringPreferencesKey("last_category_id")
    }

    val settingsFlow: Flow<UserSettings> = context.pocketlyDataStore.data
        .catch { e ->
            if (e is IOException) emit(emptyPreferences()) else throw e
        }
        .map { prefs ->
            UserSettings(
                currency = prefs[Keys.CURRENCY] ?: "USD",
                theme = prefs[Keys.THEME] ?: "system",
                reduceMotion = prefs[Keys.REDUCE_MOTION] ?: false,
                soundEffects = prefs[Keys.SOUND_EFFECTS] ?: true,
                pinHash = prefs[Keys.PIN_HASH],
                pinSalt = prefs[Keys.PIN_SALT],
                failedPinAttempts = prefs[Keys.FAILED_PIN_ATTEMPTS] ?: 0,
                lockoutUntilEpochMs = prefs[Keys.LOCKOUT_UNTIL_MS] ?: 0L,
                biometricEnabled = prefs[Keys.BIOMETRIC] ?: false,
                lockAfter = prefs[Keys.LOCK_AFTER] ?: "30s",
                onboarded = prefs[Keys.ONBOARDED] ?: false,
                demoLoaded = prefs[Keys.DEMO_LOADED] ?: false,
                profileName = prefs[Keys.PROFILE_NAME] ?: "Alex",
                avatarColor = prefs[Keys.AVATAR_COLOR] ?: "mint",
                lastAccountId = prefs[Keys.LAST_ACCOUNT_ID],
                lastCategoryId = prefs[Keys.LAST_CATEGORY_ID]
            )
        }

    suspend fun getSnapshot(): UserSettings = settingsFlow.first()

    suspend fun updateCurrency(currency: String) {
        context.pocketlyDataStore.edit { it[Keys.CURRENCY] = currency }
    }

    suspend fun updateTheme(theme: String) {
        context.pocketlyDataStore.edit { it[Keys.THEME] = theme }
    }

    suspend fun updateReduceMotion(reduce: Boolean) {
        context.pocketlyDataStore.edit { it[Keys.REDUCE_MOTION] = reduce }
    }

    suspend fun updateSoundEffects(enabled: Boolean) {
        context.pocketlyDataStore.edit { it[Keys.SOUND_EFFECTS] = enabled }
    }

    suspend fun setPin(hash: String, salt: String) {
        context.pocketlyDataStore.edit {
            it[Keys.PIN_HASH] = hash
            it[Keys.PIN_SALT] = salt
            it[Keys.FAILED_PIN_ATTEMPTS] = 0
            it[Keys.LOCKOUT_UNTIL_MS] = 0L
        }
    }

    suspend fun clearPin() {
        context.pocketlyDataStore.edit {
            it.remove(Keys.PIN_HASH)
            it.remove(Keys.PIN_SALT)
            it[Keys.BIOMETRIC] = false
            it[Keys.FAILED_PIN_ATTEMPTS] = 0
            it[Keys.LOCKOUT_UNTIL_MS] = 0L
        }
    }

    suspend fun updatePinFailures(attempts: Int, lockoutUntilMs: Long) {
        context.pocketlyDataStore.edit {
            it[Keys.FAILED_PIN_ATTEMPTS] = attempts
            it[Keys.LOCKOUT_UNTIL_MS] = lockoutUntilMs
        }
    }

    suspend fun updateBiometric(enabled: Boolean) {
        context.pocketlyDataStore.edit { it[Keys.BIOMETRIC] = enabled }
    }

    suspend fun updateLockAfter(lockAfter: String) {
        context.pocketlyDataStore.edit { it[Keys.LOCK_AFTER] = lockAfter }
    }

    suspend fun updateOnboarded(onboarded: Boolean) {
        context.pocketlyDataStore.edit { it[Keys.ONBOARDED] = onboarded }
    }

    suspend fun updateDemoLoaded(loaded: Boolean) {
        context.pocketlyDataStore.edit { it[Keys.DEMO_LOADED] = loaded }
    }

    suspend fun updateProfile(name: String, avatarColor: String) {
        context.pocketlyDataStore.edit {
            it[Keys.PROFILE_NAME] = name.trim().ifEmpty { "Alex" }
            it[Keys.AVATAR_COLOR] = avatarColor
        }
    }

    suspend fun updateLastUsed(accountId: String?, categoryId: String?) {
        context.pocketlyDataStore.edit {
            if (accountId != null) it[Keys.LAST_ACCOUNT_ID] = accountId
            if (categoryId != null) it[Keys.LAST_CATEGORY_ID] = categoryId
        }
    }
}
