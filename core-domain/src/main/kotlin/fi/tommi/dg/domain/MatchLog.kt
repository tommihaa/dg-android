package fi.tommi.dg.domain

/**
 * Ottelun siirtohistoria (`/bg/game/<id>/<n>/list`, sivuston Review-sivu).
 *
 * **Olemassaolo purkaa 26.8.2026 rajauksen, ja purku on Tommin pyyntö 30.8.2026**
 * (*"myös päättyneeseen otteluun pitäisi saada linkki"*): siirtohistoria oli rajattu ulos
 * *toistaiseksi*, ja linkki päättyneeseen otteluun tarvitsee kohteen joka osaa näyttää
 * jotain. Tämä on se kohde, luettuna sivun omasta muodosta.
 *
 * Malli on tarkoituksella tekstiä eikä tulkintaa: heitot, siirrot ja kuutiotoimet kulkevat
 * sivun omina sanoina (`61: 13/7 8/7`, `Doubles => 2`, `Takes`, `Wins 1 point`) eikä niitä
 * jäsennetä pidemmälle. Ruutu näyttää mitä sivu sanoo; siirtojen purkaminen rakenteeksi on
 * eri työ ja odottaa tarvetta (analyysipolku, `docs/AVOIMET.md`).
 */
data class MatchLogPage(
    /** Sivun `<h3>`, esim. `March 26 Deja Vu, Round 3`. Tyhjä jos sivu ei kerro. */
    val heading: String,
    /**
     * Sivun oma pituusrivi kokonaisena, esim. `11 point match`, tai null jos riviä ei löydy.
     *
     * **`Text`-pääte on nimen korjaus 1.9.2026** (H7). Kenttä oli `matchLength` kuten
     * `Match`illa ja `BoardState`illa, mutta niillä arvo on `Int?` eli ottelun pituus
     * lukuna; tässä se on koko rivi sivun sanoin. Sama sana kahdelle eri asialle luki
     * väärin juuri siellä missä kolme mallia ovat vierekkäin, ja pääte on tämän moduulin
     * oma sääntö sivun sanoille (`ratingText`, `experienceText`, `graceText`).
     */
    val matchLengthText: String?,
    val games: List<MatchLogGame>,
)

data class MatchLogGame(
    /** Sivun oma väliotsikko, esim. `Game 1`. */
    val title: String,
    /** Pelin alun pistetilanne pelaajittain, esim. `tommih : 0` ja `opponent : 1`. */
    val scoreLeft: String,
    val scoreRight: String,
    val turns: List<MatchLogTurn>,
)

/**
 * Yksi siirtovuoro: rivinumero ja kummankin pelaajan sarake sivun tekstinä.
 *
 * Tyhjä sarake on sivun oma tyhjä eikä puute: avausvuorolla toinen puoli on tyhjä, ja
 * tanssivalla pelaajalla on heitto ilman siirtoa (`66:` ilman jatkoa).
 */
data class MatchLogTurn(
    val number: String,
    val left: String,
    val right: String,
)
