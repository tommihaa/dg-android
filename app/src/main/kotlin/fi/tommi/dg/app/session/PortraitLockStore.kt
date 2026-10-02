package fi.tommi.dg.app.session

import android.content.Context
import android.content.SharedPreferences

/**
 * Pidetäänkö lukuruudut (luettelo, viestit, foorumi, asetukset) pystylukossa, laitteen oma
 * kytkin (`docs/ASETUKSET.md` luku 3). Lauta on aina vaakaan riippumatta tästä.
 *
 * **Oletus on päällä**, koska se on 24.8.2026 päätös (`docs/UI.md`, Suunta lukittiin) ja sen
 * mittaus on yhä voimassa: puhelimen vaakaikkunassa listalle jäi nolla korkeutta. Kytkin
 * syntyi Tommin havainnosta 2.9.2026: tabletilla jonon tyhjentyessä lauta palaa luetteloon
 * ja tabletti kääntyy kädessä pystyyn kesken session. Pois kytkettynä lukuruudut seuraavat
 * laitteen asentoa. Sama laji kuin [DiceStyleStore]: ei lähde koskaan verkkoon.
 */
interface PortraitLockStore {

    fun get(): Boolean

    fun save(enabled: Boolean)
}

class SharedPrefsPortraitLock(private val prefs: SharedPreferences) : PortraitLockStore {

    constructor(context: Context) : this(
        context.applicationContext.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE),
    )

    override fun get(): Boolean = prefs.getBoolean(KEY, true)

    override fun save(enabled: Boolean) {
        prefs.edit().putBoolean(KEY, enabled).apply()
    }

    private companion object {
        const val FILE_NAME = "dg_portrait_lock"
        const val KEY = "portrait_lock"
    }
}
