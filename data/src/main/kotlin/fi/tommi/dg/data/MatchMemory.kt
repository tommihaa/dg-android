package fi.tommi.dg.data

import fi.tommi.dg.data.db.SeenMatchDao
import fi.tommi.dg.data.db.toDomain
import fi.tommi.dg.data.db.toEntity
import fi.tommi.dg.domain.MatchId
import fi.tommi.dg.domain.SeenMatch
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Ottelumuisti, eli mitä sovellus on viimeksi nähnyt kustakin ottelusta.
 *
 * **Tämä ei kosketa sivustoon eikä voi koskea**, samalla tavalla kuin [ReminderBook]:
 * toteutus saa vain DAO:n. Rivit kirjoitetaan sivuilta jotka haettiin muusta syystä
 * (otteluluettelo, lauta), ja luetaan kun jokin ruutu tuntee ottelun vain numerolla.
 * Ks. [fi.tommi.dg.domain.SeenMatch].
 *
 * Poistoa ei ole, ja se on valinta eikä puute: rivi on yhden ottelun tiivistelmä, ja
 * päättynytkin ottelu saa jäädä, koska sen numero voi tulla vastaan turnauskaaviossa.
 * Kasvu on yksi rivi per ottelu.
 */
interface MatchMemory {

    /** Kaikki nähdyt ottelut numerolla. Virta, jotta lista päivittyy kun lauta suljetaan. */
    fun observeAll(): Flow<Map<MatchId, SeenMatch>>

    /**
     * Kirjaa havainnon vanhan päälle kenttä kerrallaan ([SeenMatch.mergedWith]): null ei
     * pyyhi tietoa jonka toinen sivu kertoi aiemmin.
     */
    suspend fun remember(seen: SeenMatch)

    suspend fun remember(seen: List<SeenMatch>) = seen.forEach { remember(it) }

    suspend fun count(): Int
}

class RoomMatchMemory(
    private val dao: SeenMatchDao,
) : MatchMemory {

    override fun observeAll(): Flow<Map<MatchId, SeenMatch>> =
        dao.observeAll().map { rows -> rows.associate { MatchId(it.matchId) to it.toDomain() } }

    override suspend fun remember(seen: SeenMatch) {
        val merged = dao.get(seen.matchId.value)?.toDomain()?.mergedWith(seen) ?: seen
        dao.upsert(merged.toEntity())
    }

    override suspend fun count(): Int = dao.count()
}
