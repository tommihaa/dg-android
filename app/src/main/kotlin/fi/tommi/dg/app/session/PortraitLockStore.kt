package fi.tommi.dg.app.session

import android.content.Context
import android.content.SharedPreferences

/**
 * Pidetäänkö lukuruudut (luettelo, viestit, foorumi, asetukset) pystylukossa, laitteen oma
 * kytkin (`docs/ASETUKSET.md` luku 3). Lauta on aina vaakaan riippumatta tästä.
 *
 * **Oletus on pois 10.10.2026 alkaen** (Tommin päätös, `docs/UI.md` › *Suunta on oletuksena
 * vapaa*): lukuruudut seuraavat laitteen asentoa, ja pystylukko on valittava. Mittaus
 * 24.8.2026 on yhä voimassa (puhelimen vaakaikkunassa listalle jää vähän korkeutta), mutta
 * se on nyt käyttäjän valinta eikä kaikkien oletus. Kytkin syntyi Tommin havainnosta
 * 2.9.2026: tabletilla jonon tyhjentyessä lauta palaa luetteloon ja tabletti kääntyy kädessä
 * pystyyn kesken session. Sama laji kuin [DiceStyleStore]: ei lähde koskaan verkkoon.
 */
interface PortraitLockStore {

    fun get(): Boolean

    fun save(enabled: Boolean)
}

class SharedPrefsPortraitLock(private val prefs: SharedPreferences) : PortraitLockStore {

    constructor(context: Context) : this(
        context.applicationContext.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE),
    )

    override fun get(): Boolean = prefs.getBoolean(KEY, false)

    override fun save(enabled: Boolean) {
        prefs.edit().putBoolean(KEY, enabled).apply()
    }

    private companion object {
        const val FILE_NAME = "dg_portrait_lock"
        const val KEY = "portrait_lock"
    }
}
