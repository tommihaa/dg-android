package fi.tommi.dg.net

import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.net.InetAddress
import java.net.Socket
import java.net.SocketAddress
import java.net.SocketTimeoutException
import javax.net.SocketFactory

/**
 * Yhteys joka ei synny avataan uudelleen uudesta portista (Tommin valinta A 26.9.2026).
 *
 * Hotspotin musta aukko koskee yhtä yhteyttä kerrallaan (`docs/KOHDE.md`): sen SYN-uusinnat
 * eivät koskaan läpäise, ja uusi yhteys menee läpi. Testi jäljittelee sitä pistokeluojalla,
 * jonka ensimmäiset pistokkeet eivät saa yhteyttä. Palvelimen pyyntölaskuri kertoo mitä
 * sivustolle meni, joten väite on käytös eikä asetus.
 */
class DgClientConnectRetryTest {

    private lateinit var server: MockWebServer

    private val topPage = """
        <HTML><HEAD><TITLE>DailyGammon Top Page</TITLE></HEAD><BODY>ok</BODY></HTML>
    """.trimIndent()

    @BeforeEach
    fun setUp() {
        server = MockWebServer().apply { start() }
    }

    @AfterEach
    fun tearDown() {
        server.shutdown()
    }

    private fun client(blackHoles: Int): Pair<DgClient, BlackHoleSockets> {
        val sockets = BlackHoleSockets(blackHoles)
        val client = DgClient(
            credentials = { DgCredentials("testi", "salainen") },
            baseUrl = server.url("/"),
            minRequestIntervalMillis = 0,
            httpClient = OkHttpClient.Builder().socketFactory(sockets).build(),
        )
        return client to sockets
    }

    @Test
    fun `haku saa uuden yhteyden kun ensimmäinen ei synny`() {
        server.enqueue(MockResponse().setResponseCode(200).setBody(topPage))
        val (client, sockets) = client(blackHoles = 1)

        assertInstanceOf(DgResponse.Ok::class.java, client.fetch("/bg/top"))
        assertEquals(2, sockets.created, "Toisen yrityksen piti avata uusi pistoke")
        assertEquals(1, server.requestCount)
    }

    @Test
    fun `rungollinen teko menee perille kerran kun ensimmäinen yhteys ei synny`() {
        server.enqueue(MockResponse().setResponseCode(200).setBody(topPage))
        val (client, _) = client(blackHoles = 2)

        val response = client.submitForm("/bg/move/1234/5", mapOf("move" to "a"))

        assertInstanceOf(DgResponse.Ok::class.java, response)
        assertEquals(1, server.requestCount, "Lomakkeen piti mennä perille täsmälleen kerran")
    }

    @Test
    fun `kolmen mustan aukon jälkeen katko näytetään eikä mitään lähetetä`() {
        server.enqueue(MockResponse().setResponseCode(200).setBody(topPage))
        val (client, sockets) = client(blackHoles = 3)

        val response = client.submitForm("/bg/move/1234/5", mapOf("move" to "a"))

        assertInstanceOf(DgResponse.Offline::class.java, response)
        assertEquals(3, sockets.created, "Yrityksiä piti olla kolme")
        assertEquals(0, server.requestCount)
    }

    @Test
    fun `saadun yhteyden katkeamista ei uusita rungolliselle teolle`() {
        // Raja pysyy: kun yhteys syntyi, runko on voinut lähteä, eikä sitä lähetetä toiste.
        server.enqueue(MockResponse().apply { socketPolicy = SocketPolicy.DISCONNECT_AFTER_REQUEST })
        server.enqueue(MockResponse().setResponseCode(200).setBody(topPage))
        val (client, _) = client(blackHoles = 0)

        val response = client.submitForm("/bg/move/1234/5", mapOf("move" to "a"))

        assertInstanceOf(DgResponse.Offline::class.java, response)
        assertEquals(1, server.requestCount, "Lomaketta ei saa lähettää kahdesti")
    }

    /** Pistokeluoja jonka [holes] ensimmäistä pistoketta ei saa yhteyttä. */
    private class BlackHoleSockets(private val holes: Int) : SocketFactory() {
        var created = 0
            private set

        override fun createSocket(): Socket {
            created++
            return if (created <= holes) BlackHole() else Socket()
        }

        override fun createSocket(host: String, port: Int) = throw UnsupportedOperationException()
        override fun createSocket(host: String, port: Int, local: InetAddress, localPort: Int) =
            throw UnsupportedOperationException()
        override fun createSocket(host: InetAddress, port: Int) = throw UnsupportedOperationException()
        override fun createSocket(address: InetAddress, port: Int, local: InetAddress, localPort: Int) =
            throw UnsupportedOperationException()
    }

    private class BlackHole : Socket() {
        override fun connect(endpoint: SocketAddress, timeout: Int) {
            throw SocketTimeoutException("connect timed out")
        }
    }
}
