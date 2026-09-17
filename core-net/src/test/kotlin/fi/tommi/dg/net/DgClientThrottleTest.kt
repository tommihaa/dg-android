package fi.tommi.dg.net

import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.thread

/**
 * Pyyntötahti rakenteena eikä käytäntönä.
 *
 * Nämä testit ovat olemassa siksi, että kohteesta luvataan ulospäin kaksi asiaa
 * (`docs/YHTEYDENOTTO.md`): yksi pyyntö kerrallaan ja vähintään sekunti niiden välissä.
 * Molemmat olivat aiemmin tosia vain siksi, ettei kutsupaikkoja sattunut olemaan monta.
 * Lupaus jonka voi rikkoa lisäämällä yhden kutsujan ei ole lupaus.
 *
 * Sivusto pyörii vapaaehtoisvoimin, joten tämä on kohteliaisuutta joka on halvempi
 * todistaa kuin selittää.
 */
class DgClientThrottleTest {

    private lateinit var server: MockWebServer

    private val page = "<HTML><HEAD><TITLE>DailyGammon Top Page</TITLE></HEAD><BODY>ok</BODY></HTML>"

    /** Yhtä aikaa käsittelyssä olevien pyyntöjen määrä, ja sen suurin nähty arvo. */
    private val kaynnissa = AtomicInteger(0)
    private val suurinYhtaaikainen = AtomicInteger(0)

    @BeforeEach
    fun setUp() {
        server = MockWebServer().apply {
            dispatcher = object : Dispatcher() {
                override fun dispatch(request: RecordedRequest): MockResponse {
                    val nyt = kaynnissa.incrementAndGet()
                    suurinYhtaaikainen.updateAndGet { maxOf(it, nyt) }
                    // Viive on tarpeen: ilman sitä rinnakkaiset pyynnöt ehtisivät ohi
                    // toisistaan niin nopeasti, ettei päällekkäisyys näkyisi mittarissa.
                    Thread.sleep(120)
                    kaynnissa.decrementAndGet()
                    return MockResponse().setBody(page)
                }
            }
            start()
        }
    }

    @AfterEach
    fun tearDown() {
        server.shutdown()
    }

    private fun client(intervalMillis: Long) = DgClient(
        credentials = { DgCredentials("testi", "salainen") },
        baseUrl = server.url("/"),
        minRequestIntervalMillis = intervalMillis,
    )

    @Test
    fun `kolme rinnakkaista kutsua menee yksi kerrallaan`() {
        val dg = client(0)
        val saikeet = (1..3).map { thread { dg.fetch(DgPagesPath.TOP) } }
        saikeet.forEach { it.join() }

        assertEquals(3, server.requestCount, "kaikkien pyyntöjen pitää mennä läpi")
        assertEquals(
            1,
            suurinYhtaaikainen.get(),
            "kaksi pyyntöä oli yhtä aikaa ilmassa: lukko ei kata itse pyyntöä",
        )
    }

    @Test
    fun `vali mitataan edellisen pyynnon paattymisesta`() {
        val dg = client(200)
        val alku = System.currentTimeMillis()
        repeat(2) { dg.fetch(DgPagesPath.TOP) }
        val kesto = System.currentTimeMillis() - alku

        // Kaksi pyyntöä a 120 ms, ja niiden väliin vähintään 200 ms edellisen päättymisestä.
        // Aloituksista mitattu väli riittäisi 320 ms:ään, päättymisestä mitattu ei.
        assertTrue(kesto >= 440, "kesto oli $kesto ms, odotettiin vähintään 440 ms")
    }
}

/** Polku ilman riippuvuutta `core-scrape`n vakioihin, jotta testi kertoo vain tahdista. */
private object DgPagesPath {
    const val TOP = "/bg/top"
}
