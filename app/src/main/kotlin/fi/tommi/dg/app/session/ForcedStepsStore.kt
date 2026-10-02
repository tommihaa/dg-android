package fi.tommi.dg.app.session

import android.content.Context
import android.content.SharedPreferences

/**
 * Pelataanko pakolliset askeleet valmiiksi, laitteen oma kytkin (`docs/ASETUKSET.md` luku 3).
 *
 * **Tommin vaatimus 1.9.2026, lukutapa kuitattu 2.9.2026:** *"jos joku siirto on pakollinen,
 * niin se pelataan"*, ja siirto on askel eikä vuoro. Mekanismi on `core-domain`in
 * `CompositionSession.prefillForced`, tämä kantaa vain valinnan. Sama laji kuin
 * [DiceStyleStore]: ei lähde koskaan verkkoon.
 *
 * **Oletus on pois**, ja se on riskiluokan päätös eikä maku (`docs/AVOIMET.md`): sovellus
 * tekee tässä laudalle jotain pelaajan puolesta, ja vaikka lähetys jää aina pelaajalle,
 * esipoimittu askel on väite joka pitää saada nähdä ennen kuin siihen luottaa.
 */
interface ForcedStepsStore {

    fun get(): Boolean

    fun save(enabled: Boolean)
}

class SharedPrefsForcedSteps(private val prefs: SharedPreferences) : ForcedStepsStore {

    constructor(context: Context) : this(
        context.applicationContext.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE),
    )

    override fun get(): Boolean = prefs.getBoolean(KEY, false)

    override fun save(enabled: Boolean) {
        prefs.edit().putBoolean(KEY, enabled).apply()
    }

    private companion object {
        const val FILE_NAME = "dg_forced_steps"
        const val KEY = "forced_steps"
    }
}
