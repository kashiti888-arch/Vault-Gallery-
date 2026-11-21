package com.example.vaultgallery.data.repository

import com.example.vaultgallery.data.db.AppSettings
import com.example.vaultgallery.data.db.AppSettingsDao
import com.example.vaultgallery.security.SecureLocalStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SettingsRepository(
    private val appSettingsDao: AppSettingsDao,
    private val secureStore: SecureLocalStore
) {
    val settings: Flow<AppSettings> = appSettingsDao.observeSettings().map { current ->
        current ?: AppSettings()
    }

    suspend fun getSettingsOnce(): AppSettings {
        return appSettingsDao.getSettings() ?: AppSettings().also { appSettingsDao.upsert(it) }
    }

    suspend fun updateSettings(transform: (AppSettings) -> AppSettings) {
        val current = getSettingsOnce()
        val updated = transform(current).copy(id = 1, updatedAt = System.currentTimeMillis())
        appSettingsDao.upsert(updated)
        if (updated.themeMode != current.themeMode) {
            secureStore.saveQuickThemeMode(updated.themeMode)
        }
    }
}
