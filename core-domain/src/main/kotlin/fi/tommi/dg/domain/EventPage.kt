package fi.tommi.dg.domain

/**
 * Turnaussivu `/bg/event/<id>`: säännöt ja kaavio.
 *
 * Sivu on cup-kaavio, eli taulukko jossa etenevä pelaaja on `ROWSPAN`illa venytetty solu.
 * Kaavio luetaan **ruudukkona** (3.9.2026 alkaen, aiemmin pelkkinä kierroslistoina):
 * jokainen sarake on yksi kierros, ja jokaisella solulla on rivipaikka ja korkeus
 * sivun omista `ROWSPAN`eista. Pariutus seuraa geometriasta: sarakkeen solu kattaa
 * täsmälleen ne edellisen sarakkeen solut joiden rivit osuvat sen väliin, ja ne kaksi
 * ovat kyseisen ottelun osapuolet. Mitään ei päätellä nimistä.
 */
data class EventPage(
    /** Turnauksen nimi otsikosta. */
    val name: String?,
    /** Säännöt sivun omilla otsikoilla: Game, Size, Time control. */
    val conditions: List<EventCondition>,
    val rounds: List<EventRound>,
    /**
     * Ruudukon korkeus riveinä, eli ensimmäisen kierroksen paikkojen määrä tyhjät mukaan
     * lukien. Nolla kun sivulla ei ole kaaviota.
     */
    val rows: Int = 0,
) {
    /**
     * Onko turnaus double repeat, luettuna `Game`-ehdosta. Mitattu muoto on
     * `double-repeat, 11 point matches.` (`raakasivut/event_page.html`, 27.8.2026), ja
     * tavallisen turnauksen ehto sanoo `backgammon`. Null kun sivulla ei ole `Game`-ehtoa,
     * jolloin kutsuja ei tiedä muunnelmaa eikä tämä väitä sitä.
     *
     * Tarve on `.sgf`-vienti: double repeat luopuu Crawfordista (`SUBSTANSSI.md` kohta 8),
     * eikä `.mat` kerro muunnelmaa, joten turnaussivu on ainoa varma lähde `RU[Crawford]`ille.
     */
    val doubleRepeat: Boolean?
        get() = conditions.firstOrNull { it.heading.equals("Game", ignoreCase = true) }
            ?.text?.contains("double-repeat", ignoreCase = true)

    /**
     * Kierrosmäärä `Size`-ehdosta, tai null kun ehtoa ei ole. Mitattu muoto on
     * `6 rounds, up to 64 players` kaikilla 33 tallennetulla turnaussivulla (2.10.2026).
     * Luetaan ehdosta eikä kaavion sarakkeista, koska ehto on sivun oma sana luvusta.
     * Turnauksen rarity-väri lasketaan tästä (`docs/UI.md` › *Turnausten rarity-värit*).
     */
    val roundCount: Int?
        get() = conditions.firstOrNull { it.heading.equals("Size", ignoreCase = true) }
            ?.text?.let { SIZE_ROUNDS.find(it)?.groupValues?.get(1)?.toIntOrNull() }

    /**
     * Montako ottelua vastahaarassa on pelattava ennen kuin pelaajan seuraava vastustaja
     * tiedetään, tai null kun pelaaja ei odota vastustajaa.
     *
     * Mitattu 25.9.2026 (`docs/KOHDE.md` › *Turnaussivu*): omien turnausten linkitön rivi
     * tarkoitti 28 sivulla 28:sta tätä tilaa. Pelaaja tunnistetaan ensimmäisen sarakkeen
     * profiililinkistä, koska myöhemmissä sarakkeissa on vain nimi ottelulinkkinä. Nimi
     * seurataan sarake kerrallaan niin pitkälle kuin se on ratkenneen ottelun voittaja.
     * Nimi seurataan myös lihavoimattomana, koska **vapaakierros** siirtää pelaajan
     * eteenpäin ilman ottelua: isoissa turnauksissa osa ensimmäisen sarakkeen paikoista on
     * tyhjiä, ja yksinäinen pelaaja on seuraavassa sarakkeessa pelkkänä linkkinä (mitattu
     * 25.9.2026, April 26 Deja Vu ja Tortoise Threers Champs #25). Seuraavan sarakkeen solu
     * kertoo tilan: tyhjä tarkoittaa odotusta, kesken oleva omaa ottelua ja toinen nimi
     * karsiutumista. Kahdessa jälkimmäisessä palautetaan null.
     *
     * Luku on vastahaaran ratkeamattomat ottelut: kesken olevat ja ne tyhjät paikat joihin
     * on tulossa kaksi pelaajaa. Paikka jonka alla on vain yksi pelaaja on vapaakierros eikä
     * ottelu, ja paikka jonka alla ei ole ketään ei ole mitään.
     */
    fun matchesBeforeOpponent(profilePath: String): Int? {
        val entrants = rounds.firstOrNull()?.entries ?: return null
        val me = entrants.firstOrNull { samePlayer(it.profilePath, profilePath) } ?: return null
        val row = me.top

        fun cellAt(column: Int, at: Int): EventEntry? {
            val round = rounds[column]
            return round.entries.firstOrNull { at >= it.top && at < it.top + round.span }
        }

        var reached = 0
        while (reached + 1 < rounds.size) {
            val cell = cellAt(reached + 1, row)
            if (cell == null || cell.inProgress || cell.label != me.label) break
            reached++
        }
        // Voittajasarake on viimeinen: siihen päässyt ei odota ketään.
        if (reached + 1 >= rounds.size) return null
        if (cellAt(reached + 1, row) != null) return null

        val span = rounds[reached].span
        val ownTop = row / span * span
        val blockTop = row / (2 * span) * (2 * span)
        val siblingTop = if (ownTop == blockTop) blockTop + span else blockTop

        /** Ratkeamattomat ottelut paikan alla, tai null kun paikan alla ei ole pelaajia. */
        fun unresolved(column: Int, top: Int): Int? {
            val cell = cellAt(column, top)
            if (cell != null) return if (cell.inProgress) 1 else 0
            if (column == 0) return null
            val half = rounds[column].span / 2
            val a = unresolved(column - 1, top)
            val b = unresolved(column - 1, top + half.coerceAtLeast(1))
            return when {
                a == null && b == null -> null
                a == null -> b
                b == null -> a
                else -> 1 + a + b
            }
        }

        return unresolved(reached, siblingTop)?.takeIf { it > 0 }
    }

    private fun samePlayer(a: String?, b: String): Boolean {
        if (a == null) return false
        val idA = USER_ID.find(a)?.groupValues?.get(1)
        val idB = USER_ID.find(b)?.groupValues?.get(1)
        return if (idA != null && idB != null) idA == idB else a == b
    }

    private companion object {
        val USER_ID = Regex("""/bg/user/(\d+)""")
        val SIZE_ROUNDS = Regex("""(\d+)\s+rounds?""", RegexOption.IGNORE_CASE)
    }
}

/** Yksi sääntökohta: sivun `<h4>`-otsikko ja sen jälkeinen teksti. */
data class EventCondition(
    val heading: String,
    val text: String,
)

/** Yksi kaavion sarake, esim. "Round 1" tai "Winner". */
data class EventRound(
    val title: String,
    val entries: List<EventEntry>,
    /**
     * Sarakkeen solun korkeus riveinä, sivun oma `ROWSPAN`. Mitattu 1, 2, 4, 8, ... eli
     * tuplaantuu kierroksittain; luetaan silti sivulta eikä lasketa, koska tulevien
     * kierrosten tyhjätkin solut kantavat sen.
     */
    val span: Int = 1,
)

/**
 * Yksi solu kaaviossa. Solu on jompaakumpaa lajia, eikä sivu erottele niitä muuten kuin
 * muotoilulla: **pelaaja joka on päässyt tähän kierrokseen** (linkki profiiliin) tai
 * **ottelu joka on ratkennut tai kesken** (linkki siirtolistaan).
 */
data class EventEntry(
    /** Solun teksti sivun sanoin. Voittajasolun kulmasulkeet on riisuttu. */
    val label: String,
    /** Profiilin polku, kun solu on pelaajalinkki. */
    val profilePath: String?,
    /** Ottelun siirtolistan polku, kun solu on ottelulinkki. */
    val matchPath: String?,
    /** Onko solu kesken oleva ottelu (`In Progress`). */
    val inProgress: Boolean,
    /**
     * Onko solu ratkenneen ottelun voittaja. Sivulla se on lihavoitu ja kulmasulkeissa,
     * eli sivun oma merkintätapa eikä pääteltyä.
     */
    val decided: Boolean,
    /** Solun ylin rivi ruudukossa, nollasta alkaen. Sarakkeen [EventRound.span] antaa korkeuden. */
    val top: Int = 0,
) {
    /**
     * Käyttäjänumero profiilin polusta, tai null kun solu ei ole pelaaja. Kaavio värittää
     * nimen tällä nähdyn ratingin mukaan (Tommi 26.9.2026).
     */
    val userId: String?
        get() = profilePath?.let { USER_ID.find(it)?.groupValues?.get(1) }

    private companion object {
        val USER_ID = Regex("""/bg/user/(\d+)""")
    }
}
