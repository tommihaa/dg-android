package fi.tommi.dg.app.ui

/**
 * Sivuston lomakenappien tekstit ruudulla.
 *
 * **Sivusto nimeää teot, sovellus nimeää paikat (Tommin päätös 1.9.2026).** Nappien tekstit
 * ovat sivun omia tavuja ja ne piirretään sellaisenaan, koska sivun oma teko näyttää sivun
 * omalta (25.8.2026, `docs/UI.md`). Poikkeus koskee vain kohteen nimeä: kun sivusto viittaa
 * paikkaan jolle sovelluksella on oma nimi, näytetään sovelluksen nimi. `To Top` osoittaa
 * Top Pagelle, ja sovellus on kutsunut sitä `Matches`-välilehdeksi 26.8.2026 alkaen, joten
 * sana `Top` ei esiintynyt sovelluksessa missään muualla kuin tässä yhdessä napissa.
 *
 * **Vain näyttöteksti muuttuu.** Lähetettävä arvo on yhä sivun oma merkkijono
 * ([TO_TOP]), ja kaikki ehdot lukevat sitä eivätkä näyttötekstiä: peruuttamattomien
 * tekojen joukko, kuutiotarkistus ja lomakkeen POST. Käännös on siis yksisuuntainen ja
 * kaikkein uloin kerros.
 *
 * `Next Game` ei käänny, ja se on saman säännön toinen puoli: jonon seuraavalle ottelulle
 * sovelluksella ei ole omaa nimeä, joten sivuston sana on ainoa olemassa oleva.
 */
object SubmitLabels {

    /** Sivuston oma teksti, sama joka lähetetään takaisin `submit`-kenttänä. */
    const val TO_TOP = "To Top"

    /**
     * Osoittaako nappi Top Pagelle, eli siihen paikkaan jolla on sovelluksessa oma nimi.
     *
     * Vertailu on tarkka eikä sisältävä: `Top` osana pidempää sanaa olisi eri nappi, ja
     * arvaus siitä olisi juuri se hiljainen väärä kyllä jota vastaan sivun tavut luetaan
     * sellaisenaan.
     */
    fun isToTopPage(label: String): Boolean = label == TO_TOP
}
