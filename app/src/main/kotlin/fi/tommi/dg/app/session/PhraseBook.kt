package fi.tommi.dg.app.session

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
class PhraseBook(private val store: PhraseStore) {

    private val _phrases = MutableStateFlow(store.get())

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
        _phrases.value = _phrases.value + phrase
        store.save(_phrases.value)
    }

    /**
     * Tuonti tiedostosta: liittää listan loppuun ne fraasit joita ei vielä ole, samassa
     * järjestyksessä kuin tiedostossa. Laitteen omat napit pysyvät paikoillaan (Tommin
     * päätös 16.9.2026: tuonti vain lisää). Vertailu on [normalize]n muodossa, sama sääntö
     * jolla ruutu päättää onko kentän teksti jo nappi.
     *
     * @return kuinka monta fraasia lisättiin.
     */
    fun addMissing(phrases: List<String>): Int {
        val fresh = phrases.map(::normalize).filter { it.isNotEmpty() }.distinct() - _phrases.value.toSet()
        if (fresh.isEmpty()) return 0
        _phrases.value = _phrases.value + fresh
        store.save(_phrases.value)
        return fresh.size
    }

    fun remove(phrase: String) {
        if (phrase !in _phrases.value) return
        _phrases.value = _phrases.value - phrase
        store.save(_phrases.value)
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
