package fi.tommi.dg.app.session

import android.content.Context
import android.content.SharedPreferences
import fi.tommi.dg.domain.Phrase

/**
 * Viestiruudun fraasinapit, muokattava lista (Tommin tilaus 3.9.2026: *"toteuta fraasin
 * lisääminen/poisto"*). Lista oli kiinteä 24.8.2026 alkaen, ja tallentuvat suosikit olivat
 * kirjattu idea (`docs/AVOIMET.md`); tämä on se idea tilattuna.
 *
 * Samaa lajia kuin [MessageFilterStore]: sovelluksen oma paikallinen valinta, ei sivuston
 * asetus eikä lähde koskaan verkkoon. Fraasi lähtee verkkoon vasta kun käyttäjä painaa
 * Send, ja se on eri teko.
 *
 * **Oletus on Tommin rituaalifraasit** (`SUBSTANSSI.md` kohta 58), jotta ominaisuus näyttää
 * uudessa asennuksessa samalta kuin ennen. Oletusta ei kuitenkaan sekoiteta tyhjään: kun
 * käyttäjä on poistanut viimeisenkin fraasin, lista on tyhjä eikä palaudu oletukseen
 * seuraavassa käynnistyksessä. Siksi säilössä erotetaan *ei koskaan talletettu* (oletus)
 * ja *talletettu tyhjänä* (tyhjä).
 *
 * Fraasit ovat lähetettävän viestin sisältöä eivätkä käännettävää UI-tekstiä, joten ne
 * eivät asu `strings.xml`:ssä (kohta 58: rituaalifraasit ovat englantia riippumatta siitä
 * kuka pelaa). Oletuslista on tässä samasta syystä.
 */
interface PhraseStore {

    /** Fraasit lisäyshetkineen, vanhin ensin ([Phrase], 20.10.2026). */
    fun get(): List<Phrase>

    fun save(phrases: List<Phrase>)

    companion object {
        /** Vakiofraasit joilla ottelu alkaa ja päättyy, `SUBSTANSSI.md` kohta 58. */
        val DEFAULT: List<String> = listOf("hi", "gg", "ty gg u2")
    }
}

class SharedPrefsPhrases(private val prefs: SharedPreferences) : PhraseStore {

    constructor(context: Context) : this(
        context.applicationContext.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE),
    )

    /**
     * Rivinvaihto erottimena, koska fraasi on yhden rivin teksti: [fi.tommi.dg.app.ui.MessagesViewModel.addPhrase]
     * litistää rivinvaihdot välilyönniksi ennen tallennusta. `StringSet` hylättiin, koska
     * se hukkaisi järjestyksen, ja nappien järjestys on osa sitä mitä käyttäjä tallensi.
     */
    override fun get(): List<Phrase> {
        val stored = prefs.getString(KEY, null) ?: return Phrase.fromPositions(PhraseStore.DEFAULT)
        if (stored.isEmpty()) return emptyList()
        val texts = stored.split(SEPARATOR)
        // Hetket omana avaimenaan samassa järjestyksessä (20.10.2026), jotta ennen sitä
        // tallennettu lista luetaan sellaisenaan. Puuttuva tai eri mittainen hetkirivi on vanha
        // tallennus, ja silloin hetket tulevat järjestyksestä ([Phrase.fromPositions]).
        val times = prefs.getString(KEY_ADDED, null)?.split(SEPARATOR)?.map { it.toLongOrNull() }
        if (times == null || times.size != texts.size || times.any { it == null }) return Phrase.fromPositions(texts)
        return texts.zip(times) { text, at -> Phrase(text, at!!) }
    }

    override fun save(phrases: List<Phrase>) {
        prefs.edit()
            .putString(KEY, phrases.joinToString(SEPARATOR) { it.text })
            .putString(KEY_ADDED, phrases.joinToString(SEPARATOR) { it.addedAtEpochMillis.toString() })
            .apply()
    }

    private companion object {
        const val FILE_NAME = "dg_phrases"
        const val KEY = "phrases"
        const val KEY_ADDED = "phrases_added"
        const val SEPARATOR = "\n"
    }
}
