package fi.tommi.dg.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Viesti kannassa.
 *
 * Pääavain on [fi.tommi.dg.domain.Message.id], eli sisällöstä johdettu tiiviste. Se ei ole
 * suorituskykyvalinta vaan sen suora seuraus että haku on tuhoava: sama sivu voidaan jäsentää
 * useasti, mutta sitä ei voi hakea uudestaan. Sisältötunniste tekee tallennuksesta
 * idempotentin, jolloin uudelleenjäsennys ei tuota duplikaattia eikä vaadi järjestystä.
 *
 * Sivustolla ei ole viestitunnisteita eikä arkistoa lainkaan
 * ("Messages are not saved on this server"), joten juokseva palvelinnumero ei ollut
 * vaihtoehto.
 */
@Entity(
    tableName = "messages",
    indices = [
        Index("matchId"),
        // Arkiston pääsypolku on pelaaja, joten tämä on se sarake jolla haetaan. Ks.
        // [MessageDao.observeByOpponent].
        Index("opponent"),
        // Lista näytetään uusin ensin, ja tämä on se sarake jonka mukaan järjestys tehdään.
        Index("storedAtEpochMillis"),
        // Kysytään kahteen suuntaan: mihin tämä vastasi, ja onko tähän vastattu.
        // Jälkimmäinen on haku tällä sarakkeella, ja siksi indeksi on tässä eikä viilauksena.
        Index("replyToId"),
        // Arkisto luetaan kirjautuneen tilin mukaan (16.9.2026), joten jokainen
        // ruudun kysely rajaa tällä sarakkeella.
        Index("account"),
    ],
)
data class MessageEntity(
    @PrimaryKey val id: String,
    val matchId: String?,
    /**
     * Keskustelukumppani, tai `null` jos sivu ei nimennyt häntä.
     * Ks. [fi.tommi.dg.domain.Message.opponent].
     *
     * Nullable eikä tyhjä merkkijono, ja ero on merkitsevä toisin kuin [rawHeader]illa:
     * tässä `null` tarkoittaa ettei nimeä ollut tiedossa, eikä sitä pidä voida sekoittaa
     * nimeen joka on tyhjä. Nimetön keskustelu ei ole olemassa oleva asia.
     */
    val opponent: String?,
    val sender: String,
    /** Sivulla näkynyt aikaleima raakana. Ei jäsennetä ennen kuin muoto on nähty. */
    val timestampText: String,
    val body: String,
    /**
     * Sivun oma otsikkorivi raakana. Ks. [fi.tommi.dg.domain.Message.rawHeader].
     *
     * Ei-null ja oletukseton: tyhjä merkkijono tarkoittaa ettei sivulla ollut otsikkoa,
     * eikä sitä pidä voida sekoittaa siihen ettei otsikkoa talletettu.
     */
    val rawHeader: String,
    /** [fi.tommi.dg.domain.MessageSource] nimenä. Ks. [MessageMapper]. */
    val source: String,
    /**
     * Milloin tämä laite kirjoitti viestin kantaan. **Ei** viestin lähetysaika: sivuston
     * oma aika on [timestampText], ja se on tulkitsematonta tekstiä.
     *
     * Järjestys tämän mukaan on siis saapumisjärjestys eikä lähetysjärjestys. Ne eroavat
     * silloin kun jonoon on ehtinyt kertyä useita viestejä ennen kuin ne noudetaan.
     */
    val storedAtEpochMillis: Long,
    /**
     * Sen viestin [id], johon tämä on vastaus. Ks. [fi.tommi.dg.domain.Message.replyTo].
     *
     * Nullable ja oletukseton kannassa, koska ennen 24.8.2026 tallennetut rivit eivät kanna
     * tätä tietoa eikä sitä voi johtaa jälkikäteen: sivustolla ei ole ketjuja, joten yhteys
     * on olemassa vain jos se kirjattiin lähetyshetkellä. Vanha vastaus näyttää siis
     * tavalliselta viestiltä, ja se on totta eikä puute.
     */
    val replyToId: String?,
    /**
     * Sivun oman vastauslomakkeen kolme osaa ja pituusraja, tai `null` kaikissa jos sivu
     * ei tarjonnut lomaketta tai rivi on tallennettu ennen 30.8.2026. Ks.
     * [fi.tommi.dg.domain.Message.replyForm].
     *
     * Neljä saraketta eikä yksi serialisoitu kenttä, koska Room ja SQL näkevät ne silloin
     * sellaisinaan eikä muotoa tarvitse keksiä. Lomake kelpaa vain kokonaisena:
     * [MessageMapper] palauttaa `null` jos yksikin kolmesta pakollisesta puuttuu, jotta
     * puolikas rivi ei tuota puolikasta lomaketta.
     *
     * Oletusarvot ovat tässä siksi, että vanhat testit ja kutsupaikat saavat rakentaa
     * entiteetin ilman lomaketta: puuttuva lomake on tavallinen tila eikä reunatapaus.
     */
    val replyAction: String? = null,
    /** [fi.tommi.dg.domain.FormMethod] nimenä, samasta syystä kuin [source]. */
    val replyMethod: String? = null,
    val replyField: String? = null,
    val replyMaxLength: Int? = null,
    /**
     * Tili jonka kirjautuneena tämä laite otti viestin talteen, tai `null` jos rivi on
     * tallennettu ennen 16.9.2026 eikä ensimmäinen kirjautuminen ole vielä ottanut sitä
     * omakseen (`MessageDao.claimUnowned`).
     *
     * Sarake on olemassa siksi, että samalla laitteella voi olla useampi tili (Tommin
     * päätös 15.9.2026, `docs/AVOIMET.md` › Useampi tili samalla laitteella): perheenjäsenet
     * pelaavat eri tileillä, ja sama pelaaja voi pelata eri turnauksia eri tileillä.
     * Ilman saraketta toisen tilin viestit näkyisivät ja veisivät samaan luetteloon ja
     * samaan vientitiedostoon. Yhden laitteen kaanoni ei muutu: se koskee arkiston
     * laitetta eikä tilien määrää laitteella.
     *
     * Ei osa tunnistetta ([fi.tommi.dg.domain.Message.id]) samasta syystä kuin [opponent]:
     * tunniste on viestin sisällöstä, ja omistaja on tämän laitteen tieto.
     */
    val account: String? = null,
)
