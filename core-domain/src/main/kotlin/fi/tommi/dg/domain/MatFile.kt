package fi.tommi.dg.domain

/**
 * DailyGammonin `.mat`-tiedosto (Jellyfish-muoto) jäsennettynä siirroiksi.
 *
 * **Tämä on olemassa `.sgf`-vientiä varten eikä analyysia varten** (Tommin päätös
 * 15.9.2026, `docs/AVOIMET.md` › `.sgf`-vienti GNU Backgammonia varten). Sovellus ei tulkitse
 * asemia eikä arvioi siirtoja; se lukee tiedoston rivit rakenteeksi jonka [SgfExport]
 * kirjoittaa toiseen muotoon. `SUBSTANSSI.md` kohta 9 ei laukea, koska tiedosto on
 * päättyneen ottelun.
 *
 * Muoto on luettu yhdestä mitatusta tiedostosta (`raakasivut/match_export.mat`, 1.8.2026)
 * eikä spesifikaatiosta, ja jäsennin on sen mukainen:
 *
 * ```
 *  21 point match
 *
 *  Game 1
 *  opponent : 0                        tommih : 0
 *   1) 41: 24/23 13/9                  44: 24/20 24/20 20/16* 20/16
 *   7) 64: 20/14 14/10                  Doubles => 2
 *   8)  Drops                          Wins 1 point
 * ```
 *
 * Kaksi saraketta, ja sarakeraja luetaan pelin otsikkorivistä (toisen nimen alku) eikä
 * oleteta vakioksi. Rivi jolla vain toinen sarake on täytetty (avausheiton hävinnyt ei
 * heitä, `1)` ja tyhjä vasen puoli) on tavallinen, ja sarakeraja on ainoa tapa tietää
 * kumman teko se on. Tanssi on heitto ilman siirtoja (`66:` ja tyhjää).
 *
 * Pisteet ovat tiedostossa **kummankin pelaajan omasta suunnasta** (24 on oma takakenttä,
 * 25 on muuri, 0 on ulos), toisin kuin `.sgf`:ssä. Muunnos on [SgfExport]in asia.
 *
 * **Kaksi muunnelmaa alkaa muusta kuin tavallisesta aloitusasemasta, ja tiedosto kertoo
 * sen kahdella eri tavalla** (mitattu 16.9.2026 kahdesta päättyneestä ottelusta,
 * `docs/AVOIMET.md` › `.sgf`-vienti). Nackgammonissa pelin ensimmäinen rivi on valeheitto
 * `12: Illegal play (7;0;1;...;-2;-2;0;0;0;4;0;3;...)`, jonka sulkeissa on sivuston oma
 * asemamerkkijono. Double repeatissa hyväksytty tuplaus toistetaan omana pelinään samasta
 * asemasta, eikä tiedostossa ole siitä mitään merkkiä: peli vain alkaa keskeltä
 * (`1) 32: 3/0 3/1`). Kumpikin luetaan [MatGame.setup]iksi, edellinen merkistä ja
 * jälkimmäinen ensimmäisistä heitoista, ja tämä tiedosto ei tulkitse asemaa pidemmälle.
 */
data class MatMatch(
    /** Ottelun pituus pisteinä, otsikkorivistä ` 21 point match`. */
    val length: Int,
    val games: List<MatGame>,
) {
    /** Ensimmäisen sarakkeen pelaaja, sama joka ottelussa. */
    val firstPlayer: String get() = games.first().firstPlayer
    val secondPlayer: String get() = games.first().secondPlayer

    /** Jokin peli alkaa muusta kuin tavallisesta aloitusasemasta, ks. [MatGame.setup]. */
    val startsFromSetup: Boolean get() = games.any { it.setup }

    companion object {
        /**
         * @return `null` jos teksti ei ala ottelun pituudella tai siinä ei ole yhtään
         *   peliä. Kieltäytyminen on näkyvä, väärä tiedosto ei.
         */
        fun parse(text: String): MatMatch? = MatFileParser.parse(text)
    }
}

data class MatGame(
    /** ` Game 3` → 3, tiedoston oma juokseva numero. */
    val number: Int,
    val firstPlayer: String,
    val secondPlayer: String,
    /** Pisteet pelin alussa, otsikkorivistä. */
    val firstScore: Int,
    val secondScore: Int,
    /** Teot siinä järjestyksessä kuin ne tehtiin: rivin vasen sarake ennen oikeaa. */
    val actions: List<MatAction>,
    /**
     * Peli ei ala tavallisesta aloitusasemasta. Tosi kun tiedostossa on `Illegal play (`
     * -asemarivi (nackgammon) tai kun jommankumman pelaajan ensimmäinen heitto siirtää
     * pisteeltä jolla tavallisessa aloitusasemassa ei ole nappulaa (double repeatin
     * toistettu peli). Jälkimmäinen on päättely eikä mittaus: toistettu peli jonka
     * molempien ensimmäiset heitot sattuvat näyttämään tavallisilta jää huomaamatta.
     */
    val setup: Boolean = false,
)

enum class MatSide { FIRST, SECOND }

sealed interface MatAction {
    val side: MatSide

    /**
     * Heitto ja sillä tehdyt siirrot. Tyhjä [moves] on tanssi tai muuten siirtämättä jäänyt
     * heitto; tiedosto ei erota niitä eikä tämäkään.
     */
    data class Roll(override val side: MatSide, val die1: Int, val die2: Int, val moves: List<CheckerMove>) : MatAction

    /** `Doubles => 4`: kuutio tarjotaan arvoon [to]. */
    data class Double(override val side: MatSide, val to: Int) : MatAction
    data class Take(override val side: MatSide) : MatAction
    data class Drop(override val side: MatSide) : MatAction

    /** `Wins 2 points`, pelin viimeinen rivi. Voittaja on sarakkeen pelaaja. */
    data class Win(override val side: MatSide, val points: Int) : MatAction
}

/**
 * Yksi nappula pisteeltä [from] pisteelle [to] siirtäjän omasta suunnasta: 25 on muuri,
 * 0 on ulos. [hit] on tiedoston `*`, ja se kannetaan mukana vain tiedoksi: `.sgf` ei
 * merkitse lyöntiä erikseen.
 */
data class CheckerMove(val from: Int, val to: Int, val hit: Boolean = false)

private object MatFileParser {

    private val LENGTH = Regex("""^\s*(\d+)\s+point match\s*$""", RegexOption.IGNORE_CASE)
    private val GAME = Regex("""^\s*Game\s+(\d+)\s*$""")
    private val HEADER = Regex("""^\s*(\S.*?)\s*:\s*(\d+)\s+(\S.*?)\s*:\s*(\d+)\s*$""")
    private val TURN = Regex("""^\s*(\d+)\)(.*)$""")
    private val ROLL = Regex("""^(\d)(\d):""")
    private val MOVE = Regex("""^(\d+)/(\d+)(\*?)$""")
    private val WIN = Regex("""^Wins\s+(\d+)\s+points?\b.*$""")

    /** Nackgammonin asemarivi: valeheitto ja sivuston asemamerkkijono sulkeissa. */
    private val SETUP = Regex("""\d\d: Illegal play \([^)]*\)""")

    /** Pisteet joilta tavallisen aloitusaseman ensimmäinen siirto voi lähteä, omasta suunnasta. */
    private val START_POINTS = setOf(24, 13, 8, 6)

    fun parse(text: String): MatMatch? {
        val lines = text.lines()
        val length = lines.firstNotNullOfOrNull { LENGTH.find(it)?.groupValues?.get(1)?.toInt() }
            ?: return null

        val games = mutableListOf<MatGame>()
        var number: Int? = null
        var header: MatchResult? = null
        var split = 0
        var actions = mutableListOf<MatAction>()
        var setupLine = false

        fun close() {
            val h = header ?: return
            games += MatGame(
                number = number ?: games.size + 1,
                firstPlayer = h.groupValues[1],
                secondPlayer = h.groupValues[3],
                firstScore = h.groupValues[2].toInt(),
                secondScore = h.groupValues[4].toInt(),
                actions = actions,
                setup = setupLine || startsMidGame(actions),
            )
            header = null
            actions = mutableListOf()
            setupLine = false
        }

        for (line in lines) {
            GAME.find(line)?.let {
                close()
                number = it.groupValues[1].toInt()
                return@let
            }
            if (header == null) {
                HEADER.find(line)?.let { h ->
                    header = h
                    // Toisen sarakkeen alku on toisen nimen alku otsikossa. Siirtorivillä
                    // sama sarake alkaa samasta kohdasta, ja `Doubles`-rivi yhtä aiemmin,
                    // joten raja on yhtä ennen nimeä.
                    split = line.indexOf(h.groupValues[3], startIndex = h.range.first + h.groupValues[1].length) - 1
                }
                continue
            }
            if (!TURN.containsMatchIn(line)) continue
            // Asemarivi on pidempi kuin sarakeraja, joten se korvataan samanmittaisella
            // tyhjällä: toisen sarakkeen heitto pysyy paikallaan eikä valu vasempaan.
            val cleaned = SETUP.replace(line) { setupLine = true; " ".repeat(it.value.length) }
            // Rivin alku `nn)` vie merkkejä, joten sarakeraja lasketaan koko rivistä.
            val left = cleaned.substring(0, minOf(split, cleaned.length)).replace(TURN_PREFIX, "")
            val right = if (cleaned.length > split) cleaned.substring(split) else ""
            actions += parseColumn(left, MatSide.FIRST)
            actions += parseColumn(right, MatSide.SECOND)
        }
        close()
        // Pelin viimeinen rivi ` Wins 1 point` on ilman numeroa, joten TURN ei osu siihen.
        // Se luetaan erikseen, jotta voittaja tiedetään myös silloin kun se on omalla rivillään.
        return if (games.isEmpty()) null else MatMatch(length, attachLoneWins(text, games))
    }

    private val TURN_PREFIX = Regex("""^\s*\d+\)""")

    /**
     * Kummankin pelaajan ensimmäinen siirtoheitto lähtee tavallisesta aloitusasemasta vain
     * pisteiltä 24, 13, 8 ja 6, tai pisteeltä jolle sama heitto jo toi nappulan (`13/7 7/6`).
     * Vastustaja ei voi lyödä ensimmäisellä heitollaan, joten muurikaan ei ole lähtöpiste.
     */
    private fun startsMidGame(actions: List<MatAction>): Boolean =
        MatSide.entries.any { side ->
            val first = actions.firstOrNull { it is MatAction.Roll && it.side == side && it.moves.isNotEmpty() } as MatAction.Roll?
                ?: return@any false
            val reached = mutableSetOf<Int>()
            first.moves.any { m ->
                val fromStart = m.from in START_POINTS || m.from in reached
                reached += m.to
                !fromStart
            }
        }

    private fun parseColumn(column: String, side: MatSide): List<MatAction> {
        val tokens = column.trim().split(Regex("""\s+""")).filter { it.isNotEmpty() }
        if (tokens.isEmpty()) return emptyList()
        val out = mutableListOf<MatAction>()
        var i = 0
        while (i < tokens.size) {
            val t = tokens[i]
            val roll = ROLL.find(t)
            when {
                roll != null -> {
                    val moves = mutableListOf<CheckerMove>()
                    i++
                    while (i < tokens.size) {
                        val m = MOVE.find(tokens[i]) ?: break
                        moves += CheckerMove(m.groupValues[1].toInt(), m.groupValues[2].toInt(), m.groupValues[3] == "*")
                        i++
                    }
                    out += MatAction.Roll(side, roll.groupValues[1].toInt(), roll.groupValues[2].toInt(), moves)
                }
                t == "Doubles" -> {
                    // `Doubles => 2` on kolme sanaa.
                    val to = tokens.getOrNull(i + 2)?.toIntOrNull() ?: 2
                    out += MatAction.Double(side, to)
                    i += 3
                }
                t == "Takes" -> { out += MatAction.Take(side); i++ }
                t == "Drops" -> { out += MatAction.Drop(side); i++ }
                t == "Wins" -> {
                    val rest = tokens.drop(i).joinToString(" ")
                    val points = WIN.find(rest)?.groupValues?.get(1)?.toInt() ?: 1
                    out += MatAction.Win(side, points)
                    i = tokens.size
                }
                else -> i++
            }
        }
        return out
    }

    /**
     * ` Wins 1 point` omalla rivillään ilman siirtonumeroa kuuluu edelliselle pelille, ja
     * sarake kertoo voittajan. Rivit käydään läpi pelien tahdissa: joka `Game`-otsikko
     * siirtää kohdetta.
     */
    private fun attachLoneWins(text: String, games: List<MatGame>): List<MatGame> {
        val result = games.map { it }.toMutableList()
        var index = -1
        var split = 0
        for (line in text.lines()) {
            if (GAME.containsMatchIn(line)) { index++; continue }
            if (index < 0 || index >= result.size) continue
            HEADER.find(line)?.let { h ->
                split = line.indexOf(h.groupValues[3], startIndex = h.range.first + h.groupValues[1].length) - 1
            }
            if (TURN.containsMatchIn(line)) continue
            val win = WIN.find(line.trim()) ?: continue
            val side = if (line.indexOf("Wins") >= split) MatSide.SECOND else MatSide.FIRST
            val game = result[index]
            if (game.actions.lastOrNull() is MatAction.Win) continue
            result[index] = game.copy(actions = game.actions + MatAction.Win(side, win.groupValues[1].toInt()))
        }
        return result
    }
}
