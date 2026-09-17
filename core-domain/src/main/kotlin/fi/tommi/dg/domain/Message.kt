package fi.tommi.dg.domain

import java.security.MessageDigest

/**
 * Viesti on muuttumaton tapahtuma: sitä ei muokata eikä poisteta.
 *
 * DailyGammon ei säilytä viestejä lainkaan ("Messages are not saved on this server"),
 * joten tämä on ainoa paikka jossa historia on olemassa.
 *
 * [id] johdetaan sisällöstä **ja saapumishetkestä**, ei juoksevasta numerosta. Sisältö
 * yksin ei riitä, ja syy on mitattu: pikaviestisivulla ei ole aikaleimaa lainkaan, joten
 * kaksi erikseen lähetettyä `Great match!` samalta pelaajalta olisivat erottamattomat ja
 * jälkimmäinen katoaisi hiljaa. Lyhyt kohteliaisuus ei ole reunatapaus vaan tavallisin
 * viestityyppi. Päätös 1.8.2026, perustelu `docs/KOHDE.md`.
 *
 * Idempotenssi säilyy siitä huolimatta, kunhan **leima annetaan kerran viestiä
 * muodostettaessa eikä tallennettaessa**: sama olio tuottaa aina saman tunnisteen, joten
 * epäonnistuneen kirjoituksen uusiminen on turvallista. Vain eri noudot eroavat, ja koska
 * haku on tuhoava, sama viesti voidaan noutaa vain kerran.
 */
data class Message(
    val matchId: MatchId?,
    /**
     * Kenen kanssa tämä keskustelu käydään, tai `null` jos sivu ei nimennyt sitä.
     *
     * **Ei sama tieto kuin [sender], vaikka ne osuvat usein yhteen.** Lähettäjä on se joka
     * kirjoitti tämän viestin, vastustaja se henkilö jonka historiaan viesti kuuluu. Jonon
     * sivulla nämä ovat sama nimi, koska saapunut viesti on aina toiselta. Ottelun
     * chat-ketjussa ne eroavat heti kun oma viesti tallennetaan: lähettäjä on silloin
     * käyttäjä itse, mutta keskustelu on yhä saman vastustajan kanssa.
     *
     * **Tämä on arkiston avain, ja se on tallennettava viestiin itseensä.** Perustelu on
     * `SUBSTANSSI.md`:n kohdissa 26, 29, 46 ja 47: arkiston pääsypolku on pelaaja, ja muisti
     * tarttuu nimeen. [matchId] ei kelpaa avaimeksi, koska sivusto pudottaa päättyneen
     * ottelun listalta ja tunnisteesta jää orpo numero, jota ei voi enää yhdistää henkilöön.
     * Nimi viestin omalla rivillä ei orpoudu, koska se ei tarvitse mitään muuta säilyäkseen.
     *
     * **Ei osa [id]-tiivistettä, ja se on tietoinen ero muihin kenttiin.** Tiiviste
     * tunnistaa viestitapahtuman, ja tapahtuma on sama riippumatta siitä tiesikö sovellus
     * keskustelukumppanin nimen sillä hetkellä kun sivu jäsennettiin. Jos vastustaja olisi
     * mukana tiivisteessä, jo tallennettujen viestien pääavaimet muuttuisivat tämän kentän
     * lisäämisestä, ja sama viesti ilman nimeä ja nimen kanssa olisivat kaksi eri riviä.
     */
    val opponent: String?,
    val sender: String,
    /**
     * Sivulla näkyvä aikaleima raakana, tai tyhjä jos sivu ei kerro sitä. Pikaviestisivu
     * ei kerro. Ei jäsennetä: muoto varmistuu vasta fixtureista.
     */
    val timestampText: String,
    val body: String,
    /**
     * Sivun oma otsikkorivi sellaisenaan, esimerkiksi
     * `You have received the following quick message from Tommi`.
     *
     * **Säilytetään aina eikä vain tuntemattomasta otsikosta.** Tunnettu otsikko on
     * halpa tallettaa, ja jos lajittelusääntö osoittautuu joskus vääräksi, laji on
     * johdettavissa uudelleen vain siitä tekstistä josta se alun perin johdettiin. Kenttä
     * jota täytetään vain epäonnistuessa ei auta silloin kun sääntö osui väärin mutta osui.
     *
     * Ei ole sama tieto kuin [source]: tämä on se mitä sivu sanoi, [source] on se mitä
     * säännöstä seurasi. Kun sääntö ei osu, laji on [MessageSource.UNKNOWN] ja tämä kenttä
     * on ainoa paikka jossa otsikko on tallessa.
     *
     * Tyhjä vain jos sivulla ei ole otsikkoa lainkaan.
     */
    val rawHeader: String,
    val source: MessageSource,
    /**
     * Milloin tämä laite näki viestin ensimmäisen kerran. **Osa tunnistetta.**
     *
     * Ei viestin lähetysaika: sivuston oma aika olisi [timestampText], ja pikaviestillä
     * sitä ei ole. Leima annetaan kerran sivua jäsennettäessä, ei tallennettaessa.
     */
    val receivedAtEpochMillis: Long,
    /**
     * Sen viestin [id], johon tämä viesti on vastaus, tai `null` jos tämä ei ole vastaus.
     *
     * **Tämä on tämän laitteen tietoa eikä sivuston tietoa** (Tommin päätös 24.8.2026).
     * DailyGammonissa ei ole viestiketjuja lainkaan eikä viesti kanna mitään viittausta
     * siihen mihin se vastaa, joten tämä syntyy vain siinä hetkessä jossa vastaus
     * lähetetään täältä. Sama tieto ei ole saatavissa mistään muualta jälkikäteen.
     *
     * **Lainaus lähtee mukaan vain valintaruudulla (30.8.2026), ja oletus on ilman.**
     * 24.8.2026 valinta oli ettei lainausta lähetetä lainkaan: selainlaajennus
     * `DGText2Area` ratkaisee saman tarpeen esitäyttämällä vastauskentän lainatulla
     * tekstillä, jolloin yhteys näkyy myös vastaanottajalle mutta viesti kasvaa ja sivusto
     * rivittää sen 80 merkin kohdalta. Tommi pyysi 30.8.2026 saman valinnaksi, ja rasti
     * liittää `> `-rivit lähtevän viestin alkuun laajennuksen mitatussa muodossa.
     *
     * Tämä kenttä ei nojaa lainaukseen kumpaankaan suuntaan: `> `-rivi on lähettäjän
     * selaimen tai rastin tuottamaa sisältöä joka voi aina jäädä pois
     * (`docs/TOINEN-ASIAKAS.md`), joten yhteys kirjataan täällä aina tähän kenttään.
     *
     * **Ei osa [id]-tiivistettä**, samasta syystä kuin [opponent]: tiiviste tunnistaa
     * viestitapahtuman, ja tapahtuma on sama riippumatta siitä mihin se vastasi. Jos tämä
     * olisi tiivisteessä, kentän lisääminen muuttaisi jo tallennettujen viestien
     * pääavaimet.
     */
    val replyTo: String? = null,
    /**
     * Sivun oma vastauslomake, tai `null` jos sivu ei tarjonnut sellaista tai viesti on
     * tallennettu ennen kuin lomake alettiin säilöä (30.8.2026).
     *
     * **Tämä purkaa 22.8.2026 tehdyn rajauksen, ja purku on Tommin pyyntö 30.8.2026**
     * (*"pitäisi pystyä vastaamaan vastaanotettua viestiä klikkaamalla"*). Rajaus oli
     * valinta eikä este: `POST /bg/sendmsg/<id>` on mitatusti osoitettu käyttäjälle eikä
     * viestille, joten arkistosta vastaaminen toimii vaikka viestisivu on jo kulunut.
     *
     * Periaate **osoite luetaan, ei koota** säilyy sellaisenaan: lomake on luettu sivulta
     * saapumishetkellä ja talletetaan sellaisenaan. Vanha rivi ilman lomaketta ei saa
     * vastauskenttää, koska sille ei ole mitään luettua osoitetta — kokoaminen nimestä
     * tai numerosta olisi juuri se mitä sääntö kieltää.
     *
     * **Ei osa [id]-tiivistettä**, samasta syystä kuin [opponent] ja [replyTo]: tiiviste
     * tunnistaa viestitapahtuman, ja tapahtuma on sama riippumatta siitä säilöttiinkö
     * lomake. Tiivisteessä kenttä muuttaisi jo tallennettujen viestien pääavaimet.
     */
    val replyForm: ReplyForm? = null,
) {
    // Huom: [opponent] ei ole tässä luettelossa, ja poissaolo on päätös eikä unohdus.
    // Perustelu on kentän omassa dokumentaatiossa.
    val id: String = contentHash(
        matchId?.value,
        sender,
        timestampText,
        body,
        rawHeader,
        source.name,
        receivedAtEpochMillis.toString(),
    )

    companion object {
        internal fun contentHash(vararg parts: String?): String {
            val digest = MessageDigest.getInstance("SHA-256")
            // Erotin on nollatavu, jota tekstisisällössä ei voi esiintyä. Ilman sitä
            // kenttäraja olisi siirrettävissä: ("ab","c") tiivistyisi samaksi kuin ("a","bc").
            parts.forEach { part ->
                digest.update((part ?: "").toByteArray(Charsets.UTF_8))
                digest.update(0)
            }
            return digest.digest().joinToString("") { "%02x".format(it) }
        }
    }
}

enum class MessageSource {
    /** Vastustajan siirtosarjan mukana tullut viesti. */
    GAME_MESSAGE,

    /** Profiilisivun kautta lähetetty Quick Message. */
    QUICK_MESSAGE,

    /**
     * Sivuston oma ilmoitus, esimerkiksi turnauksen voittajasta. Sivusto itse kutsuu
     * näitä nimellä `telegram message`.
     *
     * Erillinen laji tarkoituksella: `You have Messages!` kattaa nämä samoin kuin
     * henkilöviestit, ja jos ne menisivät samaan säiliöön, arkisto täyttyisi tiedotteista
     * ja se historia jota varten sovellus on tehty hukkuisi joukkoon.
     *
     * **Ratkennut 1.8.2026, ja todennettu kokeella 22.8.2026.** Tässä luki siihen asti
     * avoimena kysymys siitä saapuuko profiilin Quick Message samalla otsikolla kuin
     * ilmoitus. Se ei saavu: otsikot eroavat (`docs/KOHDE.md`, viestiosion taulukko),
     * ja `docs/TALTEENOTTO.md` sekä `raakasivut/LUEMINUT.md` ovat sanoneet niin
     * 1.8.2026 alkaen. Kysymys jäi siis tähän auki kolmeksi viikoksi sen jälkeen kun se
     * oli ratkennut muualla, eli **kielto säilyi kun sen perustelu ratkesi toisessa
     * tiedostossa**, mikä on tässä projektissa nimetty vikamuoto.
     *
     * Koe 22.8.2026 on aiempaa näyttöä vahvempi, koska se on koe eikä havainto: Tommi
     * lähetti toiselta omalta tunnukseltaan viestin profiilin lomakkeella, ja sivu
     * luokitteli sen pikaviestiksi (`DailyGammon Quick Message`, lähettäjä linkkinä,
     * vastauslomake mukana). Tunnuksen lähettämä viesti ei siis päädy tähän lajiin.
     */
    ANNOUNCEMENT,

    /**
     * Otsikko jota lajittelusääntö ei tunnistanut. **Ei arvausta eikä virhe.**
     *
     * Päätös 9.8.2026 (Tommi): oma kenttä ja oma laji, eikä otsikkoa liitetä bodyyn.
     * Kaksi eri asiaa on siksi kahdessa eri paikassa: [Message.rawHeader] säilyttää mitä
     * sivu sanoi, tämä laji tekee tuntemattomasta **löydettävän** listalta. Ilman lajia
     * viesti hautautuisi johonkin kolmesta tunnetusta eikä sitä löytäisi kuin tiivisteellä.
     *
     * Hinta maksettiin nyt eikä myöhemmin, ja syy on mitattavissa: [Message.id] on
     * sisältötiiviste, joten uusi kenttä antaa jo tallennetuille viesteille eri
     * tunnisteen. Kannassa ei ollut päätöshetkellä yhtään viestiä, koska
     * `MessageArchive.archive` oli kutsuttu vain `data`-moduulin omista testeistä.
     * Sama muutos ensimmäisen tallennetun viestin jälkeen olisi vaatinut tunnisteiden
     * uudelleenlaskennan.
     */
    UNKNOWN,
}
