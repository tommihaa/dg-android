package fi.tommi.dg.net

import fi.tommi.dg.scrape.TopPageParser
import org.jsoup.Jsoup
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import java.io.File

/**
 * Kuution kuva oikeilla lautasivuilla.
 *
 * **Mittaus eikä korjaus, ja järjestys on merkitsevä.** Laiteajossa 9.8.2026 kuutio ei
 * piirtynyt lainkaan oikeassa ottelussa, eli `BoardState.cube` oli null. Talteen otetuissa
 * fixtureissa arvo on aina 1, joten null on tulos jota yksikään niistä ei tuota, ja sillä on
 * kaksi keskenään erottamatonta selitystä: sivulla ei ole kuutiokuvaa tässä tilanteessa, tai
 * kuva on mutta jäsennin ei lue sitä. Ilman sivun tavuja korjaus olisi arvaus siitä kumpaa
 * ollaan korjaamassa.
 *
 * Haku on turvallinen samasta syystä kuin [PageCaptureLiveTest]issä: kyselyparametriton
 * `/bg/move/`-GET ei muuta sivustolla mitään, ja tarkistus kulkee saman [safeFetch]in läpi.
 * Jonoon ei kosketa.
 */
@Tag("live")
class CubeCaptureLiveTest {

    @Test
    fun `kuution kuva luetaan useasta ottelusta`() {
        val credentials = LocalCredentials.loadOrNull()
        assumeTrue(credentials != null, "local.properties puuttuu, ks. local.properties.example")
        val client = DgClient(
            credentials = { credentials },
            cookieStore = FileCookieStore(File("build/live/cookies.txt")),
        )

        val paths = TopPageParser.parse(client.safeFetch("/bg/top"))!!
            .matches
            .mapNotNull { it.playPath }
            .take(MAX_MATCHES)
        assumeTrue(paths.isNotEmpty(), "Yhdessäkään ottelussa ei ollut Play-linkkiä")

        var withoutCube: String? = null

        paths.forEachIndexed { index, path ->
            val html = client.safeFetch(path)
            val document = Jsoup.parse(html)
            val cube = document.selectFirst("img[src*=cube]")

            if (cube != null) {
                val alt = cube.attr("alt")
                // Kuution solu ja sen paikka rivistössä. Omistajuuskysymys on juuri tämä:
                // jos omistettu kuutio istuu eri rivillä kuin omistamaton, omistaja on
                // sijainti eikä uusi kenttä sivulla.
                val cell = cube.closest("td")
                val row = cell?.closest("tr")
                val rowIndex = row?.let { it.parent()?.children()?.indexOf(it) }
                println("    solu bgcolor=${cell?.attr("bgcolor")} riviIndeksi=$rowIndex")

                when {
                    alt == "1" -> Unit
                    alt.toIntOrNull() != null -> save("move_cube_owned_$alt.html", html)
                    else -> save("move_cube_$alt.html", html)
                }
            }

            if (cube == null) {
                // Kaikki kuvat joiden nimessä ei ole `pt_`: jos kuutio on sivulla jollain
                // muulla nimellä, se erottuu tästä listasta. Tyhjä lista tarkoittaa ettei
                // kuutiokuvaa ole lainkaan, eli null on oikea lukema eikä jäsennysvika.
                val others = document.select("img[src]")
                    .map { it.attr("src") }
                    .filterNot { it.contains("pt_") }
                    .distinct()
                println("[$index] $path: EI kuutiokuvaa. Muut kuvat: $others")
                if (withoutCube == null) withoutCube = html
            } else {
                println("[$index] $path: src=${cube.attr("src")} alt=${cube.attr("alt")}")
            }
        }

        withoutCube?.let { save("move_no_cube.html", it) }
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

        /**
         * Otoksen katto. Sivusto pyörii vapaaehtoisvoimin, eikä kysymykseen tarvita kaikkia
         * otteluita: riittää nähdä esiintyykö kumpikin muoto.
         */
        const val MAX_MATCHES = 6
    }
}
