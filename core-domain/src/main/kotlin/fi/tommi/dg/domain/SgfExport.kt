package fi.tommi.dg.domain

/**
 * Ottelu GNU Backgammonin `.sgf`-muodossa, merkityt asemat kommentteina.
 *
 * **Miksi tämä muoto** (Tommin päätös 15.9.2026, `docs/AVOIMET.md` › `.sgf`-vienti GNU
 * Backgammonia varten): mitattu kolmella ohjelmalla samana iltana, ja GNU on ainoa joka
 * näyttää siirron `C[]`-kommentin, ja samalla ainoa avoin ja ilmainen. XG2 ja BGBlitz
 * lukevat tiedoston mutta eivät kommenttia. Jellyfish `.mat` ei kanna kommenttia lainkaan.
 *
 * **Muoto on luettu GNU:n omasta tulosteesta** (`gnubg-cli`, `import mat` ja `save match`
 * samasta `.mat`-tiedostosta 15.9.2026), ei spesifikaatiosta. Yksi pelipuu per peli, ja
 * pelin juurisolmussa `MI`, pelaajat ja tulos. Siirto on nopat ja nappulat kirjaimina:
 * piste 1 on `a` ja 24 on `x` **ensimmäisen pelaajan suunnasta**, toisen pelaajan piste
 * `p` kirjoitetaan `25 - p`. Muuri on `y` ja ulos `z` kummallekin. Kuutio on `double`,
 * `take` ja `drop`. GNU nimeää ensimmäisen sarakkeen pelaajan `W`:ksi ja toisen `B`:ksi.
 *
 * **Merkki kohdistetaan asemalla, ei siirtonumerolla** (Tommin tilaus 16.9.2026 illalla).
 * Merkki tuntee pelin pistepareina ja sivun otsikon siirtonumeron (`Move 377`), mutta se
 * luku on sivuston tilalaskuri joka kasvaa myös siirtoa koottaessa (mitattu kaappauksista
 * 16.9.2026: `1, 3, 4, 8, 9, 12`), eikä sitä voi kohdistaa `.mat`-tiedoston siirtoon. Peli
 * sen sijaan tunnistetaan varmasti, koska pisteet pelin alussa ovat sekä merkissä että
 * tiedoston otsikossa. Pelin sisällä kohta löytyy toistamalla peli teko teolta
 * ([MatGame.positions]) ja vertaamalla asemaa merkin tallentamaan
 * ([MarkedPosition.position]). Kommentti kirjoitetaan täsmäävää tekoa **seuraavaan**
 * solmuun, koska GNU näyttää solmun kohdalla aseman ennen sen siirtoa noppineen, eli juuri
 * sen päätöksen jonka pelaaja merkitsi. Jos täsmäys osuu viimeiseen tekoon, kommentti on
 * siinä. Sama asema kahdesti samassa pelissä (molemmat tanssivat) osuu ensimmäiseen.
 *
 * **Ilman asemaa tai ilman osumaa kommentti on pelin ensimmäisessä siirtosolmussa.** Ilman
 * asemaa on merkki joka tehtiin ennen 16.9.2026 tai laudalta jonka pisteitä ei voitu lukea.
 * Ilman osumaa on esimerkiksi merkki joka tehtiin kesken sivustolla askelletun siirron,
 * koska välitila ei ole minkään teon jälkeinen asema. Kumpikin sanoo siirtonumeron, jotta
 * lukija löytää kohdan askeltamalla. Juurisolmu ei kelpaa, koska GNU pudottaa sen `C[]`:n
 * hiljaa (mitattu 16.9.2026: `load match` ja `save match` hävittivät sen, siirtosolmun
 * kommentti säilyi).
 *
 * `RU[Crawford]` kirjoitetaan kun [crawford] on tosi, ja oletus on tosi koska tavallinen
 * DailyGammon-ottelu pelataan Crawfordilla ja GNU:n oma tuonti oletti saman. Double repeat
 * luopuu Crawfordista (`SUBSTANSSI.md` kohta 8), eikä `.mat` kerro muunnelmaa, joten
 * kutsuja päättää. Sovellus ei väitä ottelutilanteesta muuta kuin mitä tiedosto sanoo.
 */
object SgfExport {

    const val APPLICATION = "DG Android:0.1"

    /**
     * @param self Kirjautuneen nimi, jotta merkin pisteet (vastustaja, itse) osataan
     *   kääntää tiedoston sarakkeiksi. Jos nimi ei ole kumpikaan pelaaja, merkkejä ei
     *   kohdisteta vaan ne kirjoitetaan ottelun ensimmäisen pelin kommenttiin
     *   sellaisinaan, jotta ne eivät katoa.
     * @param date `DT`-ominaisuus muodossa `yyyy-MM-dd`, tai null jos päivää ei tiedetä.
     */
    fun write(
        match: MatMatch,
        marks: List<MarkedPosition> = emptyList(),
        self: String? = null,
        date: String? = null,
        crawford: Boolean = true,
    ): String {
        val selfIsSecond = self != null && self == match.secondPlayer
        val selfIsFirst = self != null && self == match.firstPlayer
        val unplaced = marks.filter { !selfIsFirst && !selfIsSecond }

        return buildString {
            match.games.forEachIndexed { index, game ->
                val gameMarks = marks.filter { m ->
                    when {
                        selfIsSecond -> m.game.opponentScore == game.firstScore && m.game.selfScore == game.secondScore
                        selfIsFirst -> m.game.opponentScore == game.secondScore && m.game.selfScore == game.firstScore
                        else -> false
                    }
                } + if (index == 0) unplaced else emptyList()

                append("(;FF[4]GM[6]CA[UTF-8]AP[").append(escape(APPLICATION)).append(']')
                append("MI[length:").append(match.length).append("][game:").append(index)
                    .append("][ws:").append(game.firstScore).append("][bs:").append(game.secondScore).append(']')
                append("PW[").append(escape(game.firstPlayer)).append("]PB[").append(escape(game.secondPlayer)).append(']')
                if (index == 0 && date != null) append("DT[").append(escape(date)).append(']')
                if (crawford) append("RU[Crawford]")
                (game.actions.lastOrNull() as? MatAction.Win)?.let { win ->
                    append("RE[").append(if (win.side == MatSide.FIRST) 'W' else 'B').append('+').append(win.points).append(']')
                }
                append('\n')
                val comments = placeMarks(game, gameMarks, if (selfIsFirst) MatSide.FIRST else MatSide.SECOND)
                game.actions.forEachIndexed { i, action ->
                    val node = node(action) ?: return@forEachIndexed
                    append(';').append(node)
                    comments[i]?.let { append("C[").append(escape(it)).append(']') }
                    append('\n')
                }
                // Sulkeva sulku samalle riville kuin viimeinen solmu, kuten GNU kirjoittaa.
                if (endsWith("\n")) setLength(length - 1)
                append(")\n")
            }
        }
    }

    /**
     * Kommentit tekojen indekseillä. Asemallinen merkki menee täsmäävää tekoa seuraavaan
     * tekoon; asemattomat ja osumattomat ensimmäiseen kirjoitettavaan tekoon. Kirjoitettava
     * on teko jolla on solmu, eli [MatAction.Win] ohitetaan sekä kohteena että ensimmäisenä.
     */
    private fun placeMarks(game: MatGame, marks: List<MarkedPosition>, self: MatSide): Map<Int, String> {
        if (marks.isEmpty()) return emptyMap()
        val writable = game.actions.indices.filter { game.actions[it] !is MatAction.Win }
        val first = writable.firstOrNull() ?: return emptyMap()
        val positions = game.positions()
        val placed = mutableMapOf<Int, MutableList<String>>()
        for (mark in marks) {
            val target = mark.position?.let { wanted ->
                val hit = positions.indexOfFirst { it.matches(wanted, self) }
                if (hit < 0) null else writable.firstOrNull { it > hit } ?: writable.lastOrNull { it <= hit }
            } ?: first
            placed.getOrPut(target) { mutableListOf() } += markText(mark)
        }
        return placed.mapValues { it.value.joinToString("\n") }
    }

    /** `MARK Move 377: recube after rollback?`, tai ilman kaksoispistettä jos sana puuttuu. */
    fun markText(mark: MarkedPosition): String =
        "MARK Move ${mark.moveNumber}" + if (mark.note.isBlank()) "" else ": ${mark.note.trim()}"

    private fun node(action: MatAction): String? {
        val side = if (action.side == MatSide.FIRST) "W" else "B"
        return when (action) {
            is MatAction.Roll -> buildString {
                append(side).append('[').append(action.die1).append(action.die2)
                // GNU:n oma järjestys: lähtöpiste laskevasti, sitten kohde laskevasti
                // (mitattu tulosteesta: `8/2 8/6` kirjoitettiin `8/6 8/2`). Järjestys ei
                // muuta siirtoa, mutta sama järjestys tekee tulosteesta vertailukelpoisen.
                val ordered = action.moves.sortedWith(compareByDescending<CheckerMove> { it.from }.thenByDescending { it.to })
                for (m in ordered) append(point(m.from, action.side)).append(point(m.to, action.side))
                append(']')
            }
            is MatAction.Double -> "$side[double]"
            is MatAction.Take -> "$side[take]"
            is MatAction.Drop -> "$side[drop]"
            is MatAction.Win -> null
        }
    }

    /**
     * Piste kirjaimeksi. Ensimmäisen pelaajan piste `p` on `'a' + p - 1`, toisen pelaajan
     * `'a' + 24 - p`, koska GNU kirjoittaa laudan aina ensimmäisen pelaajan suunnasta.
     * Muuri (25) ja ulos (0) ovat samat kirjaimet kummallekin.
     */
    private fun point(p: Int, side: MatSide): Char = when (p) {
        25 -> 'y'
        0 -> 'z'
        else -> if (side == MatSide.FIRST) 'a' + (p - 1) else 'a' + (24 - p)
    }

    /** SGF:n tekstiarvo: `]` ja `\` saavat kenoviivan. */
    private fun escape(s: String): String = s.replace("\\", "\\\\").replace("]", "\\]")
}

/**
 * Tiedostonimi jakoon, sama runko kuin `.mat`-viennillä ([MatchExport.fileName]).
 */
fun sgfFileName(id: MatchId): String = "dg-${id.value}.sgf"
