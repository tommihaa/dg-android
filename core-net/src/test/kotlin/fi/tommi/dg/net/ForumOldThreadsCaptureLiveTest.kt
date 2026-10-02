package fi.tommi.dg.net

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import java.io.File

/**
 * Palstan vanhat ketjut: `Old Threads` -linkin takana oleva sivu.
 *
 * **Mittaus eikä toteutus.** Tommi huomasi 29.8.2026 laitteella, että molemmat palstat
 * näyttävät vain nykyisen indeksin eikä vanhempiin pääse. Sivustolla on siihen oma linkki
 * (`raakasivut/forum2_politics.html`: `<A HREF="/bg/forum2/politics/month">Old Threads</A>`),
 * mutta sen takana olevaa sivua ei ole kertaakaan kaapattu, joten jäsentimen kirjoittaminen
 * olisi arvaus sivun muodosta. Tämä test hakee sivun molemmilta palstoilta.
 *
 * Haku on turvallinen samasta syystä kuin [PageCaptureLiveTest]issä: kyselyparametriton GET
 * palstan listaussivulle ei muuta sivustolla mitään eikä koske jonoon. Se ei myöskään merkitse
 * ketjuja luetuiksi, koska lukumerkinnän tekee ketjun oma `read`-sivu eikä indeksi.
 */
@Tag("live")
class ForumOldThreadsCaptureLiveTest {

    @Test
    fun `vanhojen ketjujen sivu haetaan molemmilta palstoilta`() {
        val credentials = LocalCredentials.loadOrNull()
        assumeTrue(credentials != null, "local.properties puuttuu, ks. local.properties.example")
        val client = DgClient(
            credentials = { credentials },
            cookieStore = FileCookieStore(File("build/live/cookies.txt")),
        )

        listOf("main", "politics").forEach { board ->
            val html = client.safeFetch("/bg/forum2/$board/month")
            save("forum2_${board}_month.html", html)
            println("[$board] ${html.length} merkkiä, ketjulinkkejä " +
                Regex("""/bg/forum2/$board/read/""").findAll(html).count())
            // Mitä sivun oma navigaatio tarjoaa: tästä nähdään onko vanhoja sivuja useampi.
            Regex("""<A HREF=[^>]*>([^<]*(?:Threads|Index|Month|month)[^<]*)</A>""",
                RegexOption.IGNORE_CASE).findAll(html).forEach {
                println("[$board] linkki: ${it.value}")
            }
        }
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

    private fun save(name: String, html: String) {
        val target = File("../raakasivut/$name")
        target.parentFile.mkdirs()
        target.writeText(html)
        println("Tallennettu: ${target.canonicalPath} (${html.length} merkkiä)")
    }

    private companion object {
        val CONSUMING = listOf("nextgame", "submit", "skip", "move=")
    }
}
