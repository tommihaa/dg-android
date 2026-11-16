package fi.tommi.dg.net

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import java.io.File

/**
 * Hakee luettelossa nimetyt Generalin ketjut talteen (Tommin tilaus 13.11.2026).
 *
 * Luettelo on `raakasivut/palstaindeksi/dg/haettavat.txt`, yksi ketjutunnus rivin alussa.
 * Se on valittu otsikkotaulusta, jonka [ForumMonthIndexCaptureLiveTest] tuotti.
 *
 * **Ketjun haku on kuluttava teko, toisin kuin listaussivun**: `read`-sivu siirtää tilin
 * lukumerkin ketjun loppuun. Siksi indeksi haetaan ensin, ja ketju jonka indeksi näyttää
 * lukemattomaksi tai kesken luetuksi **ohitetaan**. Kokonaan luetun ketjun rivillä linkin
 * fragmentti on sama kuin viestimäärä (`docs/KOHDE.md`, palstan rakenne), joten sen haku ei
 * muuta mitään. Ketju joka ei ole indeksissä on vanha, eikä sen merkki näy missään.
 *
 * Jo tallennettu ketju ohitetaan, joten keskeytynyt ajo jatkuu siitä mihin jäi.
 */
@Tag("live")
class ForumThreadListCaptureLiveTest {

    @Test
    fun `luettelon ketjut haetaan talteen`() {
        val credentials = LocalCredentials.loadOrNull()
        assumeTrue(credentials != null, "local.properties puuttuu, ks. local.properties.example")
        val dir = File("../raakasivut/palstaindeksi/dg")
        val list = File(dir, "haettavat.txt")
        assumeTrue(list.exists(), "haettavat.txt puuttuu")
        val ids = list.readLines().mapNotNull { it.substringBefore('\t').trim().toIntOrNull() }
        val out = File(dir, "ketjut").apply { mkdirs() }
        val client = DgClient(
            credentials = { credentials },
            cookieStore = FileCookieStore(File("build/live/cookies.txt")),
        )

        val index = client.get("/bg/forum2/main")
        // Indeksin rivi: mahdollinen New-merkki, linkki fragmentteineen ja viestimäärä.
        val unread = Regex("""<TR[^>]*>(.*?)(?=<TR|</TABLE)""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
            .findAll(index).mapNotNull { row ->
                val link = Regex("""/bg/forum2/main/read/(\d+)(?:#(\d+))?""").find(row.value)
                    ?: return@mapNotNull null
                val count = Regex("""ALIGN=CENTER[^>]*>\s*(\d+)""", RegexOption.IGNORE_CASE)
                    .find(row.value)?.groupValues?.get(1)
                val fullyRead = link.groupValues[2].isNotEmpty() && link.groupValues[2] == count
                if (fullyRead) null else link.groupValues[1].toInt()
            }.toSet()
        println("Indeksissä lukematta tai kesken: ${unread.size}")

        var fetched = 0
        val skipped = mutableListOf<Int>()
        ids.forEach { id ->
            val target = File(out, "$id.html")
            if (target.exists() && target.length() > 500) return@forEach
            if (id in unread) { skipped += id; return@forEach }
            target.writeText(client.get("/bg/forum2/main/read/$id"), Charsets.UTF_8)
            fetched++
            if (fetched % 25 == 0) println("Haettu $fetched")
        }
        println("Valmis: haettu $fetched, ohitettu lukemattomina ${skipped.size}: $skipped")
    }

    private fun DgClient.get(path: String): String {
        val forbidden = CONSUMING.filter { path.contains(it, ignoreCase = true) }
        check(forbidden.isEmpty()) { "Polku ei kuulu tähän hakuun (${forbidden.joinToString()}): $path" }
        val response = fetch(path)
        assertTrue(response is DgResponse.Ok) { "Haku epäonnistui ($path): ${response::class.simpleName}" }
        return (response as DgResponse.Ok).html
    }

    private companion object {
        val CONSUMING = listOf("nextgame", "submit", "skip", "move=", "/hide/", "/add/", "/new")
    }
}
