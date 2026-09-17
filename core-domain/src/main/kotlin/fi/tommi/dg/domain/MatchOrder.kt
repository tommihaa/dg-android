package fi.tommi.dg.domain

/**
 * Otteluluettelon järjestysperuste, kun pelaaja valitsee sen itse.
 *
 * **Oletus on sivuston oma järjestys, eikä se ole makuasia.** `SUBSTANSSI.md` kohta 21:
 * sivusto listaa ottelut kiireellisyysjärjestyksessä (`Grace then Pool`), joten listan
 * alkupää on se jossa aikaa on vähiten. Oma lajittelu hävittäisi sen tiedon hiljaa, ja
 * siksi se tehdään vain pyydettäessä ja siitä pääsee takaisin.
 *
 * `key = null` **on** sivuston järjestys eikä puuttuva arvo: lista jätetään koskematta.
 */
enum class MatchSortKey { GRACE, TIME_POOL, ROUND, LENGTH, OPPONENT }

data class MatchOrder(val key: MatchSortKey? = null, val descending: Boolean = false) {

    /**
     * Sama sarake napautettuna uudelleen: nouseva, laskeva, ja takaisin sivuston
     * järjestykseen. Kolmas napautus on se joka tekee oletuksesta saavutettavan yhdellä
     * teolla eikä erillisellä nollausnapilla.
     *
     * Toinen sarake aloittaa aina nousevasta, koska laskeva olisi edellisen sarakkeen
     * valinnan periytymistä uuteen kysymykseen.
     */
    fun tapped(key: MatchSortKey): MatchOrder = when {
        this.key != key -> MatchOrder(key, descending = false)
        !descending -> MatchOrder(key, descending = true)
        else -> SITE
    }

    companion object {
        /** Sivuston oma järjestys. */
        val SITE = MatchOrder()
    }
}

/**
 * Lista pyydetyssä järjestyksessä.
 *
 * **Lajittelu on vakaa**, eli saman arvon jakavat ottelut pysyvät sivuston keskinäisessä
 * järjestyksessä. Se on tässä sisältöä eikä toteutuksen sivuseikka: kaksi ottelua joilla on
 * sama `Length` ovat yhä kiireellisyysjärjestyksessä keskenään.
 *
 * **Puuttuva arvo on viimeisenä molempiin suuntiin.** Puuttuva ei ole suurin eikä pienin,
 * ja käänteinen järjestys nostaisi muuten tyhjät rivit listan kärkeen. Puuttuvia on
 * oikeasti: profiilisivun päättyneiltä otteluilta aikakentät puuttuvat, ja ajattomassa
 * ottelussa ne ovat viivoja.
 */
fun List<Match>.ordered(order: MatchOrder): List<Match> {
    val key = order.key ?: return this
    val comparator = when (key) {
        MatchSortKey.GRACE -> byValue(order.descending) { MatchClock.minutes(it.graceText) }
        MatchSortKey.TIME_POOL -> byValue(order.descending) { MatchClock.minutes(it.timePoolText) }
        MatchSortKey.ROUND -> byValue(order.descending) { roundNumber(it.round) }
        MatchSortKey.LENGTH -> byValue(order.descending) { it.matchLength }
        // Nimet vertaillaan pienaakkosina, koska sivustolla on sekä `alydar` että
        // `Willie Wonka`, ja tavuvertailu laittaisi kaikki isolla alkavat ensin.
        MatchSortKey.OPPONENT -> byValue(order.descending) { it.opponent.displayName.lowercase() }
    }
    return sortedWith(comparator)
}

/**
 * Kierros muodossa `5/6`, eli monesko peli monesta. Vertailuun otetaan ensimmäinen luku:
 * se kertoo kuinka pitkällä ottelu on, ja se on ainoa mitä sarakkeesta voi kysyä.
 */
private fun roundNumber(round: String?): Int? =
    round?.substringBefore('/')?.trim()?.toIntOrNull()

private fun <T : Comparable<T>> byValue(
    descending: Boolean,
    selector: (Match) -> T?,
): Comparator<Match> = Comparator { first, second ->
    val a = selector(first)
    val b = selector(second)
    when {
        a == null && b == null -> 0
        a == null -> 1
        b == null -> -1
        descending -> b.compareTo(a)
        else -> a.compareTo(b)
    }
}
