package fi.tommi.dg.scrape

import org.jsoup.Jsoup
import org.jsoup.nodes.Document

/**
 * Lähetyksen kuittaussivu.
 *
 * Sivu nähtiin ensi kertaa 22.8.2026, ja se on ainoa tapa erottaa onnistunut lähetys
 * katkenneesta istunnosta: molemmat tulevat 200 OK:lla, ja jälkimmäinen on login-lomake.
 * Tunnistus on `DgPages.isMessageSentPage`, tämä lukee sen sisällön.
 */
object SendResultParser {

    /**
     * Kuittauksen oma lause, esim. `Your message has been sent to vastapelaaja`.
     *
     * **Tämä tallennetaan lähetetyn viestin otsikkoriviksi** (Tommin päätös 22.8.2026).
     * Vaihtoehdot olivat tyhjä otsikko ja oma `MessageSource`, ja tämä valittiin koska se
     * on sivun omaa tekstiä eikä sovelluksen väite, ja koska viestin suunta on siitä
     * johdettavissa uudelleen silloinkin kun muut kentät pettäisivät. Sama peruste kuin
     * `InboxItem.rawHeader`illa: otsikko säilytetään aina, koska laji voidaan johtaa
     * uudelleen vain siitä tekstistä josta se alun perin johdettiin.
     *
     * **Luku vaihtui 4.9.2026, ja syy on kaksi mitattua sivua.** Tässä luettiin runkoa
     * ensimmäiseen `<p>`:hen asti, ja sääntö oli johdettu yhdestä sivusta
     * (`message_sent.html`). Kaksi uutta kuittausta rikkoi sen kumpikin omaan suuntaansa.
     * Sivuutuksen vastauksessa lause on **`<P>`:n sisällä**, joten luettava osa oli tyhjä
     * ja lause katosi (`ignore_done.html`). Kutsun vastauksessa **ei ole yhtään `<p>`:tä**,
     * joten luku ei pysähtynyt mihinkään ja otti navigointipalkin mukaan
     * (`invite_sent.html`); kutsupaikan pituusraja pudotti tuloksen, eli sekin katosi,
     * mutta eri syystä.
     *
     * Sääntö on nyt se mikä on yhteistä kaikille kolmelle mitatulle sivulle: **rungon
     * ensimmäinen tekstirivi, kun navigointipalkki on poistettu.** Palkki on jokaisella
     * näistä sivuista, se on aina taulukko, eikä yhdelläkään ole muuta taulukkoa. Sääntö
     * kestää siis sen että lause on `<p>`:ssä, ilman `<p>`:tä tai kahden rivin päässä
     * navigoinnista.
     */
    fun notice(html: String): String? = notice(Jsoup.parse(html))

    fun notice(document: Document): String? {
        val body = document.body()?.clone() ?: return null
        // Navigointipalkki pois ennen lukua, ks. yllä. Kloonattuna, koska kutsuja voi
        // lukea samaa puuta muuhunkin eikä jäsennin saa muuttaa sitä mitä sille annettiin.
        body.select("table").remove()
        // Jonon jatkolinkki (`Next`, `Continue >`) pois samasta syystä: se on navigointia
        // eikä lausetta. Vastatarjouksen kuittauksessa (14.9.2026, `invitation_countered.html`)
        // linkki on samalla lähderivillä lauseen kanssa, joten rivin luku otti sen mukaan:
        // *Your counter-offer has been sent.Continue >*.
        body.select("a[href*=/bg/nextgame]").remove()
        return body.wholeText()
            .lineSequence()
            .map { it.trim() }
            .firstOrNull { it.isNotEmpty() }
    }
}
