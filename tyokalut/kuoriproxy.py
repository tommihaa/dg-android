# -*- coding: utf-8 -*-
"""Kuoriproxy: vastaa sovellukselle tallennetuilla sivuilla eika ota yhteytta sivustoon.

**Miksi tama on olemassa.** Osa ruuduista ei ole tilattavissa: paattymissivulle paasee vain
kun ottelu paattyy, eika sita voi jarjestaa. Ne jaivat siksi yksikkotestien varaan, ja
1.9.2026 kavi ilmi etta juuri niissa oli kaksi vikaa joita testit eivat nay: napit puuttuivat
ja teot olivat kuolleita. Tama tyokalu tekee sellaisesta ruudusta todennettavan.

**Airlock, ja se on koko turvallisuusperuste.** Palvelin ei valita yhtaan pyyntoa eteenpain:
tunnetut polut saavat tallennetun sivun ja kaikki muu saa 503. Sivustolle ei siis voi mennä
mitaan, oli napautus miten vaarassa paikassa tahansa. Se on eri asia kuin `proxy.py`, joka
valittaa kaiken ja tallentaa; tama ei valita mitaan.

Kaytto (`docs/TESTAUS.md`, kuoriproxy):

    python tyokalut/kuoriproxy.py top.html paattymissivu.html [muu.html] [profiili.html]
    python tyokalut/kuoriproxy.py top.html paattymissivu.html --resign resign.html doit.html
    adb reverse tcp:8899 tcp:8899
    adb shell settings put global http_proxy 127.0.0.1:8899

Lopuksi asetus pois (`settings put global http_proxy :0`) ja tunneli auki
(`adb reverse --remove tcp:8899`), muuten laite ei paase verkkoon.

**Kaksi rajausta joita ei saa unohtaa.** Sivut ovat tallennettuja, joten sovellus nayttaa
vanhaa tietoa; tama ei kelpaa minkaan tuoreen asian todentamiseen. Ja lahetys ei mene
sivustolle, joten se mita nakee on ruudun ja tilan kaytos, ei sivuston vastaus.
"""
import http.server, socketserver, sys, datetime, urllib.parse, time
# Vastauksen viive millisekunteina, `--viive 1500`. Lisatty 16.9.2026 odotuksen aikaisen
# palautteen (napit pois, kaari) todentamiseen: paikallinen vastaus tulee muuten alle
# 10 ms:ssa eika odotusta ehdi nahda. Palvelimen oikea kierros on 0,3-2,7 s (vertailumittaus 6).
VIIVE = 0.0
if '--viive' in sys.argv:
    i = sys.argv.index('--viive')
    VIIVE = int(sys.argv[i + 1]) / 1000.0
    del sys.argv[i:i + 2]

TOP = open(sys.argv[1], 'rb').read()
OVER = open(sys.argv[2], 'rb').read()
# Kolmas sivu on valinnainen: se on se sivu jonka lauta linkittaa mutta jota lauta ei lue,
# esimerkiksi siirtolista `/bg/game/<id>/<n>/list`.
# Paikkariippuvat tiedostot ilman `--resign`-lippua ja sen kahta tiedostoa.
ARGS = list(sys.argv[1:])
if '--resign' in ARGS:
    del ARGS[ARGS.index('--resign'):ARGS.index('--resign') + 3]
if '--nextgame' in ARGS:
    del ARGS[ARGS.index('--nextgame'):ARGS.index('--nextgame') + 2]
MUU = open(ARGS[2], 'rb').read() if len(ARGS) > 2 else None
# Neljas sivu on valinnainen: asetussivu `/bg/profile`, jolta sovellus lukee lautaan
# vaikuttavat sivuston asetukset (`SiteBoardSettings`). Lisatty 9.9.2026, kun `Home boards
# on left side` -asetuksen vaikutus paneelin puoleen oli todennettava koskematta sivustoon:
# oikean asetuksen kaantaminen olisi muuttanut Tommin omaa profiilia sivustolla.
PROFIILI = open(ARGS[3], 'rb').read() if len(ARGS) > 3 else None
# Luovutussivu ja sen kuittaussivu, `--resign resign.html doit.html`. Lisatty 14.9.2026,
# kun luovutuksen ilmoitus siirrettiin napin alle ja sen sanamuoto laitteella oli
# todennettava ilman toista oikeaa luovutusta. GET /bg/resign saa ensimmaisen, POST
# /bg/resign/doit toisen; kuittauksen jalkeinen GET saa saman listan kuin ennen, joten
# rivi ei katoa, ja se on tiedossa: todennettava on lause eika rivi.
RESIGN = DOIT = None
if '--resign' in sys.argv:
    i = sys.argv.index('--resign')
    RESIGN = open(sys.argv[i + 1], 'rb').read()
    DOIT = open(sys.argv[i + 2], 'rb').read()
# Jonon karki, `--nextgame kutsu.html`. Lisatty 14.9.2026 vastaanotetun kutsun ruudun
# todentamiseen: GET /bg/nextgame saa tiedoston, ja vastaus kutsuun (/bg/invite/<id>)
# jaa airlockiin eli saa 503. Ruutu todentuu, teko ei mene mihinkaan.
NEXTGAME = None
if '--nextgame' in sys.argv:
    NEXTGAME = open(sys.argv[sys.argv.index('--nextgame') + 1], 'rb').read()
LOG = []

class H(http.server.BaseHTTPRequestHandler):
    protocol_version = "HTTP/1.1"

    def log_message(self, *a):
        pass

    def vastaa(self, body, status=200):
        if VIIVE:
            time.sleep(VIIVE)
        self.send_response_only(status, "OK")
        self.send_header("Content-Type", "text/html")
        self.send_header("Content-Length", str(len(body)))
        self.send_header("Connection", "close")
        self.end_headers()
        if self.command != "HEAD":
            self.wfile.write(body)

    def kasittele(self):
        osat = urllib.parse.urlsplit(self.path)
        polku = osat.path + (("?" + osat.query) if osat.query else "")
        pituus = int(self.headers.get('Content-Length') or 0)
        runko = self.rfile.read(pituus) if pituus else b''
        kentat = ",".join(sorted(k for k, v in urllib.parse.parse_qsl(runko.decode('latin1')))) if runko else ""
        arvot = dict(urllib.parse.parse_qsl(runko.decode('latin1'))) if runko else {}
        if polku.startswith('/bg/top'):
            laji = 'TOP'
        elif self.command == 'POST' and arvot.get('submit') == 'To Top':
            # Sivuston oma vastaus To Topille on Top Page, ja juuri se on tassa testattava.
            laji = 'TOP'
        elif polku.startswith('/bg/move/'):
            laji = 'OVER'
        elif MUU is not None and polku.startswith('/bg/game/'):
            laji = 'MUU'
        elif NEXTGAME is not None and polku.startswith('/bg/nextgame'):
            laji = 'NEXT'
        elif DOIT is not None and polku.startswith('/bg/resign/doit'):
            laji = 'DOIT'
        elif RESIGN is not None and polku.startswith('/bg/resign'):
            laji = 'RESIGN'
        elif PROFIILI is not None and polku.startswith('/bg/profile'):
            # Myos POST tanne saa saman sivun: asetuksen tallennus ei mene sivustolle, ja
            # airlock on juuri se syy miksi tama tyokalu on turvallinen asetusten kanssa.
            laji = 'PROFIILI'
        else:
            laji = 'ESTETTY'
        rivi = "%s  %-5s %-6s %s%s" % (
            datetime.datetime.now().strftime('%H:%M:%S'), self.command, laji, polku,
            ("  kentat=" + kentat) if kentat else "")
        LOG.append(rivi)
        print(rivi, flush=True)
        if laji == 'TOP':
            self.vastaa(TOP)
        elif laji == 'OVER':
            self.vastaa(OVER)
        elif laji == 'MUU':
            self.vastaa(MUU)
        elif laji == 'PROFIILI':
            self.vastaa(PROFIILI)
        elif laji == 'RESIGN':
            self.vastaa(RESIGN)
        elif laji == 'DOIT':
            self.vastaa(DOIT)
        elif laji == 'NEXT':
            self.vastaa(NEXTGAME)
        else:
            self.vastaa(b'<html><body>estetty</body></html>', 503)

    do_GET = kasittele
    do_POST = kasittele
    do_HEAD = kasittele

class S(socketserver.ThreadingTCPServer):
    daemon_threads = True
    allow_reuse_address = False

print("Kuoriproxy 127.0.0.1:8899 - mikaan ei mene sivustolle", flush=True)
S(("127.0.0.1", 8899), H).serve_forever()
