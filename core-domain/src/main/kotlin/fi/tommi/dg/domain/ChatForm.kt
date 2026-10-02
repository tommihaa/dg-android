package fi.tommi.dg.domain

/**
 * Ottelun chat-lomake sellaisena kuin siirron jälkeinen sivu sen kirjoittaa.
 *
 * [ReplyForm]in pari toisella sivulla, ja se on olemassa samasta syystä: lähetys on
 * peruuttamaton teko oikealle ihmiselle, eikä itse koottu osoite tuottaisi virhettä vaan
 * väärän teon. Ero pikaviestiin on että tämä lomake **on sama lomake jolla vuoro päätetään**:
 * sen napit ovat `Next Game` ja `To Top`, eli viesti ja siirtyminen lähtevät yhtenä pyyntönä
 * niin kuin selaimessakin.
 *
 * **Lomake elää vain sen sivun.** Vastustajan viesti näkyy vain siirron jälkeisellä sivulla
 * eikä palvelin säilytä sitä, joten tätä ei tallenneta vaan käytetään tai menetetään.
 */
data class ChatForm(
    /** Lomakkeen `action` sivun omasta attribuutista, esim. "/bg/move/7000003/1121". */
    val action: String,
    /** Lomakkeen oma metodi. Tällä sivulla POST, mutta sitä ei oleteta täällä. */
    val method: FormMethod,
    /** Tekstikentän nimi, sivulla `chat`. Luetaan lomakkeelta eikä oleteta. */
    val field: String,
    /** Lainausvalinnan nimi, sivulla `quote`, tai null kun ruutua ei ole. */
    val quoteField: String?,
    /**
     * Lainausvalinnan arvo, eli mitä ruudusta lähtee kun se on valittuna.
     *
     * Sivun ruudussa ei ole `value`-attribuuttia lainkaan, jolloin arvo on `on`. Se ei ole
     * tämän koodin oletus vaan HTML:n oma sääntö, samoin kuin tyhjän `method`in GET
     * ([FormMethod.from]). Attribuutin arvo voittaa sen aina kun se on olemassa.
     */
    val quoteValue: String,
    /**
     * Oliko ruutu sivulla valmiiksi valittuna.
     *
     * **Merkitsevä tieto eikä kosmetiikkaa** (`docs/AVOIMET.md`, Tommin toive vanhemmista
     * viesteistä): juuri tämä valinta tekee edellisestä viestistä näkyvän vastaanottajalle,
     * ja sivu pitää sitä oletuksena päällä. Sovellus ei saa hiljaa pudottaa sitä.
     */
    val quoteCheckedByDefault: Boolean,
    /** Piilokentät sellaisenaan, sivulla `commit=1`. Ilman niitä lähetys ei tee mitään. */
    val hiddenFields: Map<String, String>,
    /** Lähetysnappien `value`-arvot sivun järjestyksessä. Nappien nimi on aina `submit`. */
    val submits: List<String>,
)

/**
 * Kirjoittaa viestin lomakkeeseen ja painaa yhtä sivun tarjoamista napeista.
 *
 * [BoardForm.press]in portti pätee tässä: nappia jota sivu ei tarjoa ei voi painaa, joten
 * kutsuja ei voi keksiä `Next Gamea` sivulle jolla sitä ei ole.
 *
 * **[ReplyForm.write]in portti ei päde, ja se muuttui 27.8.2026 laiteajon jälkeen.** Tässä
 * hylättiin siihen asti tyhjä teksti samalla perusteella kuin pikaviestissä: tyhjä lähetys
 * on vahinko ja kuluttaa teon. Tämä lomake on kuitenkin eri laji, koska **sen nappi on
 * ainoa tapa päättää vuoro**: selaimessa tyhjä kenttä ja `To Top` tarkoittaa poistumista
 * ilman viestiä, eikä se ole vahinko vaan tavallisin tapaus. Portti esti siis poistumisen
 * silloin kun ei ollut mitään sanottavaa, ja teki sovelluksesta tiukemman kuin kohde.
 *
 * Kenttä lähtee mukana myös tyhjänä, koska selain tekee niin. Arkistointi on eri asia ja
 * asuu kutsujassa: tyhjää tekstiä ei kirjata viestiksi (`BoardViewModel.sendChat`).
 *
 * **Nappi on pakollinen eikä oletettu, ja se on tämän lomakkeen tärkein ero pikaviestiin.**
 * `Next Game` siirtää jonossa eteenpäin ja `To Top` poistuu laudalta, eli sama lähetys
 * tekee kaksi eri asiaa sen mukaan kumpaa painetaan. Kumpikaan ei ole turvallisempi oletus
 * kuin toinen, joten valinta kuuluu käyttäjälle eikä tälle funktiolle.
 *
 * [quote] ei ole oletusarvoinen samasta syystä: sivun oletus on tosi, mutta oletuksen
 * toistaminen täällä tarkoittaisi että arvo tulee kahdesta paikasta. Kutsuja lukee sen
 * [ChatForm.quoteCheckedByDefault]ista ja välittää käyttäjän valinnan.
 */
fun ChatForm.write(text: String, quote: Boolean, submit: String): FormSubmission? {
    if (submit !in submits) return null

    val fields = buildMap {
        // Piilokentät ensin ja sellaisinaan, kuten [BoardForm.press]issä: ne ovat lomakkeen
        // osa siinä missä napitkin, ja `commit=1`:n pudottaminen tuottaisi pyynnön joka
        // näyttää onnistuvan muttei tee mitään.
        putAll(hiddenFields)
        // Rivinvaihdot CRLF:ksi, koska selain tekee niin (HTML:n lomakenormalisointi) ja
        // palvelin olettaa sen: paljailla LF:illä lähetetystä 8627 merkin viestistä katosi
        // vastaanottajalta tasan yksi merkki jokaisen LF-parin jäljestä (mitattu 28.8.2026,
        // `docs/KOHDE.md`). Arkistoon viesti kirjataan silti niin kuin se kirjoitettiin;
        // tämä on lankamuoto eikä sisältö.
        put(field, text.replace("\r\n", "\n").replace("\n", "\r\n"))
        // Rastittamaton ruutu ei lähde mukana lainkaan. Se on HTML:n sääntö eikä valinta,
        // ja juuri siksi valinnan pois jättäminen on eri asia kuin sen lähettäminen epätotena.
        if (quote) quoteField?.let { put(it, quoteValue) }
        put("submit", submit)
    }
    return FormSubmission(action = action, method = method, fields = fields)
}
