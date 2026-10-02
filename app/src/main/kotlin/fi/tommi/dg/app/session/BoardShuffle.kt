package fi.tommi.dg.app.session

import android.content.Context
import android.content.SharedPreferences

/**
 * Ottelukohtainen lauta (Tommin tilaus 27.9.2026: *"ottelun vaihtuessa vaihtuva lauta voisi
 * parantaa pelisession immersiota"*). Kolme valintaa samana yönä: **ottelulla on oma lauta**
 * eikä uutta arvontaa joka avauksella, **pelaaja valitsee** arvonnan laudat, ja asetus on
 * **oma kytkin**, jolloin valittu lauta jää talteen ja palaa kun kytkin sammuu. Korttien
 * rastit ovat Tommin valinta A mockupista (`docs/ASETUKSET.md` › Ottelukohtainen lauta).
 */
interface BoardShuffleStore {

    fun on(): Boolean

    fun saveOn(on: Boolean)

    /** Arvonnan laudat. Ei koskaan tyhjä: tyhjä tai lukukelvoton säilö antaa [DEFAULT_POOL]in. */
    fun pool(): Set<BoardStyle>

    fun savePool(pool: Set<BoardStyle>)

    companion object {
        /**
         * Sovelluksen omat laudat. Sivustouskollinen jää oletuksesta pois, koska se on
         * sivuston lauta eikä tuo vaihtelua sovelluksen sisältä; rastilla sen saa mukaan.
         */
        val DEFAULT_POOL: Set<BoardStyle> =
            setOf(BoardStyle.X22, BoardStyle.MONTE_CARLO_VARIANT, BoardStyle.DECO)
    }
}

/**
 * Ottelun lauta joukosta. **Sama ottelu saa aina saman laudan**, koska valinta lasketaan
 * ottelun numerosta eikä arvota: paluu otteluun tuo tutun laudan, ja tulos säilyy
 * uudelleenkäynnistyksen yli ilman tallennusta.
 *
 * Numero sekoitetaan ennen jakojäännöstä, koska peräkkäiset otteluiden numerot kiertäisivät
 * muuten laudat järjestyksessä ja sama turnauskierros näyttäisi aina samalta.
 * Järjestys luetaan enumista eikä joukosta, jotta tulos ei riipu joukon toteutuksesta.
 */
fun boardForMatch(matchId: String, pool: Set<BoardStyle>): BoardStyle {
    val ordered = BoardStyle.entries.filter { it in pool }.ifEmpty { return BoardStyle.SITE }
    var h = 1469598103934665603L
    for (c in matchId) {
        h = (h xor c.code.toLong()) * 1099511628211L
    }
    h = h xor (h ushr 29)
    return ordered[Math.floorMod(h, ordered.size.toLong()).toInt()]
}

class SharedPrefsBoardShuffle(private val prefs: SharedPreferences) : BoardShuffleStore {

    constructor(context: Context) : this(
        context.applicationContext.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE),
    )

    override fun on(): Boolean = prefs.getBoolean(KEY_ON, false)

    override fun saveOn(on: Boolean) {
        prefs.edit().putBoolean(KEY_ON, on).apply()
    }

    override fun pool(): Set<BoardStyle> {
        val stored = prefs.getStringSet(KEY_POOL, null) ?: return BoardShuffleStore.DEFAULT_POOL
        // Tuntematon nimi ohitetaan eikä kaada, sama peruste kuin tyylin luvussa.
        return BoardStyle.entries.filter { it.name in stored }.toSet()
            .ifEmpty { BoardShuffleStore.DEFAULT_POOL }
    }

    override fun savePool(pool: Set<BoardStyle>) {
        prefs.edit().putStringSet(KEY_POOL, pool.map { it.name }.toSet()).apply()
    }

    private companion object {
        const val FILE_NAME = "dg_board_shuffle"
        const val KEY_ON = "on"
        const val KEY_POOL = "pool"
    }
}
