package fi.tommi.dg.app.ui

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Käännöspolun vartija (Tommin tilaus 28.9.2026). `docs/UI.md` › Termien valinta väittää, että
 * kaikki käyttöliittymän teksti on `strings.xml`:ssä, jolloin kieli lisätään kääntämällä eikä
 * koodia muuttamalla. Ennen tätä väitettä vartioi vain muisti, ja vika olisi näkynyt vasta
 * käännöksessä: englanti toimii samalla tavalla kovakoodattuna.
 *
 * Kaksi sääntöä, molemmat luetaan Kotlin-lähteistä:
 *
 * 1. **Tekstiparametrissa ei ole kirjaimia sisältävää literaalia** (`Text("...")`,
 *    `text = "..."`, `contentDescription = "..."` ja muut alla). Merkkijonomallin muuttujat
 *    poistetaan ennen tarkistusta, joten `"$phrase ×"` ja `"$label: $value"` ovat sallittuja:
 *    niissä ei ole käännettävää sanaa.
 * 2. **`stringResource`a ei liimata `+`:lla.** Liimaus kiinnittää sanajärjestyksen koodiin, eikä
 *    kieli jossa luku tulee ensin voi kääntää sitä. Oikea muoto on muotoiluparametri
 *    (`Mark %1$d`). Merkkiliitteet (nuoli, tähti, numerointi) ovat poikkeuslistassa nimeltä.
 *
 * Lint ei käy vartijaksi: `HardcodedText` tuntee XML-näkymät mutta ei Composea.
 */
class I18nGuardTest {

    private val sources: List<File> by lazy {
        File("src/main/kotlin").walkTopDown().filter { it.isFile && it.extension == "kt" }.toList()
    }

    /** Rivit ilman kommenttirivejä, jotta KDocin esimerkit eivät laukaise. */
    private fun codeLines(file: File): List<Pair<Int, String>> =
        file.readLines().mapIndexed { i, line -> i + 1 to line }
            .filterNot { (_, line) -> line.trimStart().let { it.startsWith("//") || it.startsWith("*") || it.startsWith("/*") } }

    private val literalInTextSlot = Regex(
        """(?:\bText\(\s*|\b(?:text|title|placeholder|contentDescription|supportingText)\s*=\s*)"((?:[^"\\]|\\.)*)"""",
    )
    // Mallin muuttujat ja escapet pois: `×` on symboli, vaikka sen kirjoitusasussa on
    // kirjaimia. `label = "..."` ei ole joukossa, koska Composessa se on animaation debug-nimi;
    // näkyvä `label` on lambda `{ Text(...) }`, jonka ensimmäinen haara kattaa.
    private val template = Regex("""\$\{[^}]*}|\$[A-Za-z_][A-Za-z0-9_]*|\\u[0-9a-fA-F]{4}|\\.""")

    @Test
    fun tekstiparametrissaEiOleKovakoodattuaSanaa() {
        val hits = sources.flatMap { file ->
            codeLines(file).flatMap { (n, line) ->
                literalInTextSlot.findAll(line)
                    .filter { template.replace(it.groupValues[1], "").any(Char::isLetter) }
                    .map { "${file.name}:$n  ${line.trim()}" }
            }
        }
        assertTrue("kovakoodattu teksti, siirrä strings.xml:ään:\n" + hits.joinToString("\n"), hits.isEmpty())
    }

    /**
     * Sallitut liimaukset: tiedosto ja rivin tunnistava katkelma, perusteluna se miksi liitteessä
     * ei ole käännettävää. Uusi poikkeus lisätään tähän perusteluineen, ei ohiteta testiä.
     */
    private val allowedConcat = listOf(
        // Lajittelunapin suuntanuoli: symboli, ei sana.
        "TopScreen.kt" to "stringResource(column.labelRes) + when",
        // Tähti on sivuston oma Crawford-merkki pisteluvun perässä, sama kaikilla kielillä.
        "BoardScreen.kt" to "stringResource(R.string.board_score_away, away) + star",
        // Manuaalin numerointi (A, A.1) edeltää otsikkoa kaikilla kielillä.
        "HelpScreen.kt" to "\"\$groupLetter  \" + stringResource(group.heading)",
        "HelpScreen.kt" to "\"\$number  \" + stringResource(item.question)",
    )

    private val concat = Regex("""stringResource\([^()]*(?:\([^()]*\)[^()]*)*\)\s*\+|\+\s*stringResource\(""")

    @Test
    fun stringResourceaEiLiimata() {
        val hits = sources.flatMap { file ->
            codeLines(file).filter { (_, line) -> concat.containsMatchIn(line) }
                .filterNot { (_, line) -> allowedConcat.any { (f, frag) -> f == file.name && frag in line } }
                .map { (n, line) -> "${file.name}:$n  ${line.trim()}" }
        }
        assertTrue("liimattu stringResource, käytä muotoiluparametria:\n" + hits.joinToString("\n"), hits.isEmpty())
    }

    @Test
    fun poikkeuslistaEiOleVanhentunut() {
        val stale = allowedConcat.filterNot { (f, frag) ->
            sources.any { it.name == f && frag in it.readText() }
        }
        assertTrue("poikkeus ei enää osu koodiin, poista se listalta: $stale", stale.isEmpty())
    }
}
