package fi.tommi.dg.app.session

import android.content.Context
import android.content.SharedPreferences

/**
 * Poimitaanko ahne uloskanto valmiiksi kun kontaktia ei ole, laitteen oma kytkin
 * (`docs/ASETUKSET.md` luku 3). Tommin tilaus 2.9.2026: *"kun kontaktia ei ole niin greedy
 * bear off asetus olisi kiva"*, ja lukutapa kuitattu samana päivänä: sovellus poimii ja
 * `Submit Move` jää pelaajalle. Mekanismi on `CompositionSession.prefillGreedy`, tämä kantaa
 * vain valinnan. Sama laji kuin [ForcedStepsStore], ja oletus on pois samasta syystä: teko
 * pelaajan puolesta otetaan käyttöön nähtynä eikä oletuksena.
 */
interface GreedyBearoffStore {

    fun get(): Boolean

    fun save(enabled: Boolean)
}

class SharedPrefsGreedyBearoff(private val prefs: SharedPreferences) : GreedyBearoffStore {

    constructor(context: Context) : this(
        context.applicationContext.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE),
    )

    override fun get(): Boolean = prefs.getBoolean(KEY, false)

    override fun save(enabled: Boolean) {
        prefs.edit().putBoolean(KEY, enabled).apply()
    }

    private companion object {
        const val FILE_NAME = "dg_greedy_bearoff"
        const val KEY = "greedy_bearoff"
    }
}
