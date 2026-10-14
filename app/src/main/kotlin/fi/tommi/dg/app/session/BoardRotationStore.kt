package fi.tommi.dg.app.session

import android.content.Context
import android.content.SharedPreferences

/**
 * Seuraako lauta laitteen asentoa, laitteen oma kytkin (`docs/ASETUKSET.md` luku 3).
 *
 * **Oletus on päällä 10.10.2026 alkaen** (Tommin päätös, `docs/UI.md` › *Suunta on oletuksena
 * vapaa*): lauta seuraa laitteen asentoa, ja vaakalukko on valittava. Kytkin syntyi Tommin
 * tilauksesta 23.9.2026 oletuksena pois. Päällä ollessaan lauta piirtyy pystyssä leveyden
 * mittaan, samalla asettelulla jonka kirjoitushetki sai 19.9.2026. Kuudes arvo
 * (`docs/ARVOT.md`) on syy oletukseen: vaakalukko on tekijän oman laitteen maku, ja muilla
 * laitteilla pakotus toi testaajille toimimattomuutta. Sama laji kuin [PortraitLockStore]:
 * ei lähde koskaan verkkoon.
 */
interface BoardRotationStore {

    fun get(): Boolean

    fun save(enabled: Boolean)
}

class SharedPrefsBoardRotation(private val prefs: SharedPreferences) : BoardRotationStore {

    constructor(context: Context) : this(
        context.applicationContext.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE),
    )

    override fun get(): Boolean = prefs.getBoolean(KEY, true)

    override fun save(enabled: Boolean) {
        prefs.edit().putBoolean(KEY, enabled).apply()
    }

    private companion object {
        const val FILE_NAME = "dg_board_rotation"
        const val KEY = "board_follows_device"
    }
}
