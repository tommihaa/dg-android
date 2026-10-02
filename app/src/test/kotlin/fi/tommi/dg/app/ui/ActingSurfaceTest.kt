package fi.tommi.dg.app.ui

import fi.tommi.dg.domain.MatchOverPage
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Portin sääntö tyhjentävänä `when`inä: **uusi tila rikkoo käännöksen, ei hiljaisuutta.**
 *
 * Tämä testi on olemassa yhden mitatun virheen takia (1.9.2026). Kun päättymissivu sai oman
 * tilansa, teot eivät koskeneet sitä: `follow`, `press` ja `sendChat` alkoivat kukin omalla
 * `as? BoardUiState.Loaded ?: return` -rivillään, joten uusi tila putosi kaikista kolmesta
 * hiljaa. Ruutu piirsi napit, napautus ei tehnyt mitään, eikä yksikään testi huomannut:
 * mitään ei ollut rikki, teot vain eivät kuuluneet uudelle tilalle.
 *
 * Alla oleva `when` on tyhjentävä, eli **seuraava tila ei käänny ennen kuin joku vastaa
 * kysymykseen**: saako tältä pinnalta tehdä tekoja. Vastaus `true` tarkoittaa
 * [ActingSurface]in toteuttamista, `false` sen jättämistä, ja testi vaatii että vastaus ja
 * tyyppi ovat samaa mieltä.
 */
class ActingSurfaceTest {

    /**
     * Onko tila tekopinta. Vastaus on ihmisen päätös, ja [ActingSurface] on sen toteutus;
     * testi vertaa näitä kahta toisiinsa.
     */
    private fun BoardUiState.pitaisiTehdaTekoja(): Boolean = when (this) {
        is BoardUiState.Loaded -> true
        is BoardUiState.MatchOver -> true
        BoardUiState.Loading -> false
        is BoardUiState.NotABoard -> false
        is BoardUiState.Failed -> false
        BoardUiState.SessionExpired -> false
    }

    /**
     * Käännösaikainen väite laudasta, jota ei voi tehdä ilmentymällä: `BoardState`in
     * rakentaminen tähän vaatisi koko laudan, eikä testin kohde ole lauta vaan tyyppi.
     * Rivi kääntyy vain jos `Loaded` on [ActingSurface].
     */
    @Suppress("CAST_NEVER_SUCCEEDS", "UNUSED")
    private val laudanTyyppi: ActingSurface? = null as BoardUiState.Loaded?

    @Test
    fun `jokainen tila on joko tekopinta tai nimetty tekemattomaksi`() {
        val tilat = listOf(
            BoardUiState.Loading,
            BoardUiState.SessionExpired,
            BoardUiState.NotABoard(NotABoardKind.TopPage("<html></html>")),
            BoardUiState.Failed(Failure.Offline),
            BoardUiState.MatchOver(
                MatchOverPage(
                    matchId = null,
                    eventName = null,
                    eventPath = null,
                    roundLabel = null,
                    resultText = "pelaaja wins 2 points and the match.",
                    predicted = false,
                    matchLength = null,
                    scores = emptyList(),
                    nextGamePath = null,
                    reviewPath = null,
                    skipPath = null,
                    form = null,
                )
            ),
        )

        tilat.forEach { tila ->
            assertEquals(
                "Tila ${tila::class.simpleName} on eri mieltä itsestään",
                tila.pitaisiTehdaTekoja(),
                tila is ActingSurface,
            )
        }
    }
}
