package fi.tommi.dg.app.ui

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Merkinnän sääntö luetteloituna: **kumpi katkennut istunto vie kirjautumiseen.**
 *
 * Sama muoto kuin [ActingSurfaceTest]illa ja samasta syystä. `SignOutOnExpiry` tyhjentää
 * tunnukset ja sulkee ruudun, ja se lukee ehdon merkinnästä [SessionExpiredState], joten
 * merkintä on lupaus uloskirjauksesta eikä kuvaus istunnosta. Teon oma tila jättää käyttäjän
 * paikalleen, koska sulkeminen veisi kirjoitetun tekstin ja tallentamattoman valinnan.
 *
 * Kaksi tilaa oli merkitty vastoin sääntöä 1.9.–4.9.2026 (`ReplyUiState.SessionExpired` ja
 * `SettingsSaveResult.SessionExpired`). Kumpikaan ei muuttanut käytöstä, koska ruudut antavat
 * käsittelijälle vain oman tilansa. Juuri se teki niistä H1:n muodon: merkintä oli
 * vaikutukseton siihen asti että joku antaa virran eteenpäin, ja vasta silloin lupaus tekstin
 * säilymisestä olisi kaatunut.
 *
 * **Mitä tämä ei kata, ja se on sama raja kuin merkinnällä itsellään.** Kokonaan uusi
 * hierarkia voi unohtaa merkinnän eikä testi huomaa sitä, koska luettelo on tässä eikä
 * tyypissä. Vartiointi koskee siis kääntymistä väärin päin: merkitty tila jonka ei pitäisi
 * olla merkitty kaatuu tähän.
 */
class SessionExpiryTest {

    /**
     * Tilat ja ihmisen vastaus siihen mitä katkeaminen niissä tarkoittaa. `true` on
     * uloskirjaus ja ruudun sulkeminen, `false` on virhe ruudulla käyttäjän jäädessä
     * paikalleen.
     */
    private val vastaukset: List<Pair<Any, Boolean>> = listOf(
        // Ruudun oma tila: nämä MainActivity antaa SignOutOnExpiryille.
        BoardUiState.SessionExpired to true,
        LoungeUiState.SessionExpired to true,
        DiscussionUiState.SessionExpired to true,
        PageUiState.SessionExpired to true,
        QueueUiState.SessionExpired to true,
        SettingsUiState.SessionExpired to true,

        // Teon oma tila: käyttäjä jää paikalleen ja näkee virheen.
        ReplyUiState.SessionExpired to false,
        SettingsSaveResult.SessionExpired to false,
        ProfileActionUiState.SessionExpired to false,
        ResignUiState.SessionExpired to false,
        ExportUiState.SessionExpired to false,
    )

    @Test
    fun `merkinta kantaa vain niissa tiloissa jotka vievat kirjautumiseen`() {
        vastaukset.forEach { (tila, viekoKirjautumiseen) ->
            assertEquals(
                "${tila::class.qualifiedName} on eri mieltä itsestään",
                viekoKirjautumiseen,
                tila is SessionExpiredState,
            )
        }
    }
}
