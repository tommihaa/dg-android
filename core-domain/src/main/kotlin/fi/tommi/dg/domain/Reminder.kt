package fi.tommi.dg.domain

/**
 * Muistutus itselle, eli pelaajan oma muistiinpano yhden pelin ajaksi.
 *
 * **Tämä ei ole sivuston ominaisuus.** Selaimessa näkyvä `Reminder`-lohko tulee
 * laajennuksesta `DGText2Area` eikä DailyGammonilta, ja se on mitattu: elementit näkyvät
 * kuvakaappauksessa muttei OkHttpilla haetussa HTML:ssä (`docs/KOHDE.md`). Sivustolla ei siis
 * ole päätä johon tämä kytkettäisiin, joten muistutus on kokonaan sovelluksen omaa tietoa:
 * ei pyyntöä, ei jonon kulutusta, toimii katkossa.
 *
 * **Muistutus on viesti itselle eikä vastustajalle** (`SUBSTANSSI.md` kohta 25), ja syy on
 * kierrospohjaisen pelin perusehto: siirtojen välissä voi olla tunteja tai päiviä, eikä
 * suunnitelma säily pelaajan päässä sen yli. Vastakkainen oletus olisi ollut luonteva, koska
 * nappi jonka nimi on `Remind` voisi yhtä hyvin muistuttaa vastustajaa, ja sellainen olisi
 * kokonaan eri luokan teko.
 */
data class Reminder(
    /** Rivin tunniste kannassa. Nolla tarkoittaa riviä jota ei ole vielä tallennettu. */
    val id: Long,
    val game: GameKey,
    val text: String,
    val createdAtEpochMillis: Long,
)

/**
 * Yhden pelin tunniste ottelun sisällä.
 *
 * **Sivustolla ei ole pelin tunnistetta**, vain ottelun. Lautasivu kertoo ottelutunnuksen ja
 * kierroksen, ja polun tilatunniste muuttuu joka siirrolla, joten kumpikaan ei kelpaa: toinen
 * on liian karkea ja toinen liian hieno.
 *
 * **Tunniste on siksi ottelutunnus ja pistepari** (Tommin päätös 23.8.2026). Perustelu on
 * pisteiden käyttäytymisessä: pisteet muuttuvat vain pelin päättyessä ja kasvavat
 * monotonisesti, joten pari on ottelun sisällä ainutkertainen eikä sama pari voi palata.
 * Peruutus (`rolled back`) ei riko tätä, koska se tapahtuu pelin sisällä eikä koske pisteitä.
 *
 * **Molemmat pisteet vaaditaan, ja puuttuva piste tarkoittaa ettei tunnistetta ole.** Se on
 * varovaisuutta eikä puute: arvattu tunniste näyttäisi muistutuksen väärässä pelissä tai
 * hävittäisi sen oikeasta, ja kumpikin olisi hiljainen virhe. Rahapelissä ja muussa
 * tilanteessa jossa sivu ei kerro pisteitä muistutus ei siis ole käytettävissä lainkaan,
 * ks. `docs/AVOIMET.md`.
 */
data class GameKey(
    val matchId: MatchId,
    /** Vastustajan pisteet, eli sivun ensimmäinen paneeli. */
    val opponentScore: Int,
    /** Kirjautuneen pelaajan pisteet, eli sivun toinen paneeli. */
    val selfScore: Int,
)

/**
 * Tämän laudan peli, tai `null` jos sivu ei kertonut molempia pisteitä.
 *
 * Paneelien järjestys on sivun oma eikä sitä lajitella: kirjautunut on sivulla aina viimeinen
 * ja vastustaja ensimmäinen (mitattu 5.8.2026), ja sama oletus kantaa jo laudan värivalintaa
 * ja sivupaneelin sijaintia. Järjestyksellä on tässä väliä vain siksi, että pari `2-1` ja
 * `1-2` ovat eri pelit.
 */
val BoardState.gameKey: GameKey?
    get() {
        val opponent = players.getOrNull(0)?.score ?: return null
        val self = players.getOrNull(1)?.score ?: return null
        return GameKey(matchId, opponent, self)
    }
