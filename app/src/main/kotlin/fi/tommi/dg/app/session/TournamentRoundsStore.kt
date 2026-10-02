package fi.tommi.dg.app.session

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.mutableIntStateOf

/**
 * Turnausten kierrosmäärät turnausnumeron mukaan sellaisina kuin ne viimeksi nähtiin.
 *
 * Tommin tilaus 2.10.2026 (`docs/UI.md` › *Turnausten rarity-värit*): turnauksen nimi saa
 * harvinaisuusvärin sen mukaan montako ottelua putkeen voittaminen vaatii. Kierrosmäärä on
 * otteluluettelon `Round 3/5`:ssä, loungen ilmoittautumislistassa ja turnaussivulla, mutta
 * ei Tournament Hallissa, pelaajan turnauslistoissa eikä laudan omassa otsikossa. Nämä
 * lukevat sen täältä. Luku kirjataan vain sivuilta jotka haetaan joka tapauksessa, joten
 * varasto ei tee yhtään pyyntöä, kuten [PlayerRatingStore].
 *
 * Laitteen oma tieto, ei lähde koskaan verkkoon.
 */
interface TournamentRoundsStore {

    fun rounds(eventId: String): Int?

    fun record(eventId: String, rounds: Int)

    companion object {
        /** Ei muista mitään. Esikatselujen ja testien oletus, ja kytkin pois päältä. */
        val NONE: TournamentRoundsStore = object : TournamentRoundsStore {
            override fun rounds(eventId: String): Int? = null
            override fun record(eventId: String, rounds: Int) = Unit
        }
    }
}

class SharedPrefsTournamentRounds(private val prefs: SharedPreferences) : TournamentRoundsStore {

    constructor(context: Context) : this(
        context.applicationContext.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE),
    )

    /**
     * Kasvaa jokaisesta uudesta luvusta. [rounds] lukee sen, joten Compose piirtää lukijan
     * uudelleen kirjauksen jälkeen: Tournament Hallin rivi värittyy kun turnaussivulta
     * palataan, ja omien turnausten rivit kun Refresh on kirjannut luvut.
     */
    private val version = mutableIntStateOf(0)

    override fun rounds(eventId: String): Int? {
        version.intValue
        return prefs.getInt(eventId, 0).takeIf { it > 0 }
    }

    override fun record(eventId: String, rounds: Int) {
        if (rounds <= 0 || prefs.getInt(eventId, 0) == rounds) return
        prefs.edit().putInt(eventId, rounds).apply()
        version.intValue++
    }

    private companion object {
        const val FILE_NAME = "dg_tournament_rounds"
    }
}
