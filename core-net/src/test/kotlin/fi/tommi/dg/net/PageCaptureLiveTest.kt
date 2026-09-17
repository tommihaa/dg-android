package fi.tommi.dg.net

import fi.tommi.dg.scrape.ForumParser
import fi.tommi.dg.scrape.TopPageParser
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import java.io.File

/**
 * Kaappaa sivuja jäsentimien kirjoittamista varten.
 *
 * **Vain sivuja jotka voi hakea turvallisesti.** Tässä ei saa hakea `/bg/nextgame`:a eikä
 * mitään osoitetta jossa on `?submit=`-parametri: edellinen kuluttaa viestijonoa ja
 * jälkimmäinen tekee siirron. Ne sivut Tommi tallentaa itse selaimesta, ks.
 * `raakasivut/LUEMINUT.md`.
 *
 * Sääntö ei jää kommentin varaan: jokainen haku kulkee [safeFetch]in läpi, joka kaataa
 * testin ennen verkkoa jos polussa on kuluttava tuntomerkki. Kommentti unohtuu kun polku
 * luetaan sivulta eikä kirjoiteta käsin, tarkistus ei unohdu.
 *
 * Lautasivun lataus ilman `?submit`-parametria on pääteltävästi sivuvaikutukseton, koska
 * nopan heitto ja siirto tapahtuvat nimenomaan sillä parametrilla. Tommi hyväksyi haun
 * 29.7.2026.
 */
@Tag("live")
class PageCaptureLiveTest {

    @Test
    fun `lautasivu kaapataan jäsennystä varten`() {
        val client = clientOrSkip()

        val top = client.safeFetch("/bg/top")

        // Tilatunniste vanhenee, joten polku otetaan tuoreesta listasta eikä aiemmasta
        // kaappauksesta.
        val playPath = TopPageParser.parse(top)!!
            .matches
            .firstNotNullOfOrNull { it.playPath }
        assumeTrue(playPath != null, "Yhdessäkään ottelussa ei ollut Play-linkkiä")

        save("move_board.html", client.safeFetch(playPath!!))
    }

    /**
     * Kaappauslistan 3: Top Page hetkellä jolloin vuoro ei ole omalla kohdalla.
     *
     * `docs/KOHDE.md` nimeää aukon "ei nähty vielä" ja olettaa toisen taulukon, jonka captionia
     * ei tiedetä. Mitattu 1.8.2026: **toista taulukkoa ei ole.** Kun siirrettäviä otteluita
     * ei ole yhtään, sivulla ei ole taulukkoa lainkaan vaan lause "There are no matches
     * where you can move." ja linkki omaan profiiliin. Odottavat ottelut ovat siis eri
     * sivulla, ei toisessa taulukossa.
     *
     * Sekamuoto (osa siirrettävissä, osa ei) on yhä näkemättä, ja vasta se kertoo onko
     * captioneja kaksi. Siksi molemmat tilat tallennetaan eri nimille: tyhjä sivu ei saa
     * mennä `top_page_odottaa.html`:n paikalle, tai seuraava sessio luulee aukkoa suljetuksi.
     */
    @Test
    fun `top page kaapataan kun vuoro ei ole omalla kohdalla`() {
        val client = clientOrSkip()
        val html = client.safeFetch("/bg/top")

        val captions = Jsoup.parse(html).select("table:has(caption) caption")
            .map { it.text() }
            .filter { it.contains("match", ignoreCase = true) }
        println("Otteluataulukoiden captionit: $captions")

        val waiting = captions.filterNot { it.contains("can move", ignoreCase = true) }
        if (waiting.isNotEmpty()) {
            println("Odottavan taulukon caption: $waiting")
            save("top_page_odottaa.html", html)
            return
        }

        val empty = html.contains("no matches where you can move", ignoreCase = true)
        assumeTrue(empty) {
            "Kaikissa otteluissa on yhä oma vuoro. Tämä syntyy pelkällä odottamisella, " +
                "aja uudestaan myöhemmin."
        }
        save("top_page_tyhja.html", html)
    }

    /**
     * Kaappauslistan 8: ottelun siirtohistoria `/bg/game/<id>/<n>/list`.
     *
     * Polku luetaan Review-linkistä eikä koota itse, samasta syystä kuin lautasivun
     * tilatunniste luetaan sivulta. Lähde on profiilisivu eikä Top Page, koska Top Page on
     * tyhjä silloin kun vuoro ei ole omalla kohdalla.
     */
    @Test
    fun `ottelun siirtohistoria kaapataan`() {
        val client = clientOrSkip()
        val profile = client.fetchProfile()

        val reviewPath = profile.section("Active games")
            ?.selectFirst("a[href~=/bg/game/\\d+]")
            ?.attr("href")
        assumeTrue(reviewPath != null, "Profiililla ei ollut yhtään käynnissä olevaa ottelua")

        save("game_list.html", client.safeFetch(reviewPath!!))
    }

    /**
     * Kaappauslistan 9: päättynyt ottelu ja sen `.mat`-vienti.
     *
     * Mitattu 1.8.2026: vienti ei ole linkki ottelun sisällä vaan **oma reittinsä**
     * `/bg/export/<id>`, ja se näkyy profiilisivun taulukossa `Finished matches:`
     * Review-linkin rinnalla. Käynnissä olevilla otteluilla Export-saraketta ei ole.
     *
     * Kaksi tiedostoa, koska ne vastaavat eri kysymykseen: `match_over.html` kertoo miltä
     * päättynyt ottelu näyttää sivustolla, `match_export.mat` on se vakaampi muoto josta
     * arkisto kannattaa jäsentää.
     */
    @Test
    fun `päättynyt ottelu ja sen vienti kaapataan`() {
        val client = clientOrSkip()
        val finished = client.fetchProfile().section("Finished matches")
        assumeTrue(finished != null, "Profiililla ei ollut Finished matches -taulukkoa")

        val reviewPath = finished!!.selectFirst("a[href~=/bg/game/\\d+]")?.attr("href")
        assumeTrue(reviewPath != null, "Päättyneellä ottelulla ei ollut Review-linkkiä")
        save("match_over.html", client.safeFetch(reviewPath!!))

        val exportPath = finished.selectFirst("a[href~=/bg/export/\\d+]")?.attr("href")
        assumeTrue(exportPath != null, "Päättyneellä ottelulla ei ollut Export-linkkiä")
        save("match_export.mat", client.safeFetch(exportPath!!))
    }

    /**
     * Kaappauslistan 10: Tournament Hall `/bg/thall`.
     *
     * Sivua ei ole nähty kertaakaan, ei fixturena eikä proxy-kaappauksena: se on tunnettu
     * vain linkkinä loungen navigointipalkissa. Siksi tämä haetaan ennen kuin sivusta
     * päätetään mitään. Tommi pyysi haun 26.8.2026.
     *
     * Polku luetaan loungen linkistä eikä koota vakiosta, samasta syystä kuin muuallakin
     * tällä listalla: sivuston oma linkki on ainoa lähde joka ei ole arvaus. Lounge itse on
     * pelkkää lukua; sen muuttavat teot ovat `?action=`-linkeissä joita tämä ei koske.
     */
    @Test
    fun `tournament hall kaapataan`() {
        val client = clientOrSkip()

        val hallPath = Jsoup.parse(client.safeFetch("/bg/lounge"))
            .selectFirst("a[href^=/bg/thall]")
            ?.attr("href")
        assumeTrue(hallPath != null, "Loungella ei ollut Tournament Hall -linkkiä")

        save("tournament_hall.html", client.safeFetch(hallPath!!))
    }

    /**
     * Politics-palstan indeksi `/bg/forum2/politics`.
     *
     * Sivua ei ole nähty kertaakaan, ei fixturena eikä proxy-kaappauksena: se on tunnettu
     * vain linkkinä General-palstan omalla palstarivillä. `ForumParser` tunnistaa ketjurivit
     * merkkijonosta `/bg/forum2/main/read/`, ja `main` on ilmeisesti General-palstan tunnus,
     * joten Politicsin ketjulinkkien muoto ratkaisee jäsentimen yleistyksen. Siksi tämä
     * haetaan ennen kuin koodiin kosketaan. Tommi pyysi haun 27.8.2026.
     *
     * Haku on pelkkää lukua eikä kuluta mitään: jonon kuluttaa `/bg/nextgame` ja New-merkin
     * nollaa ketjun lukusivu `/bg/forum2/<palsta>/read/<id>`, eikä tämä hae kumpaakaan.
     *
     * Polku luetaan General-sivun omasta palstarivistä eikä koota vakiosta, samasta syystä
     * kuin muuallakin tällä listalla.
     */
    @Test
    fun `politics-palstan indeksi kaapataan`() {
        val client = clientOrSkip()

        val politicsPath = Jsoup.parse(client.safeFetch("/bg/forum2"))
            .selectFirst("a[href^=/bg/forum2/politics]")
            ?.attr("href")
        assumeTrue(politicsPath != null, "General-palstalla ei ollut Politics-linkkiä")

        save("forum2_politics.html", client.safeFetch(politicsPath!!))
    }

    /**
     * Politics-palstan ketjusivu.
     *
     * **Tämä haku ei ole pelkkää lukua sivuston silmissä**: ketjun avaaminen nollaa sen
     * New-merkin Tommin tilillä, eikä merkkiä saa takaisin. Siksi tätä ei ajettu
     * 27.8.2026 kun palstan indeksi kaapattiin, vaan vasta Tommin erillisellä luvalla
     * samana päivänä.
     *
     * Kysymys jota varten sivu haetaan: onko ketjusivun rakenne sama kuin Generalilla, eli
     * kelpaako `ForumParser.parseThread` sellaisenaan. Ketju valitaan indeksin
     * viestimäärältään pienimmäksi, jotta nollattava New-merkki koskee mahdollisimman
     * pientä osaa palstasta.
     */
    @Test
    fun `politics-palstan ketju kaapataan`() {
        val client = clientOrSkip()

        val politicsPath = Jsoup.parse(client.safeFetch("/bg/forum2"))
            .selectFirst("a[href^=/bg/forum2/politics]")
            ?.attr("href")
        assumeTrue(politicsPath != null, "General-palstalla ei ollut Politics-linkkiä")

        val index = ForumParser.parseIndex(client.safeFetch(politicsPath!!))!!
        val thread = index.threads.minByOrNull { it.postCount ?: Int.MAX_VALUE }
        assumeTrue(thread != null, "Politics-palstalla ei ollut yhtään ketjua")
        println("Avataan ketju: ${thread!!.title} (${thread.postCount} viestiä)")

        save("forum2_politics_read.html", client.safeFetch(thread.readPath))
    }

    /**
     * Pelaajalista `/bg/plist`, Loungen kolmas kohde.
     *
     * Sivua ei ole nähty kertaakaan. Loungella siihen on kaksi reittiä: tavallinen linkki
     * (*List of Players*) ja hakulomake (`POST`, kenttä `like`). Tämä hakee vain linkin
     * takana olevan sivun, koska se on GET eikä lähetä mitään; hakutuloksen muoto on eri
     * kysymys ja odottaa omaa päätöstään.
     */
    @Test
    fun `pelaajalista kaapataan`() {
        val client = clientOrSkip()

        val listPath = Jsoup.parse(client.safeFetch("/bg/lounge"))
            .selectFirst("a[href^=/bg/plist]")
            ?.attr("href")
        assumeTrue(listPath != null, "Loungella ei ollut pelaajalistan linkkiä")

        save("player_list.html", client.safeFetch(listPath!!))
    }

    /**
     * Porautumisten neljä sivua: toisen pelaajan profiili, turnaussivu sekä profiilin omat
     * `active tournaments` ja `tournament wins`.
     *
     * Kaikki neljä ovat kyselyparametrittomia GETejä eivätkä kuluta mitään. Ne haetaan
     * samalla ajolla, koska ne ovat saman työn mittaus ja polut löytyvät toisistaan:
     * turnauslinkki on ottelurivillä, ja kaksi jälkimmäistä ovat oman profiilin linkkeinä.
     *
     * **Toisen pelaajan profiili on oma kysymyksensä**, ei sama sivu kuin oma: omalla
     * profiililla on Real Name, sähköposti ja kommentti, eikä ole mitattua tietoa siitä
     * mitä toisen sivulla näkyy. Sivu haetaan pelaajalistan ensimmäiseltä riviltä, koska
     * se on tuntematon pelaaja eikä kukaan jonka kanssa Tommilla on ottelu kesken.
     */
    @Test
    fun `porautumisten sivut kaapataan`() {
        val client = clientOrSkip()

        val listPath = Jsoup.parse(client.safeFetch("/bg/lounge"))
            .selectFirst("a[href^=/bg/plist]")
            ?.attr("href")
        assumeTrue(listPath != null, "Loungella ei ollut pelaajalistan linkkiä")
        val otherPath = Jsoup.parse(client.safeFetch(listPath!!))
            .selectFirst("a[href~=^/bg/user/\\d+]")
            ?.attr("href")
        assumeTrue(otherPath != null, "Pelaajalistalla ei ollut yhtään profiililinkkiä")
        save("user_profile_other.html", client.safeFetch(otherPath!!))

        // Oma profiili on jo listalla, mutta se haetaan tässä uudestaan kahden linkin
        // takia: ne ovat vain profiilisivulla eikä niiden osoitetta koota numerosta.
        val profile = client.fetchProfile()

        val eventPath = profile.selectFirst("a[href~=^/bg/event/\\d+]")?.attr("href")
        assumeTrue(eventPath != null, "Profiililla ei ollut yhtään turnauslinkkiä")
        save("event_page.html", client.safeFetch(eventPath!!))

        val eventsPath = profile.selectFirst("a[href*=/bg/userevent/]")?.attr("href")
        assumeTrue(eventsPath != null, "Profiililla ei ollut active tournaments -linkkiä")
        save("user_events.html", client.safeFetch(eventsPath!!))

        val winsPath = profile.selectFirst("a[href*=/bg/userwins/]")?.attr("href")
        assumeTrue(winsPath != null, "Profiililla ei ollut tournament wins -linkkiä")
        save("user_wins.html", client.safeFetch(winsPath!!))
    }

    /**
     * Toisen pelaajan pelilista: profiilin oma lajittelulinkki
     * `?sort_name=1&active=1&finished=1`.
     *
     * Kysymys jota varten sivu haetaan: toisen pelaajan profiili avautuu ilman
     * kyselyparametreja eikä silloin näytä yhtään ottelua (`user_profile_other.html`,
     * *There are no active games.*), ja sivun omat lajittelulinkit ovat ainoa reitti
     * niihin. Kelpaako tulossivulle `ProfileParser` sellaisenaan, on mittaamatta, ja
     * sama muoto on myös `matches versus you` -linkillä (sama osoite `versus`-parametrilla),
     * joka rajattiin 27.8.2026 ulos juuri mittaamattomuuden takia.
     *
     * Pelaajaksi valitaan oman profiilin ensimmäinen vastustaja eikä pelaajalistan
     * tuntematon, koska tyhjä pelilista ei vastaisi kysymykseen taulukoiden muodosta.
     * Haku on kuluttamaton GET: lajittelu ei muuta mitään palvelimella.
     */
    @Test
    fun `toisen pelaajan pelilista kaapataan`() {
        val client = clientOrSkip()

        val opponentPath = client.fetchProfile()
            .selectFirst("a[href~=^/bg/user/\\d+$]")
            ?.attr("href")
        assumeTrue(opponentPath != null, "Omalla profiililla ei ollut yhtään vastustajalinkkiä")

        val sortPath = Jsoup.parse(client.safeFetch(opponentPath!!))
            .selectFirst("a[href*=sort_name]")
            ?.attr("href")
        assumeTrue(sortPath != null, "Vastustajan profiililla ei ollut lajittelulinkkiä")

        save("user_profile_other_games.html", client.safeFetch(sortPath!!))
    }

    /**
     * Sivuston oma Help `/help` ja linkkisivu `/links.html`, Info-välilehden kaksi
     * puuttuvaa riviä (`docs/AVOIMET.md`, välilehtipäätös 26.8.2026).
     *
     * Kumpaakaan ei ole nähty kertaakaan: ne ovat tunnettuja vain navigointirivin
     * linkkeinä. Molemmat ovat kyselyparametrittomia GETejä eivätkä kuluta mitään; ne
     * eivät edes ala `/bg/`-polulla, eli ne ovat sivuston staattista osaa. Tommi hyväksyi
     * haun 29.8.2026 (tehtäväksianto: Info-välilehden puuttuvat rivit).
     *
     * Polut luetaan Top Pagen navigointiriviltä eikä kirjoiteta käsin, samasta syystä
     * kuin muuallakin tällä listalla.
     */
    @Test
    fun `sivuston help ja linkkisivu kaapataan`() {
        val client = clientOrSkip()
        val top = Jsoup.parse(client.safeFetch("/bg/top"))

        val helpPath = top.selectFirst("a[href^=/help]")?.attr("href")
        assumeTrue(helpPath != null, "Top Pagella ei ollut Help-linkkiä")
        save("site_help.html", client.safeFetch(helpPath!!))

        val linksPath = top.selectFirst("a[href^=/links]")?.attr("href")
        assumeTrue(linksPath != null, "Top Pagella ei ollut Links-linkkiä")
        save("site_links.html", client.safeFetch(linksPath!!))
    }

    /**
     * Palstan kaksi lomakesivua: uuden ketjun sivu `/bg/forum2/<palsta>/new` ja kommentin
     * lisäyssivu `/bg/forum2/<palsta>/add/<id>`. Tommi pyysi haun 3.9.2026 (postaus ja
     * vastaus sovelluksesta); kumpaakaan ei ole nähty, ne ovat tunnettuja vain linkkeinä
     * indeksin alalaidassa (`Add a New Thread`) ja ketjusivun lopussa (`Add a Comment`).
     *
     * Molemmat ovat kyselyparametrittomia GETejä: lomakkeen näyttäminen ei lähetä mitään.
     * Kommenttisivun ketju valitaan niin ettei New-merkkiä kuluteta: ketjusivua ei avata,
     * vaan `add`-polku johdetaan indeksin `read`-polusta, jonka rakenne on mitattu
     * 27.8.2026 (`raakasivut/forum2_politics_read.html`, `read/64165` ja `add/64165`).
     * Ketjuksi otetaan sellainen jolla ei ole New-merkkiä, jotta arvaus polun muodosta
     * ei voi maksaa mitään vaikka sivusto avaisikin ketjun.
     */
    @Test
    fun `palstan lomakesivut kaapataan`() {
        val client = clientOrSkip()

        val politicsPath = Jsoup.parse(client.safeFetch("/bg/forum2"))
            .selectFirst("a[href^=/bg/forum2/politics]")
            ?.attr("href")
        assumeTrue(politicsPath != null, "General-palstalla ei ollut Politics-linkkiä")
        val indexHtml = client.safeFetch(politicsPath!!)

        val newPath = Jsoup.parse(indexHtml)
            .selectFirst("a[href~=^/bg/forum2/[^/]+/new$]")
            ?.attr("href")
        assumeTrue(newPath != null, "Politics-palstalla ei ollut Add a New Thread -linkkiä")
        save("forum2_politics_new.html", client.safeFetch(newPath!!))

        // Luettu ketju haetaan kummaltakin palstalta: Politicsilla ei 3.9.2026 ollut
        // yhtään, koska sen ainoa avattu ketju oli jo pudonnut indeksistä.
        val thread = (ForumParser.parseIndex(indexHtml)!!.threads +
            ForumParser.parseIndex(client.safeFetch("/bg/forum2"))!!.threads)
            .filterNot { it.isNew }
            .minByOrNull { it.postCount ?: Int.MAX_VALUE }
        assumeTrue(thread != null, "Kummallakaan palstalla ei ollut yhtään luettua ketjua")
        val addPath = thread!!.readPath.substringBefore('#').replace("/read/", "/add/")
        println("Kommenttisivu ketjuun: ${thread.title} ($addPath)")
        save("forum2_add.html", client.safeFetch(addPath))
    }
    /**
     * Oma profiilisivu, joka on osoittautunut listan tärkeimmäksi löydöksi: se listaa sekä
     * odottavat että päättyneet ottelut, eli juuri ne joita Top Page ei näytä.
     *
     * Polku luetaan Top Pagelta (`days_to_view`-parametri erottaa oman linkin
     * vastustajien linkeistä) eikä koota käyttäjänumerosta.
     */
    private fun DgClient.fetchProfile(): Document {
        val profilePath = Jsoup.parse(safeFetch("/bg/top"))
            .selectFirst("a[href*=days_to_view]")
            ?.attr("href")
        assumeTrue(profilePath != null, "Oman profiilin linkkiä ei löytynyt Top Pagelta")

        val html = safeFetch(profilePath!!)
        save("user_profile.html", html)
        return Jsoup.parse(html)
    }

    /** Profiilin taulukko captionin perusteella, esim. "Active games:". */
    private fun Document.section(caption: String): Element? =
        select("table:has(caption)").firstOrNull {
            it.selectFirst("caption")?.text()?.contains(caption, ignoreCase = true) == true
        }

    private fun clientOrSkip(): DgClient {
        val credentials = LocalCredentials.loadOrNull()
        assumeTrue(credentials != null, "local.properties puuttuu, ks. local.properties.example")
        return DgClient(
            credentials = { credentials },
            cookieStore = FileCookieStore(File("build/live/cookies.txt")),
        )
    }

    /**
     * Hakee sivun ja kaataa testin ennen verkkoa jos polku on kuluttava.
     *
     * Tarkistus on tässä eikä [DgClient]issä, koska sovellus **saa** hakea nuo osoitteet
     * käyttäjän pyynnöstä. Kielto koskee kaappausta: väärä haku ei tuottaisi virhettä
     * vaan hävittäisi viestin tai tekisi siirron oikeassa ottelussa.
     */
    private fun DgClient.safeFetch(path: String): String {
        val forbidden = CONSUMING.filter { path.contains(it, ignoreCase = true) }
        check(forbidden.isEmpty()) {
            "Polku on kuluttava (${forbidden.joinToString()}), tätä ei haeta: $path"
        }
        val response = fetch(path)
        assertTrue(response is DgResponse.Ok) {
            "Haku epäonnistui ($path): ${response::class.simpleName}"
        }
        return (response as DgResponse.Ok).html
    }

    private fun save(name: String, html: String) {
        val target = File("../raakasivut/$name")
        target.parentFile.mkdirs()
        target.writeText(html)
        println("Tallennettu: ${target.canonicalPath} (${html.length} merkkiä)")
    }

    private companion object {
        /**
         * Osoitteen tuntomerkit joilla haku muuttaa palvelimen tilaa: `nextgame` ja
         * `skip` kuluttavat viestijonoa, `submit` ja `move=` tekevät siirron.
         */
        val CONSUMING = listOf("nextgame", "submit", "skip", "move=")
    }
}
