#!/usr/bin/env python3
r"""
Lokittava välityspalvelin DailyGammonia varten.

Miksi tämä on olemassa: kaikki muut tavat saada sivu talteen vaativat, että ihminen
tunnistaa tilanteen ja painaa nappia **sillä hetkellä kun sivu on ruudulla**. Se on
kaatunut mitattuun syyhyn: DevToolsin muistibudjetti pudottaa vastausrungot, ja
`Save as...` epaonnistuu silloin hiljaa ilman virheilmoitusta. Talteenoton pitää siis
tapahtua ilman että kukaan muistaa mitään.

Tama kirjoittaa jokaisen HTML-vastauksen levylle **raakoina tavuina** sellaisenaan.
Kohde on pelkkaa HTTP:ta, joten valissa ei ole TLS:aa eika sertifikaattitemppuja tarvita.

Kolme sivutuotetta jotka eivat ole ilmeisia:

- **Merkiston kysymys ratkeaa taalla.** Selaimen tallenne on aina jo purettu merkkijono,
  eli alkuperainen tavu on menetetty. Nama tiedostot ovat tavuja, ja `istunto.tsv`
  kirjaa `Content-Type`-otsakkeen sellaisenaan, eli sen naeekin onko charset mukana.
- **Osoitejalki syntyy samalla.** `istunto.tsv` on sama tieto jonka HAR antoi, mutta
  ilman rungon katoamista.
- **Lomakkeiden kenttanimet kirjataan, arvoja ei koskaan.** Kenttien nimet ovat toistuvasti
  se mita jasennin tarvitsee (`chat`, `quote`, `commit`), ja arvoissa olisi salasana.

Turvallisuus: pyyntojen rungot **eivat paady levylle missaan muodossa**. Vain kenttien
nimet. Salasana kulkee sivustolle selkokielisena joka tapauksessa, koska sivustolla ei ole
HTTPS:aa, mutta se ei ole syy kirjoittaa sita tiedostoon.

Kaytto:

    python tyokalut/proxy.py

Aja repon juuresta ja anna sessiolle oma hakemisto, koska --hakemisto on suhteellinen:

    python tyokalut/proxy.py --hakemisto raakasivut/sessio-27-8-chat

Sitten selain osoittamaan proxyyn (oletus 127.0.0.1:8899). Chromelle oma profiili, ja
komento on PowerShellille. Aiempi cmd.exen muoto (%TEMP% ja pelkka chrome.exe) ei mennyt
lapi terminaalissa 27.8.2026: %TEMP% ei laajene PowerShellissa eika chrome.exe ole PATHissa.

    Start-Process "C:\Program Files\Google\Chrome\Application\chrome.exe" -ArgumentList "--proxy-server=http://127.0.0.1:8899", "--user-data-dir=$env:TEMP\dg-profiili"

Firefoxissa asetus on selaimen omissa asetuksissa, jolloin muut selaimet eivat hairiinny.

Tabletti tai puhelin samassa lahiverkossa:

    python tyokalut/proxy.py --osoite 0.0.0.0

Sitten laitteen Wi-Fi-asetuksiin kasinsyotetty proxy, isantana koneen lahiverkko-osoite ja
porttina 8899. Asetus on Android-laitteessa **verkkokohtainen** eika koske mobiilidataa, joten
lentotila tai verkon vaihto ohittaa proxyn hiljaa.
"""

import argparse
import datetime
import hashlib
import http.client
import os
import re
import select
import socket
import socketserver
import sys
import time
import threading
import urllib.parse
from http.server import BaseHTTPRequestHandler

# Otsakkeet jotka koskevat yhta yhteysvalia eivatka saa mennä eteenpain sellaisenaan.
HOP_BY_HOP = {
    "connection",
    "keep-alive",
    "proxy-authenticate",
    "proxy-authorization",
    "proxy-connection",
    "te",
    "trailer",
    "trailers",
    "transfer-encoding",
    "upgrade",
}

# Naista ei oteta runkoa talteen. Kuvat ovat se joka tayttaa levyn ja lokin ilman
# yhtaan uutta tietoa: lauta on luettavissa ALT-koodeista, ei kuvista.
SKIP_TYPES = ("image/", "text/css", "application/javascript", "text/javascript")


class Tallentaja:
    """Kirjoittaa vastaukset ja pitaa indeksia. Yksi lukko, koska pyynnot ovat saikeissa."""

    def __init__(self, hakemisto, kohde):
        self.hakemisto = hakemisto
        self.kohde = kohde
        self.lukko = threading.Lock()
        os.makedirs(hakemisto, exist_ok=True)
        # Laskuri jatkaa hakemiston suurimmasta numerosta, ei nollasta. Uudelleen
        # kaynnistetty proxy ylikirjoittaisi muuten aiemmat sivut hiljaa, ja juuri
        # toistamattoman sivun menettaminen on se vika jota vastaan tama tyokalu on.
        # Mitattu 4.8.2026: kesken session tehty uudelleenkaynnistys aloitti nollasta.
        self.laskuri = max(
            (
                int(nimi[:4])
                for nimi in os.listdir(hakemisto)
                if re.match(r"^\d{4}_", nimi)
            ),
            default=0,
        )
        self.indeksi = os.path.join(hakemisto, "istunto.tsv")
        if not os.path.exists(self.indeksi):
            with open(self.indeksi, "w", encoding="utf-8", newline="") as f:
                f.write(
                    "\t".join(
                        [
                            "nro", "aika", "metodi", "osoite", "kentat", "status",
                            "tyyppi", "tavuja", "tiiviste", "tiedosto", "otsakkeet",
                            "kesto_ms",
                        ]
                    )
                    + "\n"
                )

    def kirjaa(self, metodi, osoite, kentat, status, otsakkeet, runko, kesto_ms=-1):
        # `otsakkeet` on **pareja listana eika dict**, koska `Set-Cookie` toistuu.
        # `kesto_ms` on palvelimen kierros pyynnon lahetyksesta viimeiseen tavuun,
        # lisatty 16.9.2026 kitkamittausta varten: ilman sita sovelluksen ja
        # palvelimen osuutta napautuksen ja piirron valista ei voi erottaa.
        tyyppi = next(
            (v for k, v in otsakkeet if k.lower() == "content-type"), ""
        )
        oma_host = urllib.parse.urlsplit(osoite).hostname or ""
        talteen = (
            self.kohde in oma_host
            and runko is not None
            and not any(tyyppi.lower().startswith(t) for t in SKIP_TYPES)
        )

        with self.lukko:
            self.laskuri += 1
            nro = self.laskuri

        tiedosto = ""
        if talteen:
            tiedosto = "%04d_%s.html" % (nro, siisti_nimi(osoite))
            with open(os.path.join(self.hakemisto, tiedosto), "wb") as f:
                f.write(runko)

        rivi = [
            str(nro),
            datetime.datetime.now().isoformat(timespec="milliseconds"),
            metodi,
            osoite,
            kentat,
            str(status),
            tyyppi,
            str(len(runko) if runko is not None else 0),
            hashlib.sha256(runko).hexdigest()[:12] if runko else "",
            tiedosto,
            " | ".join("%s: %s" % (k, v) for k, v in otsakkeet),
            str(kesto_ms),
        ]
        with self.lukko:
            with open(self.indeksi, "a", encoding="utf-8", newline="") as f:
                f.write("\t".join(kentta.replace("\t", " ") for kentta in rivi) + "\n")

        if talteen:
            print("  %s  %s" % (tiedosto, osoite), flush=True)


def siisti_nimi(osoite):
    """
    Osoitteesta tiedostonimi. Kyselyparametri otetaan mukaan, koska juuri se erottaa
    toiminnot toisistaan (`?submit=Roll+Dice`, `?move=whff`).
    """
    osat = urllib.parse.urlsplit(osoite)
    teksti = osat.path
    if osat.query:
        teksti += "_" + osat.query
    teksti = re.sub(r"[^A-Za-z0-9]+", "_", teksti).strip("_")
    return teksti[:80] or "sivu"


def kenttien_nimet(runko, tyyppi):
    """
    Lomakkeen kenttien **nimet**, ei koskaan arvoja. Arvoissa olisi salasana.
    """
    if not runko or "form-urlencoded" not in (tyyppi or "").lower():
        return ""
    try:
        teksti = runko.decode("ascii", errors="replace")
    except Exception:
        return ""
    nimet = []
    for pari in teksti.split("&"):
        nimi = pari.split("=", 1)[0]
        if nimi and nimi not in nimet:
            nimet.append(urllib.parse.unquote_plus(nimi))
    return ",".join(nimet)


class Kasittelija(BaseHTTPRequestHandler):
    protocol_version = "HTTP/1.1"
    tallentaja = None

    def log_message(self, fmt, *args):
        pass  # oma loki riittaa, palvelimen oletusloki vain sotkisi sen

    def do_GET(self):
        self.valita("GET")

    def do_POST(self):
        self.valita("POST")

    def do_HEAD(self):
        self.valita("HEAD")

    def do_CONNECT(self):
        """
        HTTPS menee lapi lukemattomana tunnelina. Kohde on HTTP-sivusto, joten mitaan
        kiinnostavaa ei ole taalla, mutta ilman tata selainprofiili olisi kaytannossa
        rikki kaikilla muilla sivuilla.
        """
        isanta, _, portti = self.path.partition(":")
        try:
            ylos = socket.create_connection((isanta, int(portti or 443)), timeout=20)
        except OSError:
            self.send_error(502, "tunnelia ei saatu")
            return

        self.send_response_only(200, "Connection Established")
        self.end_headers()
        self.wfile.flush()

        # Molemmat pistokkeet jaavat ESTAVIKSI tahallaan, vaikka `select` on kaytossa.
        # Ne olivat aiemmin estamattomia, ja silloin `sendall` kaatui satunnaisesti
        # virheeseen `BlockingIOError: WinError 10035` kun vastaanottajan puskuri oli
        # tayntta: `sendall` ei osaa jatkaa myohemmin, vaan se on maaritelty estavalle
        # pistokkeelle. Mitattu 22.8.2026 tabletin HTTPS-liikenteesta.
        #
        # `select` kertoo tassa vain LUKUVALMIUDESTA, joten `recv` ei esta senkaan
        # jalkeen. Kirjoituspuoli saa estaa, koska jokaisella yhteydella on oma saie.
        alas = self.connection
        try:
            while True:
                valmiit, _, virheet = select.select([alas, ylos], [], [alas, ylos], 60)
                if virheet or not valmiit:
                    break
                for lahde in valmiit:
                    kohde = ylos if lahde is alas else alas
                    try:
                        data = lahde.recv(65536)
                    except OSError:
                        return
                    if not data:
                        return
                    kohde.sendall(data)
        finally:
            ylos.close()

    def valita(self, metodi):
        osat = urllib.parse.urlsplit(self.path)
        if not osat.hostname:
            self.send_error(400, "vain absoluuttinen osoite kelpaa proxylle")
            return

        pituus = int(self.headers.get("Content-Length") or 0)
        runko = self.rfile.read(pituus) if pituus else None
        kentat = kenttien_nimet(runko, self.headers.get("Content-Type"))

        otsakkeet = {
            k: v for k, v in self.headers.items() if k.lower() not in HOP_BY_HOP
        }
        # Pakkaamaton vastaus, jotta levylle menevat tavut ovat sivun omia tavuja
        # eivatka gzip-virtaa. Merkistokysymys ratkeaa vain naista.
        otsakkeet["Accept-Encoding"] = "identity"
        otsakkeet["Connection"] = "close"

        polku = osat.path or "/"
        if osat.query:
            polku += "?" + osat.query

        alku = time.monotonic()
        try:
            yhteys = http.client.HTTPConnection(
                osat.hostname, osat.port or 80, timeout=30
            )
            # Yhdistaminen omana vaiheenaan (18.9.2026), jotta 502:n syy kertoo jaiko
            # TCP-yhteys syntymatta vai vastaus tulematta. `Next Game` sai 17.9. ja 18.9.
            # `WinError 10060` 21 s:ssa, ja 21 s on Windowsin SYN-uusintojen summa
            # (3 + 6 + 12), ei Pythonin 30 s timeout joka sanoisi "timed out". Vaihe
            # kirjataan silti eksplisiittisesti, koska 10060 tulee myos avatun yhteyden
            # katketessa, ja paattely kestosta on heikompi todiste kuin vaiheen nimi.
            vaihe = "yhdistys"
            yhteys.connect()
            yhdistys_ms = int((time.monotonic() - alku) * 1000)
            vaihe = "vastaus"
            yhteys.request(metodi, polku, body=runko, headers=otsakkeet)
            vastaus = yhteys.getresponse()
            data = vastaus.read()
            kesto_ms = int((time.monotonic() - alku) * 1000)
        except Exception as virhe:  # verkko, aikakatkaisu, rikkinainen vastaus
            # Epaonnistunut valitys kirjataan samaan lokiin kuin onnistunut (17.9.2026):
            # `Next Game` odotti 21 s ja sai 502:n, eika proxysta jaanyt siita mitaan.
            # Syy oli luettavissa vain logcatista, ja poikkeuksen laji ei mistaan. Rivi
            # on ilman tiedostoa, joten `sessio.py`n sivumaara ei kasva; status 502 ja
            # otsakekentassa poikkeus ovat se mika sen erottaa.
            kesto_ms = int((time.monotonic() - alku) * 1000)
            syy = "%s %s: %s" % (vaihe, type(virhe).__name__, virhe)
            print("  502 %s %s (%d ms): %s" % (metodi, self.path, kesto_ms, syy), flush=True)
            try:
                self.tallentaja.kirjaa(
                    metodi, self.path, kentat, 502, [("X-Proxy-Error", syy)], None,
                    kesto_ms,
                )
            except Exception as kirjausvirhe:
                print("  (tallennus epaonnistui: %s)" % kirjausvirhe, flush=True)
            self.send_error(502, "valitys epaonnistui: %s" % virhe)
            return
        finally:
            try:
                yhteys.close()
            except Exception:
                pass

        # Lista eika dict: `Set-Cookie` on otsake joka **saa toistua**, ja dict sailyttaisi
        # niista vain viimeisen. Mitattu 4.8.2026: DailyGammonin kirjautuminen asettaa
        # useamman evasteen, ja yhden katoaminen naytti selaimessa silta kuin kirjautuminen
        # ei olisi tarttunut lainkaan. Ansa piilotti itsensa, koska `istunto.tsv`
        # kirjoitettiin samasta dictista eika lokissa nakynyt kuin yksi.
        vastausotsakkeet = [
            (k, v) for k, v in vastaus.getheaders() if k.lower() not in HOP_BY_HOP
        ]
        # Yhdistamisen kesto omana otsakkeena istunto.tsv:hen, jotta onnistuneidenkin
        # pyyntojen SYN-viive on luettavissa jalkikateen (vertailuarvo 502:lle).
        vastausotsakkeet.append(("X-Proxy-Connect-Ms", str(yhdistys_ms)))

        try:
            self.tallentaja.kirjaa(
                metodi, self.path, kentat, vastaus.status, vastausotsakkeet, data,
                kesto_ms,
            )
        except Exception as virhe:
            # Tallennuksen vika ei saa katkaista selailua: se olisi pahin mahdollinen
            # tapa epaonnistua, koska pelisessio on se jota ei voi toistaa.
            print("  (tallennus epaonnistui: %s)" % virhe, flush=True)

        self.send_response_only(vastaus.status, vastaus.reason)
        for nimi, arvo in vastausotsakkeet:
            if nimi.lower() != "content-length":
                self.send_header(nimi, arvo)
        self.send_header("Content-Length", str(len(data)))
        self.end_headers()
        if metodi != "HEAD":
            self.wfile.write(data)


class Palvelin(socketserver.ThreadingTCPServer):
    daemon_threads = True
    allow_reuse_address = True


def main():
    jasennin = argparse.ArgumentParser(description=__doc__)
    jasennin.add_argument("--portti", type=int, default=8899)
    # Oletus on `127.0.0.1`, koska tama on avoin valityspalvelin ilman tunnistautumista:
    # lahiverkkoon avattuna sita voi kayttaa kuka tahansa samassa verkossa. Tabletti
    # tarvitsee `--osoite 0.0.0.0`, ja se annetaan silloin kun sita tarvitaan.
    jasennin.add_argument("--osoite", default="127.0.0.1")
    jasennin.add_argument("--hakemisto", default=os.path.join("raakasivut", "sessio"))
    jasennin.add_argument("--kohde", default="dailygammon.com")
    args = jasennin.parse_args()

    Kasittelija.tallentaja = Tallentaja(args.hakemisto, args.kohde)

    palvelin = Palvelin((args.osoite, args.portti), Kasittelija)
    print("Proxy kuuntelee %s:%d" % (args.osoite, args.portti), flush=True)
    print("Talteen menee %s, hakemisto %s" % (args.kohde, args.hakemisto), flush=True)
    print("Lopetus Ctrl+C.", flush=True)
    try:
        palvelin.serve_forever()
    except KeyboardInterrupt:
        print("\nLopetettu.", flush=True)
    finally:
        palvelin.server_close()


if __name__ == "__main__":
    sys.exit(main())
