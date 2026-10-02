package fi.tommi.dg.app.session

import android.content.Context
import android.content.SharedPreferences

/**
 * Loungen neljän jakson taitto, Tournament Hallin viimeksi nähty rivimäärä ja
 * pelaajalistasta orgaanisesti löytynyt pelaajamäärä.
 *
 * Tommin tilaus 21.9.2026: otsikot ovat kytkimiä joista jakson saa kiinni ja auki, ja
 * sovellus muistaa valinnan. Players ja Tournament Hall tulivat samaan muotoon samana
 * iltana (Tommin tarkennus), ja hallin luku muistetaan jotta se on otsikossa ilman hakua:
 * tieto ei ole ensisijaista mutta kiinnostavaa. Pelaajien kokonaismäärää sivusto ei kerro
 * missään (sadan rivin sivuja ilman summaa), joten Playersin luku on se mitä on löytynyt
 * (Tommin päätös 21.9.2026: *"se mitä orgaanisesti on löytynyt, ei todellinen"*), eli
 * suurin nähty sijoitus kaikista ladatuista sivuista; ennen ensimmäistä sivua otsikossa on
 * kysymysmerkki, *"fog of unknown"*. Laitteen oma tieto kuten [QueueExplainStore], ei
 * lähde koskaan verkkoon.
 *
 * Oletukset: tarjoukset ja ilmoittautumiset auki, jolloin ruutu on sama kuin ennen
 * muutosta; Players ja Tournament Hall kiinni, koska niiden avaus hakee sivun.
 */
interface LoungeFoldStore {

    fun get(): LoungeFolds

    fun save(folds: LoungeFolds)

    /** Hallin aktiivisten turnausten määrä viime hausta, tai null kun hallia ei ole haettu. */
    fun hallCount(): Int?

    fun saveHallCount(count: Int)

}

/** Neljän jakson tila. `true` on kiinni, eli otsikko näkyy ja rivit eivät. */
data class LoungeFolds(
    val playersFolded: Boolean = true,
    val hallFolded: Boolean = true,
    val invitationsFolded: Boolean = false,
    val tournamentsFolded: Boolean = false,
)

class SharedPrefsLoungeFold(private val prefs: SharedPreferences) : LoungeFoldStore {

    constructor(context: Context) : this(
        context.applicationContext.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE),
    )

    override fun get(): LoungeFolds = LoungeFolds(
        playersFolded = prefs.getBoolean(KEY_PLAYERS, true),
        hallFolded = prefs.getBoolean(KEY_HALL, true),
        invitationsFolded = prefs.getBoolean(KEY_INVITATIONS, false),
        tournamentsFolded = prefs.getBoolean(KEY_TOURNAMENTS, false),
    )

    override fun save(folds: LoungeFolds) {
        prefs.edit()
            .putBoolean(KEY_PLAYERS, folds.playersFolded)
            .putBoolean(KEY_HALL, folds.hallFolded)
            .putBoolean(KEY_INVITATIONS, folds.invitationsFolded)
            .putBoolean(KEY_TOURNAMENTS, folds.tournamentsFolded)
            .apply()
    }

    override fun hallCount(): Int? =
        if (prefs.contains(KEY_HALL_COUNT)) prefs.getInt(KEY_HALL_COUNT, 0) else null

    override fun saveHallCount(count: Int) {
        prefs.edit().putInt(KEY_HALL_COUNT, count).apply()
    }


    private companion object {
        const val FILE_NAME = "dg_lounge_fold"
        const val KEY_PLAYERS = "players_folded"
        const val KEY_HALL = "hall_folded"
        const val KEY_INVITATIONS = "invitations_folded"
        const val KEY_TOURNAMENTS = "tournaments_folded"
        const val KEY_HALL_COUNT = "hall_count"
    }
}
