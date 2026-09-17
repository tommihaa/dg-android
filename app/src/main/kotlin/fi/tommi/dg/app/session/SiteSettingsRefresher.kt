package fi.tommi.dg.app.session

import fi.tommi.dg.app.PageFetcher
import fi.tommi.dg.domain.siteBoardSettings
import fi.tommi.dg.net.DgResponse
import fi.tommi.dg.scrape.DgPages
import fi.tommi.dg.scrape.SettingsParser

/**
 * Asetusten avaushaku, omana omistajanaan (`docs/ASETUKSET.md` luku 2, ehto 1).
 *
 * Haku ei kuulu `TopViewModel`ille, koska se väittää hakevansa oma-aloitteisesti vain Top
 * Pagen ja `TopViewModelTest` todistaa väitteen kirjaamalla jokaisen polun. Tämä luokka on
 * se toinen oma-aloitteinen haku, ja sen kohde on yhtä lailla vakio: `/bg/profile` on
 * kanonissa kuluttamaton, eikä polkua voi antaa ulkopuolelta.
 *
 * Luvun 2 kolme ehtoa näkyvät tässä suoraan: haulla on oma omistaja (tämä), epäonnistunut
 * haku ei kirjoita säilöön mitään (vanha lukema säilyy), ja kutsuja ajaa tämän taustalla
 * eikä odota tulosta (lauta piirtyy sillä mitä säilössä on).
 *
 * Katkennut istunto ohitetaan hiljaa samasta syystä kuin verkkovirhe: tunnusvaraston
 * omistaa `TopViewModel`, ja kahdesta poistajasta seuraisi kaksi totuutta samasta asiasta.
 */
class SiteSettingsRefresher(
    private val pages: PageFetcher,
    private val store: SiteSettingsStore,
) {

    /** Yksi haku, kutsuttava IO-säikeeltä. Kirjoittaa säilöön vain onnistuneesta luvusta. */
    fun refresh() {
        val response = pages.fetch(DgPages.SETTINGS_PATH) as? DgResponse.Ok ?: return
        // Null tarkoittaa väärää sivua, ja väärästä sivusta luettu tyhjä tulos säilöttynä
        // väittäisi kaikkia asetuksia tuntemattomiksi. Sääntö asuu jäsentimessä eikä tässä
        // kommentissa, ks. `DgPages`.
        val page = SettingsParser.parse(response.html) ?: return
        store.save(page.siteBoardSettings())
    }
}
