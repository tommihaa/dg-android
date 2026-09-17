package fi.tommi.dg.domain

/**
 * Arkiston vienti tiedostoon. Oma muoto eikä kannan raakakopio (päätös 14.8.2026, ks.
 * `docs/UI.md`): tämä on ihmisluettava eikä sido lukijaa DG Androidiin, kun taas
 * kannan kopio vaatisi toisen asennuksen avatakseen mitään.
 *
 * Käsin kirjoitettu eikä kirjaston varassa, koska `core-domain` ei saa riippua mistään
 * (ks. moduulin build-tiedoston kommentti). Rakenne on tasainen tarkoituksella: yksi kenttä
 * per rivi, ei sisäkkäisiä listoja viestin sisällä, jotta virheen näkee diffistä.
 *
 * Lukija on [readArchiveJson], ja se lukee kaikki versiot 1:stä alkaen: jokainen versio on
 * edellisen ylijoukko, joten puuttuva kenttä on vanha tiedosto eikä virhe.
 */
const val ARCHIVE_EXPORT_FORMAT = "fi.tommi.dg.archive"

/**
 * Nostetaan jos vientiin lisätään tai siitä poistetaan kenttä. Ei sidottu [Message.id]in
 * tiivisteeseen: vienti on kopio arkistosta, ei osa sitä, joten sen oma muoto saa muuttua
 * ilman että kannassa jo olevien viestien tunnisteet liikkuvat.
 *
 * Versiot: 1 viestit; 2 `opponent`; 3 `account` tiedoston tasolla; 4 (16.9.2026, tuonnin
 * tilaus) `reminders`, `phrases` ja viestin `replyTo`. Muoto laajeni kerran eikä kahdesti,
 * koska laitteen vaihto vie mukanaan kaiken mitä käyttäjä itse kirjoitti (`docs/AVOIMET.md`
 * › Arkiston tuonti). Vastauslomake jäi yhä pois 30.8.2026 päätöksellä: se on kyky eikä
 * sisältöä.
 */
const val ARCHIVE_EXPORT_VERSION = 4

/**
 * Tiedoston sisältö kokonaisuutena, sama olio kirjoitettaessa ja luettaessa.
 *
 * @param account Tili jonka arkisto tämä on (versio 3, 16.9.2026). Tiedoston tasolla eikä
 *   viestissä, koska vienti on aina yhden tilin: arkisto luetaan kirjautuneen tilin mukaan.
 *   `null` kirjoitetaan `null`ina eikä jätetä pois, jotta lukija näkee ettei tiliä
 *   tiedetty eikä luule vanhaa muotoa.
 * @param reminders Muistutukset kaikista otteluista. [Reminder.id] on laitteen juokseva numero
 *   eikä kulje tiedostossa; luettu muistutus saa id:ksi nollan.
 * @param phrases Fraasinapit tallennusjärjestyksessä.
 */
data class ArchiveFile(
    val exportedAtEpochMillis: Long,
    val account: String?,
    val messages: List<Message>,
    val reminders: List<Reminder> = emptyList(),
    val phrases: List<String> = emptyList(),
    val version: Int = ARCHIVE_EXPORT_VERSION,
)

/**
 * @param exportedAtEpochMillis Parametrina eikä kellosta suoraan, sama syy kuin
 *   [Message.receivedAtEpochMillis]illa: testin on voitava antaa sama luku kahdesti.
 */
fun List<Message>.toArchiveJson(
    exportedAtEpochMillis: Long,
    account: String? = null,
    reminders: List<Reminder> = emptyList(),
    phrases: List<String> = emptyList(),
): String = ArchiveFile(exportedAtEpochMillis, account, this, reminders, phrases).toJson()

fun ArchiveFile.toJson(): String {
    // `this` on tallennettava paikalliseksi: `buildString`in lohko vaihtaa vastaanottajan
    // `StringBuilder`iin, joten sen sisällä `this` ei enää olisi tämä tiedosto.
    val file = this
    return buildString {
        append("{\n")
        append("  \"format\": ").append(jsonString(ARCHIVE_EXPORT_FORMAT)).append(",\n")
        append("  \"version\": ").append(ARCHIVE_EXPORT_VERSION).append(",\n")
        append("  \"exportedAtEpochMillis\": ").append(file.exportedAtEpochMillis).append(",\n")
        append("  \"account\": ").append(file.account?.let(::jsonString) ?: "null").append(",\n")
        append("  \"messages\": [")
        if (file.messages.isEmpty()) {
            append("],\n")
        } else {
            append('\n')
            file.messages.forEachIndexed { index, message ->
                append("    {\n")
                append("      \"id\": ").append(jsonString(message.id)).append(",\n")
                append("      \"matchId\": ")
                    .append(message.matchId?.value?.let(::jsonString) ?: "null").append(",\n")
                append("      \"opponent\": ")
                    .append(message.opponent?.let(::jsonString) ?: "null").append(",\n")
                append("      \"sender\": ").append(jsonString(message.sender)).append(",\n")
                append("      \"timestampText\": ")
                    .append(jsonString(message.timestampText)).append(",\n")
                append("      \"body\": ").append(jsonString(message.body)).append(",\n")
                append("      \"rawHeader\": ").append(jsonString(message.rawHeader)).append(",\n")
                append("      \"source\": ").append(jsonString(message.source.name)).append(",\n")
                append("      \"receivedAtEpochMillis\": ")
                    .append(message.receivedAtEpochMillis).append(",\n")
                append("      \"replyTo\": ")
                    .append(message.replyTo?.let(::jsonString) ?: "null").append('\n')
                append("    }")
                append(if (index != file.messages.lastIndex) ",\n" else "\n")
            }
            append("  ],\n")
        }
        append("  \"reminders\": [")
        if (file.reminders.isEmpty()) {
            append("],\n")
        } else {
            append('\n')
            file.reminders.forEachIndexed { index, reminder ->
                append("    {\n")
                append("      \"matchId\": ").append(jsonString(reminder.game.matchId.value)).append(",\n")
                append("      \"opponentScore\": ").append(reminder.game.opponentScore).append(",\n")
                append("      \"selfScore\": ").append(reminder.game.selfScore).append(",\n")
                append("      \"text\": ").append(jsonString(reminder.text)).append(",\n")
                append("      \"createdAtEpochMillis\": ").append(reminder.createdAtEpochMillis).append('\n')
                append("    }")
                append(if (index != file.reminders.lastIndex) ",\n" else "\n")
            }
            append("  ],\n")
        }
        append("  \"phrases\": [")
        if (file.phrases.isEmpty()) {
            append("]\n")
        } else {
            append('\n')
            file.phrases.forEachIndexed { index, phrase ->
                append("    ").append(jsonString(phrase))
                append(if (index != file.phrases.lastIndex) ",\n" else "\n")
            }
            append("  ]\n")
        }
        append("}\n")
    }
}

/** JSON-merkkijono lainausmerkkeineen, ohjausmerkit paettuna. */
private fun jsonString(value: String): String = buildString {
    append('"')
    for (c in value) {
        when {
            c == '"' -> append("\\\"")
            c == '\\' -> append("\\\\")
            c == '\n' -> append("\\n")
            c == '\r' -> append("\\r")
            c == '\t' -> append("\\t")
            c.code < 0x20 -> append("\\u").append(c.code.toString(16).padStart(4, '0'))
            else -> append(c)
        }
    }
    append('"')
}
