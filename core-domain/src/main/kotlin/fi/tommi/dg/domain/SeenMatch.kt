package fi.tommi.dg.domain

/**
 * Ottelu sellaisena kuin sovellus sen viimeksi näki: vastustaja ja kierros ottelun numerolla.
 *
 * **Tämä on sovelluksen oma muisti eikä sivulta haettua tietoa** (Tommin päätös 15.9.2026,
 * *"oma täydentyvä kanta paras"*). Turnaussivu `/bg/userevent/<id>` nimeää käynnissä olevan
 * ottelun vain numerolla, ja vastustaja sekä kierros ovat luettavissa vain otteluluettelosta
 * tai laudalta. Kumpaakaan ei haeta tätä varten: rivi kirjoitetaan silloin kun sivu on
 * käsillä muusta syystä, ja luetaan kun ottelu ei ole enää luettelossa.
 *
 * **Tieto voi olla vanhaa, ja se on hyväksytty.** Ottelu putoaa luettelosta kun vuoro siirtyy
 * vastustajalle, ja jos hän pelaa pelin loppuun, kierros on jo yhden edellä. Rivi kertoo
 * siis mitä viimeksi nähtiin eikä mitä sivustolla on nyt, ja ruudun on sanottava se.
 *
 * Vaihtoehto oli oman profiilin haku, joka listaa kaikki käynnissä olevat ottelut tuoreina.
 * Se olisi yksi pyyntö per avaus, ja Tommi valitsi muistin: kanta kasvaa yhden rivin per
 * ottelu, ja kate kasvaa pelatessa ilman että sivustolle lähtee mitään.
 */
data class SeenMatch(
    val matchId: MatchId,
    /** Vastustaja, tai tyhjä viittaus kun nähdyllä sivulla ei ollut nimeä. */
    val opponent: PlayerRef,
    /**
     * Kierros ilman sanaa, luettelosta muodossa `4/5` ja laudalta `4` kun kokonaismäärää ei
     * tiedetty. Sama muoto kuin [Match.round].
     */
    val round: String?,
    val matchLength: Int?,
    val eventName: String?,
    val seenAtEpochMillis: Long,
) {
    /**
     * Uudempi havainto vanhemman päälle: uusi kenttä voittaa kun se on olemassa, muuten vanha
     * jää. Lauta ei aina anna vastustajaa (oma nimi ei tiedossa) eikä kokonaismäärää, ja
     * silloin luettelosta kirjattu arvo on parempi kuin tyhjä.
     */
    fun mergedWith(newer: SeenMatch): SeenMatch = SeenMatch(
        matchId = matchId,
        opponent = if (newer.opponent.name != null) newer.opponent else opponent,
        round = newer.round ?: round,
        matchLength = newer.matchLength ?: matchLength,
        eventName = newer.eventName ?: eventName,
        seenAtEpochMillis = maxOf(seenAtEpochMillis, newer.seenAtEpochMillis),
    )
}
