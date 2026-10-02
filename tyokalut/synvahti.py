# -*- coding: utf-8 -*-
"""SYN-vahti: avaa TCP-yhteyden sivustolle tasavalein ja kirjaa yhdistysajan.

**Miksi tama on olemassa.** `Next Game` sai proxyn takana kahdesti (17.9. ja 18.9.2026)
502:n 21 sekunnissa, ja 18.9. illalla se paikannettiin yhdistysvaiheeseen: uusi TCP-yhteys
sivustolle ei saanut SYN-ACKia, ja 21 s on Windowsin SYN-uusintojen summa (3 + 6 + 12).
Proxyn lokista ei nay, oliko koko reitti sivustolle sina hetkena poikki vai putosiko vain
se yksi SYN. Tama vahti antaa vertailurivin: jos sen yhteys samalla sekunnilla syntyi
alle sekunnissa, pudotus oli yksittainen; jos sekin odotti 21 s, reitti oli poikki.

**Mita tama ei tee.** Ei laheta yhtaan tavua HTTP:ta: yhteys avataan ja suljetaan heti,
joten jono ei kulu eika sivusto nae kuin TCP-kattelyn. Tommin sivustokaytto ei muutu.

Kaytto (`sessio.py aloita` kaynnistaa taman itse ja `lopeta` sammuttaa):

    python tyokalut/synvahti.py --hakemisto raakasivut/sessio-<pvm> [--vali 2]

Kirjoittaa hakemistoon `synvahti.tsv`: aika, isanta, ip, kesto_ms, tulos. Tulos on
`ok` tai poikkeuksen laji ja teksti. Osoite selvitetaan kerran alussa ja kirjataan, koska
sama ip tarvitaan pktmon-suodattimeen (ks. skilli, jakso 7).
"""
import argparse
import datetime
import os
import socket
import sys
import time

# Pidempi kuin Windowsin SYN-uusintojen summa (21 s), jotta pudonnut SYN kirjautuu
# kayttojarjestelman virheena (WinError 10060) eika Pythonin omana `timed out`ina.
# Sama ero jolla proxyn 502 paikannettiin 18.9.2026.
AIKAKATKAISU = 25.0


def yhdista(ip, portti):
    """Yksi TCP-kattely. Palauttaa (kesto_ms, tulos)."""
    alku = time.monotonic()
    try:
        s = socket.create_connection((ip, portti), timeout=AIKAKATKAISU)
        s.close()
        return int((time.monotonic() - alku) * 1000), "ok"
    except Exception as virhe:
        return int((time.monotonic() - alku) * 1000), "%s: %s" % (
            type(virhe).__name__, virhe)


def main():
    jasennin = argparse.ArgumentParser(description=__doc__)
    jasennin.add_argument("--hakemisto", required=True, help="minne synvahti.tsv kirjoitetaan")
    jasennin.add_argument("--isanta", default="www.dailygammon.com")
    jasennin.add_argument("--portti", type=int, default=80)
    jasennin.add_argument("--vali", type=float, default=2.0, help="sekuntia kattelyjen valissa")
    args = jasennin.parse_args()

    os.makedirs(args.hakemisto, exist_ok=True)
    polku = os.path.join(args.hakemisto, "synvahti.tsv")
    ip = socket.gethostbyname(args.isanta)
    uusi = not os.path.exists(polku) or os.path.getsize(polku) == 0
    print("SYN-vahti: %s = %s, portti %d, vali %.1f s, loki %s"
          % (args.isanta, ip, args.portti, args.vali, polku), flush=True)
    with open(polku, "a", encoding="utf-8") as tsv:
        if uusi:
            tsv.write("aika\tisanta\tip\tkesto_ms\ttulos\n")
        try:
            while True:
                kesto, tulos = yhdista(ip, args.portti)
                aika = datetime.datetime.now().isoformat(timespec="milliseconds")
                tsv.write("%s\t%s\t%s\t%d\t%s\n" % (aika, args.isanta, ip, kesto, tulos))
                tsv.flush()
                if tulos != "ok" or kesto > 5000:
                    print("  %s %d ms %s" % (aika, kesto, tulos), flush=True)
                time.sleep(max(0.0, args.vali - kesto / 1000.0))
        except KeyboardInterrupt:
            pass


if __name__ == "__main__":
    sys.exit(main())
