package fi.tommi.dg.app.session

import android.content.Context
import android.content.SharedPreferences

/**
 * Viestiruudun pelaajasuodatin, sovelluksen oma paikallinen valinta (Tommin pyyntö
 * 30.8.2026: *"pelaajakohtainen erottelu vaikka alasvetovalikossa, muista valinta"*).
 *
 * Samaa lajia kuin [BoardStyleStore]: ei sivuston asetus eikä lähde koskaan verkkoon.
 * `null` tarkoittaa ettei suodatusta ole, eli kaikki pelaajat näkyvät. Se on myös oletus,
 * koska tyhjä arkisto tai uusi asennus ei tunne yhtään nimeä jolla rajata.
 *
 * Arvo on pelaajan nimi sellaisenaan, koska nimi on arkiston pääsypolku muutenkin
 * (`MessageDao.observeByOpponent`). Nimeä ei validoida tallennettaessa: valinta joka ei
 * enää vastaa yhtään arkiston riviä näyttää tyhjän listan, ja se on totta eikä vika.
 */
interface MessageFilterStore {

    fun get(): String?

    fun save(opponent: String?)
}

class SharedPrefsMessageFilter(private val prefs: SharedPreferences) : MessageFilterStore {

    constructor(context: Context) : this(
        context.applicationContext.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE),
    )

    override fun get(): String? = prefs.getString(KEY_OPPONENT, null)

    override fun save(opponent: String?) {
        prefs.edit().apply {
            if (opponent == null) remove(KEY_OPPONENT) else putString(KEY_OPPONENT, opponent)
        }.apply()
    }

    private companion object {
        const val FILE_NAME = "dg_message_filter"
        const val KEY_OPPONENT = "opponent"
    }
}
