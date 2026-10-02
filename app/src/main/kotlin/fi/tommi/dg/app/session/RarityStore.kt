package fi.tommi.dg.app.session

import android.content.Context
import android.content.SharedPreferences

/**
 * Rarity-värit päällä vai pois, laitteen oma kytkin (Tommin tilaus 26.9.2026). Kattaa
 * pelaajanimien värin ratingin mukaan kaikissa näkymissä ja Inboxin viestimäärän värin.
 *
 * **Oletus on päällä** Tommin valinnasta, ja se on kuudennen arvon toinen nimetty poikkeus
 * (`docs/ARVOT.md`). Sama laji kuin [PortraitLockStore]: ei lähde koskaan verkkoon.
 */
interface RarityStore {

    fun get(): Boolean

    fun save(enabled: Boolean)
}

class SharedPrefsRarity(private val prefs: SharedPreferences) : RarityStore {

    constructor(context: Context) : this(
        context.applicationContext.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE),
    )

    override fun get(): Boolean = prefs.getBoolean(KEY, true)

    override fun save(enabled: Boolean) {
        prefs.edit().putBoolean(KEY, enabled).apply()
    }

    private companion object {
        const val FILE_NAME = "dg_rarity"
        const val KEY = "rarity"
    }
}
