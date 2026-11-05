package fi.tommi.dg.app.session

import android.content.Context
import android.content.SharedPreferences

/**
 * Profiilin päättyneiden ottelujen päiväraja laitteella (Pascalin toive ja Tommin valinta
 * 31.10.2026, `docs/TESTIKUTSUT.md`).
 *
 * Sama laji kuin [MatchOrderStore]: tämän laitteen oma valinta, joka ei lähde verkkoon
 * muuten kuin profiilin osoitteen lukuna. Null tarkoittaa ettei valintaa ole tehty, ja
 * silloin profiili haetaan sivun omalla linkillä sellaisenaan.
 */
interface FinishedDaysStore {

    fun get(): Int?

    fun save(days: Int)

    companion object {
        /** Muistiton varasto testeille ja ruuduille joilla ei ole profiilia. */
        val NONE: FinishedDaysStore = object : FinishedDaysStore {
            override fun get(): Int? = null
            override fun save(days: Int) = Unit
        }
    }
}

class SharedPrefsFinishedDays(private val prefs: SharedPreferences) : FinishedDaysStore {

    constructor(context: Context) : this(
        context.applicationContext.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE),
    )

    // Nolla on koko historia (`FinishedDays.ALL`), joten tallentamaton on -1.
    override fun get(): Int? =
        prefs.getInt(KEY_DAYS, -1).takeIf { it >= 0 }

    override fun save(days: Int) {
        prefs.edit().putInt(KEY_DAYS, days).apply()
    }

    private companion object {
        const val FILE_NAME = "dg_finished_days"
        const val KEY_DAYS = "days"
    }
}
