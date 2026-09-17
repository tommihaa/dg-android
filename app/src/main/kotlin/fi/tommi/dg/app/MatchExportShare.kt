package fi.tommi.dg.app

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File

/**
 * Ojentaa päättyneen ottelun `.mat`-tiedoston Androidin jakovalikkoon.
 *
 * **Välimuisti eikä tallennus.** Tiedosto kirjoitetaan `cacheDir/exports/`-kansioon, jonka
 * järjestelmä saa tyhjentää milloin vain, ja `FileProvider` antaa vastaanottajalle
 * lukuoikeuden vain tähän yhteen tiedostoon. Sovellukseen ei jää arkistoa vienneistä, ja
 * se on rajaus eikä puute: `SUBSTANSSI.md` kohta 24 avattiin 2.9.2026 reitiksi
 * työpöydälle, ei tablettiin jääväksi kopioksi.
 *
 * **Vastaanottajaa ei valita sovelluksessa.** Laite näyttää ne ohjelmat jotka ottavat
 * tekstitiedoston vastaan, eli analyysiohjelman jos sellainen on ja Driven jos sitä
 * tarvitaan. Sovellus ei tiedä eikä sen tarvitse tietää kumpi on asennettu
 * (`docs/PELAAJA.md`, analyysiohjelmat Androidilla). MIME on `text/plain`, koska `.mat`
 * on tekstiä ja tyypillä `application/octet-stream` osa vastaanottajista ei tarjoutuisi.
 * Sama reitti ja sama tyyppi `.sgf`:lle (16.9.2026), joka on myös tekstiä.
 */
object MatchExportShare {

    private const val DIR = "exports"

    fun share(context: Context, fileName: String, text: String, chooserTitle: String) {
        val dir = File(context.cacheDir, DIR).apply { mkdirs() }
        val file = File(dir, fileName)
        file.writeText(text)

        val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, fileName)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(send, chooserTitle))
    }
}
