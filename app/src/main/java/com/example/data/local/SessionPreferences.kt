package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.sessionDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "agri_signal_session"
)

class SessionPreferences(private val context: Context) {

    private val keyActivePhone = stringPreferencesKey("active_farmer_phone")

    val activePhoneFlow: Flow<String?> = context.sessionDataStore.data.map { prefs ->
        prefs[keyActivePhone]
    }

    suspend fun saveLoggedInPhone(phone: String) {
        context.sessionDataStore.edit { prefs ->
            prefs[keyActivePhone] = phone
        }
    }

    suspend fun clearSession() {
        context.sessionDataStore.edit { prefs ->
            prefs.remove(keyActivePhone)
        }
    }
}
