package com.example.blockblast

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.Locale

class SettingsViewModel(
    private val prefs: SharedPreferences
) : ViewModel() {

    private val _musicVolume = MutableStateFlow(prefs.getFloat("music_volume", 0.5f))
    val musicVolume: StateFlow<Float> = _musicVolume

    private val _soundVolume = MutableStateFlow(prefs.getFloat("sound_volume", 0.5f))
    val soundVolume: StateFlow<Float> = _soundVolume

    private val _isDarkTheme = MutableStateFlow(prefs.getBoolean("dark_theme", false))
    val isDarkTheme: StateFlow<Boolean> = _isDarkTheme

    private val _currentLanguage = MutableStateFlow(prefs.getString("language", "ru") ?: "ru")
    val currentLanguage: StateFlow<String> = _currentLanguage

    private val preferenceChangeListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        when (key) {
            "dark_theme" -> _isDarkTheme.value = prefs.getBoolean("dark_theme", false)
            "music_volume" -> _musicVolume.value = prefs.getFloat("music_volume", 0.5f)
            "sound_volume" -> _soundVolume.value = prefs.getFloat("sound_volume", 0.5f)
            "language" -> _currentLanguage.value = prefs.getString("language", "ru") ?: "ru"
        }
    }

    init {
        prefs.registerOnSharedPreferenceChangeListener(preferenceChangeListener)
    }

    override fun onCleared() {
        super.onCleared()
        prefs.unregisterOnSharedPreferenceChangeListener(preferenceChangeListener)
    }

    fun setMusicVolume(value: Float) {
        _musicVolume.value = value
        prefs.edit().putFloat("music_volume", value).apply()
    }

    fun setSoundVolume(value: Float) {
        _soundVolume.value = value
        prefs.edit().putFloat("sound_volume", value).apply()
    }

    fun setDarkTheme(isDark: Boolean) {
        _isDarkTheme.value = isDark
        prefs.edit().putBoolean("dark_theme", isDark).apply()
    }

    fun setLanguage(language: String) {
        _currentLanguage.value = language
        prefs.edit().putString("language", language).apply()
    }

    fun getCurrentLocale(): Locale {
        return when (_currentLanguage.value) {
            "en" -> Locale("en")
            else -> Locale("ru")
        }
    }
}

class SettingsViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
            val prefs = context.getSharedPreferences("settings_prefs", Context.MODE_PRIVATE)
            @Suppress("UNCHECKED_CAST")
            return SettingsViewModel(prefs) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}