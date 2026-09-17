package fi.tommi.dg.net

import fi.tommi.dg.domain.BoardForm
import fi.tommi.dg.domain.FormMethod
import fi.tommi.dg.domain.press
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * Uudelleenyrityksen raja: rungoton lähettäminen toistetaan, rungollinen ei.
 *
 * Testit ovat olemassa mitatun vian takia. 8.8.2026 Pixel 8a:lla **ensimmäinen haku yli
 * viiden sekunnin tauon jälkeen epäonnistui joka kerta** (`No connection. Nothing was
 * sent.`), ja heti perään painettu `Refresh` onnistui. Erotteleva koe ajettiin: alle viiden
 * sekunnin tauolla lauta latautui ensimmäisellä yrityksellä, eli vika riippui tauon
 * pituudesta eikä siitä mikä ruutu avattiin.
 *
 * **Sama vika osui `Submit Move`iin 14.8.2026: kolme yritystä kolmesta epäonnistui
 * täsmälleen 5 s tienoilla.** `send()` (lautasivun lomake, kyselyllinen GET, ei runkoa)
 * siirtyi silloin uusittavaan asiakkaaseen, koska katkenneella yhteydellä ei ole runkoa
 * jonka kaksinkertaistuminen olisi vaarallista; `submitForm()` (kirjautuminen, POST, on
 * runko) jäi ennalleen.
 *
 * Ero on tässä kuvattu **käytöksenä eikä asetuksena**. `retryOnConnectionFailure` on
 * kirjaston kytkin, ja kytkimen arvon väittäminen testissä olisi saman rivin toisto toisin
 * sanoin. Nämä kolme testiä sen sijaan kaatuvat myös silloin kun kytkin on oikein mutta
 * pyyntö menee väärälle asiakkaalle.
 *
 * Jälkimmäinen on se arvokkaampi: se pitää voimassa lupauksen ettei lomaketta lähetetä
 * käyttäjän puolesta kahdesti. Kohde on vapaaehtoisvoimin pyöritetty sivusto, ja siirron
 * lähetys on siellä oikeaa peliä.
 */
class DgClientRetryTest {

    private lateinit var server: MockWebServer
    private lateinit var client: DgClient

    private val topPage = """
        <HTML><HEAD><TITLE>DailyGammon Top Page</TITLE></HEAD><BODY>ok</BODY></HTML>
    """.trimIndent()

    @BeforeEach
    fun setUp() {
        server = MockWebServer().apply { start() }
        client = DgClient(
            credentials = { DgCredentials("testi", "salainen") },
            baseUrl = server.url("/"),
            minRequestIntervalMillis = 0,
        )
    }

    @AfterEach
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `katkennut haku yritetään uudelleen ja onnistuu ilman että kutsuja huomaa`() {
        // Palvelin vastaa ensimmäiseen ja sulkee yhteyden, kuten keep-alive-aikakatkaisu.
        // Toinen haku poimii altaasta kuolleen yhteyden.
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(topPage)
                .apply { socketPolicy = SocketPolicy.DISCONNECT_AT_END },
        )
        server.enqueue(MockResponse().setResponseCode(200).setBody(topPage))

        assertInstanceOf(DgResponse.Ok::class.java, client.fetch("/bg/top"))
        val second = client.fetch("/bg/top")

        assertInstanceOf(
            DgResponse.Ok::class.java,
            second,
            "Haun piti onnistua uudelleenyrityksellä, ei palauttaa Offlinea",
        )
    }

    @Test
    fun `katkennutta lomakelähetystä ei toisteta vaan se epäonnistuu näkyvästi`() {
        server.enqueue(MockResponse().apply { socketPolicy = SocketPolicy.DISCONNECT_AFTER_REQUEST })
        server.enqueue(MockResponse().setResponseCode(200).setBody(topPage))

        val response = client.submitForm("/bg/move/1234/5", mapOf("move" to "a"))

        // Lajia eikä arvoa: poikkeuksen luokka riippuu siitä missä kohdassa yhteys katkeaa,
        // eikä väite katkoksesta saa olla kiinni siitä kumman OkHttp sattuu heittämään.
        assertInstanceOf(
            DgResponse.Offline::class.java,
            response,
            "Lähetyksen piti epäonnistua näkyvästi eikä mennä perille toisella yrityksellä",
        )
        assertEquals(1, server.requestCount, "Lomaketta ei saa lähettää kahdesti")
    }

    @Test
    fun `katkennut Submit Move yritetään uudelleen ja onnistuu ilman toista lähetystä`() {
        // Sama kuolleen yhteyden koe kuin haulla, mutta lautasivun lomakkeella: rungoton
        // GET, joten OkHttpin oma uusinta on turvallinen eikä vaadi kahta pyyntöä
        // palvelimelle asti.
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(topPage)
                .apply { socketPolicy = SocketPolicy.DISCONNECT_AT_END },
        )
        server.enqueue(MockResponse().setResponseCode(200).setBody(topPage))

        val form = BoardForm(
            action = "/bg/move/1234/5",
            method = FormMethod.GET,
            pendingMove = "rrmm",
            submits = listOf("Submit Move"),
            verify = null,
        )
        val submission = requireNotNull(form.press("Submit Move"))
        val response = client.send(submission)

        assertInstanceOf(
            DgResponse.Ok::class.java,
            response,
            "Submit Moven piti onnistua uudelleenyrityksellä, ei palauttaa Offlinea",
        )
    }
}
