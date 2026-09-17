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
)
