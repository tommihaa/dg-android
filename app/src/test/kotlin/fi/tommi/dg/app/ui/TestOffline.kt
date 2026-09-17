package fi.tommi.dg.app.ui

/**
 * Poikkeuksen laji jota testien katkennut yhteys kantaa.
 *
 * Yksi paikka eikä merkkijono jokaisessa testissä, koska `DgResponse.Offline`in kenttä on
 * kirjanpitoa: mikään haara ei lue sitä, joten arvon on oltava sama kaikkialla, jotta väite
 * jonon rivin tekstistä ei ole kiinni siitä minkä testin läpi se kulki.
 *
 * Aito OkHttpin luokan nimi eikä keksitty sana, jotta testin arvo näyttää siltä mitä kentässä
 * oikeasti on. Ks. `DgClient.execute`.
 */
internal const val TEST_OFFLINE_CAUSE = "ConnectException"
