package fi.tommi.dg.domain

/**
 * Arkiston tuonti tiedostosta, eli [ArchiveFile]-viennin lukija (Tommin tilaus 16.9.2026,
 * `docs/AVOIMET.md` › Arkiston tuonti).
 *
 * **Tuonti vain lisää eikä koskaan korvaa** (Tommin päätös 16.9.2026), mutta se sääntö ei asu
 * tässä vaan kirjoittajissa: tämä lukee tiedoston olioiksi, ja `MessageArchive`,
 * `ReminderBook` ja `PhraseBook` päättävät kukin mikä on jo laitteella. Viestillä sen
 * ratkaisee sisältötiiviste, joka lasketaan tässä uudestaan kentistä eikä lueta tiedoston
 * `id`-kentästä: tiedoston sanoma tunniste voi olla käsin muokattu, ja kentistä laskettu on
 * sama sääntö jolla kanta tunnisti viestin alun perinkin.
 *
 * **Kaikki versiot luetaan.** Jokainen vientiversio on edellisen ylijoukko
 * ([ARCHIVE_EXPORT_VERSION]), joten puuttuva `opponent`, `account`, `replyTo`, `reminders`
 * tai `phrases` on vanha tiedosto eikä virhe. Uudemman version tiedosto luetaan siltä osin
 * kuin kentät tunnetaan.
 *
 * **Rikkinäinen rivi ohitetaan mutta lasketaan.** Viesti jolta puuttuu pakollinen kenttä ei
 * kaada koko tuontia, koska muut viestit ovat yhä ainoa kopio historiasta, mutta ohitus ei
 * ole hiljainen: [ArchiveImport.Read.unreadable] kertoo määrän ja ruutu näyttää sen.
 */
sealed interface ArchiveImport {

    /** Tiedosto oli tämän sovelluksen arkisto. [unreadable] on ohitettujen rivien määrä. */
    data class Read(val file: ArchiveFile, val unreadable: Int) : ArchiveImport

    /** Ei JSONia, tai JSON jonka `format` ei ole [ARCHIVE_EXPORT_FORMAT]. Mitään ei lueta. */
    data object NotAnArchive : ArchiveImport
}

fun readArchiveJson(text: String): ArchiveImport {
    val root = runCatching { JsonReader(text).readDocument() }.getOrNull() as? Map<*, *>
        ?: return ArchiveImport.NotAnArchive
    if (root["format"] != ARCHIVE_EXPORT_FORMAT) return ArchiveImport.NotAnArchive

    var unreadable = 0
    val messages = (root["messages"] as? List<*>).orEmpty().mapNotNull { item ->
        readMessage(item as? Map<*, *>).also { if (it == null) unreadable++ }
    }
    val reminders = (root["reminders"] as? List<*>).orEmpty().mapNotNull { item ->
        readReminder(item as? Map<*, *>).also { if (it == null) unreadable++ }
    }
    val phrases = (root["phrases"] as? List<*>).orEmpty().mapNotNull { item ->
        (item as? String).also { if (it == null) unreadable++ }
    }
    return ArchiveImport.Read(
        ArchiveFile(
            exportedAtEpochMillis = (root["exportedAtEpochMillis"] as? Long) ?: 0L,
            account = root["account"] as? String,
            messages = messages,
            reminders = reminders,
            phrases = phrases,
            version = (root["version"] as? Long)?.toInt() ?: 1,
        ),
        unreadable = unreadable,
    )
}

private fun readMessage(item: Map<*, *>?): Message? {
    if (item == null) return null
    val sender = item["sender"] as? String ?: return null
    val body = item["body"] as? String ?: return null
    val receivedAt = item["receivedAtEpochMillis"] as? Long ?: return null
    val sourceName = item["source"] as? String ?: return null
    return Message(
        matchId = (item["matchId"] as? String)?.let(::MatchId),
        opponent = item["opponent"] as? String,
        sender = sender,
        timestampText = item["timestampText"] as? String ?: "",
        body = body,
        rawHeader = item["rawHeader"] as? String ?: "",
        // Tuntematon nimi luetaan tuntemattomaksi lajiksi, sama sääntö kuin kannasta
        // luettaessa (`MessageMapper`): viestin säilyminen voittaa lajittelun.
        source = MessageSource.entries.firstOrNull { it.name == sourceName } ?: MessageSource.UNKNOWN,
        receivedAtEpochMillis = receivedAt,
        replyTo = item["replyTo"] as? String,
    )
}

private fun readReminder(item: Map<*, *>?): Reminder? {
    if (item == null) return null
    val matchId = item["matchId"] as? String ?: return null
    val opponentScore = (item["opponentScore"] as? Long)?.toInt() ?: return null
    val selfScore = (item["selfScore"] as? Long)?.toInt() ?: return null
    val text = item["text"] as? String ?: return null
    val createdAt = item["createdAtEpochMillis"] as? Long ?: return null
    if (text.isBlank()) return null
    return Reminder(
        id = 0,
        game = GameKey(MatchId(matchId), opponentScore, selfScore),
        text = text,
        createdAtEpochMillis = createdAt,
    )
}

/**
 * Pieni JSON-lukija, koska `core-domain` ei saa riippua kirjastosta. Lukee koko
 * kieliopin (oliot, listat, merkkijonot pakoineen, luvut, `true`, `false`, `null`) ja
 * palauttaa Kotlinin perustyyppejä: `Map`, `List`, `String`, `Long` tai `Double`, `Boolean`
 * tai `null`. Kokonaisluku on aina `Long`, jotta aikaleimat ja pisteet lukevat samalla
 * tyypillä. Virheellinen syöte heittää, ja [readArchiveJson] lukee sen "ei arkisto".
 */
private class JsonReader(private val text: String) {
    private var pos = 0

    fun readDocument(): Any? {
        val value = readValue()
        skipWhitespace()
        check(pos == text.length) { "trailing content at $pos" }
        return value
    }

    private fun readValue(): Any? {
        skipWhitespace()
        check(pos < text.length) { "unexpected end" }
        return when (val c = text[pos]) {
            '{' -> readObject()
            '[' -> readArray()
            '"' -> readString()
            't' -> literal("true", true)
            'f' -> literal("false", false)
            'n' -> literal("null", null)
            else -> if (c == '-' || c.isDigit()) readNumber() else error("unexpected '$c' at $pos")
        }
    }

    private fun readObject(): Map<String, Any?> {
        pos++ // '{'
        val result = LinkedHashMap<String, Any?>()
        skipWhitespace()
        if (peek() == '}') { pos++; return result }
        while (true) {
            skipWhitespace()
            check(peek() == '"') { "expected key at $pos" }
            val key = readString()
            skipWhitespace()
            check(peek() == ':') { "expected ':' at $pos" }
            pos++
            result[key] = readValue()
            skipWhitespace()
            when (peek()) {
                ',' -> pos++
                '}' -> { pos++; return result }
                else -> error("expected ',' or '}' at $pos")
            }
        }
    }

    private fun readArray(): List<Any?> {
        pos++ // '['
        val result = ArrayList<Any?>()
        skipWhitespace()
        if (peek() == ']') { pos++; return result }
        while (true) {
            result += readValue()
            skipWhitespace()
            when (peek()) {
                ',' -> pos++
                ']' -> { pos++; return result }
                else -> error("expected ',' or ']' at $pos")
            }
        }
    }

    private fun readString(): String {
        pos++ // '"'
        val out = StringBuilder()
        while (true) {
            check(pos < text.length) { "unterminated string" }
            when (val c = text[pos++]) {
                '"' -> return out.toString()
                '\\' -> {
                    check(pos < text.length) { "unterminated escape" }
                    when (val e = text[pos++]) {
                        '"' -> out.append('"')
                        '\\' -> out.append('\\')
                        '/' -> out.append('/')
                        'b' -> out.append('\b')
                        'f' -> out.append('')
                        'n' -> out.append('\n')
                        'r' -> out.append('\r')
                        't' -> out.append('\t')
                        'u' -> {
                            check(pos + 4 <= text.length) { "short unicode escape" }
                            out.append(text.substring(pos, pos + 4).toInt(16).toChar())
                            pos += 4
                        }
                        else -> error("bad escape '\\$e'")
                    }
                }
                else -> out.append(c)
            }
        }
    }

    private fun readNumber(): Any {
        val start = pos
        if (peek() == '-') pos++
        while (pos < text.length && (text[pos].isDigit() || text[pos] in ".eE+-")) pos++
        val literal = text.substring(start, pos)
        return literal.toLongOrNull() ?: literal.toDouble()
    }

    private fun literal(word: String, value: Any?): Any? {
        check(text.startsWith(word, pos)) { "unexpected literal at $pos" }
        pos += word.length
        return value
    }

    private fun peek(): Char? = text.getOrNull(pos)

    private fun skipWhitespace() {
        while (pos < text.length && text[pos].isWhitespace()) pos++
    }
}
