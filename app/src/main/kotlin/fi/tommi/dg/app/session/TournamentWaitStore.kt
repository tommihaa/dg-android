package fi.tommi.dg.app.session

import android.content.Context
import android.content.SharedPreferences

/**
 * Omien turnausten odotukset turnausnumeron mukaan: montako ottelua vastahaarassa on
 * pelattava ennen kuin seuraava vastustaja tiedetään (`EventPage.matchesBeforeOpponent`).
 *
 * Tommin tilaus 25.9.2026 (`docs/UI.md` › *Turnausrivin odotus ja vastustajan väri*):
 * Refresh hakee linkittömien rivien turnaussivut, ja luku säilyy seuraavaan Refreshiin
 * asti. Refresh korvaa koko sisällön, joten ratkennut odotus ei jää roikkumaan.
 *
 * Laitteen oma tieto kuten [PlayerRatingStore], ei lähde koskaan verkkoon.
 */
interface TournamentWaitStore {

    fun all(): Map<String, Int>

    fun replace(waits: Map<String, Int>)

    companion object {
        /** Ei muista mitään. Näkymämallien oletus, jotta testit eivät tarvitse varastoa. */
        val NONE: TournamentWaitStore = object : TournamentWaitStore {
            override fun all(): Map<String, Int> = emptyMap()
            override fun replace(waits: Map<String, Int>) = Unit
        }
    }
}

class SharedPrefsTournamentWaits(private val prefs: SharedPreferences) : TournamentWaitStore {

    constructor(context: Context) : this(
        context.applicationContext.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE),
    )

    override fun all(): Map<String, Int> =
        prefs.all.mapNotNull { (key, value) -> (value as? Int)?.let { key to it } }.toMap()

    override fun replace(waits: Map<String, Int>) {
        val edit = prefs.edit().clear()
        waits.forEach { (eventId, count) -> edit.putInt(eventId, count) }
        edit.apply()
    }

    private companion object {
        const val FILE_NAME = "dg_tournament_waits"
    }
}
