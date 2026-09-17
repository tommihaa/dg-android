package fi.tommi.dg.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Muistutus kannassa. Ks. [fi.tommi.dg.domain.Reminder].
 *
 * **Pelin tunniste on kolme saraketta eikä yksi merkkijono.** Yhdistetty avain (`5302842:8:7`)
 * olisi lyhyempi kirjoittaa, mutta se olisi myös laskettu arvo jonka muoto pitäisi tuntea
 * kyselyssä. Kolme saraketta kertovat mistä avain koostuu ilman että sitä puretaan missään.
 *
 * **Pääavain on juokseva numero eikä pelin tunniste**, koska yhdellä pelillä on monta
 * muistutusta. Tommin pyyntö oli nimenomaan rivejä monikossa, ja se on sama muoto kuin
 * selaimen laajennuksessa.
 *
 * Tämä taulu ei ole `messages`in kaltainen eikä sitä koske sama suoja. Viesti on ainoa kopio
 * sivustolta kadonneesta tiedosta, muistutus on käyttäjän itsensä kirjoittama rivi jonka hän
 * voi kirjoittaa uudestaan. Siksi tällä taululla on poisto ja viesteillä ei.
 */
@Entity(
    tableName = "reminders",
    indices = [Index("matchId", "opponentScore", "selfScore")],
)
data class ReminderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val matchId: String,
    /** Vastustajan pisteet muistutuksen kirjoitushetkellä. Ks. [fi.tommi.dg.domain.GameKey]. */
    val opponentScore: Int,
    /** Kirjautuneen pelaajan pisteet muistutuksen kirjoitushetkellä. */
    val selfScore: Int,
    val text: String,
    val createdAtEpochMillis: Long,
)
