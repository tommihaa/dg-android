package fi.tommi.dg.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import fi.tommi.dg.app.session.AppTheme
import fi.tommi.dg.app.session.BoardStyle
import fi.tommi.dg.domain.BoardScheme
import fi.tommi.dg.domain.CheckerColor
import fi.tommi.dg.domain.SiteBoardSettings

/** Nappulan täyttöväri ja sen päällä luettava väri, aina parina. */
internal data class CheckerPaint(
    val fill: Color,
    val onFill: Color,
    /** Nappulan reunaviiva; null on sovelluksen oma `Palette.Outline`. Deco antaa kultarenkaan. */
    val ring: Color? = null,
    /**
     * Hiuslinjat renkaan molemmin puolin, emali-inlay (Tommin valinta D 24.9.2026: kultarengas
     * ei erottunut kermanvalkoisesta nappulasta). Null piirtää pelkän renkaan.
     */
    val inlay: Color? = null,
)

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
    /** Puun syy huovalla ja keskikaistalla, null on tasainen pinta, ks. [woodGrain]. */
    val feltGrain: Color? = null,
    /** Puun syy kehyksellä ja muurilla. */
    val frameGrain: Color? = null,
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

        /** Deco-teeman lauta, ks. [DgBoard.Deco]. Roolivärit ja suunta kuten [x22]. */
        fun deco(mirrored: Boolean = false): BoardLook = BoardLook(
            wedgeEven = DgBoard.Deco.WedgeEven,
            wedgeOdd = DgBoard.Deco.WedgeOdd,
            siteCheckers = null,
            mirrored = mirrored,
            felt = DgBoard.Deco.Felt,
            frame = DgBoard.Deco.Frame,
            tray = DgBoard.Deco.Tray,
            trayOutline = DgBoard.Deco.TrayOutline,
            trayColumn = DgBoard.Deco.Tray,
            roleSelf = CheckerPaint(
                DgBoard.Deco.CheckerSelf,
                DgBoard.Deco.OnCheckerSelf,
                DgBoard.Deco.Ring,
                inlay = DgBoard.Deco.Inlay,
            ),
            roleOpponent = CheckerPaint(DgBoard.Deco.CheckerOpp, DgBoard.Deco.OnCheckerOpp, DgBoard.Deco.Ring),
        )

        /** Puulauta, ks. [DgBoard.Wood]. Roolivärit ja suunta kuten [x22]. */
        fun wood(wood: DgBoard.Wood, mirrored: Boolean = false): BoardLook = BoardLook(
            wedgeEven = wood.wedgeEven,
            wedgeOdd = wood.wedgeOdd,
            siteCheckers = null,
            mirrored = mirrored,
            felt = wood.felt,
            frame = wood.frame,
            tray = wood.tray,
            trayOutline = wood.trayOutline,
            trayColumn = wood.trayColumn,
            roleSelf = CheckerPaint(wood.checkerSelf, wood.onCheckerSelf),
            roleOpponent = CheckerPaint(wood.checkerOpp, wood.onCheckerOpp, wood.oppRing),
            feltGrain = wood.feltGrain,
            frameGrain = wood.frameGrain,
        )

        /** Tyylin puu, tai null kun tyyli ei ole puulauta. */
        fun woodOf(style: BoardStyle): DgBoard.Wood? = when (style) {
            BoardStyle.MAPLE -> DgBoard.Wood.Maple
            BoardStyle.WALNUT -> DgBoard.Wood.Walnut
            BoardStyle.OAK_LEATHER -> DgBoard.Wood.OakLeather
            BoardStyle.OLIVE -> DgBoard.Wood.Olive
            else -> null
        }

        /**
         * Vaalean Decon lauta, ks. [DgBoard.DecoLight]. Sama tyyli [BoardStyle.DECO] kuin
         * tummalla, koska vaaleus on laitteen tila eikä pelaajan tallentama valinta.
         */
        fun decoLight(mirrored: Boolean = false): BoardLook = BoardLook(
            wedgeEven = DgBoard.DecoLight.WedgeEven,
            wedgeOdd = DgBoard.DecoLight.WedgeOdd,
            siteCheckers = null,
            mirrored = mirrored,
            felt = DgBoard.DecoLight.Felt,
            frame = DgBoard.DecoLight.Frame,
            tray = DgBoard.DecoLight.Tray,
            trayOutline = DgBoard.DecoLight.TrayOutline,
            trayColumn = DgBoard.DecoLight.Tray,
            roleSelf = CheckerPaint(
                DgBoard.DecoLight.CheckerSelf,
                DgBoard.DecoLight.OnCheckerSelf,
                DgBoard.DecoLight.Ring,
                inlay = DgBoard.DecoLight.Inlay,
            ),
            roleOpponent = CheckerPaint(DgBoard.DecoLight.CheckerOpp, DgBoard.DecoLight.OnCheckerOpp, DgBoard.DecoLight.Ring),
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
         *
         * [decoLight] valitsee Deco-tyylille vaalean laudan. Kutsuja lukee sen
         * [decoLightBoard]ista, koska vaaleus riippuu teemasta ja laitteen tilasta eikä tyylistä.
         */
        fun of(
            style: BoardStyle,
            site: SiteBoardSettings,
            pageScheme: BoardScheme?,
            decoLight: Boolean = false,
        ): BoardLook {
            // Peilaus luetaan ennen tyylihaaraa, koska se koskee molempia tyylejä. Ks.
            // [x22]: ennen 9.9.2026 tämä haara palasi ennen kuin asetusta oli katsottu.
            val mirrored = site.homeBoardsLeft == true
            if (style == BoardStyle.X22) return x22(mirrored)
            if (style == BoardStyle.MONTE_CARLO_VARIANT) return monteCarloVariant(mirrored)
            if (style == BoardStyle.DECO) return if (decoLight) decoLight(mirrored) else deco(mirrored)
            woodOf(style)?.let { return wood(it, mirrored) }

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
 * Sivupaneelin ja lautaruudun ympäristön värit (Tommin päätökset 17.9.2026 ja 27.9.2026,
 * `docs/ASETUKSET.md` luku 4 › Vaalea lautaruutu). Erillinen [BoardLook]ista kahdesta syystä:
 * paneeli piirretään ennen kuin sivun skeema on tiedossa (myös lataus- ja virhetiloissa), ja
 * se riippuu tyylistä, teemasta ja laitteen tilasta eikä sivusta.
 *
 * **Tummassa tilassa paneeli seuraa lautaa.** X-22 ja `SITE` saavat [DEFAULT]in eli
 * täsmälleen entiset värit. Monte Carlo variantti saa lokerosarakkeen puun ja kolme
 * vaaleampaa sävyä, koska entiset jäivät puuta vasten alle WCAG:n 4,5:n (mitatut suhteet
 * ASETUKSET.md:ssä).
 *
 * **Vaaleassa tilassa paneeli seuraa teemaa** (27.9.2026): Deco saa norsunluun
 * ([DECO_LIGHT]) ja Plain luettelon lämpimän valkoisen ([PLAIN_LIGHT]) laudasta riippumatta.
 * Lauta itse ei muutu, joten X-22:n musta huopa jää vaalean kehyksen sisään.
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
    /** Korttien täyttö (pelaajat, ottelu, merkinnät). */
    val card: Color = DgBoard.Palette.Felt,
    /** Pääteksti paneelilla ja korteissa. */
    val text: Color = DgBoard.Palette.TextPrimary,
    /** Toissijainen teksti ja ääriviivanapit. */
    val secondary: Color = DgBoard.Palette.TextSecondary,
    /** Kaikkein hiljaisin teksti korteissa. */
    val faint: Color = DgBoard.Palette.TextMuted,
    /** Linkit, pipit ja korostus. */
    val accent: Color = DgBoard.Palette.Accent,
    /** Pip-ristiriidan punainen. */
    val conflict: Color = DgBoard.Palette.PipConflict,
    /** Ensisijaisen napin täyttö ja teksti. */
    val button: Color = DgBoard.Palette.CheckerSelf,
    val onButton: Color = DgBoard.Palette.OnCheckerSelf,
    /** Peruuttamattoman napin teksti ja reunus ensisijaisen napin päällä. */
    val irreversible: Color = DgBoard.Palette.Cube,
    val irreversibleBorder: Color = DgBoard.Palette.CubeSoft,
    /** Kermainen ääriviivanappi (`Roll Dice`-rivin toinen nappi). */
    val outlineButton: Color = DgBoard.Palette.CheckerSelf,
    /** Onko ympäristö vaalea: järjestelmäpalkin ikonit ja Rarity-värin sävy. */
    val light: Boolean = false,
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

        val DECO = PanelLook(
            background = DgBoard.Deco.PanelBg,
            outline = DgBoard.Deco.PanelOutline,
            muted = DgBoard.Deco.PanelMuted,
            cubeOutline = DgBoard.Deco.Ring,
        )

        /** Vaalea Deco, arvot `docs/ASETUKSET.md` › Vaalea lautaruutu. */
        val DECO_LIGHT = PanelLook(
            background = DgBoard.PanelLight.DecoBg,
            outline = DgBoard.PanelLight.DecoOutline,
            muted = DgBoard.PanelLight.DecoMuted,
            cubeOutline = DgBoard.DecoLight.Ring,
            card = DgBoard.PanelLight.DecoCard,
            text = DgBoard.PanelLight.DecoText,
            secondary = DgBoard.PanelLight.DecoMuted,
            faint = DgBoard.PanelLight.DecoMuted,
            accent = DgBoard.PanelLight.DecoAccent,
            conflict = DgBoard.PanelLight.Conflict,
            button = DgBoard.PanelLight.DecoButton,
            onButton = DgBoard.PanelLight.DecoOnButton,
            irreversible = DgBoard.PanelLight.DecoIrreversible,
            irreversibleBorder = DgBoard.PanelLight.DecoIrreversible,
            outlineButton = DgBoard.PanelLight.DecoMuted,
            light = true,
        )

        /** Vaalea Plain kaikilla laudoilla, arvot `docs/ASETUKSET.md` › Vaalea lautaruutu. */
        val PLAIN_LIGHT = PanelLook(
            background = DgBoard.PanelLight.PlainBg,
            outline = DgBoard.PanelLight.PlainOutline,
            muted = DgBoard.PanelLight.PlainMuted,
            cubeOutline = DgBoard.Palette.Cube,
            card = DgBoard.PanelLight.PlainCard,
            text = DgBoard.PanelLight.PlainText,
            secondary = DgBoard.PanelLight.PlainMuted,
            faint = DgBoard.PanelLight.PlainMuted,
            accent = DgBoard.PanelLight.PlainAccent,
            conflict = DgBoard.PanelLight.Conflict,
            button = DgBoard.PanelLight.PlainAccent,
            onButton = DgBoard.PanelLight.PlainBg,
            irreversible = DgBoard.PanelLight.PlainIrreversible,
            irreversibleBorder = DgBoard.PanelLight.PlainIrreversible,
            outlineButton = DgBoard.PanelLight.PlainMuted,
            light = true,
        )

        /** Vaalea Wood kaikilla laudoilla, kuten [PLAIN_LIGHT] mutta teeman puussa. */
        val WOOD_LIGHT = PanelLook(
            background = DgBoard.PanelLight.WoodBg,
            outline = DgBoard.PanelLight.WoodOutline,
            muted = DgBoard.PanelLight.WoodMuted,
            cubeOutline = DgBoard.Palette.Cube,
            card = DgBoard.PanelLight.WoodCard,
            text = DgBoard.PanelLight.WoodText,
            secondary = DgBoard.PanelLight.WoodMuted,
            faint = DgBoard.PanelLight.WoodMuted,
            accent = DgBoard.PanelLight.WoodAccent,
            conflict = DgBoard.PanelLight.Conflict,
            button = DgBoard.PanelLight.WoodButton,
            onButton = DgBoard.PanelLight.WoodBg,
            irreversible = DgBoard.PanelLight.PlainIrreversible,
            irreversibleBorder = DgBoard.PanelLight.PlainIrreversible,
            outlineButton = DgBoard.PanelLight.WoodMuted,
            light = true,
        )

        /**
         * Puulaudan paneeli: lokerosarakkeen puuta kuten Monte Carlo variantissa, ja samat
         * kaksi vaaleampaa sävyä, ks. [DgBoard.Wood].
         */
        fun wood(wood: DgBoard.Wood): PanelLook = PanelLook(
            background = wood.trayColumn,
            outline = DgBoard.MonteCarloVariant.PanelOutline,
            muted = DgBoard.MonteCarloVariant.PanelMuted,
            cubeOutline = DgBoard.Palette.CubeSoft,
        )

        /** Tumman tilan paneeli lautatyylistä. */
        fun of(style: BoardStyle): PanelLook = when (style) {
            BoardStyle.MONTE_CARLO_VARIANT -> MONTE_CARLO_VARIANT
            BoardStyle.DECO -> DECO
            else -> BoardLook.woodOf(style)?.let { wood(it) } ?: DEFAULT
        }

        /** Paneeli laitteen tilan mukaan: vaaleassa teemasta, tummassa lautatyylistä. */
        fun of(style: BoardStyle, light: Boolean, deco: Boolean, wood: Boolean = false): PanelLook = when {
            !light -> of(style)
            deco -> DECO_LIGHT
            wood -> WOOD_LIGHT
            else -> PLAIN_LIGHT
        }
    }
}

/** Paneeli tälle koostukselle: laitteen tila [dgDark]ista ja teema [LocalAppTheme]sta. */
@Composable
internal fun panelLookFor(style: BoardStyle): PanelLook =
    PanelLook.of(
        style,
        light = !dgDark(),
        deco = LocalAppTheme.current == AppTheme.DECO,
        wood = LocalAppTheme.current == AppTheme.WOOD,
    )

/**
 * Kuution ulkoasu lautatyylin mukaan (Tommin tilaus 22.9.2026: *"teemoita kuutio myös"*).
 * Erillinen [BoardLook]ista samasta syystä kuin [PanelLook]: kuutio piirtyy myös
 * pelaajakortissa, joka ei tunne laudan skeemaa. Oletus on entinen teal-kuutio arvovärein.
 *
 * Deco on mustaa emalia kultareunalla ja viistetyin kulmin (kahdeksankulmio kuten
 * odotuksen deco-kuutiossa), numerot ja robotti kultaa Poiret Onella. Arvovärit eivät kulje
 * Decossa: numero kertoo arvon, ja kulta on teeman ainoa aksentti.
 */
internal class CubeLook(
    val fill: Color,
    val robot: Color,
    val border: Color?,
    val cutCorners: Boolean,
    val fontFamily: androidx.compose.ui.text.font.FontFamily?,
    val bold: Boolean,
    private val valueColor: (Int?) -> Color,
) {
    fun onCube(value: Int?): Color = valueColor(value)

    companion object {
        val DEFAULT = CubeLook(
            fill = DgBoard.Palette.Cube,
            robot = DgBoard.Palette.OnCube,
            border = null,
            cutCorners = false,
            fontFamily = null,
            bold = true,
            valueColor = DgBoard.Palette::onCube,
        )

        val DECO = CubeLook(
            fill = DgBoard.Deco.CubeFill,
            robot = DgBoard.Deco.Ring,
            border = DgBoard.Deco.Ring,
            cutCorners = true,
            fontFamily = PoiretOne,
            bold = false,
            valueColor = { DgBoard.Deco.Ring },
        )

        fun of(style: BoardStyle): CubeLook = if (style == BoardStyle.DECO) DECO else DEFAULT
    }
}

internal val LocalCubeLook = compositionLocalOf { CubeLook.DEFAULT }

/** Lautaruudun paneelivärit; [BoardScreen] tarjoaa, paneelin osat lukevat. */
internal val LocalPanelLook = compositionLocalOf { PanelLook.DEFAULT }
