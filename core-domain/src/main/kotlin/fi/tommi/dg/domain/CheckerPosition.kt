package fi.tommi.dg.domain

/**
 * Nappula-asema kummankin pelaajan omasta suunnasta, merkin kohdistamiseen `.mat`-pelin
 * siirtoon.
 *
 * **Miksi tämä on olemassa** (Tommin tilaus 16.9.2026 illalla, *"tehdään se Mark tieto
 * sgf-tiedostoon mukaan"*, tulkinta kohdistus). Sivun otsikon `Move 377` on tilalaskuri
 * eikä ottelulokin siirtonumero (mitattu 16.9.2026, `docs/AVOIMET.md` › `.sgf`-vienti), joten
 * merkin siirtoa ei voi laskea numerosta. Asema sen sijaan on sama kummassakin lähteessä:
 * lauta merkintähetkellä ja `.mat`-pelin toisto siirto siirrolta kohtaavat täsmälleen siinä
 * solmussa johon kommentti kuuluu. Tallennetaan siis asema eikä numeroa.
 *
 * **Muoto on kummankin pelaajan oma numerointi**, sama jota `.mat` käyttää ([CheckerMove]):
 * indeksi 1..24 on piste omasta suunnasta (1..6 on koti), 25 on muuri ja 0 on ulos. Sivun
 * numerointi on katsojan ([Seat.homeIsLow] kertoo kumpaan suuntaan itse kulkee), ja
 * vastustajan piste `p` on hänen suunnastaan `25 - p`. Näin vertailu toistoon ei tarvitse
 * tietoa väreistä eikä sivun suunnasta, jotka ovat sivuston asioita.
 *
 * **Tallennusmuoto on yksi tekstirivi** (`1:` + 26 lukua + `/` + 26 lukua), koska kanta saa
 * sen sarakkeeksi ilman tyyppimuunninta ja koska tiedon voi lukea silmin. Etuliite on
 * versio: jos muoto joskus vaihtuu, vanha rivi tunnistetaan eikä lueta väärin.
 */
data class CheckerPosition(
    /** Kirjautuneen nappulat omasta suunnasta, 26 lukua: ulos, pisteet 1..24, muuri. */
    val self: List<Int>,
    /** Vastustajan nappulat hänen suunnastaan, sama muoto. */
    val opponent: List<Int>,
) {
    init {
        require(self.size == SIZE && opponent.size == SIZE) { "Asemassa on ${self.size}/${opponent.size} lukua, ei $SIZE" }
    }

    fun encode(): String = "$VERSION:" + self.joinToString(",") + "/" + opponent.joinToString(",")

    companion object {
        const val SIZE = 26
        const val BAR = 25
        const val OFF = 0
        private const val VERSION = "1"

        /** Tavallinen aloitusasema kummallekin. */
        val START: CheckerPosition = CheckerPosition(startSide(), startSide())

        fun startSide(): List<Int> = MutableList(SIZE) { 0 }.also {
            it[24] = 2; it[13] = 5; it[8] = 3; it[6] = 5
        }

        /** Tallennusrivistä takaisin, tai null jos rivi ei ole tätä muotoa. */
        fun decode(text: String): CheckerPosition? {
            val body = text.removePrefix("$VERSION:").takeIf { it != text } ?: return null
            val halves = body.split('/')
            if (halves.size != 2) return null
            val self = halves[0].split(',').map { it.toIntOrNull() ?: return null }
            val opponent = halves[1].split(',').map { it.toIntOrNull() ?: return null }
            if (self.size != SIZE || opponent.size != SIZE) return null
            return CheckerPosition(self, opponent)
        }

        /**
         * Asema laudalta [seat]in suunnasta, tai null jos lauta ei kerro pisteitä (Mini-skeema).
         *
         * Ulos kannetut ja muuri luetaan omista kentistään, koska ne eivät ole pisteitä
         * ([BoardState.borneOff], [BoardState.bar]).
         */
        fun of(board: BoardState, seat: Seat): CheckerPosition? {
            if (board.points.isEmpty()) return null
            val self = MutableList(SIZE) { 0 }
            val opponent = MutableList(SIZE) { 0 }
            for (point in board.points) {
                val owner = point.owner ?: continue
                val fromSelf = if (seat.homeIsLow) point.number else 25 - point.number
                if (owner == seat.color) self[fromSelf] = point.count else opponent[25 - fromSelf] = point.count
            }
            for (b in board.bar) if (b.color == seat.color) self[BAR] += b.count else opponent[BAR] += b.count
            for (o in board.borneOff) if (o.color == seat.color) self[OFF] += o.count else opponent[OFF] += o.count
            return CheckerPosition(self, opponent)
        }
    }
}

/**
 * Asema `.mat`-pelin toistossa, sarakkeiden mukaan: [first] on ensimmäisen sarakkeen
 * pelaaja omasta suunnastaan ja [second] toisen. Ei tiedä kumpi on kirjautunut; sen
 * kääntää [SgfExport].
 */
data class MatPosition(val first: List<Int>, val second: List<Int>) {

    fun side(side: MatSide): List<Int> = if (side == MatSide.FIRST) first else second

    /** Sama asema kuin merkissä, kun kirjautunut on [self]. */
    fun matches(mark: CheckerPosition, self: MatSide): Boolean =
        side(self) == mark.self && side(if (self == MatSide.FIRST) MatSide.SECOND else MatSide.FIRST) == mark.opponent

    /**
     * Asema heiton jälkeen. Lyönti luetaan asemasta eikä tiedoston tähdestä: kohdepisteellä
     * yksin oleva vastustajan nappula siirtyy muurille. Tiedostoa ei tarkisteta laillisuuden
     * suhteen; tämä toistaa sen mitä rivi sanoo.
     */
    fun after(roll: MatAction.Roll): MatPosition {
        val own = side(roll.side).toMutableList()
        val other = side(if (roll.side == MatSide.FIRST) MatSide.SECOND else MatSide.FIRST).toMutableList()
        for (m in roll.moves) {
            own[m.from] = own[m.from] - 1
            own[m.to] = own[m.to] + 1
            if (m.to in 1..24) {
                val theirs = 25 - m.to
                if (other[theirs] == 1) {
                    other[theirs] = 0
                    other[CheckerPosition.BAR] = other[CheckerPosition.BAR] + 1
                }
            }
        }
        return if (roll.side == MatSide.FIRST) MatPosition(own, other) else MatPosition(other, own)
    }

    companion object {
        val START = MatPosition(CheckerPosition.startSide(), CheckerPosition.startSide())
    }
}

/**
 * Asema jokaisen teon jälkeen, samassa järjestyksessä kuin [MatGame.actions]. Kuutioteko ei
 * siirrä nappulaa, joten sen kohdalla asema on sama kuin edellisen. Pelin alkuasema on
 * [MatPosition.START]; asetetusta asemasta alkavaa peliä ([MatGame.setup]) ei toisteta,
 * koska alkuasemaa ei tiedetä, ja silloin palautetaan tyhjä lista.
 */
fun MatGame.positions(): List<MatPosition> {
    if (setup) return emptyList()
    var position = MatPosition.START
    return actions.map { action ->
        if (action is MatAction.Roll) position = position.after(action)
        position
    }
}
