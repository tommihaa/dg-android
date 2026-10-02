package fi.tommi.dg.net

import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * Feature 2:n ydin todennettuna ilman verkkoa.
 *
 * Nämä testit ovat olemassa yhden mitatun havainnon takia: DailyGammon palauttaa
 * uloskirjautuneelle käyttäjälle **200 OK ja login-lomakkeen**. Asiakas joka luottaa
 * statuskoodiin luulee saaneensa dataa. Testit kuvaavat sen tilanteen tarkalleen.
 */
class DgClientSessionTest {

    private lateinit var server: MockWebServer
    private lateinit var client: DgClient

    private val loginPage = """
        <HTML><HEAD><TITLE>DailyGammon -- Please Login</TITLE></HEAD><BODY>
        <FORM ACTION=/bg/login METHOD=POST>
        <INPUT NAME=path TYPE=hidden VALUE="top/">
        <INPUT NAME=login LENGTH=32><INPUT NAME=password TYPE=password LENGTH=16>
        </FORM></BODY></HTML>
    """.trimIndent()

    /** Sanasta sanaan mitattu sivu 11.9.2026 (`raakasivut/kayttokatko.html`). */
    private val sleepingPage = """
        <HTML><HEAD><TITLE>DailyGammon Backups</TITLE></HEAD>
        <BODY BGCOLOR=#ffffff><IMG SRC=/images/dglogo.gif>
        <p>DailyGammon is sleeping -- SHH!!<p>

        Come back in half an hour or so when the daily
        backups are done, and your games will be here waiting. Gwan! Scoot!
        </BODY></HTML>
    """.trimIndent()

    private val topPage = """
        <HTML><HEAD><TITLE>DailyGammon Top Page</TITLE></HEAD><BODY>
        <TABLE><TR><TD>Match vs Someone</TD></TR></TABLE></BODY></HTML>
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
    fun `voimassa oleva istunto palauttaa sivun suoraan`() {
        server.enqueue(MockResponse().setResponseCode(200).setBody(topPage))

        val response = client.fetch("/bg/top")

        assertInstanceOf(DgResponse.Ok::class.java, response)
        assertEquals(1, server.requestCount, "Turhaa kirjautumista ei saa tehdä")
    }

    @Test
    fun `katkennut istunto uusitaan hiljaa ja kutsuja saa halutun sivun`() {
        // 1. pyyntö: palvelin vastaa 200 OK mutta antaa login-lomakkeen.
        server.enqueue(MockResponse().setResponseCode(200).setBody(loginPage))
        // 2. pyyntö: kirjautumisen POST, joka palauttaa suoraan halutun sivun.
        server.enqueue(MockResponse().setResponseCode(200).setBody(topPage))

        val response = client.fetch("/bg/top")

        assertInstanceOf(DgResponse.Ok::class.java, response)
        assertTrue((response as DgResponse.Ok).html.contains("Match vs Someone"))

        server.takeRequest() // alkuperäinen GET
        val loginRequest = server.takeRequest()
        assertEquals("POST", loginRequest.method)
        assertEquals("/bg/login", loginRequest.path)

        // Paluupolku otetaan palvelimen omasta piilokentästä, ei meidän arvauksestamme.
        val body = loginRequest.body.readUtf8()
        assertTrue(body.contains("path=top%2F"), "Paluupolku puuttui: $body")
        assertTrue(body.contains("login=testi"))

        // Kaksi pyyntöä riitti: paluupolun ansiosta erillistä uudelleenhakua ei tarvittu.
        assertEquals(2, server.requestCount)
    }

    @Test
    fun `väärät tunnukset tuottavat AuthFailed eivät hiljaista silmukkaa`() {
        server.enqueue(MockResponse().setResponseCode(200).setBody(loginPage))
        server.enqueue(MockResponse().setResponseCode(200).setBody(loginPage))

        val response = client.fetch("/bg/top")

        assertInstanceOf(DgResponse.AuthFailed::class.java, response)
        assertEquals(2, server.requestCount, "Kirjautumista ei saa yrittää loputtomasti")
    }

    @Test
    fun `yhteyskatko tuottaa Offline ei virhettä`() {
        server.shutdown()

        val response = client.fetch("/bg/top")

        assertInstanceOf(DgResponse.Offline::class.java, response)
    }

    /**
     * Nukkumissivu on HTTP 200 ilman login-lomaketta, eli ilman omaa lajia se olisi `Ok`
     * ja kutsuja saisi jäsentimelle tuntemattoman sivun. Mitattu 11.9.2026.
     */
    @Test
    fun `nukkumissivu on Sleeping eikä Ok`() {
        server.enqueue(MockResponse().setResponseCode(200).setBody(sleepingPage))

        val response = client.fetch("/bg/top")

        assertEquals(DgResponse.Sleeping, response)
    }

    /**
     * Katkon aikana kirjautuminenkin saa nukkumissivun. Se ei ole login-sivu, joten ilman
     * omaa lajia istunnon uusiminen näyttäisi onnistuneelta ja palauttaisi ilmoituksen sivuna.
     */
    @Test
    fun `istunnon uusiminen katkon aikana on Sleeping eikä onnistunut kirjautuminen`() {
        server.enqueue(MockResponse().setResponseCode(200).setBody(loginPage))
        server.enqueue(MockResponse().setResponseCode(200).setBody(sleepingPage))

        val response = client.fetch("/bg/top")

        assertEquals(DgResponse.Sleeping, response)
    }

    @Test
    fun `palvelinvirhe erotetaan katkenneesta istunnosta`() {
        server.enqueue(MockResponse().setResponseCode(503))

        val response = client.fetch("/bg/top")

        assertEquals(DgResponse.ServerError(503), response)
    }

    @Test
    fun `lomaketta ei lähetetä uudelleen jos istunto oli katkennut`() {
        // Lomakelähetys ei ole idempotentti: DailyGammonissa siirto rakentuu palvelimella
        // klikkaus kerrallaan, joten vahingossa toistettu lähetys muuttaisi pelin tilaa.
        server.enqueue(MockResponse().setResponseCode(200).setBody(loginPage))

        val response = client.submitForm("/bg/move/123", mapOf("submit" to "1"))

        assertInstanceOf(DgResponse.AuthFailed::class.java, response)
        assertEquals(1, server.requestCount)
    }
}
