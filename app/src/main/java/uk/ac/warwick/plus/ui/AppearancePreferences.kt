package uk.ac.warwick.plus.ui

import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Device preferences only; contains no account information or authentication state. */
class AppearancePreferences(private val preferences: SharedPreferences) {
    private val current = MutableStateFlow(Appearance(
        ColourTheme.fromId(preferences.getString("colour_theme", null)), preferences.getBoolean("fine_texture", false)))
    val state = current.asStateFlow()
    fun update(appearance: Appearance) {
        preferences.edit().putString("colour_theme", appearance.theme.id)
            .putBoolean("fine_texture", appearance.texture).apply()
        current.value = appearance
    }
}
