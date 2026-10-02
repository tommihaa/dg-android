package fi.tommi.dg.app.ui

import android.net.Uri
import fi.tommi.dg.domain.Match
import fi.tommi.dg.domain.PendingAction

/** Otteluluettelo, sovelluksen aloituskohde. */
const val TOP_ROUTE = "top"

/**
 * Info-välilehti: sovelluksen manuaali, sivuston omat tekstit ja uloskirjautuminen.
 *
 * Parametriton, ja se on sama asia kuin `SettingsViewModel`in puuttuva polkuparametri:
 * kohteet ovat vakio-osoitteita eivätkä sivulta luettuja linkkejä, joten reitille ei ole
 * mitään välitettävää. Tästä syystä tämä ei tarvitse `BoardRoute`n koodausta eikä sen testiä.
 *
 * **Yksi reitti usealle kohteelle, ja se on tahallista** (`docs/UI.md` 26.8.2026).
 * Välilehden sisällä porautuminen tapahtuu paikallaan kuten Discussionin ketju, ei omalle
 * reitilleen. Omalla reitillään `tabFor` palauttaisi nullin, jolloin välilehtipalkki
 * katoaisi ja ruutu näyttäisi laudalta.
 *
 * **Asetukset eivät ole tämän välilehden kohteita 4.9.2026 alkaen** (ks.
 * [SETTINGS_ROUTE]). Rivi oli tässä siihen asti, ja se poistui koska Info on lukemista ja
 * asetukset ovat säätämistä. `HELP_ROUTE` ei palannut, koska manuaaliin on yhä yksi
 * sisäänkäynti.
 */
const val INFO_ROUTE = "info"

/**
 * Asetuslomake omana ruutunaan (Tommin päätös 4.9.2026, `docs/UI.md`).
 *
 * **Sisäänkäyntejä on yksi**, otteluluettelon yläpalkin `Settings`-tekstinappi. Reitti
 * oli aluksi kahden sisäänkäynnin takia, mutta Info-välilehden `Settings`-rivi poistui
 * samana päivänä (`InfoScreen`), joten oman reitin syy on nyt toinen: sisäänkäynti on
 * yläpalkissa eikä minkään välilehden listalla, ja paikallaan piirretty ruutu jäisi
 * Matches-välilehden sisään palkkeineen.
 *
 * Reitti on välilehtien ulkopuolella samoin kuin lauta ja porautumisruutu: palkkia ei
 * piirretä, ruutu kuluttaa tilapalkin insetin itse ja paluu on järjestelmän ele tai
 * otsikkorivin `Back`. Parametriton samasta syystä kuin [INFO_ROUTE].
 */
const val SETTINGS_ROUTE = "settings"

/**
 * Merkityt asemat omana ruutunaan (Tommin toive 15.9.2026 illalla, `docs/UI.md`).
 *
 * Sisäänkäynti on otteluluettelon otsakerivin linkki, ja ruutu on välilehtien
 * ulkopuolella samoin kuin asetukset: paluu on järjestelmän ele tai otsikkorivin `Back`.
 * Parametriton samasta syystä kuin [SETTINGS_ROUTE]: lista luetaan kannasta eikä sivulta,
 * joten reitille ei ole mitään välitettävää.
 */
const val MARKS_ROUTE = "marks"

/**
 * Viestiarkisto ja jonon haku.
 *
 * Parametriton, ja se on tässä ruudussa erityisen tarkoituksellista: jonon polku **ei** kulje
 * reitin yli. Osoite luetaan Top Pagen linkistä ja annetaan näkymämallille suoraan, koska
 * reitin yli kulkeva osoite olisi koodattava ja purettava, ja tämä on kuluttava osoite:
 * väärin purettu polku ei täällä tuottaisi virhettä vaan peruuttamattoman teon.
 */
const val MESSAGES_ROUTE = "messages"

/**
 * Game Lounge. Parametriton samasta syystä kuin [MESSAGES_ROUTE]: loungen polku luetaan
 * Top Pagen navigointipalkista ja annetaan näkymämallille virtana, ei reitin yli. Sivulla
 * olevat hyväksymislinkit ovat muuttavia tekoja, joten väärin purettu polku ei tuottaisi
 * virhettä vaan väärän teon.
 */
const val LOUNGE_ROUTE = "lounge"

/**
 * Keskustelupalsta. Parametriton samoin perustein: palstan polku ja ketjujen lukusivujen
 * polut (kauttaviivoineen ja fragmentteineen) eivät kulje reitin yli lainkaan, joten
 * toista [BoardRoute]n kaltaista koodausta ei tarvita.
 */
const val DISCUSSION_ROUTE = "discussion"

/**
 * Porautumisruutu: pelaajaprofiili, turnaussivu ja pelaajan turnauslistat.
 *
 * **Yksi reitti neljälle sivulle, eikä reitti kerro kumpi sivu on kyseessä.** Se on tämän
 * projektin mitattu perusfakta sovellettuna navigointiin: sivun laji ei ole pääteltävissä
 * pyydetystä osoitteesta (`docs/KOHDE.md`), joten laji luetaan vastauksesta `DgPages`illa.
 * Reitin ainoa tehtävä on kuljettaa polku, jonka kutsuja on lukenut sivun linkistä.
 *
 * Nämä eivät ole välilehtiä vaan porautumisia rivistä, samaa lajia kuin lauta (`docs/UI.md`
 * 26.8.2026), joten välilehtipalkkia ei piirretä ja paluu tapahtuu järjestelmän eleellä.
 *
 * Koodaus on [BoardRoute]n, ja syy on sama: polku sisältää kauttaviivoja ja voi sisältää
 * kyselyn ja fragmentin. Riski on tässä pienempi kuin laudalla, koska väärin purettu polku
 * tuottaa vain tuntemattoman sivun eikä väärää tekoa, mutta koodaus on silti samaa
 * toimitusta ja `DestinationsTest` vaatii siltä saman merkkijonon takaisin.
 */
object PageRoute {

    const val ARG_PATH = "path"

    const val PATTERN = "page/{$ARG_PATH}"

    /** Reitti sivulle jonka polku on luettu jonkin sivun linkistä. */
    fun of(path: String): String = "page/${BoardRoute.encode(path)}"
}

/**
 * Lautanäkymän reitti.
 *
 * **Tämä tiedosto on olemassa yhden riskin takia.** Pelipolku (`/bg/move/5302842/541`)
 * sisältää kauttaviivoja, ja reitti erottaa osansa juuri kauttaviivalla. Polku on siis
 * koodattava matkan ajaksi ja purettava perillä, ja se on täsmälleen sitä osoitteen
 * käsittelyä jota projekti muuten välttää: koko sovelluksen kantava sääntö on "lue linkki,
 * älä laske sitä", koska väärin käsitelty osoite ei tuota virhettä vaan väärän teon.
 *
 * Riski ei ole poistettavissa navigaatiokirjastoa käyttäessä, joten se muutetaan väitteeksi:
 * koodaus ja purku ovat omassa tiedostossaan ja `DestinationsTest` vaatii niiltä merkilleen
 * saman merkkijonon takaisin. Ilman sitä väärin purkautunut polku olisi hiljainen, ja
 * hiljaisuus on tässä projektissa se vikamuoto jota vastaan suojaudutaan.
 *
 * Ottelutunniste kulkee mukana erikseen, koska sivusto voi palauttaa eri ottelun kuin
 * pyydettiin. Se on vertailukohta eikä osoitteen rakennuspalikka.
 */
object BoardRoute {

    const val ARG_PATH = "path"
    const val ARG_MATCH = "match"

    /** Reittikaava, johon `NavHost` rekisteröi kohteen. */
    const val PATTERN = "board/{$ARG_PATH}?$ARG_MATCH={$ARG_MATCH}"

    /**
     * Reitti yhdelle ottelulle. Kutsuja antaa polun sellaisenaan sivun linkistä, eikä tämä
     * muuta siitä muuta kuin koodauksen.
     *
     * Kierros ei kulje reitillä. Se kulki 8.9.2026 illan ajan (`&round=3%2F5`), mutta kattoi
     * vain napautetun ottelun eikä `Next Game` -ketjussa tulleita; nyt luettelo muistaa
     * kierrokset tunnisteella (`ListedRounds`) ja lauta kysyy omansa sivun tunnisteella.
     */
    fun of(match: Match): String? {
        val path = match.playPath ?: return null
        return "board/${encode(path)}?$ARG_MATCH=${encode(match.id.value)}"
    }

    /**
     * Reitti odottavan teon otteluun.
     *
     * Sama muoto kuin [of], ja polku tulee samasta lähteestä: `PendingAction.boardPath` on se
     * sivun oma linkki jolla ruutu alun perin avattiin, tallennettuna sellaisenaan. Osoitetta
     * ei siis koota täälläkään, vaan se kulkee kannan läpi muuttumattomana.
     *
     * Ottelutunniste on nullable, koska kaikki muuttavat teot eivät liity otteluun. Ilman
     * tunnistetta reitiltä jätetään koko kyselyosa pois, jolloin [PATTERN]in oletusarvo
     * täyttää sen; tyhjä arvo olisi eri asia, koska se olisi vertailukohta jota ei ole.
     */
    fun of(action: PendingAction): String {
        val base = "board/${encode(action.boardPath)}"
        val match = action.matchId ?: return base
        return "$base?$ARG_MATCH=${encode(match.value)}"
    }

    /**
     * Koodaus reittiä varten. `Uri.encode` ilman sallittujen merkkien listaa koodaa myös
     * kauttaviivan, mikä on tässä koko pointti: koodattu arvo ei saa sisältää reitin omia
     * erotinmerkkejä.
     */
    fun encode(value: String): String = Uri.encode(value)

    /** Purku. Navigaatiokirjasto tekee tämän itse, ja tämä on sama toimitus testattavana. */
    fun decode(value: String): String = Uri.decode(value)
}
