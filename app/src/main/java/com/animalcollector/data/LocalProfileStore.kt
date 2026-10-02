package com.animalcollector.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.profileDataStore by preferencesDataStore(name = "local_profile")

/** A device-only profile; no credentials or identity data leave the phone. */
class LocalProfileStore(private val context: Context) {
    private val nameKey = stringPreferencesKey("researcher_name")
    val name: Flow<String> = context.profileDataStore.data.map { it[nameKey] ?: "Исследователь" }
    suspend fun saveName(name: String) = context.profileDataStore.edit { it[nameKey] = name.trim().ifBlank { "Исследователь" } }
}
