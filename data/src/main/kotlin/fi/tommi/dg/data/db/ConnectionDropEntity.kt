package fi.tommi.dg.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Yhteyskatko kesken teon kannassa. Ks. [fi.tommi.dg.domain.ConnectionDrop].
 *
 * **Oma taulu eikä `pending_actions`in sarake**, koska elinkaaret ovat vastakkaiset: jonon rivi
 * on olemassa juuri niin kauan kuin teon kohtalo on auki, ja tämä rivi on olemassa siksi että
 * kohtalo ratkesi eikä syystä jäänyt mitään. Taulu ei ole `messages`in kaltainen eikä sitä
 * koske sama suoja: rivi on sovelluksen oma havainto, ja sen menetys ei hävitä mitään mitä
 * sivustolla olisi ollut.
 */
@Entity(tableName = "connection_drops")
data class ConnectionDropEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val atEpochMillis: Long,
    val matchId: String?,
    val submit: String,
    val cause: String,
)
