package fi.tommi.dg.net

import fi.tommi.dg.scrape.ForumParser
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import java.io.File

/**
 * Palstan `Preview`-vastaus: mitä sivusto palauttaa kun kommenttilomake lähetetään
 * esikatselunapilla.
 *
 * **Mittaus eikä toteutus, Tommin luvalla 14.10.2026** (yksi lähetys). Vastaussivun HTML:ää
 * ei ole tallessa: 5.9.2026 mittaus luettiin ja skripti poistettiin (`raakasivut/LUEMINUT.md`
 * › *Mittaus 5.9.2026*). Esikatselu ei julkaise mitään, ja se todennetaan tässä samoin kuin
 * silloin: ketju haetaan uudelleen, eikä viestimäärä saa muuttua eikä testitekstiä löytyä.
 *
 * Ajo: `DG_PREVIEW_CAPTURE=1 gradlew :core-net:liveTest --tests '*ForumPreviewCaptureLiveTest'`.
 * Ketjuksi valitaan luettu (ei New-merkkiä), koska sen avaaminen ei kuluta mitään. Lähetys
 * kulkee sovelluksen omaa reittiä ([fi.tommi.dg.domain.ForumComposePage.preview]), ja nappi
 * tarkistetaan ennen lähetystä.
 */
@Tag("live")
class ForumPreviewCaptureLiveTest {

    @Test
    fun `kommentin esikatselu haetaan eika ketjuun tule viestia`() {
        // Lomakkeen lähetys Tommin tilillä, joten pelkkä `liveTest` ei aja tätä: lupa on
        // kerrallinen, ja ajo vaatii `DG_PREVIEW_CAPTURE=1`:n samalla komennolla.
        assumeTrue(System.getenv("DG_PREVIEW_CAPTURE") == "1", "DG_PREVIEW_CAPTURE=1 puuttuu, ei lähetetä")
        val credentials = LocalCredentials.loadOrNull()
        assumeTrue(credentials != null, "local.properties puuttuu, ks. local.properties.example")
        val client = DgClient(
            credentials = { credentials },
            cookieStore = FileCookieStore(File("build/live/cookies.txt")),
        )

        val index = ForumParser.parseIndex(client.okHtml("/bg/forum2"))
            ?: error("Palstan indeksi ei jäsentynyt")
        val thread = index.threads.firstOrNull { !it.isNew && it.lastReadOrdinal != null }
            ?: error("Luettua ketjua ei löytynyt, joten mitään ei haeta")
        println("Ketju: ${thread.title} (${thread.readPath}, ${thread.postCount} viestiä)")

        val before = ForumParser.parseThread(client.okHtml(thread.readPath))
            ?: error("Ketju ei jäsentynyt")
        val addPath = before.addCommentPath ?: error("Ketjulla ei ole Add a Comment -linkkiä")
        val compose = ForumParser.parseCompose(client.okHtml(addPath))
            ?: error("Kommenttilomake ei jäsentynyt")

        val submission = compose.preview(title = null, comment = TEKSTI)
            ?: error("Lomakkeella ei ole esikatselunappia")
        check(submission.fields[compose.submitField] == "Preview") {
            "Nappi ei ole Preview, tätä ei lähetetä: ${submission.fields}"
        }
        val response = client.send(submission)
        assertTrue(response is DgResponse.Ok) { "Esikatselu epäonnistui: ${response::class.simpleName}" }
        val html = (response as DgResponse.Ok).html
        save("forum2_preview.html", html)

        val after = ForumParser.parseThread(client.okHtml(thread.readPath))
            ?: error("Ketju ei jäsentynyt uudelleen")
        assertEquals(before.posts.size, after.posts.size, "Ketjuun tuli viesti")
        assertFalse(after.posts.any { MERKKI in it.body }, "Testiteksti löytyi ketjusta")
        println("Ketjussa yhä ${after.posts.size} viestiä, testitekstiä ei löydy")
    }

    private fun DgClient.okHtml(path: String): String {
        val response = fetch(path)
        assertTrue(response is DgResponse.Ok) { "Haku epäonnistui ($path): ${response::class.simpleName}" }
        return (response as DgResponse.Ok).html
    }

    private fun save(name: String, html: String) {
        val target = File("../raakasivut/$name")
        target.parentFile.mkdirs()
        target.writeText(html)
        println("Tallennettu: ${target.canonicalPath} (${html.length} merkkiä)")
    }

    private companion object {
        const val MERKKI = "Preview measurement 14.10.2026"

        /**
         * Kattaa avoimet kysymykset: yksittäinen ja tyhjä rivinväli, `http://`- ja
         * `https://`-osoite rinnakkain (`docs/PALSTAPOSTAUS.md` › linkin muodostuminen),
         * HTML:n erikoismerkit ja ääkköset.
         */
        val TEKSTI = """
            $MERKKI, line one
            line two after a single line break

            Second paragraph: http://example.com and https://example.com

            a < b & c "quoted" <b>bold?</b> ääkköset
        """.trimIndent()
    }
}
