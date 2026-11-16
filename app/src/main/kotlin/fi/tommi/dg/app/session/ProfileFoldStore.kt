package fi.tommi.dg.app.session

import android.content.Context
import android.content.SharedPreferences

/**
 * Profiilin `Active games`- ja `Finished matches` -otsikoiden taitto laitteella.
 *
 * Tommin tilaus 12.11.2026: osiot avautuivat joka profiilissa auki, ja niiden pitää muistaa
 * olivatko ne auki vai kiinni. Sama laji kuin [LoungeFoldStore] ja [FinishedDaysStore]:
 * tämän laitteen oma valinta, joka koskee kaikkia profiileja eikä lähde verkkoon.
 * Oletus on auki, jolloin ruutu on sama kuin ennen muutosta.
 */
interface ProfileFoldStore {

    fun get(): ProfileFolds

    fun save(folds: ProfileFolds)

    companion object {
        /** Muistiton varasto testeille ja esikatseluille. */
        val NONE: ProfileFoldStore = object : ProfileFoldStore {
            override fun get(): ProfileFolds = ProfileFolds()
            override fun save(folds: ProfileFolds) = Unit
        }
    }
}

/** Kahden otsikon tila. `true` on kiinni, eli otsikko näkyy ja rivit eivät. */
data class ProfileFolds(
    val activeFolded: Boolean = false,
    val finishedFolded: Boolean = false,
)

class SharedPrefsProfileFold(private val prefs: SharedPreferences) : ProfileFoldStore {

    constructor(context: Context) : this(
        context.applicationContext.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE),
    )

    override fun get(): ProfileFolds = ProfileFolds(
        activeFolded = prefs.getBoolean(KEY_ACTIVE, false),
        finishedFolded = prefs.getBoolean(KEY_FINISHED, false),
    )

    override fun save(folds: ProfileFolds) {
        prefs.edit()
            .putBoolean(KEY_ACTIVE, folds.activeFolded)
            .putBoolean(KEY_FINISHED, folds.finishedFolded)
            .apply()
    }

    private companion object {
        const val FILE_NAME = "dg_profile_fold"
        const val KEY_ACTIVE = "active_folded"
        const val KEY_FINISHED = "finished_folded"
    }
}
