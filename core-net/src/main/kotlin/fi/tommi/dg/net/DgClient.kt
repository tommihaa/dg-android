package fi.tommi.dg.net

import fi.tommi.dg.domain.FormMethod
import fi.tommi.dg.domain.FormSubmission
import fi.tommi.dg.scrape.DgPages
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.net.URLEncoder
import java.nio.charset.Charset
import java.util.concurrent.TimeUnit

/**
 * Ainoa reitti ulos. Kaikki sivuhaut ja lomakelähetykset kulkevat tämän kautta, jotta
 * istunnon uusiminen ja yhteyskatkon käsittely on yhdessä paikassa eikä jokaisen
 * kutsupaikan muistin varassa.
 *
 * Kolme suunnitteluvalintaa, kukin mitatusta syystä:
 *
 * 1. Onnistuminen päätellään **sisällöstä**, ei statuskoodista. DailyGammon vastaa
 *    uloskirjautuneelle 200 OK ja login-lomakkeen.
 * 2. Uudelleenkirjautuminen on **hiljainen**. Käyttäjä näkee virheen vasta jos
 *    tunnuksetkaan eivät kelpaa.
 * 3. Pyyntöjen väliin pakotetaan minimiväli. Kohde on pieni vapaaehtoisvoimin
 *    pyöritetty sivusto vuoden 2011 Apachella, joten kohteliaisuus on osa sopimusta.
 *
 * Metodit ovat estäviä (blocking). Androidilla ne ajetaan IO-dispatcherissa. Tämä pitää
 * moduulin vapaana coroutines-riippuvuudesta ja siten myös JVM-testeissä yksinkertaisena.
 */
class DgClient(
    private val credentials: CredentialsProvider,
    cookieStore: CookieStore = InMemoryCookieStore(),
    private val baseUrl: HttpUrl = DEFAULT_BASE_URL.toHttpUrl(),
    private val minRequestIntervalMillis: Long = 1_000L,
    private val userAgent: String = DEFAULT_USER_AGENT,
    httpClient: OkHttpClient? = null,
) {

    private val base: OkHttpClient = (httpClient ?: OkHttpClient.Builder().build())
        .newBuilder()
        .cookieJar(StoredCookieJar(cookieStore))
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    /**
     * **Uudelleenyritys on päällä hakemiselle ja pois rungollisilta lähetyksiltä**, ja raja
     * kulkee siinä onko pyynnöllä runkoa jonka kaksinkertaistuminen olisi vaarallista.
     * Molemmat asiakkaat syntyvät samasta pohjasta `newBuilder()`illä, joten ne jakavat
     * yhteysaltaan ja evästeet.
     *
     * **Raja siirtyi vielä kerran 14.8.2026: kyselylliset GET-teot (`Intent.ACT_GET`) saivat
     * uusinnan takaisin.** Osoitteessa ei ole runkoa jonka kaksinkertainen lähetys
     * vaarantaisi, ja OkHttpin oma uusinta uusii vain kierrätetyn yhteyden kuoleman ennen
     * kuin mitään on lähetetty, ei koskaan sen jälkeen. Mittaus: kolme yritystä kolmesta
     * `Submit Move`illa epäonnistui täsmälleen 5 s `KeepAliveTimeout`in tienoilla
     * (`docs/KOHDE.md`), koska kokoamisen jälkeinen ihmisen tauko ylittää sen lähes aina.
     * Vain rungollinen `Intent.ACT` (POST: kirjautuminen) jäi uusimatta.
     *
     * **Raja kulki HTTP-metodissa 10.8.2026 asti, ja se oli mitattuna väärä paikka.**
     * Perustelu oli että GET ei muuta sivustolla mitään, ja se piti paikkansa niin kauan kuin
     * GET oli vain hakemista. Lautasivun jokainen toimintolomake on `method=get`
     * (`Submit Move` 84 lomaketta talteen otetuissa sivuissa, `Double` / `Roll Dice` 61,
     * `Next` 48, `Submit Greedy Bearoff` 17, `Accept` / `Decline` 4), joten metodiin sidottu
     * sääntö olisi antanut siirron lähetykselle juuri sen uusinnan jonka estämiseksi se
     * kirjoitettiin. Vika olisi ollut hiljainen ja ilmennyt vasta katkenneella yhteydellä.
     *
     * Aiemmin uudelleenyritys oli pois kummaltakin, ja perustelu oli oikea mutta liian
     * laaja: lomakelähetystä ei saa toistaa vahingossa. Se sitoi hakemisen samaan rajoitukseen
     * eikä hakeminen ole sama riski, koska GET ei muuta mitään sivustolla.
     *
     * Hinta näkyi mitattuna 8.8.2026: **ensimmäinen haku yli viiden sekunnin tauon jälkeen
     * epäonnistui joka kerta**, ja heti perään painettu `Refresh` onnistui. Yhteysaltaan
     * yhteys ehtii sulkeutua (`Apache/2.2.22`, `KeepAliveTimeout` oletuksena 5 s), ja juuri
     * tähän OkHttpin oma uusinta on tehty: se koskee pyyntöä joka ei mennyt perille, eikä ole
     * sokea uusinta. Kuorma ei siis kaksinkertaistu, vaan onnistuneita hakuja tulee yksi.
     */
    private val httpRetrying: OkHttpClient = base.newBuilder()
        .retryOnConnectionFailure(true)
        .build()

    private val httpOnce: OkHttpClient = base.newBuilder()
        .retryOnConnectionFailure(false)
        .build()

    private val throttleLock = Any()
    private var lastRequestAt = 0L

    /**
     * Hakee sivun. Jos istunto on katkennut, kirjautuu uudelleen ja palauttaa halutun
     * sivun ilman että kutsuja huomaa mitään.
     */
    fun fetch(path: String): DgResponse {
        val first = execute(Request.Builder().url(resolve(path)).get().build(), Intent.FETCH)
        return first.readBody { html ->
            if (!DgPages.isLoginPage(html)) DgResponse.Ok(html)
            else recoverSession(loginPageHtml = html, originalPath = path)
        }
    }

    /**
     * Lähettää lomakkeen. Sama istunnonpalautus kuin [fetch]illä, mutta yksi ero:
     * jos istunto oli katkennut, lomaketta EI lähetetä uudelleen automaattisesti.
     *
     * Syy on että lomakelähetys ei ole idempotentti. DailyGammonissa siirto rakentuu
     * palvelimen puolella klikkaus kerrallaan, joten vahingossa toistettu lähetys voisi
     * viedä pelin tilaan jota kukaan ei pyytänyt. Kutsuja saa [DgResponse.AuthFailed]in
     * ja päättää itse.
     *
     * **`internal` 1.9.2026 alkaen (auditoinnin H6), ja syy on portti.** Tämä ottaa käsin
     * kootun osoitteen ja kenttäkartan, eli täsmälleen sen jonka [send] on rakennettu
     * estämään: sen parametri on [FormSubmission], jonka voi tuottaa vain sivun oma lomake.
     * Kaksi ovea joista toinen on lukossa ei ole lukko. Tuotannossa tätä on kutsunut vain
     * [send] itse, joten muutos ei ottanut mitään pois; se teki `:app`in puolelta
     * mahdottomaksi sen mitä siellä ei ollut tehty. Moduulin omat testit näkevät tämän yhä.
     */
    internal fun submitForm(path: String, fields: Map<String, String>): DgResponse {
        val response = execute(
            Request.Builder().url(resolve(path)).post(formBody(fields)).build(),
            Intent.ACT,
        )
        return response.bodyOrAuthFailed()
    }

    /**
     * Lähettää lautasivun lomakkeen, eli tekee sen mitä käyttäjä painoi.
     *
     * **Parametri on tyyppi eikä polku**, ja ero on koko portin ydin. [FormSubmission]in
     * konstruktori on `core-domain`in sisäinen, joten tähän ei voi antaa itse koottua
     * osoitetta: ainoa tie arvoon on `BoardForm.press`, eli sivun oman napin painaminen.
     * Kokoamiskielto on siten käännösaikainen tae eikä kutsupaikan muistin varassa.
     *
     * **Metodi luetaan lomakkeelta eikä valita täällä** (22.8.2026). Sivusto käyttää
     * molempia: lautasivun toimintolomakkeet ovat `method=get`, kun taas chat ja
     * pikaviestin lähetys ovat POSTeja. Itse valittu metodi lähettäisi kentät paikkaan
     * josta palvelin ei niitä lue, eikä se olisi virhe vaan teko joka ei mene perille.
     * Aiemmin tämä lähetti aina GETin, mikä oli oikein niin kauan kuin ainoa käyttökohde
     * oli lauta.
     *
     * **Uusinta on päällä ([Intent.ACT_GET]), ja raja on nyt rungossa eikä tarkoituksessa
     * (mitattu ja päätetty 14.8.2026).** Katkenneella yhteydellä ei ole runkoa jonka
     * kaksinkertainen lähetys vaarantaisi: pyyntö on kokonaan osoitteessa. OkHttpin oma
     * uusinta ([httpRetrying]) uusii vain sen tapauksen jossa kierrätetty yhteys todetaan
     * kuolleeksi ennen kuin mitään on lähetetty, ei koskaan sen jälkeen kun tavuja on jo
     * mennyt, joten se ei voi tuottaa kahta suoritettua tekoa palvelimella. Löytö oli
     * mitattu: kolme yritystä kolmesta epäonnistui täsmälleen Apachen 5 s
     * `KeepAliveTimeout`in tienoilla (`docs/KOHDE.md`), koska kokoamisen ja painalluksen
     * välinen ihmisen tauko ylittää sen lähes aina.
     *
     * Kysely koodataan samalla merkistöllä ja samalla koodarilla kuin lomakerunko, koska
     * molemmat kutsuvat [encodeFields]iä. Aiemmin tämä oli kahdennettu kulku ja tässä
     * kohdassa lupaus siitä että ne pysyvät samana.
     */
    fun send(submission: FormSubmission): DgResponse = when (submission.method) {
        FormMethod.GET -> sendAsQuery(submission)
        // POST menee [submitForm]in kautta, eikä se ole pelkkä muodon vaihto. Ylläolevan
        // uusinnan perustelu nojaa siihen ettei pyynnöllä ole runkoa, ja POSTilla se
        // nojaus katoaa: uusittu runko olisi kaksi lähetettyä viestiä. Siksi tämä haara
        // menee kerran ja epäonnistuu näkyvästi ([Intent.ACT]).
        FormMethod.POST -> submitForm(submission.action, submission.fields)
    }

    private fun sendAsQuery(submission: FormSubmission): DgResponse {
        val query = encodeFields(submission.fields)
        val path = if (query.isEmpty()) submission.action else "${submission.action}?$query"
        val response = execute(Request.Builder().url(resolve(path)).get().build(), Intent.ACT_GET)
        // Sama kuin submitFormilla: katkennutta istuntoa EI korjata lähettämällä uudelleen.
        // Kutsuja saa tietää ja päättää itse.
        return response.bodyOrAuthFailed()
    }

    /**
     * Nimenomainen kirjautuminen. [returnPath] on login-lomakkeen `path`-kentän arvo,
     * eli minne palvelin ohjaa onnistuneen kirjautumisen jälkeen.
     *
     * **`internal` 1.9.2026 alkaen (auditoinnin H6).** Kirjautuminen on tämän luokan oma
     * asia: [fetch] uusii istunnon hiljaa, eikä `:app` ole koskaan kutsunut tätä. Ulos
     * näkyvä kirjautuminen on tunnusten tallentaminen ja sivun hakeminen, ei tämä.
     */
    internal fun login(returnPath: String = DEFAULT_RETURN_PATH): DgResponse {
        val creds = credentials.get() ?: return DgResponse.AuthFailed
        val body = formBody(
            mapOf(
                "path" to returnPath,
                "login" to creds.login,
                "password" to creds.password,
                "save" to "on",
            )
        )
        val response = execute(
            Request.Builder().url(resolve(DgPages.LOGIN_ACTION)).post(body).build(),
            Intent.ACT,
        )
        // Väärät tunnukset tuottavat jälleen login-sivun, eivät virhekoodia.
        return response.bodyOrAuthFailed()
    }

    /**
     * Istunnon hiljainen palautus.
     *
     * Käytetään palvelimen itsensä kertomaa paluupolkua login-lomakkeen `path`-kentästä.
     * Silloin kirjautumisvastaus ON jo haluttu sivu, eikä erillistä uudelleenhakua tarvita.
     * Tämä ei ole optimointi vaan oikeellisuuskysymys: polkumuunnos /bg/top -> "top/"
     * olisi meidän arvauksemme, palvelimen antama arvo ei ole.
     */
    private fun recoverSession(loginPageHtml: String, originalPath: String): DgResponse {
        val serverGivenReturnPath = DgPages.loginReturnPath(loginPageHtml)
        val afterLogin = login(returnPath = serverGivenReturnPath ?: DEFAULT_RETURN_PATH)

        if (afterLogin !is DgResponse.Ok) return afterLogin
        if (serverGivenReturnPath != null) return afterLogin

        // Paluupolku puuttui, joten emme tiedä mille sivulle päädyimme. Haetaan haluttu
        // sivu kerran uudestaan, tällä kertaa ilman uutta kirjautumisyritystä, jottei
        // synny silmukkaa.
        val request = Request.Builder().url(resolve(originalPath)).get().build()
        return execute(request, Intent.FETCH).bodyOrAuthFailed()
    }

    /**
     * Kuljetuksen tulos vastaukseksi. **Tämä on se kohta jossa katkos ja palvelinvirhe
     * saavat merkityksensä, ja se on yksi kohta eikä viisi.**
     *
     * Aiemmin jokainen ulospäin näkyvä metodi kirjoitti saman kahden haaran muunnoksen
     * itse ja erosi vain siinä mitä rungolle tehdään. Ero on juuri se mitä [onHtml] kantaa,
     * eli kutsupaikkaan jää se mikä siinä oikeasti on omaa: [fetch] uusii istunnon,
     * lähettävät reitit eivät.
     */
    private fun Raw.readBody(onHtml: (String) -> DgResponse): DgResponse = when (this) {
        is Raw.Offline -> DgResponse.Offline(cause)
        is Raw.HttpError -> DgResponse.ServerError(code)
        // Nukkumissivu luetaan ennen kutsupaikan omaa tulkintaa, koska se tulee jokaiselle
        // pyynnölle samana: haulle, lomakkeelle ja kirjautumiselle. Ilman tätä kirjautuminen
        // katkon aikana näyttäisi onnistuneelta (runko ei ole login-sivu) ja haku antaisi
        // tuntemattoman sivun. Mitattu 11.9.2026, ks. [DgResponse.Sleeping].
        is Raw.Body -> if (DgPages.isSleepingPage(html)) DgResponse.Sleeping else onHtml(html)
    }

    /**
     * Runko sellaisenaan, paitsi että login-sivu on [DgResponse.AuthFailed].
     *
     * Kolme reittiä (lomakelähetys, kyselyllinen teko ja kirjautuminen itse) päätyvät tähän,
     * ja niillä on eri syy samaan sääntöön: teoilla se on idempotenssi eli lähetystä ei
     * uusita automaattisesti, kirjautumisella se on että väärät tunnukset tuottavat
     * login-sivun eivätkä virhekoodia. Syyt ovat kutsupaikkojen kommenteissa, sääntö on
     * tässä kerran.
     */
    private fun Raw.bodyOrAuthFailed(): DgResponse = readBody { html ->
        if (DgPages.isLoginPage(html)) DgResponse.AuthFailed else DgResponse.Ok(html)
    }

    private fun resolve(path: String): HttpUrl =
        baseUrl.resolve(path) ?: error("Kelvoton polku: $path")

    /**
     * Lomakerunko **sivuston omalla merkistöllä**, ei OkHttpin oletuksella.
     *
     * `FormBody` prosenttikoodaa aina UTF-8:na, eli `ä` lähtisi kahtena tavuna (`%C3%A4`).
     * Palvelin lukee vastaanottamansa tavut yksitavuisena merkistönä, joten se tallentaisi
     * kaksi merkkiä yhden sijaan ja palauttaisi ne sellaisenaan takaisin. Vika olisi
     * hiljainen: mikään ei kaadu, viesti vain tallentuu rikki arkistoon.
     *
     * Mitattu 4.8.2026 profiilin `Location`-kentällä: selaimen lähettämä `ä` palasi tavuna
     * `0xE4`, `ö` tavuna `0xF6` ja `å` tavuna `0xE5`, eikä yhtään numeerista entiteettiä
     * syntynyt. Sivusto siis odottaa yksitavuista syötettä ja säilyttää sen sellaisenaan.
     *
     * **Todennettu sivustoa vasten 22.8.2026, ja siihen asti pariteetti oli päätelty.**
     * Yllä oleva mittaus koski selaimen lähettämiä tavuja, ja tämä funktio kirjoitettiin
     * sitä vasten lähettämättä itse mitään. Nyt sovelluksesta lähetetty vastaus on luettu
     * vastaanottajan tunnuksella, ja `å` näkyi oikein. Todennus kattaa yhden merkin
     * (0xE5); `ä` ja `ö` ovat yhä selaimen reitiltä.
     *
     * Merkki jota `windows-1252` ei tunne, esimerkiksi emoji, muunnetaan ensin muotoon
     * `&#N;`. Se ei ole arvaus vaan pariteetti: mitattu 5.8.2026 samalla koettimella, ja
     * selain tekee täsmälleen näin. Ks. [siirrettavaanMuotoon].
     */
    private fun formBody(fields: Map<String, String>): RequestBody =
        encodeFields(fields).toByteArray(DG_CHARSET)
            .toRequestBody("application/x-www-form-urlencoded".toMediaType())

    /**
     * Kentät `name=value&...` -muotoon sivuston merkistöllä.
     *
     * **Yhteinen runkoon ja kyselyyn, ja se on nyt rakenne eikä lupaus.** Sama arvo menee
     * samalla tavalla kummalla reitillä tahansa, koska reittejä on yksi. Aiemmin tässä oli
     * kaksi identtistä kopiota ja kommentti joka lupasi niiden pysyvän samana; lupaus ei
     * olisi huomannut jos toinen olisi muuttunut.
     */
    private fun encodeFields(fields: Map<String, String>): String =
        fields.entries.joinToString("&") { (name, value) ->
            "${urlEncode(name)}=${urlEncode(value)}"
        }

    private fun urlEncode(value: String): String =
        URLEncoder.encode(siirrettavaanMuotoon(value), DG_CHARSET.name())

    /**
     * Korvaa merkit joita [DG_CHARSET] ei tunne numeerisella merkkiviittauksella `&#N;`.
     *
     * Sivusto ei tulkitse viittausta vaan tallentaa sen tekstinä ja escapaa `&`:n
     * ulostulossa, joten emoji näkyy kaikille kirjaimellisena merkkijonona. Se on ruma,
     * ja se on silti oikein: selainkäyttäjä tuottaa saman lopputuloksen, ja sovellus joka
     * tekisi jotain muuta eroaisi kohteestaan juuri siinä kohtaa jossa käyttäjä vertaa
     * niitä rinnakkain. Aiempi `?`-korvaus hukkasi merkin kokonaan.
     *
     * **Kulku on koodipisteinä eikä merkkeinä.** Emoji on Javan merkkijonossa sijaispari,
     * ja merkki kerrallaan luettuna siitä syntyisi kaksi viittausta joista kumpikaan ei
     * ole se merkki. Vika olisi hiljainen: lopputulos näyttäisi kelvolliselta.
     *
     * Enkooderi luodaan kutsukohtaisesti, koska `CharsetEncoder` ei ole säieturvallinen.
     */
    private fun siirrettavaanMuotoon(value: String): String {
        val enkooderi = DG_CHARSET.newEncoder()
        val tulos = StringBuilder(value.length)
        var i = 0
        while (i < value.length) {
            val koodipiste = value.codePointAt(i)
            val pituus = Character.charCount(koodipiste)
            val pala = value.substring(i, i + pituus)
            if (enkooderi.canEncode(pala)) tulos.append(pala) else tulos.append("&#$koodipiste;")
            i += pituus
        }
        return tulos.toString()
    }

    /**
     * Yksi pyyntö kerrallaan, ja vähintään [minRequestIntervalMillis] edellisen päättymisestä.
     *
     * **Lukko pidetään koko pyynnön ajan eikä vain odotuksen ajan.** Aiemmin se suojasi vain
     * aikaleimaa, jolloin kaksi kutsua saattoi olla yhtä aikaa ilmassa kunhan aloitusten
     * välissä oli sekunti. Rinnakkaisuuden puuttuminen oli siis käytäntö eikä rakenne, ja
     * käytäntö ei kestä sitä että kutsupaikkoja tulee lisää. Kohde on vapaaehtoisvoimin
     * pyöritetty sivusto, joten rinnakkaisuuden estäminen on tässä arvokkaampaa kuin
     * läpimenon nopeus.
     *
     * Väli mitataan edellisen pyynnön **päättymisestä**, koska vasta silloin kuorma laskee
     * palvelimelta. Aikaleima asetetaan `finally`ssä, joten myös epäonnistunut pyyntö
     * lasketaan kuormaksi: se on palvelimen kannalta yhtä lailla käsitelty.
     */
    private fun execute(request: Request, intent: Intent): Raw = synchronized(throttleLock) {
        val waitFor = lastRequestAt + minRequestIntervalMillis - System.currentTimeMillis()
        if (waitFor > 0) Thread.sleep(waitFor)
        val withHeaders = request.newBuilder()
            .header("User-Agent", userAgent)
            .build()
        val http = if (intent == Intent.ACT) httpOnce else httpRetrying
        try {
            http.newCall(withHeaders).execute().use { response ->
                val body = response.body?.bytes()?.toString(DG_CHARSET).orEmpty()
                // 4xx ja 5xx erotetaan, mutta 200 ei vielä tarkoita onnistumista:
                // sen ratkaisee sisältö kutsujan puolella.
                if (!response.isSuccessful) Raw.HttpError(response.code) else Raw.Body(body)
            }
        } catch (e: IOException) {
            // Luokan nimi eikä viesti: nimi on vakaa ja lyhyt, kun taas `message` vaihtelee
            // OkHttpin version mukana ja voi sisältää osoitteen, eli sen kautta jonon riville
            // päätyisi tekstiä jota sinne ei ole tarkoitettu.
            Raw.Offline(e.javaClass.simpleName)
        } finally {
            lastRequestAt = System.currentTimeMillis()
        }
    }

    /**
     * Mitä pyynnöllä tarkoitetaan. Tämä ratkaisee uusinnan, ei HTTP-metodi.
     *
     * Kolme arvoa eikä totuusarvoa, koska kutsupaikassa lukee silloin mitä tarkoitetaan eikä
     * mitä siitä seuraa. `retry = false` kysyisi lukijalta miksi, nämä nimet eivät.
     *
     * **[ACT_GET] erotettiin [ACT]ista 14.8.2026.** Raja ei ole "haku vs. teko" vaan
     * "onko lähetettävä runko jonka kaksinkertaistuminen olisi vaarallista": [ACT_GET] on
     * teko ilman runkoa (osoite kantaa kaiken), [ACT] on teko jonka runko voisi mennä
     * palvelimelle kahdesti jos uusinta laukeaisi väärään aikaan. Ks. [DgClient.send]in
     * perustelu.
     */
    private enum class Intent {
        /** Sivun hakeminen. Ei muuta mitään, joten katkennut yhteys yritetään uudelleen. */
        FETCH,

        /**
         * Käyttäjän teko ilman runkoa (kyselyllinen GET). Turvallisesti uusittavissa, koska
         * OkHttpin oma uusinta uusii vain sen kun kierrätetty yhteys todetaan kuolleeksi
         * ennen kuin mitään on lähetetty.
         */
        ACT_GET,

        /** Käyttäjän teko jolla on runko (POST). Menee kerran ja epäonnistuu näkyvästi. */
        ACT,
    }

    private sealed interface Raw {
        data class Body(val html: String) : Raw
        data class HttpError(val code: Int) : Raw

        /** [cause] on poikkeuksen luokan nimi, ks. `DgResponse.Offline`. */
        data class Offline(val cause: String) : Raw
    }

    companion object {
        /** Sivusto ei tarjoa HTTPS:ää lainkaan: portti 443 hylkää yhteyden. */
        const val DEFAULT_BASE_URL = "http://www.dailygammon.com/"

        const val DEFAULT_RETURN_PATH = "top/"

        /** Tunnistautuva User-Agent on kohteliaisuus pienen sivuston ylläpitäjälle. */
        const val DEFAULT_USER_AGENT = "DGAndroid/0.1 (personal client)"

        /**
         * Vastauksen merkistö, **mitattu 4.8.2026 eikä arvattu**.
         *
         * Palvelin lähettää `Content-Type: text/html` ilman charsetia, jolloin OkHttpin
         * `string()` olettaa UTF-8:aa. Se on väärin, ja väärin peruuttamattomalla tavalla:
         * keskustelupalstan sivuilta mitatut tavut `0x92 0x93 0x94 0x97` (kaareva
         * heittomerkki, lainausmerkit, pitkä viiva) eivät ole kelvollista UTF-8:aa
         * lainkaan, joten ne muuttuisivat korvausmerkiksi ennen kuin jäsennin näkee mitään.
         *
         * `ISO-8859-1` ei myöskään kelpaa, vaikka se on lähellä ja vaikka toinen asiakas
         * käyttää sitä: samat neljä tavua ovat siinä C1-kontrollimerkkejä. Ne osuvat
         * englannin tavallisimpiin merkkeihin (`It’s`), eivät reunatapauksiin.
         *
         * Ks. `docs/KOHDE.md`, viestiosio. Kirjoitussuunta on mitattu erikseen samana päivänä:
         * lähetetty ääkkönen palaa samana tavuna, eli sivusto ei koodaa sitä entiteetiksi.
         */
        val DG_CHARSET: Charset = Charset.forName("windows-1252")
    }
}
