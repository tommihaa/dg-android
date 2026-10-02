package fi.tommi.dg.domain

/**
 * Top Page kokonaisuutena.
 */
data class TopPage(
    /**
     * Kirjautunut käyttäjä: nimi tervehdyksestä, numero ja oman profiilin polku.
     *
     * **Profiilin polku on ainoa reitti odottaviin otteluihin**, koska Top Page näyttää vain
     * ne joissa on vuoro. Se luetaan sivun linkistä eikä koota käyttäjänumerosta, samasta
     * syystä kuin kirjautumisen `path`-kenttä ja laudan tilatunniste: kyselyparametrit ovat
     * palvelimen omat.
     */
    val user: PlayerRef,
    /**
     * `You have Messages!` -linkin polku sellaisenaan, tai `null` jos linkkiä ei ollut.
     *
     * Kertoo vain että osoitteessa on jotain odottamassa, **ei** sitä että joku olisi
     * kirjoittanut käyttäjälle. Sama ilmoitus näkyy myös esimerkiksi silloin kun joku on
     * voittanut turnauksen. Lajittelu on siis tehtävä vasta sisällön haettua, eikä tästä
     * saa päätellä henkilöviestiä.
     *
     * **Polku eikä lippu, ja se on 9.8.2026 tehty muutos.** Kenttä oli aiemmin `Boolean`,
     * jolloin ainoa tapa hakea jono olisi ollut kirjoittaa `/bg/nextgame` koodiin. Se on
     * juuri se osoitteen kokoaminen jota projekti välttää, ja painavimmassa mahdollisessa
     * paikassa: jonon haku kuluttaa kohteen, joten väärä osoite ei tuottaisi virhettä vaan
     * peruuttamattoman teon. Sama muoto kuin [profilePath] ja `Match.playPath`.
     */
    val messageQueuePath: String?,
    val matches: List<Match>,
    /**
     * Game Loungen polku navigointipalkin linkistä sellaisenaan, tai `null` jos linkkiä
     * ei ollut. Sama muoto ja sama syy kuin [messageQueuePath]illa: polku luetaan sivulta
     * eikä koota koodissa. Oletusarvo on `null`, koska navigointipalkki ei ole sivun
     * sisältöä ja vanhat kutsupaikat kuvaavat sivua jonka palkkia ei luettu.
     */
    val loungePath: String? = null,
    /** Keskustelupalstan polku navigointipalkista, samoin perustein kuin [loungePath]. */
    val forumPath: String? = null,
    /**
     * Pelaajan omien käynnissä olevien turnausten polku (`/bg/userevent/<id>`) sivun
     * alalaidan *active tournaments* -linkistä, tai null jos linkkiä ei ollut. Luettu
     * 3.9.2026 alkaen Loungen Tournaments-segmenttiä varten, samoin perustein kuin
     * [loungePath]: tunnus on linkissä eikä sitä kasata.
     */
    val activeTournamentsPath: String? = null,
) {
    /** Onko jotain odottamassa. Johdettu, jottei sama tieto ole kahtena kenttänä. */
    val hasMessageNotice: Boolean get() = messageQueuePath != null

    /**
     * Montako ottelua odottaa käyttäjän siirtoa (Tommin toive 31.8.2026: *"0 odottavaa"*).
     *
     * Lasketaan vain ne joista sivu sanoo vuoron (`myTurn == true`). `null` on
     * tietämättömyys eikä nolla, eikä sitä lasketa kumpaankaan suuntaan; ks. [Match.myTurn].
     * Johdettu samasta syystä kuin [hasMessageNotice]: luku on luettelon ominaisuus eikä
     * oma kenttä, joten se ei voi olla eri mieltä rivien kanssa.
     */
    val yourTurnCount: Int get() = matches.count { it.myTurn == true }
}
