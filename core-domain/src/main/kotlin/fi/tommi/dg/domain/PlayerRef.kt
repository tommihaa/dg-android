package fi.tommi.dg.domain

/**
 * Viittaus yhteen pelaajaan sivun omin tiedoin: nimi, käyttäjänumero ja profiilin polku.
 *
 * **Nostettu 1.9.2026 kompositioauditoinnin havainnosta H7** (`docs/AUDITOINTI-KOMPOSITIO.md`).
 * Sama pelaaja esiintyi kymmenenä eri pari-muotona: `Opponent(id, name, profilePath)`,
 * `playerName`/`playerId`/`playerPath`, `userName`/`userId`, `authorName`/`authorId`,
 * `winnerName`/`winnerId` ja `sender`/`senderId`. Muodot eivät eronneet sisällöltään vaan
 * vain sanoiltaan, ja kutsupaikka joutui purkamaan kunkin erikseen vaikka teko oli sama:
 * avaa tämän pelaajan profiili tai kirjoita hänelle.
 *
 * **Rooli säilyy kentän nimessä, muoto ei ole enää roolin varassa.** Rivi jolla on useampi
 * osapuoli nimeää ne yhä rooleittain (`winner`, `author`, `opponent`), koska pelkkä `player`
 * kertoisi silloin vähemmän kuin sivu itse. Vaihtui se, että jokainen niistä on nyt tätä
 * samaa tyyppiä, jolloin sama apu kelpaa kaikille.
 *
 * **Kentät ovat nullable, koska sivu ei aina nimeä.** Ystävyysottelun rivillä ei ole
 * profiililinkkiä lainkaan, jolloin nimi ja numero puuttuvat molemmat. Tyhjä viittaus on
 * silti viittaus: se kertoo että sivulla on tämä rooli mutta sisältö jäi lukematta, mikä
 * on eri asia kuin rooli jota sivulla ei ole (esim. käynnissä olevan turnauksen voittaja,
 * jossa kenttä itse on null).
 *
 * Numeroa ei koota nimestä eikä nimeä numerosta. Molemmat luetaan sivun omasta linkistä,
 * sama sääntö kuin osoitteilla.
 */
data class PlayerRef(
    /** Pelaajan nimi sivun sanoin, tai null kun sivu ei nimennyt ketään. */
    val name: String? = null,
    /** Käyttäjänumero `/bg/user/`-linkistä, tai null kun linkkiä ei ole. */
    val userId: String? = null,
    /**
     * Profiilisivun polku sivun omasta linkistä.
     *
     * Luetaan eikä koota numerosta, vaikka muoto olisi arvattavissa: sivuston omat linkit
     * kantavat joskus kyselyn (`?days_to_view=...`) jota koottu osoite ei toistaisi.
     */
    val profilePath: String? = null,
) {
    /** Nimi näytettävässä muodossa. Tyhjä silloin kun sivu ei nimennyt ketään. */
    val displayName: String get() = name.orEmpty()

    /** Onko sivu nimennyt tämän pelaajan lainkaan. */
    val isNamed: Boolean get() = !name.isNullOrBlank()
}
