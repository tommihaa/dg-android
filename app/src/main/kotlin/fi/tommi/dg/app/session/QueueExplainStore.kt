package fi.tommi.dg.app.session

import android.content.Context
import android.content.SharedPreferences

/**
 * Onko Inboxin *Take an item* -selityskappale jo näytetty kerran kokonaisena.
 *
 * Tommin tilaus 21.9.2026: kappale on raskas, joten se on auki vain ensimmäisellä
 * kerralla ja sen jälkeen oletuksena pienennettynä otsikon takana. Laitteen oma tieto
 * kuten [DiceStyleStore], ei lähde koskaan verkkoon.
 */
interface QueueExplainStore {

    fun seen(): Boolean

    fun markSeen()
}

class SharedPrefsQueueExplain(private val prefs: SharedPreferences) : QueueExplainStore {

    constructor(context: Context) : this(
        context.applicationContext.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE),
    )

    override fun seen(): Boolean = prefs.getBoolean(KEY, false)

    override fun markSeen() {
        prefs.edit().putBoolean(KEY, true).apply()
    }

    private companion object {
        const val FILE_NAME = "dg_queue_explain"
        const val KEY = "seen"
    }
}
