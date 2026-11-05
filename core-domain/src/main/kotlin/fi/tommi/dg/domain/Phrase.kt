package fi.tommi.dg.domain

/**
 * Fraasinappi lisäyshetkineen (Tommin vahvistus 20.10.2026, `docs/AVOIMET.md` › Pikaviestien
 * järjestys lisäyshetken mukaan).
 *
 * **Järjestys on hetken mukaan vanhin ensin.** Uusi nappi saa tallennushetken ja menee
 * loppuun, kuten ennenkin. Hetki on olemassa siksi, että kahden laitteen listat yhdistyvät
 * samaan järjestykseen: tuonnissa sama fraasi pitää aikaisemman hetkensä, ja lista
 * järjestetään hetken mukaan.
 *
 * @param addedAtEpochMillis Tallennushetki. Ennen 20.10.2026 tallennetulla fraasilla hetkeä
 *   ei ole, ja se saa järjestyksestään juoksevan numeron ([fromPositions]).
 */
data class Phrase(val text: String, val addedAtEpochMillis: Long) {

    companion object {
        /**
         * Hetket järjestyksestä: ensimmäinen 1, toinen 2 ja niin edelleen. Pienet luvut ovat
         * tarkoituksella vanhempia kuin mikään oikea kellonaika, joten vanha nappi pysyy
         * ennen uutta. Kaksi laitetta joilla on sama järjestys saavat samat hetket.
         */
        fun fromPositions(texts: List<String>): List<Phrase> =
            texts.mapIndexed { index, text -> Phrase(text, index + 1L) }

        /**
         * Lista hetken mukaan, vanhin ensin. Saman hetken fraasit järjestetään tekstin mukaan,
         * jotta kaksi laitetta päätyy samaan järjestykseen vaikka niiden vanhat listat
         * olisivat olleet eri järjestyksessä (tasapeli syntyy vain [fromPositions]in hetkistä).
         */
        fun ordered(phrases: List<Phrase>): List<Phrase> =
            phrases.sortedWith(compareBy<Phrase> { it.addedAtEpochMillis }.thenBy { it.text })
    }
}
