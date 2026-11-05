package fi.tommi.dg.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withLink

/**
 * Pelaajan nimi on linkki pelaajakortille kaikkialla missä pelaaja on tunnistettu, ellei
 * nimen kuulu viedä muualle (Tommin tilaus 2.11.2026: *"jollei ole tarkoituksenmukaista
 * viedä toisaalle, kuten postaukseen"*). Inboxin keskustelulista avaa keskustelun ja
 * cup-kaavion myöhemmän kierroksen solu ottelun, joten niissä nimi ei ole profiililinkki.
 */

/**
 * Käyttäjänumero nimellä laitteen nimihakemistosta (`PlayerRatingStore.userIdByName`).
 * Annetaan rarity-kytkimestä riippumatta, koska linkki ei ole väri.
 */
internal val LocalPlayerIdByName = staticCompositionLocalOf<(String) -> String?> { { null } }

/**
 * Profiilin polku: sivun oma linkki ensin, sitten käyttäjänumero ja viimeksi nimi
 * hakemistosta. Null kun mitään ei tunneta tai nimi on tyhjä.
 */
@Composable
internal fun playerProfilePath(name: String?, userId: String? = null, profilePath: String? = null): String? =
    profilePath
        ?: userId?.takeIf { it.isNotBlank() }?.let(::userPath)
        ?: name?.trim()?.takeIf { it.isNotEmpty() }?.let(LocalPlayerIdByName.current)?.let(::userPath)

private fun userPath(userId: String) = "/bg/user/$userId"

/** Profiilin avaaja, tai null kun ruudulla ei ole navigointia (esikatselu, testi). */
@Composable
internal fun playerOpener(): ((String) -> Unit)? = LocalPlayerMenu.current?.openPath

/**
 * Napautus nimellä avaa profiilin, kun polku tunnetaan. Erillisille nimiteksteille; lauseen
 * sisällä oleva nimi linkitetään [textWithPlayerColor]illa.
 */
@Composable
internal fun Modifier.playerLink(name: String?, userId: String? = null, profilePath: String? = null): Modifier {
    val path = playerProfilePath(name, userId, profilePath) ?: return this
    val open = playerOpener() ?: return this
    return this.clickable { open(path) }
}

/**
 * [text], jossa nimen viimeinen esiintymä on [color]issa ja linkki profiiliin [path]illa.
 * Ilman väriä ja polkua teksti palaa sellaisenaan. Linkkiä ei alleviivata: nimi on linkki
 * kaikkialla, joten merkki ei erottaisi mitään.
 */
internal fun nameSpan(
    text: String,
    name: String?,
    color: Color?,
    path: String?,
    open: ((String) -> Unit)?,
): AnnotatedString = buildAnnotatedString {
    val at = name?.takeIf { it.isNotEmpty() }?.let { text.lastIndexOf(it) } ?: -1
    if (at < 0) {
        append(text)
        return@buildAnnotatedString
    }
    append(text, 0, at)
    val style = SpanStyle(color = color ?: Color.Unspecified)
    if (path != null && open != null) {
        withLink(LinkAnnotation.Clickable(path, TextLinkStyles(style)) { open(path) }) {
            append(name)
        }
    } else {
        pushStyle(style)
        append(name)
        pop()
    }
    append(text, at + name!!.length, text.length)
}
