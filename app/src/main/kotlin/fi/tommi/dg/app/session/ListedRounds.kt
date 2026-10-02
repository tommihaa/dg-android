package fi.tommi.dg.app.session

import fi.tommi.dg.domain.Match
import fi.tommi.dg.domain.MatchId
import java.util.concurrent.ConcurrentHashMap

/**
 * Otteluluettelon kierrokset muodossa `3/5`, ottelutunnisteella.
 *
 * **Miksi tämä on olemassa (8.9.2026).** Lautasivun otsikko sanoo vain `Round 3`, ja
 * kierrosten kokonaismäärä on ainoastaan Top Pagen `Round`-sarakkeessa. Ensimmäinen toteutus
 * samana iltana vei arvon reitin kyselyparametrina laudalle, mutta se kattoi vain napautetun
 * ottelun: pelisessiossa `sessio-8-9-yo5` Tommi avasi yhden ottelun ja pelasi loput neljä
 * `Next Game` -ketjussa, ja jokainen niistä näytti pelkän `Round 3` vaikka luettelo tiesi
 * `3/5`. Nyt luettelo muistaa kaikkien rivien kierrokset tähän, ja lauta kysyy omansa sivun
 * omalla tunnisteella. Ketjussa tullut ottelu on sama ottelu jonka luettelo jo listasi.
 *
 * **Ei kyky sivustolla eikä säilö.** Tämä ei tuota yhtään pyyntöä ja unohtuu prosessin
 * kanssa: tyhjä muisti tarkoittaa vain että rivi on sivun oma `Round 3`. Sivuston viiva
 * (liigaottelu) jätetään muistamatta, koska se ei ole kierros.
 */
class ListedRounds {

    private val rounds = ConcurrentHashMap<MatchId, String>()

    fun remember(matches: List<Match>) {
        for (match in matches) {
            val round = match.round?.takeIf { it.isNotBlank() && it != "-" } ?: continue
            rounds[match.id] = round
        }
    }

    fun of(id: MatchId): String? = rounds[id]
}
