package fi.tommi.dg.app.ui

import androidx.annotation.StringRes
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import fi.tommi.dg.app.R

/**
 * Pelaajan manuaali.
 *
 * **Ruutu on olemassa yhden päätöksen takia** (Tommi 9.8.2026). Lautaruudun omat napit
 * purettiin ja tilalle tulivat eleet: veto alaspäin hakee sivun uudelleen ja reunapyyhkäisy
 * vie takaisin. Jokainen niistä on näkymätön, ja arvattava ele ei ole ele. Sen sijaan että
 * eleet olisi jätetty löydettäviksi, ne on kirjoitettu tähän. Sivun toimintonapit palasivat
 * lautaruudulle 10.8.2026 pelaamisen myötä, mutta eleet jäivät ja tämä ruutu niiden mukana.
 *
 * Sanavalinta on kanonista: `CLAUDE.md`:n Kenelle-osio rajaa kohderyhmän yhden laitteen
 * **pelaajaksi**, joten teksti puhuu pelaajalle eikä käyttäjälle.
 *
 * Teksti on englantia kuten muukin käyttöliittymä (päätös 5.8.2026), ja samasta syystä:
 * DailyGammonin omat termit ovat sivun tekstiä eikä niille ole suomenkielistä vastinetta.
 *
 * **Manuaali kertoo myös sen mitä sovellus ei tee.** Lupaus kapeni 10.8.2026 lukutilasta
 * painallusehtoon: mikään ei lähde sivustolle ilman pelaajan painallusta, eikä ottelun
 * avaaminen kuluta vuoroa. Pelaajan on voitava luottaa siihen ilman että hän lukee koodia.
 *
 * **Kirjoitettu uusiksi 15.9.2026** (Tommin tilaus: *"tämä pitää olla luotettavaa silloinkin
 * kun sovellus etenee jaettavaksi tai play-kauppaan"*). Jatkossa manuaali on osa toimintoa:
 * käyttäjälle näkyvän toiminnon muutos päivittää sen samassa commitissa (`docs/OHJE.md`), ja
 * `HelpConsistencyTest` vartioi että nimetyt asiat ovat tekstissä.
 *
 * **FAQ-muoto 15.9.2026 illalla** (Tommi: *"aikamoinen wall of text, saisiko siitä FAQ
 * version"* ja *"erot DailyGammonin saittiin myös näkyviksi"*). Kaksi syytä seinään: rivi
 * oli vaakatabletilla noin 190 merkkiä, ja 12 jaksoa oli kerralla auki eikä otsikko kertonut
 * mihin kysymykseen kappale vastaa. Nyt ruudulla on kysymyslista ryhmittäin, vastaus aukeaa
 * napautuksella, ja lukupinta on rajattu [READING_WIDTH]:iin. Erot sivustoon ovat oma ryhmä
 * väitteinä, koska ero on hyödyllisin juuri väitteenä; muut ryhmät ovat kysymyksiä.
 * Vastaustekstit ovat entiset kappaleet, joten `docs/OHJE.md`:n totuustaulukko pätee
 * avaimittain.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    Scaffold(
        modifier = modifier,
        // Läpinäkyvä, jotta ruudun tausta ja sen kuvio näkyvät läpi: ne
        // maalataan kerran `MainActivity`ssä (`Modifier.dgScreenBackground`).
        containerColor = Color.Transparent,
        topBar = {
            // Back-nappi palasi 26.8.2026 kun manuaali siirtyi Info-välilehden sisään:
            // se ei ole enää oma välilehtensä vaan listarivin takana, ja porautumisesta on
            // päästävä takaisin. Järjestelmä-back tekee saman, ks. `MainActivity`.
            TopAppBar(
                colors = dgTopAppBarColors(),
                title = { Text(stringResource(R.string.help_title)) },
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text(stringResource(R.string.board_back))
                    }
                },
            )
        },
    ) { insets ->
        // Kehys mittaa sarakkeen korkeuden ja piirtää täytekuvan sen alle (`DgFill.kt`).
        DgFillFrame(modifier = Modifier.padding(insets)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .dgReadingSurface(),
                contentAlignment = Alignment.TopCenter,
            ) {
                // Lukupinta täyttää leveyden, teksti ei: vaakatabletilla koko leveys antoi
                // noin 190 merkin rivin, ja silmä ei löydä seuraavan rivin alkua.
                Column(
                    modifier = Modifier
                        .widthIn(max = READING_WIDTH)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                ) {
                    // Järjestys on lukujärjestys pelaajalle joka avaa sovelluksen ensimmäistä
                    // kertaa: mikä tämä on, mikä eroaa sivustosta, välilehdet, luettelo, lauta,
                    // viestit, muut välilehdet ja asetukset, ja lopuksi lupaus siitä mitä ei
                    // tapahdu. `HelpConsistencyTest` vartioi että jokainen `help_q_*` on tässä.
                    // Yksi vastaus auki kerrallaan ja tunnus muotoa ryhmä.kohta, ryhmä
                    // kirjaimena (Tommi 15.9.2026 illalla: "numeroida, jotta niihin olisi
                    // helpompi viitata", "siten, että numeron poistuessa kaikki ei mene
                    // uusiksi" ja "ryhmät ehkä numeroinnin sijaan aakkosilla"). Juokseva
                    // numero olisi siirtänyt kaikki seuraavat; ryhmäkohtainen siirtää vain
                    // saman ryhmän loppupään, ja kirjain erottaa ryhmän kohdasta (B.7).
                    var open by rememberSaveable { mutableStateOf<Int?>(null) }
                    HELP_GROUPS.forEachIndexed { g, group ->
                        HelpGroupView(
                            group = group,
                            groupLetter = ('A' + g).toString(),
                            openQuestion = open,
                            onToggle = { q -> open = if (open == q) null else q },
                        )
                    }
                }
            }
        }
    }
}

/** Noin 80 merkkiä `bodyMedium`-tekstiä; sama luokka kuin kirjan palsta. */
private val READING_WIDTH = 640.dp

internal data class HelpItem(@StringRes val question: Int, @StringRes val answer: Int)

internal data class HelpGroup(@StringRes val heading: Int, val items: List<HelpItem>)

internal val HELP_GROUPS = listOf(
    HelpGroup(R.string.help_group_about, listOf(HelpItem(R.string.help_q_about, R.string.help_a_about))),
    HelpGroup(
        R.string.help_group_differences,
        listOf(
            HelpItem(R.string.help_q_d_messages, R.string.help_a_d_messages),
            HelpItem(R.string.help_q_d_session, R.string.help_a_d_session),
            HelpItem(R.string.help_q_d_sort, R.string.help_a_d_sort),
            HelpItem(R.string.help_q_d_local, R.string.help_a_d_local),
            HelpItem(R.string.help_q_d_place, R.string.help_a_d_place),
            HelpItem(R.string.help_q_d_tournament, R.string.help_a_d_tournament),
            HelpItem(R.string.help_q_d_archive, R.string.help_a_d_archive),
            HelpItem(R.string.help_q_d_accounts, R.string.help_a_d_accounts),
            HelpItem(R.string.help_q_d_reminders, R.string.help_a_d_reminders),
            HelpItem(R.string.help_q_d_marks, R.string.help_a_d_marks),
            HelpItem(R.string.help_q_d_look, R.string.help_a_d_look),
            HelpItem(R.string.help_q_d_confirm, R.string.help_a_d_confirm),
            HelpItem(R.string.help_q_d_unconfirmed, R.string.help_a_d_unconfirmed),
        ),
    ),
    HelpGroup(R.string.help_group_tabs, listOf(HelpItem(R.string.help_q_tabs, R.string.help_a_tabs))),
    HelpGroup(
        R.string.help_group_matches,
        listOf(
            HelpItem(R.string.help_q_order, R.string.help_a_order),
            HelpItem(R.string.help_q_open, R.string.help_a_open),
            HelpItem(R.string.help_q_tournaments, R.string.help_a_tournaments),
        ),
    ),
    HelpGroup(
        R.string.help_group_board,
        listOf(
            HelpItem(R.string.help_q_move, R.string.help_a_move),
            HelpItem(R.string.help_q_forced, R.string.help_a_forced),
            HelpItem(R.string.help_q_double, R.string.help_a_double),
            HelpItem(R.string.help_q_unconfirmed, R.string.help_a_unconfirmed),
            HelpItem(R.string.help_q_gestures, R.string.help_a_gestures),
            HelpItem(R.string.help_q_beside, R.string.help_a_beside),
            HelpItem(R.string.help_q_skip, R.string.help_a_skip),
            HelpItem(R.string.help_q_look, R.string.help_a_look),
        ),
    ),
    HelpGroup(
        R.string.help_group_messages,
        listOf(
            HelpItem(R.string.help_q_take, R.string.help_a_take),
            HelpItem(R.string.help_q_queue, R.string.help_a_queue),
            HelpItem(R.string.help_q_reply, R.string.help_a_reply),
            HelpItem(R.string.help_q_archive, R.string.help_a_archive),
            HelpItem(R.string.help_q_one_device, R.string.help_a_one_device),
            HelpItem(R.string.help_q_move_device, R.string.help_a_move_device),
        ),
    ),
    HelpGroup(
        R.string.help_group_lounge,
        listOf(
            HelpItem(R.string.help_q_join, R.string.help_a_join),
            HelpItem(R.string.help_q_forum, R.string.help_a_forum),
            HelpItem(R.string.help_q_settings, R.string.help_a_settings),
        ),
    ),
    HelpGroup(R.string.help_group_never, listOf(HelpItem(R.string.help_q_never, R.string.help_a_never))),
)

@Composable
private fun HelpGroupView(
    group: HelpGroup,
    groupLetter: String,
    openQuestion: Int?,
    onToggle: (Int) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = "$groupLetter  " + stringResource(group.heading),
            style = MaterialTheme.typography.titleSmall,
            // Sama sääntö kuin muualla, ks. `LoungeScreen.SectionHeading`.
            color = MaterialTheme.colorScheme.primary,
        )
        group.items.forEachIndexed { i, item ->
            HelpItemView(
                number = "$groupLetter.${i + 1}",
                item = item,
                open = openQuestion == item.question,
                onToggle = { onToggle(item.question) },
            )
        }
    }
}

/**
 * Numeroitu kysymys aina näkyvissä, vastaus napautuksella. Avoin kohta säilyy käännön yli
 * (`rememberSaveable` kutsujassa), koska manuaalia luetaan usein juuri laitetta kääntäen.
 * Numero on `stringResource`in ulkopuolella, jotta kysymysteksti pysyy sellaisenaan
 * `HelpConsistencyTest`in luettavana.
 */
@Composable
private fun HelpItemView(number: String, item: HelpItem, open: Boolean, onToggle: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .animateContentSize()
            .padding(vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = "$number  " + stringResource(item.question),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        if (open) {
            Text(
                text = stringResource(item.answer),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 24.dp),
            )
        }
    }
}
