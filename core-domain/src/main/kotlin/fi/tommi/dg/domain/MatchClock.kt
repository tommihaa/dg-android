package fi.tommi.dg.domain

/**
 * Otteluluettelon aikakenttien tulkinta.
 *
 * `Match.graceText` ja `Match.timePoolText` pysyvät tekstinä, koska ne ovat sitä mitä sivu
 * sanoo. Tämä on erillinen lukija sen päälle: se ei muuta mallia eikä sitä mitä ruudulla
 * näytetään, vaan kertoo vain onko lukema lähellä nollaa.
 *
 * Yksikkö on mitattu eikä oletettu (`docs/KOHDE.md`, 25.8.2026): kaksi lukemaa 12 h 16 min
 * välein pudottivat kuusi ottelua täsmälleen kuluneen ajan verran, joten kentät ovat
 * **tunnit:minuutit**. Minuutit:sekunnit olisi tyhjentänyt jokaisen kentän moninkertaisesti.
 *
 * Kello on kaksivaiheinen (sama mittaus): Grace kuluu ensin, ja vasta sen loputtua Time Pool
 * alkaa kulua. Ottelun hävittää Time Poolin loppuminen, ei gracen, ja siksi näitä kahta
 * lukemaa ei saa merkitä samalla tavalla.
 */
object MatchClock {
    /**
     * Time Poolin varoitusraja minuutteina: 48 tuntia.
     *
     * Raja on Tommin valinta 26.8.2026 eikä mittaustulos. Mitatut poolilukemat ovat satoja
     * tunteja (`234:39`, `579:21`), joten alle kahden vuorokauden pooli on selvästi eri
     * kokoluokkaa kuin tavallinen rivi.
     */
    const val POOL_WARNING_MINUTES: Int = 48 * 60

    /**
     * Kenttä minuutteina, tai null jos se ei ole muotoa `tunnit:minuutit`.
     *
     * Null on oikea vastaus useammalle kuin yhdelle syylle: profiilisivun päättyneiltä
     * otteluilta kentät puuttuvat kokonaan, ajattomassa ottelussa ne ovat `-`, ja
     * `Finished matches` -taulukossa on nähty myös `n/a`. Yksikään näistä ei ole nolla, ja
     * nollaksi tulkittuna jokainen tuottaisi väärän varoituksen.
     */
    fun minutes(text: String?): Int? {
        val value = text?.trim().orEmpty()
        val colon = value.indexOf(':')
        if (colon <= 0 || colon != value.lastIndexOf(':')) return null
        val hours = value.substring(0, colon)
        val mins = value.substring(colon + 1)
        if (mins.length != 2) return null
        if (!hours.all { it.isDigit() } || !mins.all { it.isDigit() }) return null
        val h = hours.toIntOrNull() ?: return null
        val m = mins.toIntOrNull() ?: return null
        if (m > 59) return null
        return h * 60 + m
    }

    /** Grace on kulunut loppuun, eli Time Pool kuluu juuri nyt. */
    fun graceSpent(match: Match): Boolean = minutes(match.graceText) == 0

    /** Time Poolia on alle varoitusrajan, eli ottelu on lähellä aikatappiota. */
    fun poolLow(match: Match): Boolean {
        val minutes = minutes(match.timePoolText) ?: return false
        return minutes < POOL_WARNING_MINUTES
    }
}
