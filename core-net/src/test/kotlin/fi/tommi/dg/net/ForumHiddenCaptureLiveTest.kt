package fi.tommi.dg.net

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import java.io.File

/**
 * Palstan piilotetut ketjut: `Show Hidden Threads` -linkin takana oleva sivu.
 *
 * **Mittaus eikä toteutus.** Tommi tilasi 22.9.2026 molemmille palstoille rivikohtaisen
 * Hide-napin ja alalaitaan Show Hidden Threads -napin. Indeksin linkit on mitattu
 * (`raakasivut/forum2_politics.html`: `/bg/forum2/politics/hide/64166` ja
 * `/bg/forum2/politics/listhidden`), mutta listan takana olevaa sivua ei ole kaapattu.
 *
 * Haku on turvallinen samasta syystä kuin [ForumOldThreadsCaptureLiveTest]issä:
 * kyselyparametriton GET palstan listaussivulle ei muuta sivustolla mitään eikä koske
 * jonoon. **`hide`-polkua ei haeta**, koska se on teko joka piilottaa ketjun.
 */
@Tag("live")
class ForumHiddenCaptureLiveTest {

    @Test
    fun `piilotettujen ketjujen sivu haetaan molemmilta palstoilta`() {
        val credentials = LocalCredentials.loadOrNull()
        assumeTrue(credentials != null, "local.properties puuttuu, ks. local.properties.example")
        val client = DgClient(
            credentials = { credentials },
            cookieStore = FileCookieStore(File("build/live/cookies.txt")),
        )

        listOf("main", "politics").forEach { board ->
            val html = client.safeFetch("/bg/forum2/$board/listhidden")
            save("forum2_${board}_listhidden.html", html)
            println("[$board] ${html.length} merkkiä, ketjulinkkejä " +
                Regex("""/bg/forum2/$board/read/""").findAll(html).count())
            Regex("""<A HREF=[^>]*>[^<]*</A>""", RegexOption.IGNORE_CASE).findAll(html)
                .filter { "forum2" in it.value }
                .forEach { println("[$board] linkki: ${it.value}") }
        }
    }

    /** Sama portti kuin [PageCaptureLiveTest]issä, ja lisäksi `hide`. */
    private fun DgClient.safeFetch(path: String): String {
        val forbidden = CONSUMING.filter { path.contains(it, ignoreCase = true) }
        check(forbidden.isEmpty()) {
            "Polku on kuluttava tai kirjoittava (${forbidden.joinToString()}), tätä ei haeta: $path"
        }
        val response = fetch(path)
        assertTrue(response is DgResponse.Ok) {
            "Haku epäonnistui ($path): ${response::class.simpleName}"
        }
        return (response as DgResponse.Ok).html
    }

    private fun save(name: String, html: String) {
        val target = File("../raakasivut/$name")
        target.parentFile.mkdirs()
        target.writeText(html)
        println("Tallennettu: ${target.canonicalPath} (${html.length} merkkiä)")
    }

    private companion object {
        val CONSUMING = listOf("nextgame", "submit", "skip", "move=", "/hide/", "unhide")
    }
}
