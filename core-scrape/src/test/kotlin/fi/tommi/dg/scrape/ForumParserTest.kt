package fi.tommi.dg.scrape

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ForumParserTest {

    private fun fixture(name: String, charset: java.nio.charset.Charset = Charsets.UTF_8): String =
        checkNotNull(javaClass.getResourceAsStream("/fixtures/$name")) {
            "Fixture puuttuu: $name"
        }.reader(charset).readText()

    private val index = ForumParser.parseIndex(fixture("forum_index.html"))!!
    private val politics = ForumParser.parseIndex(fixture("forum_index_politics.html"))!!
    private val thread = ForumParser.parseThread(fixture("forum_read.html"))!!
    private val month = ForumParser.parseArchive(fixture("forum_month.html"))!!
    private val composeNew = ForumParser.parseCompose(fixture("forum_compose_new.html"))!!
    private val composeAdd = ForumParser.parseCompose(fixture("forum_compose_add.html"))!!

    @Test
    fun `palstan nimi luetaan otsikosta`() {
        assertEquals("General", index.boardName)
    }

    @Test
    fun `kaikki ketjurivit loytyvat ja otsikkorivi ei tule mukaan`() {
        assertEquals(4, index.threads.size)
    }

    @Test
    fun `ketjurivi jasentyy kokonaan`() {
        val thread = index.threads.first()
        assertEquals("Opening Drills", thread.title)
        assertEquals("/bg/forum2/main/read/60001#3", thread.readPath)
        assertEquals(13, thread.postCount)
        assertEquals("ekakirjoittaja", thread.poster)
        assertTrue(thread.isNew)
    }

    @Test
    fun `palstarivi luetaan sivun jarjestyksessa ja nykyinen jaa ilman polkua`() {
        assertEquals(listOf("General", "Politics"), index.boards.map { it.name })
        assertNull(index.boards.first().path)
        assertEquals("/bg/forum2/politics", index.boards[1].path)
    }

    @Test
    fun `politics-sivulla nykyinen ja linkitetty ovat toisin pain`() {
        // Sama rivi toisesta suunnasta: General on täällä linkki eikä `/bg/forum2` vaan
        // `/bg/forum2/main`, joten polkua ei voi koota vaan se luetaan sivulta.
        assertEquals(listOf("General", "Politics"), politics.boards.map { it.name })
        assertEquals("/bg/forum2/main", politics.boards.first().path)
        assertNull(politics.boards[1].path)
    }

    @Test
    fun `sivun omat toimintolinkit eivat paady palstariville`() {
        // `/bg/forum2/main/new`, `listhidden` ja `month` ovat samaa alkua mutta syvempiä,
        // eikä yläpalkin `/bg/forum2` ole palsta lainkaan.
        assertEquals(2, index.boards.size)
    }

    @Test
    fun `politics-palstan ketjut jasentyvat vaikka tunnus ei ole main`() {
        assertEquals(3, politics.threads.size)
        assertEquals("Politics", politics.boardName)
        val first = politics.threads.first()
        assertEquals("Kokouspaikan valinta", first.title)
        assertEquals("/bg/forum2/politics/read/61001", first.readPath)
        assertEquals(35, first.postCount)
        assertTrue(first.isNew)
        assertFalse(politics.threads.last().isNew)
    }

    @Test
    fun `fragmentiton read-polku sailyy sellaisenaan`() {
        assertEquals("/bg/forum2/main/read/60002", index.threads[1].readPath)
    }

    @Test
    fun `otsikon sana New ei tee ketjusta lukematonta`() {
        // New-merkki on oma <FONT><B>New</B></FONT> -elementtinsä; otsikko "New Champion
        // Announced!" sisältää saman sanan eikä saa laueta.
        val champion = index.threads.first { it.title == "New Champion Announced!" }
        assertFalse(champion.isNew)
    }

    @Test
    fun `ketjun otsikko ja viestimaara luetaan`() {
        assertEquals("What Happened at the Sample Event?", thread.title)
        assertEquals(3, thread.posts.size)
    }

    @Test
    fun `monikappaleinen runko sailyttaa kappaleet ja linkin tekstin`() {
        val body = thread.posts.first().body
        val paragraphs = body.split("\n\n")
        assertEquals(4, paragraphs.size)
        assertTrue(paragraphs[0].startsWith("To set the scene:"))
        // Ulkoinen linkki litistyy näkyväksi tekstikseen; ruutu on lukutilassa.
        assertEquals("http://www.example.com/video?si=abc123", paragraphs[3])
    }

    @Test
    fun `allekirjoitus jasentyy eika vuoda runkoon`() {
        val first = thread.posts.first()
        assertEquals(1, first.ordinal)
        assertEquals("ekakirjoittaja", first.author.name)
        assertEquals("10001", first.author.userId)
        assertEquals("Sat Aug 1 23:40:45 2026", first.postedAtText)
        assertFalse(first.body.contains("Posted by"), "Allekirjoitus vuoti runkoon: ${first.body}")
    }

    @Test
    fun `lainausrivi purkautuu entiteetista`() {
        assertTrue(thread.posts[1].body.startsWith("> This is the second paragraph")) {
            "Lainaus: ${thread.posts[1].body}"
        }
    }

    @Test
    fun `new from here -otsake ei ole viesti eika kuulu runkoon`() {
        val last = thread.posts.last()
        assertEquals(3, last.ordinal)
        assertTrue(last.body.startsWith("A short closing post"))
        assertFalse(thread.posts.any { it.body.contains("New From Here") })
    }

    @Test
    fun `viimeisen viestin runko ei sisalla footeria`() {
        val last = thread.posts.last()
        assertFalse(last.body.contains("Add a Comment"), "Footer vuoti runkoon: ${last.body}")
        assertFalse(last.body.contains("Back to index"))
    }

    @Test
    fun `windows-1252-fixture jasentyy merkkeineen`() {
        // forum_thread.html on repon ainoa ei-UTF-8-tiedosto, ja juuri se on sen
        // tarkoitus: mitatut windows-1252-tavut. Luku tapahtuu samalla merkistöllä jolla
        // DgClient purkaa palstasivut.
        val page = ForumParser.parseThread(
            fixture("forum_thread.html", charset("windows-1252"))
        )!!
        assertEquals(3, page.posts.size)
        assertEquals("testipelaaja", page.posts.first().author.name)
        // U+2019 on windows-1252:n 0x92, se merkki joka erottaa merkistön Latin-1:stä.
        assertTrue(page.posts.first().body.contains('’')) {
            "Merkistö hukkui: ${page.posts.first().body}"
        }
    }

    @Test
    fun `indeksi tarjoaa reitin vanhempiin ketjuihin`() {
        // Indeksi on sivustolla kuluvan hetken lista, ja tämä on sivun ainoa reitti
        // vanhempiin (Tommin havainto laitteelta 29.8.2026).
        assertEquals("/bg/forum2/main/month", index.archivePath)
        assertEquals("/bg/forum2/politics/month", politics.archivePath)
    }

    @Test
    fun `indeksin ketjurivilla ei ole aikaa`() {
        // Indeksin neljäs solu on Hide-linkki. Ilman linkittömyysehtoa sen teksti
        // luettaisiin ajaksi, ja rivillä lukisi "Hide".
        assertTrue(index.threads.all { it.postedAtText == null })
    }

    @Test
    fun `kuukausisivun ketjut luetaan aikoineen`() {
        assertEquals("General", month.boardName)
        assertEquals("Threads created in August 2026", month.monthLabel)
        assertEquals(3, month.threads.size)

        val first = month.threads.first()
        assertEquals("Player missing in action", first.title)
        assertEquals("/bg/forum2/main/read/64175", first.readPath)
        assertEquals(3, first.postCount)
        assertEquals("alfa", first.poster)
        assertEquals("Fri Aug 28 18:54:43 2026", first.postedAtText)
        // Kuukausisivulla ei ole New-merkkejä lainkaan.
        assertFalse(month.threads.any { it.isNew })
    }

    @Test
    fun `kuukausiluettelo ja paluulinkki luetaan sivun omassa jarjestyksessa`() {
        assertEquals(
            listOf("August 2026", "July 2026", "June 2026", "January 2004"),
            month.months.map { it.label },
        )
        // Sivun `&nbsp;` purkautuu välilyöntimerkiksi, joka ei ole tavallinen välilyönti.
        assertTrue(month.months.none { it.label.contains('\u00a0') })
        assertEquals("/bg/forum2/main/month/2026/8", month.months.first().path)
        // Paluulinkki tunnistetaan tekstistä: toisen palstan linkki on samaa polkumuotoa.
        assertEquals("/bg/forum2/main", month.indexPath)
    }

    @Test
    fun `kuukausisivu ja indeksi erottuvat toisistaan`() {
        // Kuukausisivu täyttää myös indeksin tuntomerkin (siinä on ketjulinkkejä), joten
        // erottelu on kuukausiluettelo ja tarkistusten järjestys on merkitsevä.
        assertTrue(DgPages.isForumArchive(fixture("forum_month.html")))
        assertFalse(DgPages.isForumArchive(fixture("forum_index.html")))
        assertTrue(DgPages.isForumIndex(fixture("forum_month.html")))
    }

    @Test
    fun `indeksi ja ketju tarjoavat sivun omat kirjoituslinkit`() {
        // Polkuja ei koota: uuden ketjun linkki on indeksin alalaidassa ja kommentin
        // linkki ketjusivun lopussa, kumpikin palstan tunnuksella.
        assertEquals("/bg/forum2/main/new", index.newThreadPath)
        assertEquals("/bg/forum2/politics/new", politics.newThreadPath)
        assertEquals("/bg/forum2/main/add/60001", thread.addCommentPath)
    }

    @Test
    fun `uuden ketjun lomake luetaan kenttineen ja rajoineen`() {
        assertEquals("/bg/forum2/politics/submitnew", composeNew.action)
        assertEquals(fi.tommi.dg.domain.FormMethod.POST, composeNew.method)
        assertEquals("Politics", composeNew.boardName)
        assertEquals("New Thread", composeNew.heading)
        assertTrue(composeNew.isNewThread)
        assertEquals("title", composeNew.titleField)
        assertEquals(80, composeNew.titleMaxLength)
        assertEquals("comment", composeNew.commentField)
        // Kaksi samannimistä nappia: esikatselu jää pois, lähetys menee arvoineen.
        assertEquals("submit", composeNew.submitField)
        assertEquals("Create New Thread", composeNew.submitLabel)
    }

    @Test
    fun `kommentin lomake on ilman otsikkokenttaa ja otsake on ketjun`() {
        assertEquals("/bg/forum2/main/submitadd/60001", composeAdd.action)
        assertEquals("What Happened at the Sample Event?", composeAdd.heading)
        assertFalse(composeAdd.isNewThread)
        assertNull(composeAdd.titleField)
        assertEquals("Submit Comment", composeAdd.submitLabel)
    }

    @Test
    fun `lomake kirjoitetaan sivun kentilla ja napin arvolla`() {
        val submission = composeNew.write(title = " Otsikko ", comment = "Runko\nkahdella rivillä")!!
        assertEquals("/bg/forum2/politics/submitnew", submission.action)
        assertEquals(
            listOf("title" to "Otsikko", "comment" to "Runko\nkahdella rivillä", "submit" to "Create New Thread"),
            submission.fields.toList(),
        )
        val comment = composeAdd.write(title = null, comment = "Vastaus")!!
        assertEquals(listOf("comment" to "Vastaus", "submit" to "Submit Comment"), comment.fields.toList())
    }

    @Test
    fun `tyhja teksti tai liian pitka otsikko ei tuota lomaketta`() {
        assertNull(composeNew.write(title = "Otsikko", comment = "  "))
        assertNull(composeNew.write(title = "", comment = "Runko"))
        assertNull(composeNew.write(title = "x".repeat(81), comment = "Runko"))
        assertNull(composeAdd.write(title = null, comment = ""))
    }

    @Test
    fun `muu sivu ei ole kirjoituslomake`() {
        assertNull(ForumParser.parseCompose(fixture("forum_index.html")))
        assertNull(ForumParser.parseCompose(fixture("forum_read.html")))
    }
}
