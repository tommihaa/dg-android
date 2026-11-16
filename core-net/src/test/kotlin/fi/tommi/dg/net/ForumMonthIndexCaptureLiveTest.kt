package fi.tommi.dg.net

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import java.io.File

/**
 * Generalin koko otsikkoluettelo: jokainen `Old Threads` -kuukausisivu talteen.
 *
 * **Mittaus eikä toteutus** (Tommin tilaus 13.11.2026). Tarkoitus on nähdä otsikoista, mitä
 * pelaajien itse järjestämistä turnauksista ja toivotuista ominaisuuksista on kirjoitettu,
 * ennen kuin yhtään ketjua haetaan. Kuukausisivulla on ketjun otsikko, viestimäärä,
 * aloittaja ja aika, joten runkoja ei tarvita tähän vaiheeseen.
 *
 * Haku on turvallinen samasta syystä kuin [ForumOldThreadsCaptureLiveTest]issä: listaussivu
 * ei merkitse ketjuja luetuiksi eikä koske jonoon. Pyyntöjä on yksi per kuukausi (noin 275),
 * [DgClient]in omalla minimivälillä. Jo tallennettu kuukausi ohitetaan, joten keskeytynyt ajo
 * jatkuu siitä mihin jäi. Kuluva kuukausi haetaan aina uudestaan, koska se kasvaa.
 */
@Tag("live")
class ForumMonthIndexCaptureLiveTest {

    @Test
    fun `Generalin kuukausisivut haetaan talteen`() {
        val credentials = LocalCredentials.loadOrNull()
        assumeTrue(credentials != null, "local.properties puuttuu, ks. local.properties.example")
        val client = DgClient(
            credentials = { credentials },
            cookieStore = FileCookieStore(File("build/live/cookies.txt")),
        )
        val board = "main"
        val dir = File("../raakasivut/palstaindeksi/dg").apply { mkdirs() }

        val first = client.safeFetch("/bg/forum2/$board/month")
        File(dir, "${board}_nykyinen.html").writeText(first, Charsets.UTF_8)
        val months = Regex("""/bg/forum2/$board/month/(\d{4})/(\d{1,2})""")
            .findAll(first).map { it.groupValues[1].toInt() to it.groupValues[2].toInt() }
            .distinct().toList()
        println("Kuukausilinkkejä: ${months.size}")
        assertTrue(months.size > 200) { "Kuukausilinkkejä odotettua vähemmän: ${months.size}" }

        var fetched = 0
        months.forEach { (year, month) ->
            val target = File(dir, "%s_%04d_%02d.html".format(board, year, month))
            if (target.exists() && target.length() > 500) return@forEach
            val html = client.safeFetch("/bg/forum2/$board/month/$year/$month")
            target.writeText(html, Charsets.UTF_8)
            fetched++
            if (fetched % 25 == 0) println("Haettu $fetched")
        }
        println("Valmis: haettu $fetched, kansiossa ${dir.listFiles()?.size} tiedostoa")
    }

    /** Sama portti kuin [PageCaptureLiveTest]issä, ks. sen perustelu. */
    private fun DgClient.safeFetch(path: String): String {
        val forbidden = CONSUMING.filter { path.contains(it, ignoreCase = true) }
        check(forbidden.isEmpty()) {
            "Polku on kuluttava (${forbidden.joinToString()}), tätä ei haeta: $path"
        }
        val response = fetch(path)
        assertTrue(response is DgResponse.Ok) {
            "Haku epäonnistui ($path): ${response::class.simpleName}"
        }
        return (response as DgResponse.Ok).html
    }

    private companion object {
        val CONSUMING = listOf("nextgame", "submit", "skip", "move=", "/read/", "/hide/")
    }
}
