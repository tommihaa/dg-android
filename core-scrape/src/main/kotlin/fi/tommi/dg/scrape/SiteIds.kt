package fi.tommi.dg.scrape

/**
 * Tunnisteen lukeminen sivuston linkistä, yksi koti kaikille jäsentimille.
 *
 * Nostettu 31.8.2026 kompositioauditoinnin perusteella Tommin päätöksellä. [QuickMessageForms]
 * kirjaa moduulin säännön: nosto toisella käytöllä, koska kaksi lukijaa samalle sivun
 * tosiasialle ajautuvat erilleen. Pelaajatunnus luettiin kahdeksassa ja turnaustunnus
 * viidessä jäsentimessä omalla, sanasta sanaan samalla kopiolla. Sääntö oli siis kirjattu
 * mutta soveltamatta juuri siihen operaatioon jota toistetaan eniten.
 *
 * **Ottelutunnus jäi tarkoituksella nostamatta.** Sitä luetaan kolmella eri kuviolla:
 * [ChatParser] hyväksyy vain `/bg/move/`-polun, [TopPageParser] ja [ProfileParser] myös
 * `/bg/game/`-polun, ja [BoardParser]illa on oma varakuvionsa. Kuviot eivät hyväksy samoja
 * osoitteita, joten yhtenäistäminen voisi muuttaa toimintaa ja odottaa omaa päätöstään.
 */
internal object SiteIds {

    private val USER_ID = Regex("""/bg/user/(\d+)""")
    private val EVENT_ID = Regex("""/bg/event/(\d+)""")

    /** Pelaajatunnus osoitteesta, tai null kun osoite ei ole pelaajalinkki. */
    fun userId(href: String): String? = USER_ID.find(href)?.groupValues?.get(1)

    /** Turnaustunnus osoitteesta, tai null kun osoite ei ole turnauslinkki. */
    fun eventId(href: String): String? = EVENT_ID.find(href)?.groupValues?.get(1)
}
