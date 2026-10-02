# -*- coding: utf-8 -*-
"""
Kehysten välinen keskimuutos ruutunauhasta, tulos tsv-riveinä `aika_s<TAB>diff`.

Miksi tämä on olemassa: vaste mitataan nauhalta eikä koodista (muisti "liikkuva UI
nauhoitetaan"). Napautuksen jälkeinen ensimmäinen iso kehysmuutos on se hetki jolloin
lauta piirtyi, ja sen lukeminen 20 fps:llä koko nauhasta käsin ei onnistu. Tämä laskee
joka kehykselle harmaasävyn keskimuutoksen edelliseen ja kirjoittaa sen nauhan viereen
tiedostoon `<nauha>_diff.tsv`, jonka `sidonta.py` lukee.

Kynnykset ovat lukijan: `sidonta.py` pitää 1,5 pienenä ja 3,0 kehyksen muutoksena.
Yksi nappula tai noppa jää alle 3,0:n, joten nappulasiirron vaste luetaan silmällä
(`raakasivut/LUEMINUT.md`, vertailumittaus 6).

Kehys pienennetään 280×175:een ennen erotusta. Se riittää laudan ilmestymiseen ja tekee
sadan megan nauhasta sekunneissa luettavan; `show_touches`-rengas ei ylitä kynnystä tällä
leveydellä (mitattu 16.9.2026, ei erikseen todennettu kehyksestä).

Käyttö:

    python tyokalut/kehysdiff.py raakasivut/sessio-<pvm>/nauhat/nauha1.mp4 [nauha2.mp4 ...]

Vaatii `numpy` ja `imageio_ffmpeg` (ffmpeg tulee paketin mukana, ei erillistä asennusta).
Ensimmäinen ajo 16.9.2026 klo 18 scratchpadista, versioitu samana iltana.
"""
import subprocess
import sys

import numpy as np
import imageio_ffmpeg

if hasattr(sys.stdout, "reconfigure"):
    sys.stdout.reconfigure(encoding="utf-8", errors="replace")

FF = imageio_ffmpeg.get_ffmpeg_exe()
W, H, FPS = 280, 175, 20


def laske(nauha):
    """Kirjoittaa `<nauha>_diff.tsv`:n ja palauttaa kehysten määrän."""
    p = subprocess.Popen(
        [FF, "-loglevel", "error", "-i", nauha, "-vf", "fps=%d,scale=%d:%d" % (FPS, W, H),
         "-f", "rawvideo", "-pix_fmt", "gray", "-"],
        stdout=subprocess.PIPE,
    )
    edellinen = None
    i = 0
    with open(nauha.rsplit(".", 1)[0] + "_diff.tsv", "w") as ulos:
        while True:
            puskuri = p.stdout.read(W * H)
            if len(puskuri) < W * H:
                break
            kehys = np.frombuffer(puskuri, np.uint8).astype(np.int16)
            d = 0.0 if edellinen is None else float(np.abs(kehys - edellinen).mean())
            ulos.write("%.2f\t%.2f\n" % (i / FPS, d))
            edellinen = kehys
            i += 1
    p.wait()
    return i


def main():
    if len(sys.argv) < 2:
        print(__doc__)
        return 2
    for nauha in sys.argv[1:]:
        print(nauha, laske(nauha), "kehystä")
    return 0


if __name__ == "__main__":
    sys.exit(main())
