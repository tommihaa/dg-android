package fi.tommi.dg.domain

/**
 * Pelaajan omat turnaukset: `/bg/userevent/<id>` (käynnissä) ja `/bg/userwins/<id>`
 * (voitetut).
 *
 * **Nämä ovat pelaajan omia, toisin kuin Tournament Hall**, joka on koko sivuston lista.
 * Kumpikin sivu on yksi taulukko ja saman muotoinen sisältö, joten ne ovat sama malli
 * kahdella listalla eivätkä kaksi mallia: [rows] on aina sen sivun rivit joka haettiin.
 */
data class PlayerTournaments(
    /** Kenen turnaukset: nimi ja profiilin polku otsikon linkistä. */
    val player: PlayerRef,
    val rows: List<PlayerTournamentRow>,
)

/**
 * Yksi rivi. Sarakkeet eroavat sivuittain, ja tyhjä kenttä kertoo kumpi sivu oli kyseessä:
 * käynnissä olevalla on [winsText] ja mahdollinen [activeMatchPath], voitetulla [whenText].
 */
data class PlayerTournamentRow(
    val name: String?,
    /** Turnauksen numero `/bg/event/`-linkistä. */
    val eventId: String?,
    /** Turnaussivun polku sellaisenaan. */
    val eventPath: String?,
    /** Voittojen määrä turnauksessa sivun sanoin, `/bg/userevent/`-sivulla. */
    val winsText: String?,
    /**
     * Käynnissä olevan ottelun **katselulaudan** polku, kun rivillä on `Active Game` -linkki.
     * Puuttuu riviltä silloin kun pelaajalla ei ole ottelua käynnissä kyseisessä
     * turnauksessa (kierros odottaa vastustajaa tai on jo pelattu).
     *
     * **Linkki ei kerro vuorosta.** Tässä luki 27.8.–15.9.2026 *"kun vuoroa ei ole kesken"*,
     * ja laiteajo 15.9.2026 osoitti sen vääräksi: linkki oli rivillä myös otteluissa joita
     * Top Page ei listannut, eli joissa vuoro oli vastustajalla. Siksi vastustaja löytyy
     * otteluluettelosta vain osalle riveistä ([activeMatchId]).
     *
     * Muoto on `/bg/game/<id>/`, siis ilman siirtonumeroa ja ilman `/list`iä: eri osoiteperhe
     * kuin turnauskaavion solut, jotka osoittavat siirtolistaan. Tässä luki 27.8.2026 alkaen
     * *"siirtolistan polku"*, ja se oli oletus eikä mittaus; korjattu 1.9.2026 kun sivu
     * katsottiin selaimella. Sivu on lauta ilman tekoja: navigointi `First`/`Prev`/`Next`/
     * `Last`, linkki `Move` pelattavalle sivulle ja `List of Moves` siirtolistalle
     * (`docs/KOHDE.md`).
     */
    val activeMatchPath: String?,
    /** Voiton ajankohta sivun sanoin, `/bg/userwins/`-sivulla. */
    val whenText: String?,
) {
    /**
     * Käynnissä olevan ottelun numero [activeMatchPath]ista, tai null kun linkkiä ei ole.
     *
     * Sivu ei nimeä vastustajaa (Tommin havainto 15.9.2026), mutta numero on sama jolla
     * ottelu on otteluluettelossa ([Match.id]), joten vastustaja luetaan sieltä. Luetaan
     * tässä eikä jäsentimessä, koska kyse on saman polun toisesta muodosta eikä sivun
     * uudesta tiedosta.
     */
    val activeMatchId: MatchId?
        get() = activeMatchPath
            ?.let { ACTIVE_MATCH_ID.find(it)?.groupValues?.get(1) }
            ?.let(::MatchId)

    private companion object {
        val ACTIVE_MATCH_ID = Regex("""/bg/game/(\d+)""")
    }
}
