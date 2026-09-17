package fi.tommi.dg.app.ui

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Laudan leveyden johtaminen.
 *
 * Testi on olemassa siksi että sama luku lasketaan 9.8.2026 alkaen **kahdessa paikassa**:
 * `LoadedBoard` laskee kehyksen leveyden ennen riviä, ja `Board` laskee piirtoalueen sen
 * sisällä. Kummankin virhe on hiljainen samalla tavalla kuin puolikas lauta oli: liian leveä
 * lauta työntäisi sivupaneelin ulos rivistä ilman virheilmoitusta.
 *
 * Mitat ovat Pixel 8a:n vaakaikkuna (914 x 411 dp), eli sama laite jolta korjattavat puutteet
 * mitattiin.
 */
class DgBoardWidthTest {

    /** Numerorivin korkeus tyyliskaalasta luettuna, `labelSmall` + pehmusteet. */
    private val numberRow = 20.dp

    /**
     * Laudan riville jäävä korkeus Pixel 8a:lla: 411 dp:n vaakaikkuna miinus `Column`in oma
     * 8 dp:n pehmuste. **Tässä luki ensin 411**, eli ikkunan korkeus sellaisenaan, ja se antoi
     * nappulaksi 30,0 dp kun oikea luku on 29,2. Laskenta oli siis oikein ja syöte väärin.
     */
    private val PIXEL_8A_HEIGHT = 403.dp

    /**
     * Laudalle jäävä leveys samalla laitteella: 914 dp miinus laatikon omat reunat (32).
     *
     * **Tässä luki 666 dp 9.8.2026 asti**, koska sivupaneeli (200) ja yksi väli (16)
     * vähennettiin ensin. Paneeli siirtyi päällepiirroksi samana päivänä, joten koko
     * leveys on laudan. Pixel 8a:lla luku ei muuta nappulaa, koska korkeus rajoittaa jo
     * ennen sitä; pienellä puhelimella se on koko ero, ks. oma testinsä alla.
     */
    private val PIXEL_8A_BOARD_SLOT = 882.dp

    /**
     * Pieni puhelin vaaka-asennossa, 640 x 360 dp, miinus samat reunat. Laitetta ei ole
     * ajettu, joten tämä on laskettu eikä mitattu, ja se on tässä juuri siksi: se on ainoa
     * tapa sanoa mitään pienestä näytöstä ennen kuin sellainen on kädessä.
     */
    private val SMALL_PHONE_BOARD_SLOT = 608.dp
    private val SMALL_PHONE_HEIGHT = 352.dp

    /**
     * Galaxy Tab S7+ vaaka-asennossa, mitattu kuvakaappauksesta 14.8.2026: ikkuna on
     * 1317,6 x 824,5 dp tiheydellä 340 dpi. Laite on tässä siksi, että se on ainoa jolla
     * nappulan yläraja ylipäätään laukeaa.
     *
     * **Laudalle tarjottu leveys on 1285,6 dp eikä ikkunan 1317,6 dp**, koska ruudun oma
     * pehmuste vie 32 dp ennen tätä laskentaa. Ero on sama 32 dp kuin puhelimen luvussa yllä,
     * ja se kaatoi ensimmäisen ylärajan 14.8.2026: ikkunasta laskettuna ylijäämä näytti
     * riittävän paneeliin, ja laitteella se jäi kuusi dp kynnyksen alle. Testi antoi vihreän,
     * koska sama väärä luku oli sen omana syötteenä.
     */
    private val TAB_S7_BOARD_SLOT = 1285.6f.dp
    private val TAB_S7_HEIGHT = 824.5f.dp

    /** Sivupaneelin kynnys ja väli, samat luvut kuin `BoardScreen`issa. */
    private val PANEL_MIN = 150.dp
    private val PANEL_GAP = 8.dp

    /**
     * **Yläraja on olemassa jotta sivupaneeli mahtuu tabletille, ja tämä on se väite.**
     *
     * Ennen ylärajaa lauta otti 1285,6 dp koko 1317,6 dp:n ikkunasta eli jätti 32 dp, jolloin
     * paneelia ei piirretty ja ruudulta puuttuivat pisteet, pipit ja pelaajien nimet. Vika
     * löytyi laitteelta eikä testeistä, ja tämä testi on kirjoitettu sen jälkeen: se ei siis
     * ole näyttö siitä että ehto olisi ollut kunnossa, vaan siitä ettei se palaa.
     *
     * Väite on marginaalina eikä tasan kynnyksenä. Sama tila olisi saatu venyttämällä
     * keskitilaa 200 dp:hen, mutta silloin ylijäämä olisi ollut tasan 150 dp, ja asettelu
     * vaihtaisi muotoaan pienimmästäkin muutoksesta ikkunan mitoissa.
     */
    @Test
    fun `tabletilla jaa tilaa sivupaneelille`() {
        val frame = DgBoard.frameWidth(TAB_S7_BOARD_SLOT, TAB_S7_HEIGHT, numberRow)
        val spare = TAB_S7_BOARD_SLOT - frame - PANEL_GAP

        assertTrue("laudan viereen jäi vain $spare", spare >= PANEL_MIN)
        // Marginaali eikä kynnys: 14.8.2026 ensimmäinen yläraja jäi kuusi dp vajaaksi, ja
        // vika näkyi vasta laitteella. Väljyys on se mikä erottaa toimivan ehdon onnekkaasta.
        assertTrue("ylijäämä $spare istuu kynnyksellä eikä sen päällä", spare.value >= 170f)
    }

    /**
     * Yläraja koskee vain leveää laitetta. Puhelimella nappula on 30 dp:n luokkaa, eli
     * kaukana katosta, ja juuri se on ehdon tarkoitus: tabletin korjaus ei saa kutistaa
     * puhelinta.
     */
    @Test
    fun `ylaraja ei laukea puhelimella`() {
        val frame = DgBoard.frameWidth(PIXEL_8A_BOARD_SLOT, PIXEL_8A_HEIGHT, numberRow)
        val column = (frame - DgBoard.FRAME_PAD_H * 2) / (DgBoard.PLAY_COLUMNS + DgBoard.TRAY_PER_COLUMN)
        val checker = column * DgBoard.CHECKER_PER_COLUMN

        assertTrue("nappula $checker osui kattoon ${DgBoard.CHECKER_MAX}", checker < DgBoard.CHECKER_MAX)
    }

    @Test
    fun `kehys on piirtoalue plus pehmuste molemmin puolin`() {
        val frame = DgBoard.frameWidth(
            availableWidth = 500.dp,
            availableHeight = 411.dp,
            numberRowHeight = numberRow,
        )
        val content = DgBoard.contentWidth(
            width = 500.dp - DgBoard.FRAME_PAD_H * 2,
            height = 411.dp - DgBoard.FRAME_PAD_V * 2,
            numberRowHeight = numberRow,
        )
        assertEquals((content + DgBoard.FRAME_PAD_H * 2).value, frame.value, 0.01f)
    }

    /**
     * Kehyksen antama leveys ja sen sisällä uudelleen laskettu piirtoalue ovat sama luku.
     * Tämä on se kohta jossa kaksi laskijaa voisi ajautua erilleen.
     */
    @Test
    fun `sisapuolen oma laskenta paatyy samaan leveyteen`() {
        val available = 560.dp
        val height = 411.dp
        val frame = DgBoard.frameWidth(available, height, numberRow)

        // Kehyksen sisällä `Board` mittaa oman pehmusteensa jälkeen jäävän tilan.
        val inner = DgBoard.contentWidth(
            width = frame - DgBoard.FRAME_PAD_H * 2,
            height = height - DgBoard.FRAME_PAD_V * 2,
            numberRowHeight = numberRow,
        )
        assertEquals((frame - DgBoard.FRAME_PAD_H * 2).value, inner.value, 0.01f)
    }

    /**
     * Puhelimen vaakaikkunassa korkeus on rajoittava mitta, joten lauta **ei** ota kaikkea
     * tarjottua leveyttä vaan jättää marginaalin. Ehto oli aiemmin se että sivupaneeli
     * mahtuu; paneelin siirryttyä päällepiirroksi se on nyt se että lauta pysyy laudan
     * muotoisena eikä veny ikkunan levyiseksi.
     */
    @Test
    fun `korkeus rajoittaa ja leveytta jaa yli`() {
        val available = PIXEL_8A_BOARD_SLOT
        val frame = DgBoard.frameWidth(available, PIXEL_8A_HEIGHT, numberRow)
        assertTrue("kehys $frame ei saa täyttää tarjottua $available", frame < available)
    }

    /**
     * **Mitä muutokset ostivat, Pixel 8a:n luvuilla.** Nappula oli 8.8.2026 mitattuna 26,8 dp,
     * kun 32 dp:n otsikkorivi vei korkeutta ja kehys sai koko leveyden. Rivin poisto antoi
     * kiilalle sen 32 dp ja nosti nappulan 29,2 dp:hen.
     *
     * **Kolmas askel tuli 10.8.2026 ja se on 31,2 dp.** Keskitila oli siihen asti kiinteä
     * 59 dp, ja se korvattiin kuution mitalla eli `nappula * 1,26`. Tällä laitteella se on
     * 39,3 dp, joten kiiloille vapautui noin 20 dp. Luku on nyt selvästi yli DG Mobilen
     * mitatun 29,7 dp:n, mikä on odotettu: sillä on otsikkorivi jota meillä ei ole.
     *
     * Vapautuminen ei ollut tämän muutoksen tarkoitus vaan sivutuote. Kiinteä keskitila
     * purettiin siksi että se litisti kuution suorakaiteeksi tabletilla, ja korkeus tuli
     * kaupan päälle.
     *
     * **Neljäs askel on 14.8.2026 ja se menee alaspäin: 29,3 dp** (Tommin valinta). Keskitila
     * irtosi kuution mitasta ja on nyt kaksi nappulaa, koska kuution mitta oli alaraja eikä
     * korkeus. Puhelin maksaa siitä 1,9 dp, ja se on tiedossa oleva hinta eikä yllätys: suhde
     * on yhteinen, ja täällä korkeus rajoittaa.
     *
     * **Nappulan yläraja ei näy tässä luvussa lainkaan**, ja se on juuri se mitä siltä
     * odotetaan: 56 dp on tällä laitteella kaukana, joten tabletin korjaus ei kutista
     * puhelinta.
     *
     * Testin toinen tarkoitus on ennallaan: luku kertoo heti jos ruudulle lisätään jotain
     * joka ottaa leveyttä. Väliaikainen nappisarake olisi jättänyt aikanaan 27,4 dp. Alaraja
     * 26,8 dp on siksi tässä yhä: se on mitattu lähtötaso, eikä keskitilan kasvattaminen saa
     * viedä nappulaa sen alle.
     */
    @Test
    fun `nappula kasvoi otsikkorivin poistuttua`() {
        val frame = DgBoard.frameWidth(PIXEL_8A_BOARD_SLOT, PIXEL_8A_HEIGHT, numberRow)
        val content = frame - DgBoard.FRAME_PAD_H * 2
        val column = content / (DgBoard.PLAY_COLUMNS + DgBoard.TRAY_PER_COLUMN)
        val checker = column * DgBoard.CHECKER_PER_COLUMN

        assertEquals(29.3f, checker.value, 0.2f)
        assertTrue("nappula $checker ei kasvanut mitatusta 26,8 dp:stä", checker.value > 26.8f)
    }

    /**
     * Keskitila on palan mittainen, eli kuutio mahtuu siihen neliönä.
     *
     * **Tämä on se väite jonka kiinteä 59 dp rikkoi**, ja se rikkoutui hiljaa: `Modifier.size`
     * rajautuu tulevaan rajoitteeseen, joka oli kiinteä vain korkeussuunnassa, joten kuutio jäi
     * leveäksi ja madaltui. Galaxy Tab S7+:lla se oli mitattuna 79,5 x 57,9 dp.
     *
     * Väite on tässä eikä ruudun testissä, koska se on laskettavissa: jos keskitila on
     * vähintään kuution korkuinen, mikään rajoite ei voi litistää sitä.
     */
    @Test
    fun `kuutio mahtuu keskitilaan neliona`() {
        val korkeudet = listOf(352.dp, 403.dp, 700.dp, 824.dp)
        korkeudet.forEach { korkeus ->
            val checker = DgBoard.checkerFromHeight(korkeus - numberRow * 2)
            val band = DgBoard.bandHeight(checker)
            val cube = checker * DgBoard.CUBE_PER_CHECKER
            assertTrue(
                "korkeudella $korkeus kuutio $cube ei mahdu keskitilaan $band",
                band.value >= cube.value - 0.01f,
            )
        }
    }

    /**
     * Kiilat ja keskitila täyttävät tarjotun korkeuden tasan, kun korkeus rajoittaa.
     *
     * Tämä on [DgBoard.checkerFromHeight]in koko perustelu tarkistettavassa muodossa: kehä
     * (nappula → kiila → keskitila → nappula) ratkeaa kerralla, ei iteroimalla. Jos kaava
     * joskus muuttuu, tämä kaatuu ennen kuin lauta leikkautuu laitteella.
     */
    @Test
    fun `kiilat ja keskitila tayttavat korkeuden tasan`() {
        val inner = 600.dp
        val checker = DgBoard.checkerFromHeight(inner)
        val band = DgBoard.bandHeight(checker)
        val wedge = checker * DgBoard.CHECKERS_APART

        assertEquals(inner.value, (wedge * 2 + band).value, 0.01f)
    }

    /**
     * **Sivupaneelin purku maksaa siellä missä leveys rajoittaa, ei siellä missä korkeus.**
     * Pixel 8a:lla nappula on sama 29,2 dp kuin paneelin kanssa, koska kiila loppui jo
     * korkeuteen. Pienellä puhelimella sama muutos on koko ero, ja tämä testi on se paikka
     * jossa väite on tarkistettavissa ilman laitetta.
     *
     * Luvut ovat laskettuja eivätkä mitattuja, ja toleranssi on siksi väljä: testi vahtii
     * suuruusluokkaa ja suuntaa, ei desimaalia. Se mikä tässä pitää pysyä totena on että
     * pieni näyttö hyötyy enemmän kuin iso.
     */
    @Test
    fun `pieni puhelin voitti leveyden vapautumisesta enemman kuin iso`() {
        fun nappula(slot: androidx.compose.ui.unit.Dp, height: androidx.compose.ui.unit.Dp) =
            ((DgBoard.frameWidth(slot, height, numberRow) - DgBoard.FRAME_PAD_H * 2) /
                (DgBoard.PLAY_COLUMNS + DgBoard.TRAY_PER_COLUMN) * DgBoard.CHECKER_PER_COLUMN)
                .value

        val isoNyt = nappula(PIXEL_8A_BOARD_SLOT, PIXEL_8A_HEIGHT)
        val isoEnnen = nappula(PIXEL_8A_BOARD_SLOT - 216.dp, PIXEL_8A_HEIGHT)
        val pieniNyt = nappula(SMALL_PHONE_BOARD_SLOT, SMALL_PHONE_HEIGHT)
        val pieniEnnen = nappula(SMALL_PHONE_BOARD_SLOT - 216.dp, SMALL_PHONE_HEIGHT)

        assertEquals("iso näyttö ei muuttunut, koska korkeus rajoitti jo", isoEnnen, isoNyt, 0.1f)
        assertTrue(
            "pieni näyttö ei kasvanut: $pieniEnnen -> $pieniNyt",
            pieniNyt > pieniEnnen + 4f,
        )
    }

    /**
     * Isolla näytöllä leveys ei enää rajoita, ja lauta kasvaa korkeuden mukana. Tämä on se
     * ehto jonka varassa sama asettelu palvelee sekä puhelinta että tablettia: mittoja ei ole
     * sidottu kumpaankaan laitteeseen.
     */
    @Test
    fun `korkeampi ikkuna kasvattaa lautaa`() {
        val kapea = DgBoard.frameWidth(900.dp, 411.dp, numberRow)
        val korkea = DgBoard.frameWidth(900.dp, 700.dp, numberRow)
        assertTrue("$korkea ei ole suurempi kuin $kapea", korkea > kapea)
    }

    /** Mahdoton tila ei saa tuottaa negatiivista leveyttä vaan pienimmän mahdollisen laudan. */
    @Test
    fun `olematon tila ei tuota negatiivista leveytta`() {
        val frame = DgBoard.frameWidth(0.dp, 0.dp, numberRow)
        assertTrue("kehys $frame on negatiivinen", frame.value > 0f)
    }
}
