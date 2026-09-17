package fi.tommi.dg.domain

/**
 * Keskustelupalstan indeksi (`/bg/forum2`), eli yhden palstan ketjulista.
 *
 * Palstoja on kaksi, General ja Politics, ja sivu näyttää aina yhden niistä.
 */
data class ForumIndex(
    /** Palstan nimi otsikosta, esim. "General". */
    val boardName: String?,
    /**
     * Sivun oma palstarivi järjestyksessä, esim. General ja Politics. Tyhjä jos riviä ei
     * tunnistettu, ja silloin ruutu näyttää vain ketjulistan.
     */
    val boards: List<ForumBoard>,
    val threads: List<ForumThread>,
    /**
     * Sivun oma `Old Threads` -linkki, esim. `/bg/forum2/main/month`. Indeksi näyttää vain
     * kuluvan hetken ketjut, ja tämä on ainoa reitti vanhempiin; null kun linkkiä ei ole.
     */
    val archivePath: String? = null,
    /**
     * Sivun oma `Add a New Thread` -linkki, esim. `/bg/forum2/main/new`. Null kun linkkiä
     * ei ole (kuukausiarkistossa sitä ei ole).
     */
    val newThreadPath: String? = null,
)

/**
 * Yksi palsta sivun palstarivillä.
 *
 * **Näkyvillä oleva palsta on sivulla lihavoitua tekstiä eikä linkki**, ja se on tässä
 * mallissa [path]in puuttuminen. Valinta luetaan siis sivulta eikä pidetä kirjaa itse.
 */
data class ForumBoard(
    /** Palstan nimi sivun omalla sanalla, esim. "General". */
    val name: String,
    /**
     * Polku palstalle, tai null kun tämä on se palsta jota sivu näyttää. Polku on
     * sivukohtainen: General on General-sivulla `/bg/forum2` ja Politics-sivulla
     * `/bg/forum2/main` (mitattu 27.8.2026), joten sitä ei koota vaan luetaan linkistä.
     */
    val path: String?,
)

/**
 * Yksi ketjurivi indeksissä.
 */
data class ForumThread(
    val title: String,
    /**
     * Lukusivun polku sellaisenaan fragmentteineen, esim.
     * `/bg/forum2/main/read/64107#3`. Fragmentti on palvelimen oma osoitin ensimmäiseen
     * lukemattomaan viestiin, ja se säilytetään koska polkua ei koota.
     */
    val readPath: String,
    /** Viestien määrä ketjussa, tai null kun solu ei ole luku. */
    val postCount: Int?,
    /** Ketjun aloittaja. */
    val poster: String?,
    /** Onko rivillä punainen New-merkki, eli onko ketjussa lukematonta. */
    val isNew: Boolean,
    /**
     * Ketjun aloitusaika sivun sanoin, esim. "Fri Aug 28 18:54:43 2026". Vain
     * kuukausisivulla: indeksissä ei ole aikasaraketta lainkaan, ja silloin tämä on null.
     * Raakateksti eikä jäsennetty päiväys, samasta syystä kuin [ForumPost.postedAtText].
     */
    val postedAtText: String? = null,
)

/**
 * Palstan kuukausiarkisto (`/bg/forum2/<palsta>/month` ja `.../month/<vuosi>/<kk>`).
 *
 * **Sivu on ketjulista siinä missä indeksikin**, ja siksi se käyttää samaa [ForumThread]iä
 * eikä omaa riviään. Erot ovat mitattuja: kuukausisivulla on aikasarake, ei New-merkkejä
 * eikä Hide-linkkejä, ja perässä on linkki jokaiseen kuukauteen tammikuusta 2004 alkaen.
 */
data class ForumArchivePage(
    /** Palstan nimi otsikosta, esim. "General". */
    val boardName: String?,
    /** Sivun oma kuvaus näytettävästä kuukaudesta, esim. "Threads created in August 2026". */
    val monthLabel: String?,
    val threads: List<ForumThread>,
    /** Sivun `Previous Months` -linkit sivun omassa järjestyksessä, uusin ensin. */
    val months: List<ForumMonthLink>,
    /** Paluulinkki palstan indeksiin, sivun oma `Back to Index`. */
    val indexPath: String?,
)

/** Yksi kuukausi arkiston kuukausiluettelossa. */
data class ForumMonthLink(
    /** Kuukauden nimi sivun sanoin, esim. "August 2026". */
    val label: String,
    val path: String,
)

/**
 * Yksi ketju luettuna (`/bg/forum2/main/read/<id>`).
 */
data class ForumThreadPage(
    /** Ketjun otsikko sivun `<h2>`-elementistä. */
    val title: String?,
    val posts: List<ForumPost>,
    /**
     * Sivun oma `Add a Comment` -linkki, esim. `/bg/forum2/main/add/60001`. Polkua ei
     * koota ketjun tunnuksesta vaan luetaan linkistä; null kun linkkiä ei ole.
     */
    val addCommentPath: String? = null,
)

/**
 * Palstan kirjoituslomake: uuden ketjun sivu (`/bg/forum2/<palsta>/new`) tai kommentin
 * lisäyssivu (`/bg/forum2/<palsta>/add/<id>`). Mitattu 3.9.2026, `docs/KOHDE.md`.
 *
 * Sivulla on yksi `POST`-lomake ja **kaksi samannimistä nappia** (`submit`: `Preview` ja
 * lähetys), joten lähetettävä nappi kulkee kenttänä arvoineen eikä sitä jätetä pois.
 * Uuden ketjun lomakkeella on lisäksi otsikkokenttä, jonka pituusraja luetaan sivulta.
 *
 * Palstan viestiä ei voi muokata eikä poistaa itse (`SUBSTANSSI.md` kohta 59), joten
 * kirjoittaminen ei ole kevyt teko: lähetystä edeltää vahvistus, ja se on ruudun asia.
 */
data class ForumComposePage(
    val action: String,
    val method: FormMethod,
    /** Palstan nimi otsikosta, esim. "General". */
    val boardName: String?,
    /** Sivun `<h2>`: uudella ketjulla `New Thread`, kommentilla ketjun otsikko. */
    val heading: String?,
    /** Otsikkokentän nimi, vain uuden ketjun lomakkeella. */
    val titleField: String?,
    /** Otsikkokentän `maxlength` sivun sanoin, tai null jos sitä ei ole. */
    val titleMaxLength: Int?,
    /** Tekstialueen nimi, sivulla `comment`. */
    val commentField: String,
    /** Lähetysnapin nimi, sivulla `submit`. */
    val submitField: String,
    /** Lähetysnapin arvo sivun sanoin: `Create New Thread` tai `Submit Comment`. */
    val submitLabel: String,
) {
    /** Onko tämä uuden ketjun lomake (otsikkokenttä) eikä kommentti. */
    val isNewThread: Boolean get() = titleField != null

    /**
     * Lomake lähetettäväksi. Null jos teksti on tyhjä, tai jos uuden ketjun otsikko on tyhjä
     * tai sivun rajaa pitempi: sivuston oma selain ei päästäisi sitäkään läpi.
     */
    fun write(title: String?, comment: String): FormSubmission? {
        if (comment.isBlank()) return null
        val fields = linkedMapOf<String, String>()
        titleField?.let { field ->
            val value = title?.trim().orEmpty()
            if (value.isEmpty()) return null
            if (titleMaxLength != null && value.length > titleMaxLength) return null
            fields[field] = value
        }
        fields[commentField] = comment
        fields[submitField] = submitLabel
        return FormSubmission(action = action, method = method, fields = fields)
    }
}

/**
 * Yksi viesti ketjussa.
 *
 * Viestillä ei ole sivulla muuta rakennetta kuin ankkuri, runko ja allekirjoitusrivi,
 * joten tässä ei ole enempää kenttiä.
 */
data class ForumPost(
    /** Viestin järjestysnumero sivun omasta `<a name=N>`-ankkurista. */
    val ordinal: Int?,
    /**
     * Runko tekstinä, kappaleet eroteltuina tyhjällä rivillä. Ulkoiset linkit ovat
     * mukana näkyvänä tekstinään; ruutu on lukutilassa eikä avaa niitä.
     */
    val body: String,
    /** Kirjoittaja sivun omasta allekirjoitusrivistä. */
    val author: PlayerRef,
    /**
     * Aikaleima sivun sanoin, esim. "Sat Aug  1 23:40:45 2026". Raakateksti eikä
     * jäsennetty päiväys: sivu ei kerro aikavyöhykettä, ja arvattu vyöhyke olisi väite
     * jota ei voi todentaa.
     */
    val postedAtText: String?,
)
