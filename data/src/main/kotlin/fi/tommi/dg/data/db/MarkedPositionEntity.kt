package fi.tommi.dg.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Merkitty asema kannassa. Ks. [fi.tommi.dg.domain.MarkedPosition].
 *
 * Pelin tunniste on kolme saraketta samasta syystä kuin [ReminderEntity]ssä: yhdistetty
 * avain olisi laskettu arvo jonka muoto pitäisi tuntea kyselyssä. Indeksi on ottelulle
 * eikä pelille, koska merkit luetaan ottelun jälkeen ottelun mitalta eikä yhden pelin.
 *
 * Poisto on olemassa kuten muistutuksilla ja eri syystä kuin viesteillä: rivin kirjoitti
 * käyttäjä itse, ja poisto on se teko jolla hän kuittaa aseman pohdituksi.
 */
@Entity(
    tableName = "marked_positions",
    indices = [Index("matchId")],
)
data class MarkedPositionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val matchId: String,
    /** Vastustajan pisteet merkin kirjoitushetkellä. Ks. [fi.tommi.dg.domain.GameKey]. */
    val opponentScore: Int,
    /** Kirjautuneen pelaajan pisteet merkin kirjoitushetkellä. */
    val selfScore: Int,
    val moveNumber: Int,
    val note: String,
    val createdAtEpochMillis: Long,
    /**
     * Asema merkintähetkellä [fi.tommi.dg.domain.CheckerPosition.encode]-muodossa, tai null.
     * Sarake tuli versiossa 11 (16.9.2026), ja vanhat rivit jäävät nulliksi: asemaa ei voi
     * laskea jälkikäteen, koska sivusto ei säilytä lautaa siirtonumerolla.
     */
    val position: String? = null,
)
