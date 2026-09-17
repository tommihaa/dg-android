package fi.tommi.dg.domain

/**
 * Paikallinen siirron kokoaminen: kirjaimet kertyvät laitteessa ja verkossa käydään vasta
 * `Submit Move`lla. Design ja sen mittausperusta: `docs/ARKKITEHTUURI.md`, paikallinen
 * kokoaminen. Mekanismi johon tämä nojaa on mitattu (`docs/KOHDE.md`): kirjain on
 * lähtöpiste sivun numeroinnissa (`a`=1 ... `x`=24, `y`=muuri), ja `S`-etuliite vaihtaa
 * noppajärjestyksen ennen ensimmäistä kirjainta.
 *
 * **Klikkausjärjestys on vapaa, vuoron kokonaisuus ratkaisee.** Tämä ei ole oletus vaan
 * fixtureista mitattu (24.8.2026): muurisivu tarjoaa myös muita lähtöjä kuin muurin
 * (`move_bar_entry.html`), ja uloskantosivu tarjoaa uloskannon ennen kuin ylin piste on
 * tyhjennetty (`move_greedy_bearoff.html`). Kumpikin askel on klassisessa järjestyksessä
 * laiton juuri sillä hetkellä ja laillinen osana kokonaista vuoroa. Sivusto siis sallii
 * vuoron askelten poiminnan missä järjestyksessä vain, ja laillisuus koskee kokonaisuutta.
 *
 * **Sivun linkit ovat oraakkeli.** [LocalComposition.begin] vertaa generaattorin
 * ensimmäisen askeleen joukkoa sivun omiin linkkeihin ja kieltäytyy (`null`) jos ne ovat
 * eri mieltä. Silloin lauta kulkee vanhaa reittiä eikä paikallista kokoamista käytetä:
 * vika näkyy hitautena eikä koskaan vääränä siirtona.
 */

/** Pelaajan istuin: oma väri ja kulkusuunta sivun numeroinnissa. */
data class Seat(val color: CheckerColor, val homeIsLow: Boolean)

/**
 * Istuin pip-vahdin päättelyllä, tai null kun sitä ei voi tietää.
 *
 * Sama päättely kuin näkymän `selfCheckerColor`issa, mutta suunta otetaan talteen, koska
 * siirtolaskenta tarvitsee sen. **Moniselitteinen asema palauttaa nullin eikä ensimmäistä
 * osumaa:** symmetrinen lauta täsmää molemmilla suunnilla, ja väärä suunta tuottaisi
 * oikean näköisen mutta väärän siirron. Null pudottaa laudan vanhaan reittiin, ja se on
 * tässä halvin turvallinen tulos.
 *
 * **Pistelukujen tasapeli oli tässä hiljainen virhe 1.9.2026 asti.** Kun molemmilla on
 * sama pip-luku, `selfPips` täsmää sekä keltaiseen että siniseen, ja `when` valitsi
 * ensimmäisen haaran eli aina keltaisen. Sinisenä pelatessa istuin oli silloin väärä, eikä
 * mikään kertonut siitä: [LocalComposition.begin] ei löytänyt omia noppia, kokoaminen
 * pudottautui vanhaan reittiin ja käyttäjä maksoi kaksi ylimääräistä pyyntöä vuoroa kohti.
 * Sama tasapeli oli jäljitetty 31.8.2026 nappuloiden värinvaihtoon (`docs/AVOIMET.md`);
 * tämä on saman virheen toinen ilmentymä toisessa paikassa.
 *
 * **Tasapelin ratkaisee sivun oma noppa eikä väriarvaus**, ja se on mitattu koko kaapatusta
 * korpuksesta: 745 lautasivulla joilla on oma vuoro ja yksiselitteinen pip-luku, noppien
 * omistaja on **poikkeuksetta** sama kuin pip-vahdin istuin. Nopat kelpaavat siis
 * ristiriidan ratkaisijaksi juuri siellä missä pip-vahti ei osaa valita, eivätkä ne kanna
 * skeeman väripalettia mukanaan niin kuin nimisolun `BGCOLOR` kantaisi
 * (`docs/KOHDE.md`).
 */
fun BoardState.resolveSeat(): Seat? {
    val selfPips = players.lastOrNull()?.pips ?: return null
    if (points.isEmpty()) return null
    val panel = players.mapNotNull { it.pips }
    if (panel.size < 2) return null
    val panelSet = panel.toSet()

    val seats = buildList {
        listOf(true, false).forEach { yellowHomeIsLow ->
            val yellow = totalPipCount(CheckerColor.YELLOW, yellowHomeIsLow)
            val blue = totalPipCount(CheckerColor.BLUE, !yellowHomeIsLow)
            if (setOf(yellow, blue) == panelSet) {
                val color = when {
                    // Yksiselitteinen pip-luku ratkaisee kuten ennenkin.
                    yellow != blue -> when (selfPips) {
                        yellow -> CheckerColor.YELLOW
                        blue -> CheckerColor.BLUE
                        else -> null
                    }
                    // Tasapeli: pip-luku ei erota puolia, joten se luetaan nopista.
                    else -> ownDiceColor()
                }
                if (color != null) {
                    val homeIsLow = if (color == CheckerColor.YELLOW) yellowHomeIsLow else !yellowHomeIsLow
                    add(Seat(color, homeIsLow))
                }
            }
        }
    }
    return seats.distinct().singleOrNull()
}

/**
 * Oman puolen väri sivun nopista, tai null kun sivu ei sano sitä yksiselitteisesti.
 *
 * **Ehto on tarkalleen se jonka mittaus kattaa, eikä yhtään laajempi.** Sivulla on oltava
 * oma askel tarjolla (siirtolinkki tai muurilta tulo) ja noppien omistajan on oltava yksi.
 * Silloin ja vain silloin nopat ovat mitatusti sen pelaajan jonka vuoro on, eli katsojan
 * omat. Ilman vuoroehtoa tämä väittäisi myös vastustajan vuoron sivuista, joita mittaus ei
 * kata.
 */
private fun BoardState.ownDiceColor(): CheckerColor? {
    val ownStepOffered = moves.isNotEmpty() || bar.any { it.href != null }
    if (!ownStepOffered) return null
    return dice.map { it.owner }.distinct().singleOrNull()
}

/** Muurin pistenumero tässä moduulissa. Sivulla muurilla ei ole numeroa, kirjain on `y`. */
const val BAR_POINT = 25

/**
 * Kirjain sivun pistenumerosta. Kuvaus on mitattu (`a`=1 ... `x`=24, `y`=muuri) ja
 * vahdittu kahdesta suunnasta: oraakkeli vertaa sivun omia linkkikirjaimia tähän
 * jokaisella laudalla ennen kuin yhtään kirjainta rakennetaan. Tietoinen poikkeus
 * osoitteenrakennuskieltoon, kuitattu 24.8.2026; ks. `docs/ARKKITEHTUURI.md`.
 */
fun moveLetter(fromPoint: Int): Char =
    if (fromPoint == BAR_POINT) 'y' else 'a' + (fromPoint - 1)

/** Yksi askel: lähtö sivun numerolla ([BAR_POINT] = muuri) ja käytetty noppa. */
data class MoveStep(val fromPoint: Int, val die: Int)

/**
 * Asema siirtolaskentaa varten, omina etäisyyksinä.
 *
 * Etäisyys on matka ulos: 1..6 on oma kotipesä, [BAR_POINT] muuri. Sivun numerosta tähän
 * päästään istuimen suunnalla, ja takaisin [toPagePoint]illa. Vastustajasta riittää
 * miehitys omalla etäisyysakselilla, koska vain se vaikuttaa oman siirron laillisuuteen,
 * eikä miehitys muutu oman vuoron aikana muuten kuin syödyn blotin osalta.
 */
internal data class EnginePosition(
    val seat: Seat,
    /** Omat nappulat etäisyyksittäin, indeksi 1..24; indeksi [BAR_POINT] on muuri. */
    val own: List<Int>,
    /** Vastustajan nappulat oman etäisyysakselin kohdissa 1..24. */
    val opp: List<Int>,
) {
    fun toPagePoint(distance: Int): Int =
        if (distance == BAR_POINT) BAR_POINT else if (seat.homeIsLow) distance else 25 - distance

    fun fromPagePoint(point: Int): Int =
        if (point == BAR_POINT) BAR_POINT else if (seat.homeIsLow) point else 25 - point

    private val canBearOff: Boolean
        get() = own[BAR_POINT] == 0 && (7..24).all { own[it] == 0 }

    /**
     * Etäisyydet joilta noppa [die] voi siirtää **klassisessa järjestyksessä juuri nyt**.
     * Muurilta tulon pakko on tässä, koska klassinen järjestys on se jolla kokonaiset
     * vuorot generoidaan; vapaa klikkausjärjestys tulee vasta [CompositionSession]issa.
     */
    fun legalFroms(die: Int): List<Int> {
        if (own[BAR_POINT] > 0) {
            val entry = BAR_POINT - die
            return if (opp[entry] <= 1) listOf(BAR_POINT) else emptyList()
        }
        return (1..24).filter { from ->
            if (own[from] == 0) return@filter false
            val target = from - die
            when {
                target >= 1 -> opp[target] <= 1
                else -> canBearOff && (die == from || (from + 1..6).all { own[it] == 0 })
            }
        }
    }

    /** Asema askeleen jälkeen. Kutsujan vastuulla on askeleen laillisuus. */
    fun apply(fromDistance: Int, die: Int): EnginePosition {
        val newOwn = own.toMutableList()
        val newOpp = opp.toMutableList()
        newOwn[fromDistance] = newOwn[fromDistance] - 1
        val target = fromDistance - die
        if (target >= 1) {
            if (newOpp[target] == 1) newOpp[target] = 0
            newOwn[target] = newOwn[target] + 1
        }
        return copy(own = newOwn, opp = newOpp)
    }
}

internal fun enginePosition(board: BoardState, seat: Seat): EnginePosition {
    val own = MutableList(BAR_POINT + 1) { 0 }
    val opp = MutableList(BAR_POINT + 1) { 0 }
    board.points.forEach { point ->
        val distance = if (seat.homeIsLow) point.number else 25 - point.number
        when (point.owner) {
            seat.color -> own[distance] = point.count
            null -> Unit
            else -> opp[distance] = point.count
        }
    }
    own[BAR_POINT] = board.bar.firstOrNull { it.color == seat.color }?.count ?: 0
    return EnginePosition(seat, own, opp)
}

/** Kokonainen vuoro: askeleet klassisessa järjestyksessä, vertailut monijoukkona. */
data class Turn(val steps: List<MoveStep>) {
    val multiset: Map<MoveStep, Int> by lazy { steps.groupingBy { it }.eachCount() }

    fun containsAll(picked: List<MoveStep>): Boolean {
        val pickedCounts = picked.groupingBy { it }.eachCount()
        return pickedCounts.all { (step, count) -> (multiset[step] ?: 0) >= count }
    }

    fun matches(picked: List<MoveStep>): Boolean =
        picked.size == steps.size && containsAll(picked)
}

/**
 * Kaikki lailliset kokonaiset vuorot, pakkosäännöt mukana.
 *
 * Generointi on klassinen: nopat käytetään jossakin järjestyksessä ja jokainen askel on
 * laillinen omalla hetkellään. Pakkosäännöt tulevat vertailusta eivätkä erillisistä
 * ehdoista: vain pisimmät vuorot kelpaavat, ja jos pisin on yhden askeleen mittainen ja
 * kumpikin noppa kelpaisi yksinään, vain isompi noppa kelpaa. Nämä ovat backgammonin
 * vakiosäännöt; ensimmäisen askeleen osalta oraakkeli todentaa yhtäpitävyyden sivun
 * kanssa jokaisella laudalla.
 */
fun legalTurns(board: BoardState, seat: Seat, dice: List<Int>): List<Turn> {
    val position = enginePosition(board, seat)
    val isDouble = dice.distinct().size == 1
    val orders: List<List<Int>> = if (isDouble) {
        listOf(List(4) { dice.first() })
    } else {
        listOf(dice, dice.reversed())
    }

    val raw = orders.flatMap { order -> sequencesFor(position, order) }
    val maxLength = raw.maxOfOrNull { it.size } ?: 0
    if (maxLength == 0) return emptyList()

    val mandatedDie: Int? = if (maxLength == 1 && !isDouble) {
        val playable = dice.distinct().filter { die -> position.legalFroms(die).isNotEmpty() }
        if (playable.size == 2) playable.max() else null
    } else {
        null
    }

    return raw.filter { steps ->
        steps.size == maxLength && (mandatedDie == null || steps.first().die == mandatedDie)
    }.map(::Turn).distinctBy { it.multiset }
}

private fun sequencesFor(position: EnginePosition, dice: List<Int>): List<List<MoveStep>> {
    if (dice.isEmpty()) return listOf(emptyList())
    val die = dice.first()
    val froms = position.legalFroms(die)
    if (froms.isEmpty()) return listOf(emptyList())
    return froms.flatMap { from ->
        val step = MoveStep(position.toPagePoint(from), die)
        sequencesFor(position.apply(from, die), dice.drop(1)).map { rest -> listOf(step) + rest }
    }
}

/**
 * Kokoamissessio: käyttäjän poimimat askeleet ja se mitä niistä seuraa. Arvo on
 * muuttumaton; jokainen teko palauttaa uuden session, joten peruminen on paluu aiempaan
 * arvoon.
 *
 * **Poimintajärjestys on vapaa mutta poimittu joukko on aina jonkin laillisen vuoron
 * osajoukko.** Napautus valitsee nopan ahneesti nykyisestä noppajärjestyksestä: ensimmäinen
 * vielä käyttämätön noppa jolla askel sopii johonkin yhteensopivaan vuoroon. Sama piste voi
 * siis siirtyä kummalla tahansa nopalla, ja järjestyksen vaihto ([swap]) on se teko jolla
 * käyttäjä valitsee toisen, kuten sivullakin.
 */
class CompositionSession private constructor(
    private val board: BoardState,
    val seat: Seat,
    /** Nopat näytetyssä järjestyksessä, ennen [swapped]-vaihtoa. */
    private val displayedDice: List<Int>,
    private val turns: List<Turn>,
    /** Lomakkeen pohjaosoite sivun omista linkeistä luettuna, ilman kyselyä. */
    private val action: String,
    val swapped: Boolean,
    val steps: List<MoveStep>,
) {
    /** Kirjainjono sellaisena kuin se lähtee, esim. `Sfe` tai `whff`. */
    val letters: String
        get() = buildString {
            if (swapped) append('S')
            steps.forEach { append(moveLetter(it.fromPoint)) }
        }

    private val compatibleTurns: List<Turn>
        get() = turns.filter { it.containsAll(steps) }

    /** Noppien kulutusjärjestys nykyisellä [swapped]-valinnalla. */
    private val diceOrder: List<Int>
        get() = when {
            displayedDice.distinct().size == 1 -> List(4) { displayedDice.first() }
            swapped -> displayedDice.reversed()
            else -> displayedDice
        }

    /** Heitetyt nopat näytetyssä järjestyksessä, tuplaa laajentamatta. Ks. [boardNow]. */
    private fun shownDice(): List<Int> =
        if (swapped) displayedDice.reversed() else displayedDice

    /** Piirrettävät nopat kulutusosuuksineen, ks. [boardNow]. */
    private fun diceForDisplay(siteDice: Boolean): List<Die> {
        val shown = shownDice()
        if (siteDice && shown.distinct().size == 1) {
            // Sivuston tupla: kuutio i kantaa askeleet 2i ja 2i+1.
            return shown.mapIndexed { i, value ->
                Die(value, seat.color, (steps.size - 2 * i).coerceIn(0, 2) / 2f)
            }
        }
        // Pelatut merkitään vasemmalta: tuplassa harmaus etenee järjestyksessä.
        val used = steps.map { it.die }.toMutableList()
        return (if (siteDice) shown else diceOrder).map { value ->
            Die(value, seat.color, if (used.remove(value)) 1f else 0f)
        }
    }

    /** Nopat joita ei ole vielä käytetty, kulutusjärjestyksessä. */
    fun remainingDice(): List<Int> {
        val remaining = diceOrder.toMutableList()
        steps.forEach { remaining.remove(it.die) }
        return remaining
    }

    /** Asema tähän mennessä poimittujen askelten jälkeen, naiivisti sovellettuna. */
    private fun currentPosition(): EnginePosition {
        var position = enginePosition(board, seat)
        steps.forEach { position = position.apply(position.fromPagePoint(it.fromPoint), it.die) }
        return position
    }

    /**
     * Seuraavan napautuksen lailliset lähdöt sivun numeroina, [BAR_POINT] on muuri.
     *
     * Kaksi ehtoa, ja molemmat on mitattu fixtureista: askel kuuluu johonkin
     * yhteensopivaan vuoroon, ja lähdöllä on oma nappula **tämänhetkisessä** asemassa.
     * Jälkimmäinen erottaa tämän pelkästä vuoron jäsenyydestä: piste johon nappula
     * ilmestyy vasta kesken vuoron ei ole vielä tarjolla, koska sivun linkki kiertää
     * nappulan kuvaa eikä pistettä (`move_bar_entry.html` ei tarjoa tulopistettä,
     * `move_greedy_bearoff.html` ei tarjoa välipistettä).
     */
    fun legalFromPoints(): Set<Int> =
        buildSet {
            val remaining = remainingDice().distinct()
            val position = currentPosition()
            compatibleTurns.forEach { turn ->
                turn.steps.forEach { step ->
                    val used = steps.count { it == step }
                    val occupied = position.own[position.fromPagePoint(step.fromPoint)] > 0
                    if (step.die in remaining && (turn.multiset[step] ?: 0) > used && occupied) {
                        add(step.fromPoint)
                    }
                }
            }
        }

    /** Vuoro on täysi kun poiminta kattaa jonkin laillisen vuoron kokonaan. */
    val isComplete: Boolean
        get() = turns.any { it.matches(steps) }

    /** Vaihto on sivun tapaan mahdollinen vain ennen ensimmäistä poimintaa. */
    val canSwap: Boolean
        get() = steps.isEmpty() && displayedDice.distinct().size == 2

    fun swap(): CompositionSession? =
        if (canSwap) copy(swapped = !swapped) else null

    /**
     * Poimii askeleen pisteeltä. Noppa valitaan ahneesti: ensimmäinen käyttämätön noppa
     * kulutusjärjestyksessä jolla poiminta pysyy jonkin laillisen vuoron sisällä.
     */
    fun step(fromPoint: Int): CompositionSession? {
        if (fromPoint !in legalFromPoints()) return null
        val die = remainingDice().firstOrNull { candidate ->
            val picked = steps + MoveStep(fromPoint, candidate)
            turns.any { it.containsAll(picked) }
        } ?: return null
        return copy(steps = steps + MoveStep(fromPoint, die))
    }

    /** Tyhjentää koko kokoamisen, samoin kuin sivun oma `Undo Move`. */
    fun undo(): CompositionSession = copy(steps = emptyList())

    /**
     * Pakolliset askeleet: ne jotka ovat mukana **jokaisessa** laillisessa vuorossa,
     * monijoukon leikkauksena (sama askel kahdesti vain jos se on kahdesti jokaisessa).
     *
     * Tämä on Tommin vaatimuksen *"jos joku siirto on pakollinen, niin se pelataan"*
     * (2.9.2026, `docs/AVOIMET.md`) täsmällinen muoto: siirto on askel eikä vuoro. Vuoro
     * ei mitatusti ole koskaan kokonaan pakollinen sivuston valintasivulla (sivusto hoitaa
     * sen itse), mutta yksittäinen askel on joka viidennessä vuorossa, ja se on lähes aina
     * muurilta tulo tai uloskanto.
     */
    fun forcedSteps(): Map<MoveStep, Int> = intersection(turns)

    /**
     * Ahne uloskanto: askeleet jotka poimitaan valmiiksi kun kontaktia ei ole (Tommin tilaus
     * 2.9.2026, `docs/AVOIMET.md`). Lähetys jää pelaajalle kuten pakollisissa askelissa.
     *
     * Sääntö on mitattu sivuston omasta `Submit Greedy Bearoff` -esitäytöstä (56 lautaa,
     * `GreedyKorpusTest`): esitäyttö on aina vuoro joka **kantaa ulos eniten**, ja sivusto
     * tarjoaa nappinsa täsmälleen silloin kun sellaisen vuoron loppuasema on yksikäsitteinen
     * eikä kontaktia ole. Tämä tekee saman ja jatkaa siitä mihin sivusto jättää: kun eniten
     * ulos kantavia loppuasemia on useampi, poimitaan niiden yhteinen osa (monijoukon
     * leikkaus, sama kuin [forcedSteps] mutta rajattuna ahneisiin vuoroihin) ja valinta jää
     * pelaajalle. Se on Tommin sulkulause *"voisi toimia jo aiemminkin"*.
     *
     * Tyhjä kolmessa tapauksessa: kontakti (jokin oma nappula on vielä vastustajan nappulan
     * takana, muurilla oleva vastustaja lasketaan takimmaiseksi), yksikään vuoro ei kanna
     * ulos, tai ahneiden vuorojen leikkaus on tyhjä. Kun tulos ei ole tyhjä, se sisältää
     * aina [forcedSteps]:n, koska pakollinen askel on jokaisessa vuorossa.
     */
    fun greedySteps(): Map<MoveStep, Int> {
        if (turns.isEmpty()) return emptyMap()
        val start = enginePosition(board, seat)
        val oppBar = board.bar.firstOrNull { it.color != seat.color }?.count ?: 0
        val highestOwn = (BAR_POINT downTo 1).firstOrNull { start.own[it] > 0 } ?: 0
        val lowestOpp = if (oppBar > 0) 0 else (1..24).firstOrNull { start.opp[it] > 0 } ?: Int.MAX_VALUE
        if (highestOwn > lowestOpp) return emptyMap()

        fun borneOff(turn: Turn): Int = turn.steps.count { start.fromPagePoint(it.fromPoint) - it.die < 1 }
        val best = turns.maxOf(::borneOff)
        if (best == 0) return emptyMap()
        val greedy = turns.filter { borneOff(it) == best }
        val positions = greedy.map { turn ->
            turn.steps.fold(start) { position, step -> position.apply(position.fromPagePoint(step.fromPoint), step.die) }.own
        }.toSet()
        return if (positions.size == 1) greedy.first().multiset else intersection(greedy)
    }

    /** Askeleet jotka ovat jokaisessa annetussa vuorossa, monijoukon leikkauksena. */
    private fun intersection(turns: List<Turn>): Map<MoveStep, Int> =
        turns.map { it.multiset }.reduceOrNull { acc, m ->
            acc.filterKeys { it in m }.mapValues { (step, count) -> minOf(count, m.getValue(step)) }
        } ?: emptyMap()

    /**
     * Sessio jossa pakolliset askeleet on poimittu valmiiksi, tai tämä sellaisenaan kun
     * pakollisia ei ole, jokin on jo poimittu tai poiminta ei tavoita niitä täsmälleen.
     *
     * **Poiminta kulkee saman [step]-reitin kautta kuin napautus**, eikä askelta aseteta
     * suoraan, jotta [submission]in toisto päätyy varmasti samaan monijoukkoon: toisto
     * käyttää samaa ahnetta noppavalintaa. Piste poimitaan vasta kun se on tarjolla
     * ([legalFromPoints]), koska pakollinen askel voi lähteä pisteeltä jolle nappula
     * ilmestyy vasta kesken vuoron. Jos ahne valinta osuu väärään noppaan, yritetään
     * noppajärjestyksen vaihdolla; jos sekään ei tuota täsmälleen pakollisia askeleita,
     * mitään ei poimita ja pelaaja tekee sen itse. Väärää askelta ei siis voi syntyä tässä:
     * lopputulos on joko täsmälleen pakollinen monijoukko tai tyhjä.
     *
     * Lähetys jää aina pelaajalle. Pakolliset askeleet eivät mitatusti koskaan täytä koko
     * vuoroa, ja vaikka täyttäisivät, `Submit Move` on yhä painallus.
     */
    fun prefillForced(): CompositionSession = prefill(forcedSteps())

    /**
     * Sessio jossa ahneen uloskannon askeleet ([greedySteps]) on poimittu valmiiksi, samalla
     * reitillä ja samoilla takeilla kuin [prefillForced]. Kun ahne vuoro on yksikäsitteinen,
     * tulos on täysi vuoro ja lomake näkyy heti; `Submit Move` on silti pelaajan painallus.
     */
    fun prefillGreedy(): CompositionSession = prefill(greedySteps())

    private fun prefill(target: Map<MoveStep, Int>): CompositionSession {
        if (steps.isNotEmpty()) return this
        if (target.isEmpty()) return this
        val points = target.flatMap { (step, count) -> List(count) { step.fromPoint } }

        listOfNotNull(this, swap()).forEach { start ->
            var session = start
            val remaining = points.toMutableList()
            while (remaining.isNotEmpty()) {
                val offered = session.legalFromPoints()
                val point = remaining.firstOrNull { it in offered } ?: break
                session = session.step(point) ?: break
                remaining.remove(point)
            }
            if (remaining.isEmpty() && session.steps.groupingBy { it }.eachCount() == target) {
                return session
            }
        }
        return this
    }

    /**
     * Lauta poimittujen askelten jälkeen näkymää varten: pisteet, muuri ja ulos kannetut
     * päivitetty, nopiksi jäävät käyttämättömät. Tämä on paikallinen väite eikä sivun
     * tila, ja lähetyksen vastaus korvaa sen aina.
     *
     * **Siirtolinkit, peruminen, Swap Dice ja lomake ovat synteettisiä `local:`-osoitteita**
     * ([LOCAL_STEP_PREFIX], [LOCAL_UNDO], [LOCAL_SWAP]), jotta näkymä piirtää kokoamisen
     * samoilla komponenteilla kuin sivun oman tilan. Ne eivät voi karata verkkoon:
     * `BoardViewModel.follow` sieppaa etuliitteen ennen jäsenyystarkistusta, ja sivun
     * omiin linkkeihin ne eivät kuulu, joten portin jäsenyysehto hylkäisi ne joka
     * tapauksessa. Lomake ilmestyy vasta kun vuoro on täysi, samoin kuin sivullakin.
     *
     * **[siteDice] koskee vain noppien piirtoa, ei laillisuutta.** Arvolla `true` nopiksi
     * jäävät heitetyt nopat sellaisinaan, eli tasan kaksi jotka eivät vähene kokoamisen
     * aikana: se on sivuston mitattu käytös (27.8.2026, 2698 kaapattua lautasivua ja
     * jokaisella kaksi noppakuvaa, myös tuplan kokoamisen keskellä). Oletusarvo `false`
     * antaa siirtolaskurin: yksi kuutio per siirto, tupla neljänä.
     *
     * **Pelattu noppa ei katoa vaan harmaantuu** (Tommin tilaus 2.9.2026), kummassakin
     * esityksessä: kuutioiden määrä ei muutu kokoamisen aikana, ja [Die.spent] kertoo
     * kuinka suuri osa kuutiosta on käytetty. Laskurissa ja sivuston ei-tuplassa se on
     * 0 tai 1; sivuston tuplassa kaksi kuutiota kantavat neljä askelta, joten kuutio
     * harmaantuu puolikas kerrallaan. Askelten poiminta, [legalFromPoints] ja kirjainjono
     * käyttävät [remainingDice]iä kummallakin arvolla, joten valinta ei voi muuttaa sitä
     * mitä lähetetään.
     */
    fun boardNow(siteDice: Boolean = false): BoardState {
        var position = enginePosition(board, seat)
        var oppBar = board.bar.firstOrNull { it.color != seat.color }?.count ?: 0
        var ownOff = board.borneOff.firstOrNull { it.color == seat.color }?.count ?: 0
        steps.forEach { step ->
            val from = position.fromPagePoint(step.fromPoint)
            val target = from - step.die
            if (target >= 1 && position.opp[target] == 1) oppBar += 1
            if (target < 1) ownOff += 1
            position = position.apply(from, step.die)
        }

        val opponentColor = CheckerColor.entries.first { it != seat.color }
        val points = board.points.map { point ->
            val distance = position.fromPagePoint(point.number)
            val ownCount = position.own[distance]
            val oppCount = position.opp[distance]
            when {
                ownCount > 0 -> point.copy(owner = seat.color, count = ownCount)
                oppCount > 0 -> point.copy(owner = opponentColor, count = oppCount)
                else -> point.copy(owner = null, count = 0)
            }
        }
        val froms = legalFromPoints()
        val bar = buildList {
            if (position.own[BAR_POINT] > 0) {
                val href = if (BAR_POINT in froms) "$LOCAL_STEP_PREFIX$BAR_POINT" else null
                add(BarCheckers(seat.color, position.own[BAR_POINT], href = href))
            }
            if (oppBar > 0) add(BarCheckers(opponentColor, oppBar))
        }
        val borneOff = buildList {
            if (ownOff > 0) add(BorneOffCheckers(seat.color, ownOff))
            board.borneOff.firstOrNull { it.color == opponentColor }?.let { add(it) }
        }
        return board.copy(
            points = points,
            bar = bar,
            borneOff = borneOff,
            // Sivuston esityksessä nopat ovat heitetyt kaksi näytetyssä järjestyksessä;
            // vaihdon jälkeen käännettyinä kuten sivun oma Swap Dice tekee.
            // Avausheitto luetaan sivun laudasta eikä kootusta: ensimmäinen askel vie
            // aseman pois lähtöasemasta, ja esityksen on pysyttävä koko vuoron ajan.
            dice = board.openingRoll?.present(diceForDisplay(siteDice)) ?: diceForDisplay(siteDice),
            moves = froms.filter { it != BAR_POINT }.sorted().map { from ->
                MoveLink(from, moveLetter(from).toString(), "$LOCAL_STEP_PREFIX$from")
            },
            commands = if (canSwap) listOf(CommandLink(SWAP_DICE, LOCAL_SWAP)) else emptyList(),
            form = if (isComplete) {
                BoardForm(
                    action = action,
                    method = FormMethod.GET,
                    pendingMove = letters,
                    submits = listOf(SUBMIT_MOVE),
                    verify = null,
                )
            } else {
                null
            },
            undoHref = if (steps.isNotEmpty()) LOCAL_UNDO else null,
        )
    }

    /**
     * Lähetys kun vuoro on täysi: pohjaosoite sivun linkeistä, kirjaimet mitatusta
     * kuvauksesta ja `Submit Move` mitattuna vakiona. Metodi on GET kuten selaimen oma
     * siirtolomake. Konstruktoripolku kulkee `core-domain`in sisällä, joten keksittyä
     * lähetystä ei voi koota muualta, sama tae kuin [BoardForm.press]illä.
     *
     * **Kirjainjonon tulkinta on sama molemmin puolin, ja se varmistetaan toistolla.**
     * Kirjain ei kanna noppaa, joten palvelimen on jaettava nopat kirjaimille itse.
     * Sivun oma stepwise-käytös on mitatusti täydentyvyyspohjainen (linkki tarjotaan
     * askeleelle joka on laiton juuri nyt mutta laillinen osana kokonaista vuoroa),
     * joten jako on sama ahne valinta jota [step] käyttää. Tämä metodi toistaa jaon
     * kirjaimista puhtaalta pöydältä ja lähettää vain jos toisto päätyy samaan
     * monijoukkoon; muuten palautetaan null ja lauta putoaa vanhaan reittiin. Ehto ei
     * voi laueta [step]-polulla, koska poiminta käyttää samaa sääntöä, ja se on tässä
     * juuri siksi että lähetys ei saa nojata siihen päättelyyn hiljaa.
     */
    fun submission(): FormSubmission? {
        if (!isComplete) return null

        var replay = CompositionSession(board, seat, displayedDice, turns, action, swapped, emptyList())
        steps.forEach { picked ->
            replay = replay.step(picked.fromPoint) ?: return null
        }
        val replayCounts = replay.steps.groupingBy { it }.eachCount()
        val ownCounts = steps.groupingBy { it }.eachCount()
        if (replayCounts != ownCounts) return null

        return FormSubmission(
            action = action,
            method = FormMethod.GET,
            fields = linkedMapOf("move" to letters, "submit" to SUBMIT_MOVE),
        )
    }

    private fun copy(swapped: Boolean = this.swapped, steps: List<MoveStep> = this.steps) =
        CompositionSession(board, seat, displayedDice, turns, action, swapped, steps)

    companion object {
        const val SUBMIT_MOVE = "Submit Move"

        /** Sivun oma komennon nimi, sama teksti jota sivukin käyttää. */
        const val SWAP_DICE = "Swap Dice"

        /**
         * Synteettisten osoitteiden etuliitteet. Näillä ei ole vastinetta sivustolla, ja
         * juuri siksi ne alkavat sanalla joka ei ole polku: jos sieppaus joskus pettäisi,
         * jäsenyystarkistus hylkää ne eikä mikään pyyntö lähde.
         */
        const val LOCAL_STEP_PREFIX = "local:step:"
        const val LOCAL_UNDO = "local:undo"
        const val LOCAL_SWAP = "local:swap"

        internal fun start(
            board: BoardState,
            seat: Seat,
            displayedDice: List<Int>,
            turns: List<Turn>,
            action: String,
        ): CompositionSession =
            CompositionSession(board, seat, displayedDice, turns, action, swapped = false, steps = emptyList())
    }
}

object LocalComposition {
    /**
     * Aloittaa paikallisen kokoamisen, tai palauttaa null kun sitä ei voi tehdä
     * turvallisesti. Null ei ole virhe vaan pudotus vanhaan reittiin.
     *
     * Ehdot järjestyksessä: sivulla ei ole kesken jäänyttä palvelinkokoamista
     * (`Undo Move` kertoisi siitä), istuin ratkeaa pip-vahdista, sivulla on omat nopat
     * ja siirtolinkkejä, pohjaosoite löytyy linkeistä, ja **oraakkeli**: generaattorin
     * ensimmäisen askeleen joukko ja kirjaimet täsmäävät sivun omiin linkkeihin.
     *
     * [playForcedSteps] poimii pakolliset askeleet valmiiksi ([CompositionSession.prefillForced])
     * oraakkelin jälkeen: oraakkeli vertaa tyhjän session lähtöjä sivuun, ja esipoiminta
     * tehdään vain laudalle jonka sivu on jo hyväksynyt. Oletus on pois, koska tämä on
     * laitteen asetus (`docs/ASETUKSET.md` luku 3) eikä kokoamisen ominaisuus.
     *
     * [playGreedyBearoff] poimii ahneen uloskannon ([CompositionSession.prefillGreedy]) samoin
     * ehdoin. Se kokeillaan ensin, koska sen tulos sisältää pakolliset askeleet aina kun se
     * ei ole tyhjä; pakolliset poimitaan vain jos ahne ei poiminut mitään.
     */
    fun begin(
        board: BoardState,
        playForcedSteps: Boolean = false,
        playGreedyBearoff: Boolean = false,
    ): CompositionSession? {
        if (board.undoHref != null) return null
        val seat = board.resolveSeat() ?: return null
        val ownDice = board.dice.filter { it.owner == seat.color }.map { it.value }
        if (ownDice.size != 2) return null
        if (board.moves.isEmpty() && board.bar.none { it.color == seat.color && it.href != null }) return null

        val action = actionFrom(board) ?: return null
        val turns = legalTurns(board, seat, ownDice)
        if (turns.isEmpty()) return null

        val session = CompositionSession.start(board, seat, ownDice, turns, action)
        if (!oracleAgrees(board, seat, session)) return null
        val greedy = if (playGreedyBearoff) session.prefillGreedy() else session
        return if (greedy.steps.isEmpty() && playForcedSteps) greedy.prefillForced() else greedy
    }

    private fun actionFrom(board: BoardState): String? {
        val href = board.moves.firstOrNull()?.href
            ?: board.bar.firstOrNull { it.href != null }?.href
            ?: return null
        return href.substringBefore('?').takeIf { it.isNotBlank() }
    }

    /**
     * Sivun linkkijoukon ja generaattorin ensimmäisen askeleen vertailu, molempiin
     * suuntiin ja kirjaimineen.
     *
     * Vertailu vaatii täsmälleen saman joukon: puuttuva linkki tarkoittaisi että sivu
     * sallii jotain jota generaattori ei tunne, ylimääräinen että generaattori sallisi
     * jotain jota sivu ei tarjoa. Kumpikin on syy olla koskematta.
     */
    private fun oracleAgrees(
        board: BoardState,
        seat: Seat,
        session: CompositionSession,
    ): Boolean {
        val pageFroms = buildSet {
            board.moves.forEach { add(it.fromPoint) }
            if (board.bar.any { it.color == seat.color && it.href != null }) add(BAR_POINT)
        }
        if (pageFroms != session.legalFromPoints()) return false

        return board.moves.all { link ->
            link.code == moveLetter(link.fromPoint).toString()
        }
    }
}
