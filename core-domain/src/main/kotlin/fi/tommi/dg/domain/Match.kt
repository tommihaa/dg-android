package fi.tommi.dg.domain

/**
 * Yksi ottelu sellaisena kuin se näkyy otteluluettelossa (Top Page tai profiilisivu).
 *
 * Aikakentät ovat tarkoituksella tekstiä eivätkä kestoja. Sivulla ne näyttävät muodolta
 * `278:29` ja `0:00`, mutta yksikkö ei ole luettavissa sivulta: tunnit ja minuutit on
 * uskottava tulkinta korrespondenssipelissä, mutta se on silti tulkinta. Muunnos tehdään
 * vasta kun yksikkö on varmistettu, eikä arvausta piiloteta tyyppiin.
 */
data class Match(
    /** Pelin tunniste, sama molemmissa linkeissä: /bg/game/<id>/... ja /bg/move/<id>/... */
    val id: MatchId,
    val eventName: String,
    val eventId: String?,
    /**
     * Turnaussivun polku sivun omasta linkistä, tai null kun tapahtumalla ei ole linkkiä.
     * Ystävyysottelulla se on pelkkää tekstiä (mitattu 22.8.2026), joten puuttuva linkki
     * on sivun muoto eikä puuttuva tieto.
     */
    val eventPath: String? = null,
    /** Vastustaja sivun omasta linkistä. Tyhjä viittaus kun riviltä puuttuu linkki. */
    val opponent: PlayerRef,
    /**
     * Voiko käyttäjä siirtää nyt. Luetaan taulukon captionista, ei riviltä: DailyGammon
     * ryhmittelee ottelut taulukoihin otsikolla "Matches where you can move:".
     *
     * **null tarkoittaa ettei sivu kerro sitä**, ei sitä että vuoroa ei ole. Profiilisivun
     * `Active games:` on juuri tällainen: siellä ottelut ovat, mutta vuorosta ei ole sanaa.
     * `false` olisi siinä arvaus, ja se olisi väärä juuri niissä otteluissa joita
     * käyttäjä eniten etsii.
     */
    val myTurn: Boolean?,
    /** Kierros muodossa "5/6", eli monesko peli monesta. */
    val round: String?,
    /** Ottelun pituus pisteinä, esim. 9. */
    val matchLength: Int?,
    val graceText: String?,
    val timePoolText: String?,
    /** Polku siirtonäkymään, esim. /bg/move/5302842/541 */
    val playPath: String?,
    /** Polku ottelun selaukseen, esim. /bg/game/5302842/0/list */
    val reviewPath: String?,
    /**
     * Polku `.mat`-vientiin, esim. /bg/export/5259276.
     *
     * Linkin olemassaolo on samalla merkki siitä että ottelu on päättynyt: keskeneräisellä
     * ottelulla sitä ei ole lainkaan.
     */
    val exportPath: String? = null,
)

@JvmInline
value class MatchId(val value: String) {
    init {
        require(value.isNotBlank()) { "MatchId ei voi olla tyhjä" }
    }

    override fun toString(): String = value
}
