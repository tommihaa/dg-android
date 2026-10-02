package fi.tommi.dg.domain

/**
 * Ottelun päättymissivu, eli se sivu jonka sivusto antaa kun ottelu on ratkennut, sekä
 * 6.9.2026 alkaen pelin päättymissivu ilman lautaa ([matchContinues]).
 *
 * **Oma tyyppinsä eikä laudan erikoistapaus**, koska sivulla ei ole lautaa lainkaan: siinä on
 * tulos, pistetaulukko ja sivun omat linkit. Mitattu 31.8.2026 kahdesta oikeasta ottelusta
 * (`raakasivut/sessio-31-8-ilta`), ja se korjaa aiemman tilanteen jossa sivu päätyi
 * `NotABoardKind.WrongPage`en: sovellus näytti *"This is no longer a board"* eikä lukenut
 * sivulta mitään.
 *
 * **Sivun tärkein osa ei ole tulos vaan chat-kenttä**, joka on siinä muodossa jonka
 * `ChatParser` jo lukee. Ottelun loppu on se hetki jolloin kiitokset kirjoitetaan, ja
 * DailyGammon ei säilytä keskustelua, joten selaimessa luettu loppuviesti katoaa
 * tavoittamattomiin. Siksi tämä sivu kuuluu sovellukseen eikä selaimeen.
 *
 * Kaikki kentät ovat nullable paitsi [resultText]: kaksi mitattua muunnelmaa eroavat
 * toisistaan (chat-lomake vastaan pelkkä `Next Game` -linkki), eikä puuttuvaa osaa arvata.
 */
data class MatchOverPage(
    val matchId: MatchId?,
    /** Turnauksen tai tapahtuman nimi, esim. "Slower Sevens #4562". */
    val eventName: String?,
    val eventPath: String?,
    /** Kierros sivun omin sanoin, esim. "Round 3". */
    val roundLabel: String?,
    /** Tulos sivun omana lauseena, esim. "tommih wins 2 points and the match." */
    val resultText: String,
    /**
     * Onko kyse pelin päättymisestä kesken ottelun (`true`) vai ottelun lopusta (`false`).
     *
     * Mitattu 6.9.2026: tuplauksen hylkääminen päättää pelin heti, ja sivulla ei ole lautaa
     * lainkaan. Sivun muoto on muuten sama kuin ottelun lopussa, joten se luetaan samalla
     * jäsentimellä; ero on tulosrivin sanoissa (`and the match` puuttuu) ja siinä että
     * lomakkeen nappi on `Next` eikä `Next Game`.
     *
     * Oletus on `false`, koska ottelun loppu oli 31.8.2026 alkaen tämän tyypin ainoa
     * tapaus; oletus koskee siis vanhaa lukijaa eikä uutta sivua.
     */
    val matchContinues: Boolean = false,
    /**
     * Sivun oma merkintä `(Predicted result)`.
     *
     * Merkitsevä tieto eikä koriste: sivusto laskee lopputuloksen etukäteen silloin kun
     * jäljellä olevat siirrot eivät voi enää muuttaa sitä, ja ruudun on sanottava se
     * samoin kuin sivu sanoo.
     */
    val predicted: Boolean,
    val matchLength: Int?,
    /** Pistetaulukon rivit sivun järjestyksessä, voittaja ensin. */
    val scores: List<MatchOverScore>,
    /** `Next Game>>` linkkinä. Muissa muunnelmissa sama teko on lomakkeen nappi, jolloin null. */
    val nextGamePath: String?,
    /**
     * Sivun oma nappilomake silloin kun sivulla ei ole chattia, muuten null.
     *
     * Ks. [MatchOverForm]: chat-muunnelmassa napit ovat chat-lomakkeen omia, eikä samaa
     * nappia lueta kahdesta paikasta.
     */
    val form: MatchOverForm?,
    /** `<<Review Game`, eli pelatun ottelun siirtolista (`MatchLogParser`). */
    val reviewPath: String?,
    val skipPath: String?,
)

data class MatchOverScore(
    /** Kumman pelaajan pisteet, sivun omasta linkistä. */
    val player: PlayerRef,
    val score: Int?,
)

/**
 * Päättymissivun oma lomake, eli `Next Game` ja `To Top` silloin kun sivulla ei ole chattia.
 *
 * **Kolmas muunnelma, mitattu 1.9.2026 laitteelta** (`raakasivut/sessio-1-9`, ottelu 5304226).
 * Kaksi ensimmäistä olivat chat-lomake nappeineen ja pelkkä `Next Game>>` -linkki. Tämä on
 * kolmas: napit ovat olemassa lomakkeena (`<form method=post>`, piilokenttä `commit=1`)
 * mutta chat-kenttää ei ole lainkaan. Ruutu näytti siksi vain `Review game`n ja `Skip game`n,
 * eli ottelusta ei päässyt eteenpäin sovelluksen sisällä; se oli sama jumi jonka takia koko
 * sivutyyppi luettiin, kaventuneena.
 *
 * **Tämä ei ole [ChatForm]in kaksoiskappale vaan sen puuttuva puolisko.** Chat-muunnelmassa
 * samat napit ovat chat-lomakkeen omia, ja ne lähtevät `ChatForm.write`n kautta; tätä tyyppiä
 * ei silloin ole. Kumpi luetaan, ratkeaa siitä onko sivulla tekstikenttää, eikä sama nappi
 * koskaan tule kahdesta lähteestä.
 */
data class MatchOverForm(
    /** `action` sellaisenaan, esim. "/bg/move/5304226/1491". */
    val action: String,
    /** Lomakkeen oma metodi. Mitatulla sivulla POST, mutta sitä ei oleteta täällä. */
    val method: FormMethod,
    /** Piilokentät sellaisinaan, sivulla `commit=1`. Ilman niitä lähetys ei tee mitään. */
    val hiddenFields: Map<String, String>,
    /** Lähetysnappien `value`-arvot sivun järjestyksessä. Nappien nimi on aina `submit`. */
    val submits: List<String>,
)

/**
 * Painaa yhtä sivun tarjoamista napeista.
 *
 * Sama portti kuin [BoardForm.press]issä ja [ChatForm.write]ssä, ja samasta syystä: nappia
 * jota sivu ei tarjoa ei voi painaa, eikä kutsupaikka voi koota lähetystä itse, koska
 * [FormSubmission]in konstruktori on tämän moduulin sisäinen.
 *
 * Piilokentät menevät ensin ja sellaisinaan. `commit=1`:n pudottaminen tuottaisi pyynnön
 * joka näyttää onnistuvan muttei tee mitään, ja se vika on tässä projektissa jo kertaalleen
 * mitattu (`BoardForm.hiddenFields`, 22.8.2026).
 */
fun MatchOverForm.press(submit: String): FormSubmission? {
    if (submit !in submits) return null
    return FormSubmission(
        action = action,
        method = method,
        fields = buildMap {
            putAll(hiddenFields)
            put("submit", submit)
        },
    )
}

/**
 * Sivun omat osoitteet joukkona, eli se mitä käyttäjä voi täältä seurata.
 *
 * [BoardState.ownLinks]in pari, ja olemassa samasta syystä: seurattava osoite on sivun oma
 * eikä kutsujan kokoama. Lomake ei ole tässä, koska se ei ole osoite vaan lomake, ja se
 * kulkee [MatchOverForm.press]in kautta.
 */
fun MatchOverPage.ownLinks(): Set<String> =
    setOfNotNull(nextGamePath, reviewPath, skipPath)
