package fi.tommi.dg.domain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * `.mat` → `.sgf` GNU Backgammonin omaa tulostetta vasten.
 *
 * Odotusarvot ovat `gnubg-cli`:n kirjoittamat solmut samasta ottelusta (15.9.2026,
 * `raakasivut/kommentti_test.sgf`, pelit 1 ja 2 kokonaan). Fixture on saman ottelun neljä
 * ensimmäistä peliä vastustajan nimi anonymisoituna (`raakasivut/LUEMINUT.md`).
 */
class SgfExportTest {

    private val mat: String by lazy {
        javaClass.getResourceAsStream("/match_four_games.mat")!!.readBytes().decodeToString()
    }

    private fun nodes(tree: String): List<String> =
        Regex(""";([WB]\[[^\]]*\])""").findAll(tree).map { it.groupValues[1] }.toList()

    private fun trees(sgf: String): List<String> = sgf.split("\n(").mapIndexed { i, t -> if (i == 0) t else "($t" }

    @Test
    fun `jasennin lukee pituuden, pelit, pisteet ja teot`() {
        val match = MatMatch.parse(mat)!!

        assertEquals(21, match.length)
        assertEquals(4, match.games.size)
        assertEquals("opponent", match.firstPlayer)
        assertEquals("tommih", match.secondPlayer)
        val g2 = match.games[1]
        assertEquals(0, g2.firstScore)
        assertEquals(1, g2.secondScore)
        // Tanssi on heitto ilman siirtoja, eikä se siirrä vuoroa väärälle pelaajalle.
        val dance = g2.actions[2] as MatAction.Roll
        assertEquals(MatSide.FIRST, dance.side)
        assertEquals(6 to 6, dance.die1 to dance.die2)
        assertTrue(dance.moves.isEmpty())
        // Lyönti kulkee mukana tiedoksi.
        val hit = (g2.actions[1] as MatAction.Roll).moves[1]
        assertEquals(CheckerMove(13, 9, hit = true), hit)
    }

    @Test
    fun `avausheiton havinnyt ei heita, ja rivi jolla vain oikea sarake kuuluu toiselle`() {
        val g3 = MatMatch.parse(mat)!!.games[2]

        val first = g3.actions.first() as MatAction.Roll
        assertEquals(MatSide.SECOND, first.side)
        assertEquals(4 to 2, first.die1 to first.die2)
    }

    @Test
    fun `voitto omalla rivillaan kuuluu sarakkeen pelaajalle`() {
        // Peli 4 päättyy `Doubles => 2 / Drops` ja `Wins 1 point` on seuraavalla rivillä
        // ilman numeroa, ensimmäisen sarakkeen kohdalla.
        val g4 = MatMatch.parse(mat)!!.games[3]

        assertEquals(MatAction.Win(MatSide.FIRST, 1), g4.actions.last())
        assertEquals(MatAction.Drop(MatSide.SECOND), g4.actions[g4.actions.lastIndex - 1])
    }

    @Test
    fun `tavallinen ottelu ei ala asetetusta asemasta`() {
        val match = MatMatch.parse(mat)!!
        assertTrue(match.games.none { it.setup })
        assertEquals(false, match.startsFromSetup)
    }

    @Test
    fun `nackgammonin asemarivi luetaan setupiksi eikä se sotke sarakkeita`() {
        // Mitattu 16.9.2026 (`raakasivut/LUEMINUT.md`): jokainen peli alkaa valeheitolla
        // `12: Illegal play (...)`, jonka sulkeissa on sivuston oma asemamerkkijono. Rivi
        // on pidempi kuin sarakeraja, ja toisen sarakkeen heitto on sen perässä.
        val nack = javaClass.getResourceAsStream("/match_nackgammon.mat")!!.readBytes().decodeToString()
        val match = MatMatch.parse(nack)!!

        assertEquals(7, match.length)
        assertEquals(4, match.games.size)
        assertTrue(match.games.all { it.setup })
        assertTrue(match.startsFromSetup)
        // Peli 1: valeheitto ei ole teko, ja toisen pelaajan 51 on ensimmäinen teko.
        val first = match.games[0].actions.first() as MatAction.Roll
        assertEquals(MatSide.SECOND, first.side)
        assertEquals(5 to 1, first.die1 to first.die2)
        assertEquals(listOf(CheckerMove(24, 23), CheckerMove(23, 18)), first.moves)
        // Peli 2: asemarivi on oikeassa sarakkeessa yksin, ja ensimmäinen teko on rivin 2 62.
        val g2 = match.games[1].actions.first() as MatAction.Roll
        assertEquals(MatSide.FIRST, g2.side)
        assertEquals(6 to 2, g2.die1 to g2.die2)
        assertEquals(MatAction.Win(MatSide.SECOND, 4), match.games[0].actions.last())
    }

    @Test
    fun `double repeatin toistettu peli alkaa keskelta ja luetaan setupiksi`() {
        // Mitattu 16.9.2026 DRive To Five -ottelusta: hyväksytty tuplaus toistetaan omana
        // pelinään samasta asemasta, eikä tiedosto merkitse sitä mitenkään. Pelit 3, 4, 6
        // ja 8 alkavat keskeltä; muissa ensimmäiset heitot lähtevät aloitusasemasta.
        val dr = javaClass.getResourceAsStream("/match_double_repeat.mat")!!.readBytes().decodeToString()
        val match = MatMatch.parse(dr)!!

        assertEquals(9, match.games.size)
        assertEquals(listOf(3, 4, 6, 8), match.games.filter { it.setup }.map { it.number })
        assertTrue(match.startsFromSetup)
        // Peli 8 alkaa toisen pelaajan `61: 13/7 7/6`, joka on laillinen avaus; keskeltä
        // alkaminen näkyy vasta ensimmäisen pelaajan heitosta `51: 7/2 5/4`.
        val g8 = match.games[7]
        val second = g8.actions.first() as MatAction.Roll
        assertEquals(MatSide.SECOND, second.side)
        assertEquals(listOf(CheckerMove(13, 7), CheckerMove(7, 6)), second.moves)
    }

    @Test
    fun `solmut ovat samat kuin GNU Backgammonin omassa tulosteessa`() {
        val sgf = SgfExport.write(MatMatch.parse(mat)!!)
        val puut = trees(sgf)

        assertEquals(4, puut.size)
        assertEquals(ODOTETTU_0, nodes(puut[0]))
        assertEquals(ODOTETTU_1, nodes(puut[1]))
    }

    @Test
    fun `juurisolmu nimeaa pelin, pisteet, pelaajat ja tuloksen kuten GNU`() {
        val sgf = SgfExport.write(MatMatch.parse(mat)!!, date = "2026-09-16")
        val puut = trees(sgf)

        assertTrue(puut[0].startsWith("(;FF[4]GM[6]CA[UTF-8]AP[DG Android:0.1]MI[length:21][game:0][ws:0][bs:0]PW[opponent]PB[tommih]DT[2026-09-16]RU[Crawford]RE[B+1]"))
        assertTrue(puut[1].startsWith("(;FF[4]GM[6]CA[UTF-8]AP[DG Android:0.1]MI[length:21][game:1][ws:0][bs:1]PW[opponent]PB[tommih]RU[Crawford]RE[B+1]"))
        // Peli 4 voitti ensimmäinen pelaaja.
        assertTrue(puut[3].contains("[ws:0][bs:4]") && puut[3].contains("RE[W+1]"))
        assertTrue(puut[0].trimEnd().endsWith(";W[drop])"))
    }

    @Test
    fun `merkki kohdistuu peliin pisteparilla ja kaantyy sarakkeiksi kirjautuneen mukaan`() {
        val mark = MarkedPosition(
            id = 1,
            game = GameKey(MatchId("5290918"), opponentScore = 0, selfScore = 2),
            moveNumber = 377,
            note = "recube after rollback?",
            createdAtEpochMillis = 0,
        )
        val sgf = SgfExport.write(MatMatch.parse(mat)!!, marks = listOf(mark), self = "tommih")
        val puut = trees(sgf)

        // tommih on toinen sarake, joten (vastustaja 0, itse 2) on peli jonka bs on 2.
        assertTrue(puut[2].contains("[ws:0][bs:2]"))
        // Ensimmäisessä siirtosolmussa, koska GNU pudottaa juurisolmun kommentin.
        assertTrue(puut[2].contains(";B[42qusu]C[MARK Move 377: recube after rollback?]\n"), puut[2].take(300))
        assertTrue(puut[0].contains("C[").not() && puut[1].contains("C[").not())
    }

    @Test
    fun `asemallinen merkki kohdistuu paatoksen solmuun`() {
        // Peli 1 rivin 3 vasen sarake on W 51: 23/18 6/5, ja sen jälkeinen asema on se jonka
        // tommih näki ennen omaa 64: 8/2 6/2. Käsin laskettu riveistä 1-3.
        val asema = CheckerPosition(
            self = side(16 to 2, 13 to 4, 8 to 3, 6 to 6),
            opponent = side(20 to 2, 18 to 1, 13 to 4, 8 to 3, 6 to 4, 5 to 1),
        )
        val mark = MarkedPosition(1, GameKey(MatchId("5290918"), 0, 0), 377, "cube?", 0, position = asema)
        val sgf = SgfExport.write(MatMatch.parse(mat)!!, marks = listOf(mark), self = "tommih")
        val puu = trees(sgf)[0]

        assertTrue(puu.contains(";B[64qwsw]C[MARK Move 377: cube?]\n"), puu)
        assertEquals(1, puu.split("C[").size - 1)
        // Ensimmäinen solmu jää ilman kommenttia, toisin kuin asemattomalla merkillä.
        assertTrue(puu.contains(";W[41xwmi]\n"), puu)
    }

    @Test
    fun `osumaton asema menee pelin alkuun, viimeinen osuma viimeiseen solmuun ja samat solmut yhdistyvat`() {
        val match = MatMatch.parse(mat)!!
        val g1 = match.games[0]
        // Toiston viimeinen asema (double, drop ja win eivät siirrä) osuu ensimmäisenä
        // B:n viimeiseen heittoon ja kommentti menee sitä seuraavaan tekoon, joka on double.
        val viimeinen = g1.positions().last()
        val doubleen = MarkedPosition(1, GameKey(MatchId("1"), 0, 0), 400, "", 0, position = CheckerPosition(viimeinen.second, viimeinen.first))
        val eiOsu = MarkedPosition(2, GameKey(MatchId("1"), 0, 0), 401, "mid-move", 0, position = CheckerPosition(side(1 to 15), CheckerPosition.START.opponent))
        val asematon = MarkedPosition(3, GameKey(MatchId("1"), 0, 0), 402, "old", 0)
        val sgf = SgfExport.write(match, marks = listOf(doubleen, eiOsu, asematon), self = "tommih")
        val puu = trees(sgf)[0]

        assertTrue(puu.contains(";B[double]C[MARK Move 400]\n"), puu)
        assertTrue(puu.contains(";W[41xwmi]C[MARK Move 401: mid-move\nMARK Move 402: old]\n"), puu)
    }

    private fun side(vararg pisteet: Pair<Int, Int>): List<Int> =
        MutableList(CheckerPosition.SIZE) { 0 }.also { for ((p, n) in pisteet) it[p] = n }

    @Test
    fun `merkki ilman sanaa ja ilman tunnistettua pelaajaa ei katoa`() {
        val mark = MarkedPosition(1, GameKey(MatchId("1"), 0, 1), 42, "", 0)
        val sgf = SgfExport.write(MatMatch.parse(mat)!!, marks = listOf(mark), self = "joku muu")

        assertTrue(trees(sgf)[0].contains("C[MARK Move 42]"))
    }

    @Test
    fun `hakasulku kommentissa saa kenoviivan`() {
        val mark = MarkedPosition(1, GameKey(MatchId("1"), 0, 0), 5, "a]b", 0)
        val sgf = SgfExport.write(MatMatch.parse(mat)!!, marks = listOf(mark), self = "tommih")

        assertTrue(sgf.contains("""C[MARK Move 5: a\]b]"""))
    }

    @Test
    fun `html tai tyhja ei ole ottelu`() {
        assertNull(MatMatch.parse(""))
        assertNull(MatMatch.parse("<html><body>login</body></html>"))
    }

    private companion object {
        val ODOTETTU_0 = listOf(
            "W[41xwmi]", "B[44aeaeeiei]", "W[54ytxt]", "B[52lqqs]",
            "W[51wrfe]", "B[64qwsw]", "W[21trfe]", "B[54lqqu]",
            "W[53hcfc]", "B[62qsqw]", "W[44rnrnhdhd]", "B[22ikiksusu]",
            "W[64tnnj]", "B[double]", "W[drop]",
        )
        val ODOTETTU_1 = listOf(
            "W[62xrrp]", "B[43adlp]", "W[66]", "B[52pusu]",
            "W[22ywxvfdfd]", "B[21yaln]", "W[54wrrn]", "B[54yeae]",
            "W[63vppm]", "B[44lpptswsw]", "W[63nhfc]", "B[55ejejqvqv]",
            "W[11hghgdcdc]", "B[61qwwx]", "W[31hghe]", "B[66jpjplrlr]",
            "W[43mjea]", "B[54yept]", "W[53mjgb]", "B[65ekkp]",
            "W[63mjmg]", "B[double]", "W[drop]",
        )
    }
}
