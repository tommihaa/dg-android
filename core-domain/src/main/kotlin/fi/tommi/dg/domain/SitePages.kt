package fi.tommi.dg.domain

/**
 * Sivuston oma Help (`/help`), luettavaksi jäsennettynä.
 *
 * Sivu on staattinen FAQ vuodelta 2010, ei sovelluksen tilaa: kysymyksiä ja vastauksia
 * `h2`-osastoittain. Vastaus on valmiiksi luettavaa tekstiä ([SiteHelpEntry.answer]),
 * koska ruutu ei piirrä sivun HTML:ää vaan sen sisällön; muunnos on jäsentimen työ.
 */
data class SiteHelpPage(val sections: List<SiteHelpSection>)

data class SiteHelpSection(
    val title: String,
    val entries: List<SiteHelpEntry>,
)

data class SiteHelpEntry(
    val question: String,
    /**
     * Vastaus tekstinä: kappaleet tyhjällä rivillä, luettelot `• `-riveinä. Linkit ovat
     * litistyneet näkyväksi tekstikseen, eli lukija näkee mitä sivu sanoo muttei voi
     * seurata sivun sisäisiä viittauksia. Se on tietoinen raja eikä puute: tämä ruutu
     * on manuaali eikä selain.
     */
    val answer: String,
)

/**
 * Sivuston linkkisivu (`/links.html`): ulkoisia osoitteita selitteineen, osastoittain.
 */
data class SiteLinksPage(val sections: List<SiteLinksSection>)

data class SiteLinksSection(
    val title: String,
    val links: List<SiteLink>,
)

data class SiteLink(
    val title: String,
    /**
     * Osoite sellaisenaan sivun omasta linkistä. Nämä ovat ulkoisia sivustoja, joten
     * avaus kuuluu laitteen selaimelle eikä tälle sovellukselle.
     */
    val url: String,
    /** Sivun oma selite linkin vieressä, tai null kun sitä ei ole. */
    val note: String?,
)
