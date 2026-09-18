package fi.tommi.dg.app.ui

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import fi.tommi.dg.app.session.BoardStyle
import fi.tommi.dg.domain.BoardScheme
import fi.tommi.dg.domain.CheckerColor
import fi.tommi.dg.domain.SiteBoardSettings

/** Nappulan täyttöväri ja sen päällä luettava väri, aina parina. */
internal data class CheckerPaint(val fill: Color, val onFill: Color)

/**
 * Laudan piirtotapa yhtenä arvona: kiilavärit, nappulavärit ja suunta.
 *
 * Tämä on `docs/ASETUKSET.md` luvun 4 sääntö tyyppinä. `SITE`n ja sovelluksen omien
 * tyylien välillä on tasan kolme eroa, ja tämä kantaa niistä kaksi (värit ja suunnan);
 * kolmas eli pip-luvun näkyvyys ei tarvitse kenttää, koska piilotettu pip ei ole sivun
 * paneelissa lainkaan ja muurin välin luku piirtyy paneelista.
 *
 * **Huopa, kehys ja lokero tulivat mukaan 13.9.2026** (Tommin päätös, luvun 4 tarkennus),
 * kun Monte Carlo variant sai vihreän huovan. Sovelluksen omat tyylit saavat erota
 * toisistaan näissä, `SITE` piirtää ne X-22:n väreillä. Kuutio ja mitat ovat yhä
 * sovelluksen omaa eivätkä kulje tässä.
 */
internal class BoardLook private constructor(
    val wedgeEven: Color,
    val wedgeOdd: Color,
    /**
     * Nappulaväri sivun omasta väristä, tai null kun väri on rooli (X-22).
     *
     * Roolivärin menetys sivustouskollisessa tilassa on tarkoitus eikä regressio:
     * juuri se tekee sovelluksesta ja selaimesta rinnastettavia.
     */
    private val siteCheckers: Map<CheckerColor, CheckerPaint>?,
    /**
     * Peilataanko lauta (`Home boards on left side`).
     *
     * Peilaus tehdään **asetuksen perusteella eikä sivun asettelusta päättelemällä**:
     * jäsennin normalisoi suunnan pois ennen mallia, ja piirto saa oman tietonsa omasta
     * lähteestään. `docs/UI.md`:n kielto koskee laudalta päättelyä eikä tätä.
     */
    val mirrored: Boolean,
    val felt: Color = DgBoard.Palette.Felt,
    val frame: Color = DgBoard.Palette.Frame,
    val tray: Color = DgBoard.Palette.Tray,
    val trayOutline: Color = DgBoard.Palette.TrayOutline,
    /**
     * Ulos kannettujen sarakkeen tausta. X-22:ssa ja sivustouskollisessa sama kuin huopa,
     * kuten tähänkin asti. Monte Carlo variantissa kotelon puuta (Tommin päätös 13.9.2026:
     * *"bear-off-paneeli ei"* saa olla huovan värinen), mutta **kehystä tummempaa 17.9.2026
     * alkaen**: kehyksen värisenä sarake sulautui kehykseen eikä sillä ollut reunaa
     * kummallakaan puolella, ks. [DgBoard.MonteCarloVariant.TrayColumn]. `OFF`-lokero
     * erottuu sarakkeesta yhä syvennyksenä.
     */
    val trayColumn: Color = felt,
    /**
     * Keskikaistan tausta, huopa kaikissa tyyleissä. Monte Carlo variantti ensin (Tommin
     * päätös 13.9.2026 viiden kuorimuunnelman jälkeen: *"keskikaista oli parempi huovan
     * värisenä, off-lokero erotettuna"*), jolloin huopa jatkuu yhtenäisenä ja nopat makaavat
     * sillä kuten oikealla laudalla. Muuri piirretään kaistan läpi, ks. `MiddleStrip`. X-22
     * seurasi perässä 14.9.2026 (*"keskikaista pitäisi olla laudan huovan värinen kuten
     * eilen monte carlo variantille tehtiin"*), ja **sivustouskollinen 16.9.2026** (Tommin
     * havainto Pixelin pelisessiossa: *"huopa ja keskikaista pitäisi olla sama väri"*).
     * Kehyksen värinen kaista oli ~~sivustouskollisen oma~~ 14.9. asti; nyt oletus on huopa
     * eikä yksikään tyyli ylikirjoita sitä. Lokerosarake on erillinen valinta.
     */
    val band: Color = felt,
    /** Oman nappulan maali kun väri on rooli (sovelluksen omat tyylit). */
    val roleSelf: CheckerPaint = CheckerPaint(
        DgBoard.Palette.CheckerSelf,
        DgBoard.Palette.OnCheckerSelf,
    ),
    /** Vastustajan nappulan maali kun väri on rooli. */
    val roleOpponent: CheckerPaint = CheckerPaint(
        DgBoard.Palette.CheckerOpp,
        DgBoard.Palette.OnCheckerOpp,
    ),
) {

    /** Sivun väriin sidottu maali, tai null kun tässä tilassa väri on rooli. */
    fun sitePaint(color: CheckerColor): CheckerPaint? = siteCheckers?.getValue(color)

    companion object {

        /**
         * Monte Carlo X-22: roolivärit, ja suunta sivuston asetuksesta.
         *
         * **Suunta ei ollut ennen parametri lainkaan, ja se oli vika** (mitattu kuorella
         * 9.9.2026). `mirrored` oli kiinteästi epätosi, joten `Home boards on left side` ei
         * tehnyt X-22-tyylillä yhtään mitään: lauta ei peilautunut eikä sivupaneeli
         * siirtynyt. Asetus luettiin oikein ja säilössä luki `true`, joten mikään ei
         * kertonut ettei se vaikuta. Vika osui juuri siihen tyyliin joka Tommilla on päällä.
         *
         * **Kaanoni tarkennettiin eikä koodia väännetty sen ympäri** (Tommin päätös
         * 9.9.2026). X-22:n kiinteys koskee kiiloja ja värejä, eli sitä miltä lauta näyttää.
         * Se ei koske sitä kummalla puolella pelaajan kotikenttä on, koska se on pelaajan
         * oma asetus sivustolla eikä lautatyylin ominaisuus. [of] sanoi tämän jo omassa
         * tekstissään ("peilaus tulee eri kentästä kuin värit"), mutta tämä haara palasi
         * ennen sitä.
         */
        fun x22(mirrored: Boolean = false): BoardLook = BoardLook(
            wedgeEven = DgBoard.Palette.WedgeEven,
            wedgeOdd = DgBoard.Palette.WedgeOdd,
            siteCheckers = null,
            mirrored = mirrored,
        )

        /**
         * Monte Carlo variant: vihreä huopa, oranssi ja kerma kiila, valkoinen oma ja musta
         * vastustaja. Roolivärit ja suunta kuten [x22]. Ks. [DgBoard.MonteCarloVariant].
         */
        fun monteCarloVariant(mirrored: Boolean = false): BoardLook = BoardLook(
            wedgeEven = DgBoard.MonteCarloVariant.WedgeEven,
            wedgeOdd = DgBoard.MonteCarloVariant.WedgeOdd,
            siteCheckers = null,
            mirrored = mirrored,
            felt = DgBoard.MonteCarloVariant.Felt,
            frame = DgBoard.MonteCarloVariant.Frame,
            tray = DgBoard.MonteCarloVariant.Tray,
            trayOutline = DgBoard.MonteCarloVariant.TrayOutline,
            trayColumn = DgBoard.MonteCarloVariant.TrayColumn,
            roleSelf = CheckerPaint(
                DgBoard.MonteCarloVariant.CheckerSelf,
                DgBoard.MonteCarloVariant.OnCheckerSelf,
            ),
            roleOpponent = CheckerPaint(
                DgBoard.MonteCarloVariant.CheckerOpp,
                DgBoard.MonteCarloVariant.OnCheckerOpp,
            ),
        )

        /**
         * Ratkaisee piirtotavan tyylistä, säilötyistä asetuksista ja sivun omasta skeemasta.
         *
         * Skeeman ensisijainen lähde on **lautasivu itse** ([BoardScheme] luetaan sivun
         * kuvapoluista): se kertoo mitkä kuvat juuri tällä sivulla olivat, kun taas säilö
         * kertoo mitä asetussivulla luki viimeksi. Säilö on varasuunta sivulle joka ei
         * sanonut skeemastaan mitään.
         *
         * Kaksi tapausta piirtyy X-22-väreillä vaikka tyyli on sivustouskollinen, ja
         * kumpikin on rajaus eikä vika: tuntematon skeema (kummastakaan lähteestä ei
         * tietoa, eikä sivuston värejä saa väittää arvaamalla) ja Mini (ainoa skeema jota
         * sovellus ei tue, eikä sen värejä ole mitattu). Peilaus noudattaa asetusta
         * silti, koska se tulee eri kentästä kuin värit. **Sama koskee X-22:ta 9.9.2026
         * alkaen**, ks. [x22].
         */
        fun of(style: BoardStyle, site: SiteBoardSettings, pageScheme: BoardScheme?): BoardLook {
            // Peilaus luetaan ennen tyylihaaraa, koska se koskee molempia tyylejä. Ks.
            // [x22]: ennen 9.9.2026 tämä haara palasi ennen kuin asetusta oli katsottu.
            val mirrored = site.homeBoardsLeft == true
            if (style == BoardStyle.X22) return x22(mirrored)
            if (style == BoardStyle.MONTE_CARLO_VARIANT) return monteCarloVariant(mirrored)

            val scheme = pageScheme ?: site.scheme
            val colors = when (scheme) {
                BoardScheme.CLASSIC -> DgBoard.SiteClassic
                BoardScheme.BLUE_WHITE -> DgBoard.SiteBlueWhite
                BoardScheme.MINI, null -> null
            }
            if (colors == null) {
                return BoardLook(
                    wedgeEven = DgBoard.Palette.WedgeEven,
                    wedgeOdd = DgBoard.Palette.WedgeOdd,
                    siteCheckers = null,
                    mirrored = mirrored,
                )
            }
            return BoardLook(
                // Parillinen piste on sivustolla tumma kiila, mitattu fixturesta
                // `move_board.html` (13 vaalea, 14 tumma). Ks. [DgBoard.SiteScheme].
                wedgeEven = colors.wedgeDark,
                wedgeOdd = colors.wedgeLight,
                siteCheckers = mapOf(
                    CheckerColor.YELLOW to
                        CheckerPaint(colors.checkerYellow, colors.onCheckerYellow),
                    CheckerColor.BLUE to
                        CheckerPaint(colors.checkerBlue, colors.onCheckerBlue),
                ),
                mirrored = mirrored,
            )
        }
    }
}

/**
 * Sivupaneelin neljä väriä jotka seuraavat lautatyyliä (Tommin päätös 17.9.2026, `docs/ASETUKSET.md`
 * luku 4). Erillinen [BoardLook]ista kahdesta syystä: paneeli piirretään ennen kuin sivun
 * skeema on tiedossa (myös lataus- ja virhetiloissa), ja se riippuu vain tyylistä.
 *
 * X-22 ja `SITE` saavat [DEFAULT]in eli täsmälleen entiset värit. Monte Carlo variantti saa
 * lokerosarakkeen puun ja kolme vaaleampaa sävyä, koska entiset jäivät puuta vasten alle
 * WCAG:n 4,5:n (mitatut suhteet ASETUKSET.md:ssä). Kortit pysyvät mustina, joten korttien
 * sisällä värit eivät kulje tässä.
 */
internal data class PanelLook(
    /** Paneelin ja koko lautaruudun tausta. */
    val background: Color,
    /** Korttien reunaviiva, kun kortilla ei ole omaa korostusta. */
    val outline: Color,
    /** Hiljainen teksti suoraan paneelilla: nauhojen ×, kenttien nimiöt, vahtirivit. */
    val muted: Color,
    /** Kuution omistajan kortin kehys. */
    val cubeOutline: Color,
) {
    companion object {
        val DEFAULT = PanelLook(
            background = DgBoard.Palette.PanelBg,
            outline = DgBoard.Palette.PanelOutline,
            muted = DgBoard.Palette.TextMuted,
            cubeOutline = DgBoard.Palette.Cube,
        )

        val MONTE_CARLO_VARIANT = PanelLook(
            background = DgBoard.MonteCarloVariant.TrayColumn,
            outline = DgBoard.MonteCarloVariant.PanelOutline,
            muted = DgBoard.MonteCarloVariant.PanelMuted,
            cubeOutline = DgBoard.Palette.CubeSoft,
        )

        fun of(style: BoardStyle): PanelLook =
            if (style == BoardStyle.MONTE_CARLO_VARIANT) MONTE_CARLO_VARIANT else DEFAULT
    }
}

/** Lautaruudun paneelivärit; [BoardScreen] tarjoaa, paneelin osat lukevat. */
internal val LocalPanelLook = compositionLocalOf { PanelLook.DEFAULT }
