package fi.tommi.dg.domain

/**
 * Ne sivuston asetukset jotka vaikuttavat lautaan, luettuna [SettingsPage]sta.
 *
 * Kenttiä on kolme eikä yksitoista, ja rajaus on mitattu eikä valittu
 * (`docs/ASETUKSET.md` luku 1): `Hide pip counts`, `Home boards on left side` ja
 * `Board Scheme`. Taustaväri jäi ulos Tommin päätöksellä 24.8.2026.
 *
 * Jokainen kenttä on kolmiarvoinen samasta syystä kuin vahtien tulokset: **puuttuva
 * tieto ei ole sama asia kuin pois päältä oleva asetus.** Null tarkoittaa ettei sivu
 * sanonut asiasta mitään tunnistettavaa, ja silloin lauta ei saa väittää asetusta
 * kumpaankaan suuntaan.
 */
data class SiteBoardSettings(
    /** `Hide pip counts`, tai null kun selitettä ei löytynyt sivulta. */
    val hidePips: Boolean?,
    /** `Home boards on left side`, tai null kun selitettä ei löytynyt sivulta. */
    val homeBoardsLeft: Boolean?,
    /** `Board Scheme` -radioryhmän valinta, tai null kun ryhmää tai valintaa ei ole. */
    val scheme: BoardScheme?,
) {
    companion object {
        /** Ei tietoa mistään kolmesta. Säilön lähtötila ennen ensimmäistä onnistunutta lukua. */
        val UNKNOWN = SiteBoardSettings(hidePips = null, homeBoardsLeft = null, scheme = null)
    }
}

/**
 * Lautaan vaikuttavat asetukset tästä sivusta.
 *
 * **Valintaruudut tunnistetaan selitteestä eikä numerosta**, sama peruste kuin
 * [SettingsPage.nameFor]issa: kenttien nimet ovat paljaita numeroita, ja numeroon
 * kovakoodattu tulkinta osuisi renumeroinnin jälkeen hiljaa väärään asetukseen. Jos
 * sivuston teksti muuttuu, tulos on null eikä väärä väite.
 *
 * Radioryhmän nimi `board` on sanallinen eikä numero, joten se luetaan nimellä; arvo
 * on lomakkeen numero, ja hakemistovastaavuus (arvo + 1) on mitattu 3.8.2026
 * (`docs/KOHDE.md`). Tuntematon arvo on null, ei arvaus.
 */
fun SettingsPage.siteBoardSettings(): SiteBoardSettings =
    SiteBoardSettings(
        hidePips = nameFor(HIDE_PIPS_LABEL)?.let { it in checked },
        homeBoardsLeft = nameFor(HOME_BOARDS_LEFT_LABEL)?.let { it in checked },
        scheme = when (radios[BOARD_SCHEME_GROUP]) {
            "0" -> BoardScheme.CLASSIC
            "1" -> BoardScheme.BLUE_WHITE
            "2" -> BoardScheme.MINI
            else -> null
        },
    )

private const val HIDE_PIPS_LABEL = "Hide pip counts"
private const val HOME_BOARDS_LEFT_LABEL = "Home boards on left side"
private const val BOARD_SCHEME_GROUP = "board"
