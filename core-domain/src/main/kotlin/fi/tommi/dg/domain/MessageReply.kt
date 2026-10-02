package fi.tommi.dg.domain

/**
 * Saapuneen viestin vastauslomake sellaisena kuin sivu sen kirjoittaa.
 *
 * Tämä on [BoardForm]in pari toisessa aiheessa, ja se on olemassa samasta syystä: lähetys
 * on peruuttamaton teko oikealle ihmiselle, eikä itse koottu osoite tuottaisi virhettä
 * vaan väärän teon. Osoite, kentän nimi ja pituusraja luetaan siis sivulta eikä
 * kirjoiteta koodiin.
 *
 * ~~**Lomake elää vain saapumishetken.**~~ **Kumottu 30.8.2026 Tommin pyynnöstä:** lomake
 * talletetaan viestin mukana ([Message.replyForm]), joten arkiston viestiin voi vastata
 * jälkikäteen. Sivu itse katoaa yhä lukuhetkellä; se mikä säilyy on tämä sivulta luettu
 * lomake, ei sivu. Välillä 22.–30.8.2026 rajaus oli toisin päin (vain juuri saapunut),
 * ja se oli valinta eikä este, ks. seuraava kappale.
 *
 * Mitattu 22.8.2026 raaoista tavuista: sama lomake on myös pelaajan profiilisivulla, ja
 * `POST /bg/sendmsg/<id>` on osoitettu käyttäjälle eikä viestille. Siksi talletettu
 * lomake ei vanhene viestin mukana: osoite viittaa henkilöön, joka on olemassa
 * senkin jälkeen kun viestisivu on kulunut.
 */
data class ReplyForm(
    /** Lomakkeen `action`, esim. "/bg/sendmsg/33060". */
    val action: String,
    /** Lomakkeen oma metodi. Pikaviestillä POST, mutta sitä ei oleteta täällä. */
    val method: FormMethod,
    /** Kentän nimi, sivulla `text`. Luetaan lomakkeelta eikä oleteta. */
    val field: String,
    /**
     * Sivun oma `maxlength`, aidolla sivulla 80, tai null jos lomakkeessa ei ole sitä.
     *
     * **Luetaan sivulta muttei enää noudateta (24.8.2026, mitattu).** Kenttä on tässä siksi
     * että se on sivun oma tosiasia, ja sen lukeminen on halpaa. Se ei kuitenkaan enää estä
     * lähettämistä: mittaus osoitti että palvelin ottaa vastaan selvästi pidemmän viestin.
     * Perustelu on `docs/UI.md`:ssä ja mittauksen luvut `docs/KOHDE.md`:ssä.
     *
     * Null ei tarkoita rajattomuutta vaan sitä ettei rajaa tiedetä. Ero säilyy vaikka
     * kumpikaan ei enää estä mitään, koska se on eri väite sivusta.
     */
    val maxLength: Int?,
)

/**
 * Kirjoittaa vastauksen lomakkeeseen, tai palauttaa null jos sitä ei voi lähettää.
 *
 * `BoardForm.press`in pari: `press` ei voi painaa nappia jota sivu ei tarjoa, tämä ei voi
 * kirjoittaa lomakkeeseen jota sivulla ei ollut, koska [ReplyForm] syntyy vain jäsentimessä.
 *
 * Null yhdestä syystä: **tyhjä teksti**. Tyhjän viestin lähettäminen on aina vahinko, ja se
 * kuluttaisi teon.
 *
 * ~~Yli sivun rajan menevä teksti tuotti aiemmin myös nullin.~~ **Ehto poistettu 24.8.2026,
 * ja syy on mittaus eikä mielipide.** Alkuperäinen perustelu oli, että liian pitkä viesti
 * menisi perille puolikkaana eikä käyttäjä näkisi mitä hänestä jäi sanomatta. Mittaus
 * kumosi juuri sen oletuksen: 1329 merkin viesti tallentui kokonaisena, eli palvelin ei
 * leikkaa vaan **rivittää** tekstin 80 merkin kohdalta. Rivitys on näkyvä ja luettava, ei
 * katoavaa sisältöä, joten estettävää ei enää ole.
 *
 * Mikä rivityksessä silti muuttuu, ja se kuuluu tähän vaikkei se estä lähettämistä: pitkä
 * katkeamaton merkkijono katkeaa keskeltä, koska sanaväliä ei ole. Osoite menee siis perille
 * kahdelle riville jaettuna. Se on lähettäjän tiedettävä asia eikä koodin estettävä.
 */
fun ReplyForm.write(text: String): FormSubmission? {
    if (text.isBlank()) return null
    return FormSubmission(action = action, method = method, fields = mapOf(field to text))
}
