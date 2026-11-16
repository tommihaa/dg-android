package fi.tommi.dg.app.session

import android.content.Context
import android.content.SharedPreferences
import fi.tommi.dg.app.ui.OwnTournamentCount

/**
 * Omien turnausten viimeksi luettu lukumäärä polkuineen, laitteella.
 *
 * Tommin vastaus 12.11.2026: Top Pagen `Tournaments (n)` -segmenttiin *"riittää viimeisin
 * tieto"*. Lista haetaan yhä vasta segmentin avauksesta, mutta viimeksi nähty luku säilyy
 * sovelluksen uudelleenkäynnistyksen yli, joten segmentti kantaa luvun heti. Polku kulkee
 * mukana, jotta toisen tilin luku ei näy: Top Page vertaa sitä omaan linkkiinsä.
 */
interface TournamentCountStore {

    fun get(): OwnTournamentCount?

    fun save(count: OwnTournamentCount)
}

class SharedPrefsTournamentCount(private val prefs: SharedPreferences) : TournamentCountStore {

    constructor(context: Context) : this(
        context.applicationContext.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE),
    )

    override fun get(): OwnTournamentCount? {
        val path = prefs.getString(KEY_PATH, null) ?: return null
        if (!prefs.contains(KEY_COUNT)) return null
        return OwnTournamentCount(path, prefs.getInt(KEY_COUNT, 0))
    }

    override fun save(count: OwnTournamentCount) {
        prefs.edit().putString(KEY_PATH, count.path).putInt(KEY_COUNT, count.count).apply()
    }

    private companion object {
        const val FILE_NAME = "dg_tournament_count"
        const val KEY_PATH = "path"
        const val KEY_COUNT = "count"
    }
}
