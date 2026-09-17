package fi.tommi.dg.domain

/**
 * Päättyneen ottelun `.mat`-tiedosto sellaisena kuin sivusto sen antaa (`/bg/export/<id>`).
 *
 * **Sovellus ei tulkitse sisältöä.** Tiedosto ojennetaan eteenpäin analyysiohjelmalle tai
 * pilveen, ja sen ainoa tarkistus on ettei se ole HTML-sivu: palvelin vastaa 200:lla myös
 * silloin kun se antaa jotain muuta kuin tiedoston, ja jaettu login-sivu `.mat`-nimellä
 * olisi hiljainen väärä tiedosto. Ehto *"point match"* on mitattu yhdestä tallennetusta
 * viennistä (`raakasivut/match_export.mat`, 1.8.2026, ensimmäinen rivi ` 21 point match`)
 * ja on tiedostomuodon oma otsikko; jos sivusto joskus antaa viennin ilman sitä, tulos on
 * näkyvä kieltäytyminen eikä väärä tiedosto.
 *
 * Kaanoni: `SUBSTANSSI.md` kohta 24 sulki tämän vaiheen 15.8.2026, ja Tommi avasi sen
 * 2.9.2026. Rajaus säilyy siinä että tiedosto ei jää sovellukseen: se kirjoitetaan
 * välimuistiin jakoa varten eikä arkistoon.
 */
data class MatchExport(val id: MatchId, val text: String) {

    /** Tiedostonimi jakoon. Tunnus on sivuston oma, joten nimi on vakaa yli jakojen. */
    val fileName: String get() = "dg-${id.value}.mat"

    companion object {
        fun read(id: MatchId, body: String): MatchExport? {
            if (body.isBlank()) return null
            if (body.contains("<html", ignoreCase = true)) return null
            if (!body.contains("point match", ignoreCase = true)) return null
            return MatchExport(id, body)
        }
    }
}
