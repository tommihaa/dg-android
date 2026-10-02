package fi.tommi.dg.net

import fi.tommi.dg.scrape.DgPages
import fi.tommi.dg.scrape.TopPageParser
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import java.io.File

/**
 * Vaiheen 1 hyväksymistesti: kirjautuminen oikeaan DailyGammoniin ja Top Pagen haku.
 *
 * Ajo: `gradlew :core-net:liveTest`. Ei kuulu tavalliseen `test`-ajoon, koska se on
 * hidas, riippuu vieraasta palvelimesta ja vaatii tunnukset.
 *
 * Sivutuote on tärkeämpi kuin väite: testi tallentaa haetun sivun tiedostoon, jotta
 * Top Page -parseri voidaan kirjoittaa oikeaa HTML:ää vasten eikä arvauksen varassa.
 */
@Tag("live")
class DgClientLiveTest {

    private lateinit var client: DgClient
    private lateinit var cookieFile: File

    @BeforeEach
    fun setUp() {
        val credentials = LocalCredentials.loadOrNull()
        assumeTrue(
            credentials != null,
            "local.properties puuttuu tai dg.login/dg.password on tyhjä. Ks. local.properties.example.",
        )
        cookieFile = File("build/live/cookies.txt")
        client = DgClient(
            credentials = { credentials },
            cookieStore = FileCookieStore(cookieFile),
        )
    }

    @Test
    fun `kirjautuminen onnistuu ja top page saadaan haettua`() {
        val loggedIn = client.login()
        assertTrue(loggedIn is DgResponse.Ok) {
            "Kirjautuminen epäonnistui: ${loggedIn::class.simpleName}"
        }

        val top = client.fetch("/bg/top")
        assertTrue(top is DgResponse.Ok) {
            "Top Pagen haku epäonnistui: ${top::class.simpleName}"
        }

        val html = (top as DgResponse.Ok).html
        assertFalse(DgPages.isLoginPage(html), "Saatiin login-sivu, ei Top Pagea")

        val captured = File("build/captured/top_page.html")
        captured.parentFile.mkdirs()
        captured.writeText(html)

        println("Top Page haettu: ${html.length} merkkiä")
        println("Tallennettu: ${captured.absolutePath}")
        println("HUOM: tiedosto sisältää käyttäjänimiä. Anonymisoi ennen kuin siirrät sen fixtureksi.")

        // Jäsennys todennetaan oikeaa sivua vasten eikä pelkkää anonymisoitua fixturea:
        // anonymisointi voisi vahingossa korjata jotain mikä oikeassa datassa on rikki.
        val page = TopPageParser.parse(html)!!
        // Tyhjä lista on kelvollinen tulos eikä jäsennysvirhe. Mitattu 1.8.2026: kun
        // yhdessäkään ottelussa ei ole oma vuoro, Top Pagella ei ole taulukkoa lainkaan
        // vaan lause "There are no matches where you can move.". Ero on tärkeä, koska
        // väite tässä koskee jäsennintä eikä sivuston senhetkistä sisältöä.
        assumeTrue(page.matches.isNotEmpty()) {
            "Top Page oli tyhjä (ei siirrettäviä otteluita), joten jäsennystä ei voi " +
                "todentaa nyt. Aja uudestaan kun vuoro on omalla kohdalla."
        }
        assertTrue(
            page.matches.all { it.opponent.displayName.isNotBlank() },
            "Vastustajan nimi jäi tyhjäksi jollakin rivillä",
        )
        assertTrue(
            page.matches.all { it.id.value.isNotBlank() },
            "Pelitunniste jäi tyhjäksi jollakin rivillä",
        )
        // Nimiä ei tulosteta: ne ovat muiden ihmisten tietoja.
        println("Jäsennettiin ${page.matches.size} ottelua, joista vuoro ${page.matches.count { it.myTurn == true }}:ssa")
        println("Ilmoituslippu (/bg/nextgame): ${page.hasMessageNotice}")
        println("Otteluita ilman tapahtumatunnistetta: ${page.matches.count { it.eventId == null }}")
        println("Otteluita ilman pituutta: ${page.matches.count { it.matchLength == null }}")
    }

    @Test
    fun `katkennut istunto korjautuu ilman käyttäjän toimia`() {
        // Varmistetaan ensin toimiva istunto.
        assertTrue(client.login() is DgResponse.Ok)

        // Simuloidaan istunnon katkeamista poistamalla keksit levyltä. Tämä on sama
        // tilanne kuin DG Mobilessa, jossa käyttäjä joutuu kirjautumaan uudestaan.
        cookieFile.delete()

        val recovered = client.fetch("/bg/top")
        assertTrue(recovered is DgResponse.Ok) {
            "Istunto ei palautunut itsestään: ${recovered::class.simpleName}"
        }
        assertFalse(DgPages.isLoginPage((recovered as DgResponse.Ok).html))
        println("Istunto palautui hiljaa, käyttäjälle ei näytetty mitään.")
    }
}
