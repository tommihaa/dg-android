package fi.tommi.dg.scrape

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

/** Rakenne on kartan oikeasta KML-viennistä 9.10.2026, nimet ja paikat vaihdettu. */
class PlayersMapParserTest {

    private val kml = """
        <?xml version="1.0" encoding="UTF-8"?>
        <kml xmlns="http://www.opengis.net/kml/2.2">
          <Document>
            <name><![CDATA[DailyGammon players' locations]]></name>
            <Folder>
              <name>players</name>
              <Placemark>
                <name>Pelaaja Yksi</name>
                <Point><coordinates>
                  29.8474185,62.7643378,0
                </coordinates></Point>
              </Placemark>
              <Placemark>
                <name>toinen</name>
                <Point><coordinates>-74.0,40.7,0</coordinates></Point>
              </Placemark>
            </Folder>
          </Document>
        </kml>
    """.trimIndent()

    @Test
    fun `nimi loytyy ja koordinaatit kaantyvat leveys ensin`() {
        assertEquals(MapPoint(62.7643378, 29.8474185), PlayersMapParser.locate(kml, "Pelaaja Yksi"))
    }

    @Test
    fun `kirjainkoko ei ratkaise`() {
        assertEquals(MapPoint(40.7, -74.0), PlayersMapParser.locate(kml, "TOINEN"))
    }

    @Test
    fun `kansion nimi ei ole pelaaja eika puuttuva nimi loydy`() {
        assertNull(PlayersMapParser.locate(kml, "players"))
        assertNull(PlayersMapParser.locate(kml, "kolmas"))
        assertNull(PlayersMapParser.locate(kml, " "))
    }
}
