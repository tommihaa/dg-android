package fi.tommi.dg.domain

/**
 * Vastustajan edellinen siirto nuolina (Tommin tilaus ja kuittaus 29.9.2026 illalla,
 * `docs/AVOIMET.md` › siirtonuolet): *"vastustajan edelliset siirtonuolet olisi kiva nähdä
 * ennen omaa siirtoa"*.
 *
 * **Lähde on siirtolista eikä lauta.** Lautasivu näyttää vain aseman. Edellisen laudan
 * vertailu ei erottaisi `8/4 4/2`:ta `8/2`:sta. Siirtolista (`/bg/game/<id>/<n>/list`, sivun
 * oma `<<Review Game`) kertoo jokaisen askeleen. Sen merkintä on mitattu aidosta sivusta
 * (`raakasivut/game_list.html`, 29.9.2026): jokainen askel on oma `a/b`-parinsa siirtäjän omassa
 * suunnassa, lyönti on `*` kohteen perässä, muuri on `25` ja uloskanto `0`. Tuplat kirjoitetaan
 * auki (`13/10 10/7 8/5 8/5`) eikä `(2)`-merkintää käytetä.
 */
data class LoggedStep(
    /** Lähtö siirtäjän omana pisteenä, [BAR_POINT] = muuri. */
    val from: Int,
    /** Kohde siirtäjän omana pisteenä, 0 = kannettu ulos. */
    val to: Int,
    val hit: Boolean,
)

private val STEP = Regex("""(\d+)/(\d+)(\*?)""")

/**
 * Siirtolistan yhden puolen askeleet sivun tekstistä (`62: 24/18 9/7`), tai tyhjä kun puolella
 * ei ole siirtoa: pelkkä heitto, tanssi (`66:` ilman jatkoa) tai kuutiotoimi
 * (`Doubles => 2`, `Takes`). Kuutiotoimen luku ei ole askel, koska siinä ei ole kauttaviivaa.
 */
fun parseLoggedSteps(side: String): List<LoggedStep> =
    STEP.findAll(side).mapNotNull { match ->
        val from = match.groupValues[1].toInt()
        val to = match.groupValues[2].toInt()
        if (from !in 1..BAR_POINT || to !in 0..24 || to >= from) {
            null
        } else {
            LoggedStep(from, to, match.groupValues[3] == "*")
        }
    }.toList()

/**
 * Pelaajan [player] viimeinen siirto ottelun viimeisessä pelissä, **vain jos se on listan
 * viimeinen siirto**. Muuten null.
 *
 * Puoli luetaan pelin pistetilanneriviltä (`alpha : 0`), koska sarakkeen paikka riippuu
 * siitä kumpi aloitti ottelun eikä siitä kumpi katsoo. Jos kumpikaan nimi ei täsmää, tulos
 * on null eikä arvaus.
 *
 * Toisen pelaajan solu saa olla viimeisen siirron perässä vain ilman askelia: oma heitto
 * (`43:`) tulee listaan `Roll Dice`n jälkeen ennen kuin siirto on lähetetty. Jos toisella on
 * sen jälkeen askelia, vastustajan siirto ei ole enää edellinen. Silloin ei piirretä mitään.
 * Kuutiotoimi tai tanssi viimeisenä antaa myös nullin: siirtoa ei ollut.
 */
fun MatchLogPage.lastMoveOf(player: String): List<LoggedStep>? {
    val game = games.lastOrNull() ?: return null
    val name = player.trim()
    if (name.isEmpty()) return null
    fun owner(score: String) = score.substringBeforeLast(':').trim()
    val left = when {
        owner(game.scoreLeft).equals(name, ignoreCase = true) -> true
        owner(game.scoreRight).equals(name, ignoreCase = true) -> false
        else -> return null
    }
    val cells = game.turns.flatMap { listOf(true to it.left, false to it.right) }
        .filter { (_, text) -> text.isNotBlank() }
    val last = cells.indexOfLast { (isLeft, _) -> isLeft == left }
    if (last < 0) return null
    if (cells.drop(last + 1).any { (_, text) -> parseLoggedSteps(text).isNotEmpty() }) return null
    return parseLoggedSteps(cells[last].second).takeIf { it.isNotEmpty() }
}

/**
 * Yltääkö lista laudan hetkeen: tosi vain kun listan viimeinen tilalaskuri on enintään yhden
 * laudan `Move`-luvun alla. Tuntematon kumpi tahansa on epätosi, jolloin nuolia ei piirretä.
 *
 * **Syy on mitattu 30.9.2026** (Tommin havainto: samat nuolet toistuivat vuorosta toiseen).
 * Lista oli laudasta 13 tilaa jäljessä, joten sen viimeinen vastustajan solu oli vanha
 * `11/5 8/5`. Purkutarkistus [opponentArrows]issa ei huomannut sitä, koska kohdepisteessä oli
 * yhä vastustajan nappuloita. Tarkistus kertoo vain että askeleet *voivat* olla laudalla, ei
 * että ne ovat viimeisimmät.
 *
 * Marginaali yksi on sama mittaus: asema 915 oli vastustajan siirron jälkeen ja 916 oma
 * heitto samassa asemassa. Tuore lista päättyy siis vähintään laskuriin `Move − 1`.
 */
fun MatchLogPage.reaches(boardMove: Int?): Boolean {
    val last = lastState ?: return false
    return boardMove != null && last >= boardMove - 1
}

/**
 * Askeleet nuolina tällä laudalla, tai null kun ne eivät sovi siihen.
 *
 * Lauta on siirron **jälkeen**, joten askeleet puretaan ensin taaksepäin siirtoa edeltävään
 * asemaan ja simuloidaan sitten eteenpäin samalla tavalla kuin oma kokoaminen
 * ([CompositionSession.arrows]): häntä on lähtöpisteen päällimmäinen, kärki kohteen seuraava
 * vapaa paikka. Purku on samalla tarkistus. Jos yhdenkin askeleen kohteessa ei ole
 * vastustajan nappulaa, lista ja lauta ovat eri hetkistä, eikä nuolia piirretä.
 *
 * Pisteet käännetään sivun numeroiksi istuimesta: vastustajan koti on vastapäätä omaa, joten
 * sen piste `d` on sivulla `25 − d` kun oma koti on matalissa numeroissa, muuten `d`.
 */
fun opponentArrows(board: BoardState, seat: Seat, steps: List<LoggedStep>): List<MoveArrow>? {
    if (steps.isEmpty()) return null
    val color = if (seat.color == CheckerColor.YELLOW) CheckerColor.BLUE else CheckerColor.YELLOW
    fun page(point: Int) = if (seat.homeIsLow) 25 - point else point

    val count = IntArray(BAR_POINT + 1)
    board.points.filter { it.owner == color }.forEach { count[it.number] = it.count }
    var bar = board.bar.firstOrNull { it.color == color }?.count ?: 0
    var off = board.borneOff.firstOrNull { it.color == color }?.count ?: 0

    for (step in steps.asReversed()) {
        if (step.to == 0) {
            if (off == 0) return null
            off--
        } else {
            val target = page(step.to)
            if (count[target] == 0) return null
            count[target]--
        }
        if (step.from == BAR_POINT) bar++ else count[page(step.from)]++
    }

    return steps.map { step ->
        val tail = if (step.from == BAR_POINT) {
            bar--
            ArrowEnd.OnBar(bar)
        } else {
            val from = page(step.from)
            count[from]--
            ArrowEnd.OnPoint(from, count[from])
        }
        val head = if (step.to == 0) {
            ArrowEnd.Off
        } else {
            val to = page(step.to)
            ArrowEnd.OnPoint(to, count[to]).also { count[to]++ }
        }
        MoveArrow(tail, head)
    }
}
