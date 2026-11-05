package fi.tommi.dg.app.session

import fi.tommi.dg.domain.Phrase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Fraasinappien lista muistissa, yksi koko sovellukselle.
 *
 * **Miksi tämä ei ole näkymämallissa.** Sama rivi on kahdessa ruudussa 3.9.2026 alkaen
 * (Tommin tilaus: *"lisää sama fraasirivi laudan chat-korttiin"*): viestiruudun
 * vastauskentässä ja laudan chat-kortissa. Lista on yksi ja muokkaus jommassakummassa
 * näkyy heti toisessa, joten sen omistaja on sovellus eikä kumpikaan ruutu.
 * Näkymämallit ja ruudut lukevat [phrases]-virtaa ja kutsuvat [add]ia ja [remove]a.
 *
 * Säilö ([PhraseStore]) kantaa listan käynnistysten yli; tämä kantaa sen session ajan ja
 * kirjoittaa jokaisen muutoksen heti säilöön.
 */
class PhraseBook(
    private val store: PhraseStore,
    /** Lisäyshetken kello. Parametrina, jotta testi voi antaa saman hetken kahdesti. */
    private val now: () -> Long = System::currentTimeMillis,
) {

    private val _entries = MutableStateFlow(Phrase.ordered(store.get()))

    /** Fraasit lisäyshetkineen, vanhin ensin. Viennin lähde (`ArchiveFile.phrases`). */
    val entries: StateFlow<List<Phrase>> = _entries.asStateFlow()

    private val _phrases = MutableStateFlow(_entries.value.map { it.text })

    /**
     * Fraasit tallennusjärjestyksessä. Tyhjä lista on tyhjä eikä palaudu oletukseen
     * (ks. [PhraseStore]).
     */
    val phrases: StateFlow<List<String>> = _phrases.asStateFlow()

    /**
     * Tallentaa tekstin uudeksi fraasiksi. Lähde on kirjoituskenttä: käyttäjä kirjoittaa
     * fraasin kuten minkä tahansa viestin ja painaa tallennusta, joten fraasi on aina
     * teksti jonka hän olisi valmis lähettämään sellaisenaan.
     *
     * Tyhjä ja jo tallennettu ohitetaan hiljaa: nappi on ruudulla pois päältä kummassakin
     * tapauksessa, joten tämä on vain varmistus.
     */
    fun add(text: String) {
        val phrase = normalize(text)
        if (phrase.isEmpty() || phrase in _phrases.value) return
        write(_entries.value + Phrase(phrase, now()))
    }

    /**
     * Tuonti tiedostosta: lisää ne fraasit joita ei vielä ole ja järjestää listan lisäyshetken
     * mukaan (Tommin vahvistus 20.10.2026, `docs/AVOIMET.md`). Jos sama fraasi on jo
     * laitteella, sen hetkeksi jää aikaisempi. **Poikkeus "tuonti vain lisää" -sääntöön
     * (16.9.2026) koskee vain järjestystä:** laitteen omia nappeja voi siirtyä, mutta mitään
     * ei poisteta eikä tekstiä muuteta. Näin kahden laitteen ristiintuonti päätyy samaan
     * järjestykseen kummassakin. Vertailu on [normalize]n muodossa, sama sääntö jolla ruutu
     * päättää onko kentän teksti jo nappi.
     *
     * @return kuinka monta fraasia lisättiin.
     */
    fun addMissing(phrases: List<Phrase>): Int {
        val byText = LinkedHashMap<String, Phrase>()
        for (own in _entries.value) byText[own.text] = own
        var added = 0
        for (incoming in phrases) {
            val text = normalize(incoming.text)
            if (text.isEmpty()) continue
            val own = byText[text]
            if (own == null) {
                byText[text] = Phrase(text, incoming.addedAtEpochMillis)
                added++
            } else if (incoming.addedAtEpochMillis < own.addedAtEpochMillis) {
                byText[text] = own.copy(addedAtEpochMillis = incoming.addedAtEpochMillis)
            }
        }
        val merged = Phrase.ordered(byText.values.toList())
        if (merged != _entries.value) write(merged)
        return added
    }

    fun remove(phrase: String) {
        if (phrase !in _phrases.value) return
        write(_entries.value.filterNot { it.text == phrase })
    }

    private fun write(phrases: List<Phrase>) {
        _entries.value = phrases
        _phrases.value = phrases.map { it.text }
        store.save(phrases)
    }

    companion object {
        /**
         * Fraasi sellaisena kuin se tallennetaan: yksi rivi ilman reunavälejä, **pienellä**.
         * Ruutu käyttää samaa muotoa päättäessään onko kentän teksti jo napiksi tallennettu,
         * jotta nappi ja lista eivät ole eri mieltä siitä mikä on sama fraasi.
         *
         * Rivinvaihdot litistetään välilyönniksi, koska nappi on yhden rivin teksti ja säilö
         * erottaa fraasit rivinvaihdolla. Pienennys on Tommin päätös 3.9.2026 laiteajon
         * havaintoon: näppäimistö kirjoitti `gl` muodossa `Gl`, ja rituaalifraasit ovat
         * pienellä. Pienennys koskee vain tallennettavaa fraasia, ei kentän tekstiä.
         */
        fun normalize(text: String): String =
            text.lines().joinToString(" ") { it.trim() }.trim().replace(Regex(" {2,}"), " ").lowercase()
    }
}
