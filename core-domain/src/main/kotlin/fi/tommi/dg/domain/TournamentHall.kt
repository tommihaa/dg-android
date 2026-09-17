package fi.tommi.dg.domain

/**
 * Tournament Hall `/bg/thall`: koko sivuston turnauslista kahtena taulukkona.
 *
 * **Tämä ei ole pelaajan oma turnaustieto** (mitattu 26.8.2026): kaapatulla sivulla oli
 * 2848 aktiivista ja 340 päättynyttä riviä, ja Tommin oma nimi esiintyi kerran. Pelaajan
 * omat turnaukset ovat profiilin `/bg/userevent/<id>`:ssä, joka on eri sivu.
 */
data class TournamentHall(
    val active: List<HallTournament>,
    val finished: List<HallTournament>,
)

/**
 * Yksi turnausrivi. Sama tyyppi molemmissa taulukoissa, ja ero on siinä kummassa listassa
 * rivi on: aktiivisella [dateText] on alkamisaika ja voittajaa ei ole, päättyneellä
 * [dateText] on päättymisaika ja voittaja on rivillä.
 */
data class HallTournament(
    val name: String?,
    /** Turnauksen numero `/bg/event/`-linkistä. */
    val eventId: String?,
    /** Turnaussivun polku sellaisenaan. */
    val eventPath: String?,
    /** Aika sivun sanoin, esim. "2026-08-26 14:12:04". Merkitys tulee listasta. */
    val dateText: String?,
    /**
     * Voittaja sivun omasta linkistä, tai null kun turnaus on yhä käynnissä.
     *
     * Null ja tyhjä viittaus ovat eri asioita: null tarkoittaa ettei riville kuulu
     * voittajaa lainkaan, tyhjä sitä että voittajasarake oli olemassa muttei luettavissa.
     */
    val winner: PlayerRef?,
)
