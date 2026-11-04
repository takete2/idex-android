package dev.idex.android.data

import android.content.Context

/** On-device research log. Nothing here leaves the phone. */
class DexPrefs(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("idex", Context.MODE_PRIVATE)

    fun language(): AppLanguage =
        AppLanguage.fromId(prefs.getString(KEY_LANGUAGE, null)) ?: AppLanguage.systemDefault()

    fun setLanguage(language: AppLanguage) {
        prefs.edit().putString(KEY_LANGUAGE, language.id).apply()
    }

    fun seen(): Set<Int> =
        prefs.getStringSet(KEY_SEEN, emptySet()).orEmpty().mapNotNull { it.toIntOrNull() }.toSet()

    fun setSeen(ids: Set<Int>) {
        prefs.edit().putStringSet(KEY_SEEN, ids.map { it.toString() }.toSet()).apply()
    }

    fun lastViewed(): Int = prefs.getInt(KEY_LAST, SAMPLE_SPECIES_ID).let { if (it in 1..151) it else SAMPLE_SPECIES_ID }

    fun setLastViewed(id: Int) {
        prefs.edit().putInt(KEY_LAST, id).apply()
    }

    fun soundEnabled(): Boolean = prefs.getBoolean(KEY_SOUND, true)

    fun setSoundEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SOUND, enabled).apply()
    }

    companion object {
        /** Poliwhirl. The cover LCD opens on this species. */
        const val SAMPLE_SPECIES_ID = 61
        private const val KEY_LANGUAGE = "language"
        private const val KEY_SEEN = "seen"
        private const val KEY_LAST = "lastViewed"
        private const val KEY_SOUND = "soundEnabled"
    }
}
