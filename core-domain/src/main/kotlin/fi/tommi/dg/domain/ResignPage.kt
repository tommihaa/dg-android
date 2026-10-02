package fi.tommi.dg.domain

/**
 * Sivuston luovutussivu `/bg/resign` (`DailyGammon: Resignation Page`), mitattu 3.9.2026
 * kaappauksesta `raakasivut/sessio-3-9-resign/0006_bg_resign.html` (`docs/KOHDE.md`).
 *
 * Yksi lomake `POST /bg/resign/doit`, jossa on taulukko `Unfinished matches:` samoin
 * sarakkein kuin profiilin ottelutaulukossa (#, Event, Grace, Time Pool, Round, Length,
 * Opponent, Review) ja rivin viimeisenä soluna valintaruutu jonka **nimi on ottelun
 * tunnus** ja jolla ei ole arvoa. Taulukon alla on sivun oma varmistusruutu `reellysure`
 * tekstillä *Yes! I really want to resign.* ja nappi `Resign Selected Matches`.
 *
 * Luovutus on sivustolla tavallinen valikkotoiminto eikä draama (`SUBSTANSSI.md` kohta 36):
 * keskeyttäminen on pelaajan oma asia. Siksi tämä malli ei lisää sivun päälle omaa
 * varmistusta; sivun oma ruutu on se varmistus, ja se lähetetään sivun omalla nimellä.
 */
data class ResignPage(
    val action: String,
    val method: FormMethod,
    /** Keskeneräiset ottelut sivun järjestyksessä, kukin oman valintaruutunsa kanssa. */
    val rows: List<ResignRow>,
    /** Varmistusruudun kentän nimi, sivulla `reellysure`. Null jos sivulla ei ole sitä. */
    val confirmField: String?,
    /** Varmistusruudun viereinen teksti sivun sanoin. */
    val confirmLabel: String?,
    /** Lähetysnapin teksti sivun sanoin, `Resign Selected Matches`. */
    val submitLabel: String,
) {
    /**
     * Lomake lähetettäväksi valituille otteluille, tai null jos mitään ei ole valittu tai
     * sivulla ei ole varmistusruutua. Arvoton valintaruutu lähtee arvolla `on`, kuten
     * selaimessa. Valinta joka ei ole sivun rivi jätetään pois eikä keksitä.
     */
    fun write(selected: Set<MatchId>): FormSubmission? {
        val confirm = confirmField ?: return null
        val fields = linkedMapOf<String, String>()
        rows.filter { it.match.id in selected }.forEach { fields[it.field] = "on" }
        if (fields.isEmpty()) return null
        fields[confirm] = "on"
        return FormSubmission(action = action, method = method, fields = fields)
    }
}

/** Yksi luovutussivun rivi: ottelu ja sen valintaruudun kentän nimi (ottelun tunnus). */
data class ResignRow(
    val match: Match,
    val field: String,
)

/**
 * Luovutuksen kuittaussivun sisältö, eli sivuston oma lukema teostaan.
 *
 * Mitattu 4.9.2026 (`docs/KOHDE.md`): vastaus `POST /bg/resign/doit`iin on oma sivunsa
 * `DailyGammon: Post Resignation`, jonka runko on yksi lause muotoa `1 match resigned.`
 * Tämä on siis se luku jonka **sivusto** sanoo luovuttaneensa, eikä sama asia kuin
 * luovutussivulta kadonneiden rivien määrä; ne ovat kaksi eri todistetta samasta teosta.
 *
 * [notice] on lause sellaisenaan, koska ruutu näyttää sen sivuston sanoin. [count] on siitä
 * luettu luku, koska sovellus vertaa sitä valittujen määrään.
 */
data class ResignOutcome(
    val count: Int,
    val notice: String,
)
