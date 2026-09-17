package fi.tommi.dg.domain

/**
 * Game Lounge kokonaisuutena: kutsut turnauksen ulkopuolisiin otteluihin ja
 * turnausten ilmoittautumislista.
 *
 * Loungessa ei ole käynnissä olevia otteluita (mitattu 22.8.2026, `docs/KOHDE.md`),
 * joten tämä malli ei tiedä otteluista mitään.
 */
data class LoungePage(
    /** Tervehdyksestä luettu nimi, esim. "Welcome to the DailyGammon waiting lounge, X." */
    val userName: String?,
    val invitations: List<LoungeInvitation>,
    val tournaments: List<LoungeTournament>,
    /**
     * Pelaajalistan polku sivun omasta linkistä (*List of Players*), tai null jos linkkiä
     * ei ole. Sama syy kuin `TopPage.loungePath`illa: sivuston oma linkki on ainoa lähde
     * joka ei ole arvaus.
     */
    val playerListPath: String? = null,
    /** Tournament Hallin polku sivun omasta linkistä, tai null jos linkkiä ei ole. */
    val tournamentHallPath: String? = null,
    /**
     * Loungen oma pelaajahaku, tai null jos lomaketta ei ole sivulla. Luettu sivulta
     * kenttineen (Tommin tilaus 3.9.2026, aiemmin rajattu ulos lähetyksenä).
     */
    val searchForm: PlayerSearchForm? = null,
)

/**
 * Pelaajahaun lomake `POST /bg/plist`: tekstikenttä `like` ja piilokenttä `type=name`
 * (mitattu loungesta 27.8.2026, `docs/KOHDE.md`). Sivun sanoin *"Find a player whose name
 * starts with"*, eli haku on alkuosahaku eikä sisältöhaku.
 *
 * Sama doktriini kuin [ReplyForm]illa: osoite, metodi ja kenttien nimet luetaan sivulta,
 * eikä mitään niistä kirjoiteta koodiin. Piilokentät lähtevät sellaisinaan sivun
 * järjestyksessä, koska ilman `type`-kenttää palvelin ei tiedä mitä haetaan.
 */
data class PlayerSearchForm(
    val action: String,
    val method: FormMethod,
    /** Hakusanan kenttä, esim. `like`. */
    val field: String,
    /** Piilokentät nimineen ja arvoineen, sivun järjestyksessä. */
    val hidden: Map<String, String> = emptyMap(),
    val maxLength: Int? = null,
)

/** Hakulomake täytettynä, tai null kun hakusana on tyhjä: tyhjää hakua ei lähetetä. */
fun PlayerSearchForm.write(query: String): FormSubmission? {
    val text = query.trim()
    if (text.isEmpty()) return null
    return FormSubmission(action = action, method = method, fields = mapOf(field to text) + hidden)
}

/**
 * Yksi rivi taulukosta "Matches waiting for opponents:".
 */
data class LoungeInvitation(
    /** Pelimuoto sivun sanoin: backgammon, nack tai double-repeat. */
    val variant: String?,
    /** Ottelun pituus, tai null kun solu ei ole luku (rahapelit). */
    val length: Int?,
    /** Tarjoaja sivun omasta linkistä. */
    val player: PlayerRef,
    /** Aikaraja sivun sanoin, esim. "Once a Day (200/+4/24)". */
    val timeout: String?,
    /** Tarjoajan kommentti, tai null kun solu on tyhjä. */
    val comment: String?,
    /**
     * Join-linkin href sellaisenaan kyselyineen, esim.
     * `/bg/lounge?action=accept&id=11&userid=20311`, tai null kun rivillä ei ole
     * Join-linkkiä (oma tarjous).
     *
     * Polku luetaan sivulta eikä koota, samasta syystä kuin `TopPage.messageQueuePath`
     * ja painavammin: hyväksyminen aloittaa ottelun heti, joten väärin koottu osoite ei
     * tuottaisi virhettä vaan peruuttamattoman teon.
     */
    val joinPath: String?,
)

/**
 * Yksi rivi taulukosta "Tournament Sign-Up".
 *
 * Rivin viimeinen solu on yksi kolmesta (mitattu 3.9.2026 kaappauksesta
 * `sessio-3-9/0004_bg_lounge.html`, `docs/KOHDE.md`): `Sign Up` -linkki kun
 * ilmoittautuminen on auki, `Cancel Signup` -linkki kun tähän turnaukseen on jo
 * ilmoittauduttu, tai pelkkä `&nbsp;` kun kumpikaan ei ole tarjolla. [signupPath] ja
 * [cancelPath] eivät siis ole yhtä aikaa nollasta poikkeavia, ja sivu kertoo itse kumpi
 * on voimassa. Ilmoittautumisen toteutus on `docs/AVOIMET.md`:n päätöskohta.
 */
data class LoungeTournament(
    val name: String?,
    /** Turnauksen numero `/bg/event/`-linkistä. */
    val eventId: String?,
    /** Turnaussivun polku sellaisenaan. */
    val eventPath: String?,
    val variant: String?,
    val length: Int?,
    val rounds: Int?,
    /** Aikapoolin perusarvo sivun sanoin, esim. "150:00". */
    val timeText: String?,
    /** Lisäys per siirto sivun sanoin, esim. "+2:00". */
    val incrementText: String?,
    /** Grace sivun sanoin, esim. "24:00". */
    val graceText: String?,
    /** Sign Up -linkin href sellaisenaan, tai null kun ilmoittautuminen ei ole auki. */
    val signupPath: String?,
    /**
     * Cancel Signup -linkin href sellaisenaan, tai null kun tähän turnaukseen ei ole
     * ilmoittauduttu. Linkin olemassaolo on samalla sivun oma tieto siitä että
     * ilmoittautuminen on voimassa.
     */
    val cancelPath: String? = null,
    /**
     * Onko rivin perässä `Has Note` -solu. Turnauksen järjestäjän vapaa teksti asuu
     * turnaussivulla, ja se on tarkoitettu luettavaksi ennen ilmoittautumista
     * (`SUBSTANSSI.md`), joten ruutu voi sanoa sen olemassaolon ennen tekoa.
     */
    val hasNote: Boolean = false,
)

/**
 * Painaa kutsun oman Join-linkin, eli hyväksyy tarjouksen.
 *
 * Kolmas ja toistaiseksi viimeinen [FormSubmission]in tuottaja `BoardForm.press`in ja
 * `ReplyForm.write`n rinnalla; sopimusmuutos on kirjattu `docs/UI.md`:hen 26.8.2026.
 * Takuu on sama kokoamattomuus: lähetys on sivun oma href sellaisenaan tyhjin kentin,
 * joten linkin oma kysely kulkee koskemattomana eikä mitään kasata.
 *
 * Join on sivulla tavallinen GET-linkki eikä lomake (mitattu kaappauksesta
 * `raakasivut/sessio/0574_bg_lounge.html`), joten metodi on GET vakiona eikä luettuna:
 * luettavaa `method`-attribuuttia ei ole olemassa.
 *
 * Palauttaa null kun rivillä ei ole Join-linkkiä, eli omaa tarjousta ei voi hyväksyä.
 */
fun LoungeInvitation.join(): FormSubmission? =
    joinPath?.let { FormSubmission(action = it, method = FormMethod.GET, fields = emptyMap()) }

/**
 * Painaa rivin oman Sign Up -linkin, eli ilmoittautuu turnaukseen.
 *
 * Neljäs [FormSubmission]in tuottaja (Tommin tilaus 3.9.2026, kaanonimuutos
 * `SUBSTANSSI.md` kohtaan 52). Sama takuu kuin [join]illa: sivun oma href sellaisenaan
 * tyhjin kentin, GET vakiona koska linkki ei ole lomake. Null kun ilmoittautuminen ei
 * ole auki tai siihen on jo ilmoittauduttu.
 */
fun LoungeTournament.signUp(): FormSubmission? =
    signupPath?.let { FormSubmission(action = it, method = FormMethod.GET, fields = emptyMap()) }

/**
 * Painaa rivin oman Cancel Signup -linkin, eli peruu ilmoittautumisen. Viides tuottaja,
 * sama takuu. Null kun riviltä puuttuu linkki, eli ilmoittautumista ei ole voimassa.
 */
fun LoungeTournament.cancelSignup(): FormSubmission? =
    cancelPath?.let { FormSubmission(action = it, method = FormMethod.GET, fields = emptyMap()) }
