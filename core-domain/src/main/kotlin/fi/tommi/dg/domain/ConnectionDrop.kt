package fi.tommi.dg.domain

/**
 * Yhteyskatko kesken teon, eli yksi `Board not confirmed` -kortti historiana.
 *
 * **Tämä on olemassa siksi että kortin syy katosi kortin mukana** (Tommin tilaus 18.9.2026,
 * *"punainen yläpalkki on esiintynyt tallentamattomissa pelisessioissa ilman huomaamaani
 * yhteyskatkoa"*). Jonon rivi (`PendingAction`) kantaa poikkeuksen luokan nimen, mutta rivi
 * poistuu kun seuraava teko onnistuu tai käyttäjä painaa OK, eikä sovellus lokita sitä
 * mihinkään. Laitteen kannasta luettu laskuri sanoi 18.9.2026 että katko oli osunut 41 kertaa,
 * eikä yhdestäkään ollut jäljellä muuta kuin se luku. Tämä rivi jää.
 *
 * **Rivi on loki eikä tila.** Mikään ei lue sitä takaisin päätöksen pohjaksi: kortti ja jono
 * toimivat kuten ennen, ja tämä on lukijalle. Siksi [cause] saa olla teksti (poikkeuksen
 * luokan nimi, `SocketTimeoutException` tai `UnknownHostException`), samasta syystä kuin
 * jonon `lastErrorText`.
 *
 * Kirjataan vain katko (`DgResponse.Offline`), ei sivuston virhettä eikä nukkumisilmoitusta,
 * koska ne ovat sivuston omia vastauksia ja näkyvät sirussa eikä kortissa (Tommin päätös
 * 17.9.2026, *"palkki vain yhteyskatkosta"*). Historia vastaa kysymykseen milloin kortti tuli
 * ja miksi, ja sen rajaus on sama kuin kortin.
 */
data class ConnectionDrop(
    /** Rivin tunniste kannassa. Nolla tarkoittaa riviä jota ei ole vielä tallennettu. */
    val id: Long,
    val atEpochMillis: Long,
    /** Ottelu jossa teko oli, tai `null` kun teko ei liittynyt otteluun. */
    val matchId: MatchId?,
    /** Painetun napin nimi sivun omalla sanalla, esim. `Submit Move`. */
    val submit: String,
    /** Poikkeuksen luokan nimi verkkokerroksesta, esim. `SocketTimeoutException`. */
    val cause: String,
)
