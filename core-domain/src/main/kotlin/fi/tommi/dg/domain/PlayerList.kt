package fi.tommi.dg.domain

/**
 * Pelaajalista `/bg/plist`, eli koko sivuston pelaajat sadan rivin sivuina.
 *
 * **Sivu on koko sivuston lista eikä pelaajan oma**, samoin kuin Tournament Hall. Oletus
 * on lajittelu ratingin mukaan, joten ensimmäinen sivu on sivuston kärki eikä sillä ole
 * mitään tekemistä katsojan oman sijoituksen kanssa.
 */
data class PlayerList(
    val players: List<PlayerRow>,
    /**
     * Sivun omat lajittelu- ja sivutuslinkit järjestyksessä ja sivun omilla sanoilla
     * (*Sort By Name*, *Next 100*). Näitä ei luokitella lajittelijoiksi ja sivuttajiksi,
     * koska ero olisi luettava linkin tekstistä eli arvattava; ruutu näyttää ne rivinä
     * sellaisenaan.
     */
    val links: List<PlayerListLink>,
)

/**
 * Yksi rivi pelaajalistassa.
 *
 * Rating ja kokemus ovat tekstinä eivätkä lukuina, samasta syystä kuin ottelurivin
 * `graceText`: sivun oma muoto säilyy, eikä desimaalien tai tuhaterottimen tulkinta
 * lipsahda väitteeksi jota sivu ei tee.
 */
data class PlayerRow(
    /** Sijoitus listalla sivun omassa lajittelussa, esim. 1. Null jos solu ei ole luku. */
    val rank: Int?,
    /** Rivin pelaaja: nimi, numero ja profiilin polku sivun omasta linkistä. */
    val player: PlayerRef,
    /** Rating sivun sanoin, esim. "2326.39". */
    val ratingText: String?,
    /** Kokemuspisteet sivun sanoin, esim. "10765". */
    val experienceText: String?,
)

/** Sivun oma linkki tekstinä ja polkuna. */
data class PlayerListLink(
    val label: String,
    val path: String,
)
