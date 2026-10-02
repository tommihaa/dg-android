package fi.tommi.dg.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Viimeksi nähty ottelu kannassa. Ks. [fi.tommi.dg.domain.SeenMatch].
 *
 * **Pääavain on ottelun numero**, koska rivi kuvaa ottelua eikä havaintoa: uusi havainto
 * kirjoitetaan vanhan päälle eikä sen rinnalle. Yksi rivi per ottelu on koko kasvu, ja se on
 * se hitaus jonka Tommi laski päättäessään muistin haun sijaan (15.9.2026).
 *
 * Tämä taulu ei ole `messages`in kaltainen eikä sitä koske sama suoja. Rivi on sovelluksen
 * oma tiivistelmä sivuista jotka ovat sivustolla yhä, joten sen menetys ei hävitä mitään
 * jota ei voisi nähdä uudestaan.
 */
@Entity(tableName = "seen_matches")
data class SeenMatchEntity(
    @PrimaryKey val matchId: String,
    val opponentName: String?,
    val opponentId: String?,
    val opponentPath: String?,
    /** Kierros ilman sanaa, `4/5` tai `4`. */
    val round: String?,
    val matchLength: Int?,
    val eventName: String?,
    val seenAtEpochMillis: Long,
)
