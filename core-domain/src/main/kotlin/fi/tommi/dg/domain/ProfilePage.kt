package fi.tommi.dg.domain

/**
 * Pelaajan profiilisivu (`/bg/user/<id>?days_to_view=...&active=1&finished=1`).
 *
 * Tämä on se sivu jolla ottelut oikeasti ovat. Top Page näyttää vain ne joissa on vuoro, ja
 * kun niitä ei ole yhtään, se ei sisällä ottelutaulukkoa lainkaan. Odottavat ottelut eivät
 * siis ole Top Pagen toisessa taulukossa vaan täällä.
 *
 * Sivu on **jonkun** pelaajan profiili, ei välttämättä kirjautuneen. Osoitteen voi rakentaa
 * kenelle tahansa, joten kentät on nimetty profiilin kohteen mukaan eikä omiksi. Se ettei
 * sivu itse kerro kumpi tapaus on kyseessä, on syy olla nimeämättä näitä `userName`ksi.
 */
data class ProfilePage(
    /**
     * Kenen profiili: nimi otsikosta ja numero sivun omista linkeistä.
     *
     * Polku jää tyhjäksi, koska tämä **on** se sivu johon polku osoittaisi.
     */
    val player: PlayerRef,
    /** Taulukko `Active games:`. */
    val activeMatches: List<Match>,
    /** Taulukko `Finished matches:`. Näillä on lisäksi `.mat`-vientilinkki. */
    val finishedMatches: List<Match>,
    /** Rating sivun sanoin, esim. "2219.9". Teksti eikä luku, kuten pelaajalistassa. */
    val ratingText: String? = null,
    /** Kokemuspisteet sivun sanoin. */
    val experienceText: String? = null,
    /**
     * Sivun oma esittelytaulukko: Real Name, Location, Home Page, Comment ja omalla
     * profiililla E-mail. Rivit ovat sellaisenaan sivun järjestyksessä, koska kentät
     * riippuvat siitä mitä pelaaja on täyttänyt eikä listaa voi kiinnittää koodiin.
     */
    val fields: List<ProfileField> = emptyList(),
    /** Polku sivun linkistä *active tournaments*. */
    val activeTournamentsPath: String? = null,
    /** Polku sivun linkistä *tournament wins*. */
    val tournamentWinsPath: String? = null,
    /**
     * Polku sivun omasta lajittelulinkistä (*Sort X's games by opponent*), joka on ainoa
     * reitti pelaajan otteluihin silloin kun profiili avattiin ilman kyselyparametreja:
     * ilman parametreja sivulla ei ole ottelutaulukoita lainkaan (mitattu 27.8.2026,
     * fixture `profile_page_games.html`). Linkeistä valitaan vastustajan mukaan lajitteleva,
     * koska jokin sivun kolmesta lajittelusta on pakko valita eikä lajittelematonta
     * muotoa ole sivulla tarjolla.
     */
    val gamesPath: String? = null,
    /**
     * Polku sivun linkistä *matches versus you*, joka on vain toisen pelaajan profiililla.
     * Sama sivumuoto `versus`-suodattimella, joten sama jäsennin lukee tuloksen.
     */
    val versusPath: String? = null,
    /**
     * Sivun oma *Quick message* -lomake (`POST /bg/sendmsg/<id>`), tai null jos sivulla ei
     * ole sellaista.
     *
     * **Tämä on keskustelun aloittaminen**, ja se on eri asia kuin vastaaminen. Jonon
     * viestisivulla sama lomake on vastaus juuri saapuneeseen viestiin ja elää vain
     * lukuhetken; täällä se on olemassa aina, koska profiilin voi avata milloin tahansa.
     * Sama tyyppi ja sama lukija (`QuickMessageForms`), eri merkitys.
     *
     * **Null omalla profiililla, ja se on sivun oma ero eikä sovelluksen sääntö.** Sivusto
     * ei tarjoa lomaketta itselleen lähettämiseen, joten kenttä katoaa omalta profiililta
     * itsestään eikä ruudun tarvitse tietää kenen profiili on kyseessä. Kumpi tapaus on,
     * ei muutenkaan ole luettavissa sivulta, ks. luokan oma kuvaus.
     */
    val messageForm: ReplyForm? = null,
    /**
     * Sivun oma ottelukutsu (`POST /bg/invite/new`), vain toisen pelaajan profiililla.
     * Ulkona rajauksena 27.8.–3.9.2026, ks. [InviteForm].
     */
    val inviteForm: InviteForm? = null,
    /**
     * Sivun oma sivuutus (`GET /bg/ignore`), vain toisen pelaajan profiililla. Napin
     * teksti kertoo suunnan, ks. [IgnoreForm].
     */
    val ignoreForm: IgnoreForm? = null,
)

/**
 * Yksi rivi profiilin esittelytaulukosta.
 *
 * Arvo on tekstinä silloinkin kun se on sivulla linkki (kotisivu, sähköposti): ruutu on
 * lukutilassa eikä avaa ulkoisia osoitteita, ja **profiilikenttiin voi kirjoittaa mitä
 * tahansa** (mitattu 27.8.2026: yhden pelaajan Real Name oli lista YouTube-linkkejä).
 */
data class ProfileField(
    val label: String,
    val text: String,
)
