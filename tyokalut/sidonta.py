# -*- coding: utf-8 -*-
"""
Sitoo logcatin napautukset proxyn pyyntöriveihin ja nauhan kehysmuutoksiin.

Miksi tämä on olemassa: vertailumittaus 6 (16.9.2026, `raakasivut/LUEMINUT.md`) tarvitsi
saman napautuksen kolmesta lähteestä, jotka ovat eri kelloissa ja eri tiedostoissa.
Logcat kertoo napautuksen hetken ja sovelluksen (`ViewPostIme pointer 1`, activityn nimi),
`istunto.tsv` kertoo pyynnön keston palvelimella (`kesto_ms`, PC:n kello), nauhan diff
(`kehysdiff.py`) kertoo milloin ruutu muuttui, ja tämän sovelluksen `DgKitka`-rivit
kertovat komposition. Tulos on yksi rivi per napautus, tsv, sarakkeet otsikkorivillä.

Kolme kelloa ja niiden sidonta ovat kutsujan vastuulla, koska ne luetaan mittauksesta
eikä niitä voi päätellä tiedostoista:

  --edella S      tabletin kello PC:tä edellä sekunteina (istunto.tsv on PC:n kellossa,
                  logcat ja nauhan alku tabletin). Vertailumittaus 6: 0.78.
  --nauha N=ALKU  nauhan nimi ja sen ensimmäisen kehyksen hetki tabletin kellossa,
                  esim. nauha1=18:02:28.44. Toistettava. Alku luetaan ankkurista, joka
                  16.9. oli DG Mobilen laudan ilmestyminen (`Displayed MatchPlayer`),
                  ja se on noin 0,1 s aikaisessa; ks. LUEMINUT.md, luvut ovat sen
                  verran liian pieniä molemmilla sovelluksilla.

Hakemistosta luetaan `logcat.txt`, `istunto.tsv` ja `nauhat/<N>_diff.tsv`. Päivä luetaan
logcatin riveistä eikä anneta, jotta skripti ei sido itseään yhteen päivään.

Käyttö:

    python tyokalut/sidonta.py --hakemisto raakasivut/sessio-16-9-ilta2 --edella 0.78 \\
        --nauha nauha1=18:02:28.44 --nauha nauha2=18:05:29.05 --nauha nauha3=18:08:29.68

Sarakkeet: napautus (tabletin kello), sov (DGM = DG Mobile, DGA = tämä sovellus), teko
(submit-arvo tai polku), rivi (istunto.tsv:n nro), palvelin_ms (kesto_ms), pyynto_alku-nap
(pyynnön alku suhteessa napautukseen, s), kehys_ms (ensimmäinen diff ≥ 3,0 napautuksen
jälkeen), kitka_piirretty_ms (DgKitka, vain DGA), huiput (dt:diff, kaikki ≥ 1,5).
Pyyntö sidotaan napautukseen ikkunassa -0,15…0,9 s, ja kehysikkuna päättyy seuraavaan
napautukseen tai 3 sekuntiin.

Ensimmäinen ajo 16.9.2026 klo 18 scratchpadista, versioitu samana iltana samoin luvuin.
"""
import argparse
import csv
import datetime as dt
import re
import sys

if hasattr(sys.stdout, "reconfigure"):
    sys.stdout.reconfigure(encoding="utf-8", errors="replace")


def sekunnit(s):
    """'18:04:25.694' -> sekunteja vuorokauden alusta."""
    h, m, sec = s.split(":")
    return int(h) * 3600 + int(m) * 60 + float(sec)


def lue_nauhat(hakemisto, nauhat):
    diffit = {}
    for nimi, alku in nauhat:
        rivit = [r.split("\t") for r in open("%s/nauhat/%s_diff.tsv" % (hakemisto, nimi))]
        diffit[nimi] = (sekunnit(alku), [(float(a), float(b)) for a, b in rivit])
    return diffit


def lue_logcat(hakemisto):
    """Napautukset (hetki, activity) ja DgKitka-rivit (hetki, laji, ms, loppu)."""
    napautukset = []
    kitka = []
    with open("%s/logcat.txt" % hakemisto, encoding="utf-8", errors="replace") as f:
        for rivi in f:
            m = re.match(
                r"\d\d-\d\d (\S+) .*ViewRootImpl@\w+\[(MatchPlayer|MainActivity)\]: "
                r"ViewPostIme pointer 1", rivi)
            if m:
                napautukset.append((sekunnit(m.group(1)), m.group(2)))
            m = re.match(
                r"\d\d-\d\d (\S+) .*DgKitka : (alku|piirretty) (?:\+(\d+) ms )?\(?(.*?)\)?$",
                rivi.rstrip())
            if m:
                kitka.append((sekunnit(m.group(1)), m.group(2), m.group(3), m.group(4)))
    return napautukset, kitka


def lue_pyynnot(hakemisto, edella):
    """(alku, loppu, kesto_ms, metodi, teko, nro) tabletin kellossa."""
    pyynnot = []
    with open("%s/istunto.tsv" % hakemisto, encoding="utf-8") as f:
        for r in csv.DictReader(f, delimiter="\t"):
            loppu = sekunnit(r["aika"].split("T")[1]) + edella
            kesto = int(r["kesto_ms"])
            osoite = r["osoite"].split("/bg/")[1]
            teko = re.search(r"submit=([^&]+)", osoite)
            if teko:
                teko = teko.group(1).replace("+", " ")
            elif r["kentat"] == "commit,submit":
                teko = "Next Game"
            else:
                teko = osoite.split("?")[0]
            pyynnot.append((loppu - kesto / 1000, loppu, kesto, r["metodi"], teko, r["nro"]))
    return pyynnot


def sido(napautukset, kitka, pyynnot, diffit):
    print("napautus\tsov\tteko\trivi\tpalvelin_ms\tpyynto_alku-nap\tkehys_ms"
          "\tkitka_piirretty_ms\thuiput(dt:diff)")
    for hetki, activity in napautukset:
        sov = "DGM" if activity == "MatchPlayer" else "DGA"
        ehdokkaat = [p for p in pyynnot if -0.15 <= p[0] - hetki <= 0.9]
        pyynto = min(ehdokkaat, key=lambda p: p[0]) if ehdokkaat else None
        # Seuraava napautus rajaa ikkunan.
        seuraava = min([x for x, _ in napautukset if x > hetki], default=hetki + 3)
        ikkuna = min(seuraava, hetki + 3.0)
        huiput = []
        for _, (alku, rivit) in diffit.items():
            for kehys_t, d in rivit:
                abs_t = alku + kehys_t
                if hetki + 0.05 <= abs_t <= ikkuna and d >= 1.5:
                    huiput.append((round(abs_t - hetki, 2), d))
        huiput.sort()
        kehys = next((h[0] for h in huiput if h[1] >= 3), None)
        piirretty = ""
        if sov == "DGA":
            alut = [k for k in kitka if k[1] == "alku" and abs(k[0] - hetki) < 0.08]
            if alut:
                p = [k for k in kitka if k[1] == "piirretty" and 0 < k[0] - alut[0][0] < 3]
                piirretty = p[0][2] if p else ""
        hh = " ".join("%s:%.0f" % (a, b) for a, b in huiput[:8])
        print("\t".join(str(x) for x in (
            dt.timedelta(seconds=hetki), sov,
            pyynto[4] if pyynto else "-",
            pyynto[5] if pyynto else "",
            pyynto[2] if pyynto else "",
            round(pyynto[0] - hetki, 2) if pyynto else "",
            "" if kehys is None else int(kehys * 1000),
            piirretty, hh)))


def main():
    jasennin = argparse.ArgumentParser(
        description="Sitoo napautukset pyyntöihin ja kehyksiin.",
        formatter_class=argparse.RawDescriptionHelpFormatter, epilog=__doc__)
    jasennin.add_argument("--hakemisto", default=".",
                          help="sessiohakemisto (logcat.txt, istunto.tsv, nauhat/)")
    jasennin.add_argument("--edella", type=float, required=True,
                          help="tabletin kello PC:tä edellä, s")
    jasennin.add_argument("--nauha", action="append", default=[], metavar="NIMI=HH:MM:SS.ss",
                          help="nauhan nimi ja ensimmäisen kehyksen hetki tabletin kellossa")
    a = jasennin.parse_args()
    nauhat = [tuple(n.split("=", 1)) for n in a.nauha]
    napautukset, kitka = lue_logcat(a.hakemisto)
    pyynnot = lue_pyynnot(a.hakemisto, a.edella)
    diffit = lue_nauhat(a.hakemisto, nauhat)
    sido(napautukset, kitka, pyynnot, diffit)
    return 0


if __name__ == "__main__":
    sys.exit(main())
