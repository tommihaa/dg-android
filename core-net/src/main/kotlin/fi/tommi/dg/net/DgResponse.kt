package fi.tommi.dg.net

/**
 * Verkkokerroksen ainoa ulospäin näkyvä tulos.
 *
 * Huomaa mitä listalla EI ole: HTTP-statuskoodia onnistumisen mittarina. DailyGammon
 * palauttaa uloskirjautuneelle käyttäjälle 200 OK ja login-lomakkeen, joten statuskoodi
 * ei erota onnistunutta hakua katkenneesta istunnosta.
 */
sealed interface DgResponse {

    /** Pyydetty sivu, todennetusti sisäänkirjautuneena. */
    data class Ok(val html: String) : DgResponse

    /**
     * Istunto oli katkennut ja uudelleenkirjautuminenkin palautti login-sivun.
     * Vasta tämä saa näkyä käyttäjälle: yksittäinen katkennut istunto korjataan hiljaa.
     */
    data object AuthFailed : DgResponse

    /**
     * Ei yhteyttä. Muuttava toiminto kuuluu jonoon, lukupyyntö välimuistiin.
     *
     * [cause] on sen poikkeuksen luokan nimi joka pyynnön kaatoi, esimerkiksi
     * `SocketTimeoutException` tai `UnknownHostException`. **Kenttä on kirjanpitoa eikä
     * logiikkaa:** mikään haara ei lue sitä eikä käyttäjälle näkyvä teksti muutu sen mukaan,
     * koska kaikki nämä tarkoittavat samaa asiaa siinä mielessä joka käyttäjää koskee, eli
     * ettei tiedetä ehtikö pyyntö perille.
     *
     * **Lisätty 10.8.2026, ja syy on että laji katosi.** Ennen tätä `catch (e: IOException)`
     * pudotti poikkeuksen kokonaan, joten aito yhteyskatko ja jokin muu IO-vika olivat
     * erottamattomia sekä ruudulla että jonon rivillä. Muoto on sama kuin
     * `Message.rawHeader`illa: tuntematonta ei arvata eikä pudoteta, se säilytetään siinä
     * muodossa jossa se saapui.
     *
     * Sanoma eikä poikkeusolio, koska tämä tyyppi kulkee näkymämalleihin ja kantaan asti:
     * `PendingAction.lastErrorText` on tekstikenttä, eikä poikkeusta voi tallentaa siihen.
     */
    data class Offline(val cause: String) : DgResponse

    data class ServerError(val code: Int) : DgResponse

    /**
     * Sivusto nukkuu: päivittäinen varmuuskopio on kesken ja jokainen pyyntö saa saman
     * ilmoitussivun (`DgPages.isSleepingPage`, mitattu 11.9.2026).
     *
     * Oma lajinsa eikä [ServerError], koska koodia ei ole: vastaus on HTTP 200. Eikä [Ok],
     * koska runko ei ole pyydetty sivu. Katko on lähes päivittäinen (`SUBSTANSSI.md`
     * kohta 49), joten tämä on normaalitila jonka ruutu sanoo omin sanoin, ei virhe.
     * Teko ei ole epäselvä kuten [Offline]ssa: palvelin vastasi, ja vastaus oli ilmoitus.
     */
    data object Sleeping : DgResponse
}
