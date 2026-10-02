package fi.tommi.dg.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Lähtevä muuttava toiminto joka odottaa yhteyttä.
 *
 * Jono on tarkoituksella **vain lähtevälle** liikenteelle. Hakujonoa ei ole eikä saa olla:
 * `/bg/nextgame` kuluttaa itseään, joten ennakoiva nouto hävittäisi juuri sen viestin jota
 * se yrittää hakea. Lukupyyntö tehdään käyttäjän pyynnöstä tai ei lainkaan.
 *
 * **Taulun muoto vaihtui 10.8.2026, ja muutos on suurempi kuin sarakelista.** Tässä oli
 * siihen asti yksi kenttä `path`, eli sivun linkistä luettu valmis osoite. Se kelpaa
 * linkille muttei lomakkeelle: lomakkeesta lähtevä osoite on `action` ja kentät, ja niistä
 * koottu merkkijono olisi kannassa juuri se koottu osoite jota koko portti kieltää
 * (`fi.tommi.dg.domain.FormSubmission`). Rivi kuvaa siksi **painallusta eikä lähetystä**:
 * millä sivulla oltiin ja mitä nappia painettiin. Perustelu kokonaisuudessaan on
 * `fi.tommi.dg.domain.PendingAction`issa, eikä sitä toisteta tässä.
 *
 * [boardPath] on **sivun omasta linkistä luettu** osoite sellaisenaan. Sitä ei rakenneta
 * täällä eikä missään muuallakaan: DailyGammonin polussa on tilatunniste joka muuttuu
 * askeleittain (`/bg/move/<id>/541` -> `/561`), eikä sen arvaus tuottaisi virhettä vaan
 * väärän siirron oikeassa ottelussa.
 */
@Entity(
    tableName = "pending_actions",
    indices = [Index("matchId")],
)
data class PendingActionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** Mihin otteluun toiminto liittyy, jos liittyy. Chat ilman ottelua on `null`. */
    val matchId: String?,
    /** Lautasivun sisäänkäyntipolku sivun linkistä, esim. `/bg/move/5302842/561`. */
    val boardPath: String,
    /** Painetun napin nimi sivun omalla sanalla, esim. `Submit Move`. */
    val submit: String,
    /**
     * Lomakkeen piilokentän `move`-arvo painallushetkellä, tai `null` kun kenttää ei ollut.
     *
     * Tämä on jonon oma vastaus siihen kysymykseen jota verkkokerros ei voi vastata: menikö
     * teko perille. Palvelin tyhjentää kokoamisen lähetyksen jälkeen, joten sama arvo
     * tuoreella sivulla tarkoittaa että siirto on yhä lähettämättä.
     */
    val pendingMove: String?,
    /** Rastittiko käyttäjä sivun vahvistusruudun. Hänen elensä, ei sovelluksen politiikka. */
    val verified: Boolean,
    val createdAtEpochMillis: Long,
    /**
     * Uusintojen määrä, ja **aina 0 kaudesta 8.9.2026 alkaen**. Uusintanappi poistui silloin,
     * eikä mikään kasvata tätä enää; domainin `PendingAction` ei tunne kenttää lainkaan.
     *
     * Sarake jäi kantaan tarkoituksella. Sen poisto olisi skeemamuutos ja vaatisi migraation,
     * eli oikean riskin kaikkien laitteiden kannoille pelkän siisteyden vuoksi. Tyhjä sarake
     * ei valehtele kenellekään, koska sitä ei lueta mistään.
     */
    val attempts: Int = 0,
    /** Viimeisimmän katkoksen syy raakana, tai `null` jos katkosta ei ole kirjattu. */
    val lastErrorText: String? = null,
)
