package fi.tommi.dg.app.session

import android.content.Context
import android.content.SharedPreferences

/**
 * Laiteasetusten neljä ryhmää Theme-valinnan alla (Tommin valinta 27.9.2026). Järjestys on
 * ruudun järjestys, ja [key] on tallennusavain eikä näkyvä nimi.
 */
enum class DeviceSettingsGroup(val key: String) {
    BOARD("board"),
    PLAYING("playing"),
    LISTS("lists"),
    DEVICE("device"),
}

/**
 * Mitkä laiteasetusten ryhmät ovat kiinni. Tommin valinta 27.9.2026: ryhmät taittuvat
 * otsikostaan kuten Loungen jaksot ([LoungeFoldStore]), oletuksena kaikki auki, ja tila
 * muistetaan laitteella. Ei lähde koskaan verkkoon.
 */
interface SettingsFoldStore {

    fun get(): Set<DeviceSettingsGroup>

    fun save(folded: Set<DeviceSettingsGroup>)
}

class SharedPrefsSettingsFold(private val prefs: SharedPreferences) : SettingsFoldStore {

    constructor(context: Context) : this(
        context.applicationContext.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE),
    )

    override fun get(): Set<DeviceSettingsGroup> {
        val keys = prefs.getStringSet(KEY_FOLDED, emptySet()).orEmpty()
        return DeviceSettingsGroup.entries.filterTo(mutableSetOf()) { it.key in keys }
    }

    override fun save(folded: Set<DeviceSettingsGroup>) {
        prefs.edit().putStringSet(KEY_FOLDED, folded.mapTo(mutableSetOf()) { it.key }).apply()
    }

    private companion object {
        const val FILE_NAME = "dg_settings_fold"
        const val KEY_FOLDED = "folded"
    }
}
