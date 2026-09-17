package fi.tommi.dg.net

import fi.tommi.dg.domain.SettingsPage
import fi.tommi.dg.domain.SettingsUpdate
import fi.tommi.dg.domain.update
import fi.tommi.dg.scrape.DgPages
import fi.tommi.dg.scrape.SettingsParser

/**
 * Asetusten luku ja kirjoitus live-ajoissa.
 *
 * **Tämä on tarkoituksella yksi kopio.** Asetuslomake on ainoa kohta jossa väärin luettu
 * sivu tuhoaa tilan jota ei ole enää missään, joten kaksi rinnakkaista toteutusta samasta
 * logiikasta olisi juuri se riski jonka `SettingsParser` poisti. Jos toinen live-testi
 * tarvitsee tätä, se kutsuu näitä eikä kirjoita omaansa.
 *
 * Varsinainen jäsennys asuu `SettingsParser`issa, koska live-testiä ei voi ajaa ilman
 * tunnuksia eikä se siis todenna itseään. Täällä on vain se osa joka vaatii verkon.
 */

internal fun DgClient.okOrFail(path: String): String {
    val response = fetch(path)
    check(response is DgResponse.Ok) { "Haku epäonnistui ($path): ${response::class.simpleName}" }
    return response.html
}

/** Lukee asetussivun ja varmistaa että vastaus oikeasti oli asetussivu. */
internal fun DgClient.liveSettings(): SettingsPage {
    val html = okOrFail(DgPages.SETTINGS_PATH)
    // Tunnistus ajetaan jäsentimen sisällä (`DgPages`), joten null on tässä sama väite
    // kuin erillinen `check` ennen 1.9.2026: vastaus ei ollut asetussivu.
    return checkNotNull(SettingsParser.parse(html)) { "Vastaus ei ollut asetussivu" }
}

/**
 * Lähettää **koko** asetuslomakkeen.
 *
 * Osittaista lähetystä ei ole eikä voi olla: rastittamaton valintaruutu ei lähde mukana
 * lainkaan, joten palvelin ei erota "tätä ei muutettu" ja "tämä otettiin pois" toisistaan.
 * Siksi kutsuja antaa aina koko tilan, ja muutettava kohta johdetaan alkutilasta.
 *
 * **Kokoaminen ja sen vartiot asuvat 25.8.2026 alkaen tuotantokoodissa** (`SettingsPage.update`
 * `core-domain`issa). Tämä funktio on nyt vain live-ajon kuori, ja se on `docs/ASETUKSET.md`
 * luvun 5 ehto: yksi kopio pysyy yhtenä kopiona, ja live-testi kutsuu tuotantokoodia eikä
 * säilytä omaansa. Aiemmin logiikka oli täällä, koska kiertokoe tarvitsi sen ennen kuin
 * ruutua oli olemassa.
 *
 * Lähetystä edeltävä uusi luku on tässä sama teko kuin ruudulla: [original] on se sivu josta
 * tila luettiin, ja [latest] se jolle lähetetään. Live-ajossa ne ovat lähes aina samat, ja
 * juuri siksi tämä on halpa tapa pitää kutsutapa samana molemmilla puolilla.
 *
 * @param original sivulta luettu alkutila, myös se osa jota ei muuteta
 * @param checked rastitettavat valintaruudut kokonaisuudessaan
 * @param radios radioryhmien valinnat kokonaisuudessaan
 */
internal fun DgClient.writeSettings(
    original: SettingsPage,
    checked: Set<String> = original.checked,
    radios: Map<String, String> = original.radios,
    latest: SettingsPage = original,
) {
    val update = original.update(latest, checked = checked, radios = radios)
    check(update is SettingsUpdate.Ready) { "Lähetys pysäytettiin ennen verkkoa: $update" }

    val response = send(update.submission)
    check(response is DgResponse.Ok) {
        "Asetusten tallennus epäonnistui: ${response::class.simpleName}"
    }
}
