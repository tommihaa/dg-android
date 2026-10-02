package fi.tommi.dg.app.session

import android.content.Context
import android.content.SharedPreferences

/**
 * Seuraako lauta laitteen asentoa, laitteen oma kytkin (`docs/ASETUKSET.md` luku 3).
 *
 * **Oletus on pois**, jolloin lauta on vaakaan kuten 24.8.2026 päätös sanoo (`docs/UI.md`,
 * Suunta lukittiin). Kytkin syntyi Tommin tilauksesta 23.9.2026: pelaaminen myös pystytilassa.
 * Päällä ollessaan lauta piirtyy pystyssä leveyden mittaan, samalla asettelulla jonka
 * kirjoitushetki sai 19.9.2026. Kuudes arvo (`docs/ARVOT.md`) on syy oletukseen: tekijän oma
 * lisä on valittava, ja perusasettelu on se johon mitoitus on tehty. Sama laji kuin
 * [PortraitLockStore]: ei lähde koskaan verkkoon.
 */
interface BoardRotationStore {

    fun get(): Boolean

    fun save(enabled: Boolean)
}

class SharedPrefsBoardRotation(private val prefs: SharedPreferences) : BoardRotationStore {

    constructor(context: Context) : this(
        context.applicationContext.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE),
    )

    override fun get(): Boolean = prefs.getBoolean(KEY, false)

    override fun save(enabled: Boolean) {
        prefs.edit().putBoolean(KEY, enabled).apply()
    }

    private companion object {
        const val FILE_NAME = "dg_board_rotation"
        const val KEY = "board_follows_device"
    }
}
