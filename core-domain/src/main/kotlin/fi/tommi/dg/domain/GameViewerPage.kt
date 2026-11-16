package fi.tommi.dg.domain

/**
 * Sivuston katselulauta (`/bg/game/<id>/` ja `/bg/game/<id>/<n>/<laskuri>`), sivun
 * `Active Game` -linkin kohde.
 *
 * **Rajaus 1.9.2026 purettiin 8.11.2026 Tommin suunnalla A.** Asema luetaan sivulta
 * ([board] on sama [BoardState] jonka lautasivu tuottaa) eikä lasketa siirtolistasta,
 * koska kirjattu raja on *"siirtolista näytetään sivun sanoin tulkitsematta siirtoja"*.
 * Liikkuminen kulkee sivun omien linkkien kautta, eikä seuraavaa osoitetta koota laskurista.
 *
 * Sivulla ei ole lomaketta eikä siirtolinkkejä. Navigoinnin neljä nimeä ovat sivulla aina,
 * mutta reunassa ne ovat pelkkää tekstiä: ensimmäisessä asemassa `First` ja `Prev`,
 * viimeisessä `Next` ja `Last` (mitattu 8.11.2026, `raakasivut/katselulauta-8-11`).
 * Linkitön nimi on null eikä keksitty polku.
 */
data class GameViewerPage(
    val board: BoardState,
    val firstPath: String?,
    val prevPath: String?,
    val nextPath: String?,
    val lastPath: String?,
    /** Sivun `List of Moves`, esim. `/bg/game/<id>/0/list`. */
    val listPath: String?,
)
