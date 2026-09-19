#!/usr/bin/env python3
"""
Pelisession talteenoton kytkin: proxy, tabletin asetus ja todennus yhtenä komentona.

Miksi tämä on olemassa: `proxy.py` on oikea työkalu mutta se ei tiedä mitään tabletista,
ja kytkeminen on ollut kolmen erillisen käsityön sarja (käynnistä proxy, syötä osoite
laitteen asetuksiin, muista tarkistaa menikö mitään läpi). Kolmas vaihe on se joka jää
tekemättä, ja **ilman sitä putki näyttää päällä olevalta silloinkin kun se ei ole**.

Mitattu ansa 22.8.2026, joka on tämän tiedoston suora syy. Edellisen illan proxy oli jäänyt
päälle eri työhakemistoon, ja se oli varannut portin ensin. Uusi proxy käynnistyi
virheittä, tabletin liikenne kulki, kaikki näytti toimivan, ja sivut kirjoittuivat
Projects-juureen orvoksi hakemistoksi jota kukaan ei versioi. Molemmat prosessit sanoivat
`Proxy kuuntelee 0.0.0.0:8899`, eikä kumpikaan valehdellut. Tämä on sokea koetin
(`KÄSITTEISTÖ.md` §0.2): komento onnistui ja raportoi oikean näköisesti väärästä paikasta.

Siksi jokainen komento täällä **todentaa lopputuloksen eikä omaa suoritustaan**:
`aloita` hyväksyy itsensä vasta kun tabletin tekemä pyyntö on ilmestynyt tämän session
omaan `istunto.tsv`:hen, ja `tila` lukee saman asian uudelleen sen sijaan että muistaisi.

Käyttö:

    python tyokalut/sessio.py aloita     # proxy päälle, tabletti kiinni, todennus
    python tyokalut/sessio.py tila       # onko putki oikeasti päällä juuri nyt
    python tyokalut/sessio.py lopeta     # tabletin asetus pois, proxy alas, yhteenveto

`aloita` käynnistää 18.9.2026 alkaen myös SYN-vahdin (`synvahti.py`, `--ei-vahtia` estää):
TCP-kättely sivustolle kahden sekunnin välein ilman HTTP:tä, loki `synvahti.tsv`. Se on
vertailurivi proxyn 502:lle, joka paikannettiin pudonneeseen SYNiin (`docs/KOHDE.md`).
    python tyokalut/sessio.py siivoa     # luettele yli kahden vrk:n vanhat nauhat (ei poista)
    python tyokalut/sessio.py yhdista    # langaton adb: parikytkentä kerran, sitten yhdistys

Langaton adb tuli 12.9.2026 (Tommin tilaus, *"langaton adb ehdottomasti mukaan"*). Havainto
`sessio-12-9-ilta`: kaapeli oli löysässä eikä se katkaissut mitään, koska proxy kulkee wifin
yli koneen lähiverkko-osoitteeseen. Kaapelia tarvitsee vain adb, eli asetuksen kirjoitus ja
purku, nauhan pätkän aloitus ja nauhojen veto. `yhdista` tekee parikytkennän (`adb pair`,
kerran per laite per kone, koodi laitteen ruudulta) ja yhdistyksen (`adb connect`, joka
kerta, koska portti vaihtuu). Kännykkä jätettiin ulkopuolelle samana iltana (Tommin päätös), mutta
`--laite` on kaikissa komennoissa: kahden laitteen tilanne ei ole enää virhe vaan valinta.

Nauha tulee mukana `aloita`ssa oletuksena (Tommin päätös 9.9.2026): *"aina kun pyydän
proxy-sessiota, niin nauhoita-toiminto samaan säästää vaivaa"*. Ruutu on se puoli jota loki
ei voi näyttää, koska sivu näyttää lokissa samalta riippumatta siitä mitä sovellus siitä
piirsi. Hinta on noin 7,5 Mt minuutissa, ja `siivoa` on olemassa siksi että se kertyy
hiljaa. Nauha katsotaan saman session aikana eikä arkistoida: kirjaus jää, nauha ei.

Sama ansa toisessa muodossa, korjattu 23.8.2026: `aloita --hakemisto X` ja `lopeta`
ilman lippua raportoivat eri hakemistoa, joten yhteenveto laski oletushakemiston 908
riviä sen session 22 sivun sijaan. Proxy ja laitteen asetus menivät oikein, vain luku
oli väärästä paikasta. Korjaus on samaa lajia kuin muukin tässä tiedostossa: `tila` ja
`lopeta` lukevat hakemiston **käynnissä olevan proxyn komentoriviltä** eivätkä
oletuksesta, eli elävästä tilasta eikä muistista. Jos kuuntelijaa ei ole eikä lippua
annettu, hakemistoa ei voi tietää, ja se sanotaan ääneen.

Rajaus: laitteen asetus on **globaali** `http_proxy` eikä wifi-verkon oma. Ero on siinä
että globaali koskee myös mobiilidataa, joten `lopeta` on ajettava ennen kuin tabletti
lähtee tästä verkosta. Verkkokohtainen asetus vaatisi käyttöliittymän eikä ole ajettavissa
`adb`:llä.
"""

import argparse
import os
import re
import shutil
import socket
import subprocess
import sys
import time

if hasattr(sys.stdout, "reconfigure"):
    sys.stdout.reconfigure(encoding="utf-8", errors="replace")

JUURI = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
PORTTI = 8899
# Testiosoite jonka vastaus on pieni ja jonka tunnistaa lokista. Kohde on sama sivusto,
# koska proxy kirjaa vain sen: vieras osoite menisi läpi jättämättä jälkeä, ja silloin
# todennus todistaisi vain sen että jokin vastasi.
KOETINOSOITE = "http://dailygammon.com/bg/"
OLETUSHAKEMISTO = os.path.join(JUURI, "raakasivut", "sessio")
# Nauhan työhakemisto laitteella. Oma kansio eikä `/sdcard`in juuri, jotta `lopeta` voi
# vetää ja tyhjentää sen kokonaisuutena eikä nimiä arvaamalla.
NAUHAKANSIO = "/sdcard/dg-nauha"
# Lippu jonka olemassaolo pitää laitteen silmukkaa käynnissä. Pysäytys on tämän poisto
# eikä prosessin tappo, jotta viimeinen pätkä ehtii sulkea mp4:nsä siististi.
NAUHALIPPU = NAUHAKANSIO + "/kaynnissa"
# `screenrecord`in oma yläraja on 180 s (v1.3), joten pidempi sessio on pätkittävä.
NAUHAPATKA = 180


def adb_polku():
    """`adb` ei ole PATHissa tällä koneella, joten se etsitään SDK:n vakiopaikasta."""
    loydetty = shutil.which("adb")
    if loydetty:
        return loydetty
    oletus = os.path.join(
        os.environ.get("LOCALAPPDATA", ""), "Android", "Sdk", "platform-tools", "adb.exe"
    )
    if os.path.exists(oletus):
        return oletus
    kuole("adb ei löytynyt PATHista eikä SDK:n vakiopaikasta.")


# Valittu laite (adb:n serial, USB-sarja tai `ip:portti`). Asetetaan `main`issa ennen
# komentoa, jotta jokainen `adb`-kutsu osuu samaan laitteeseen kun kytkettynä on kaksi.
LAITE = None


def adb(*argumentit, tarkista=True):
    kohdistus = ["-s", LAITE] if LAITE else []
    tulos = subprocess.run(
        [adb_polku(), *kohdistus, *argumentit], capture_output=True, text=True, timeout=60
    )
    if tarkista and tulos.returncode != 0:
        kuole("adb %s epäonnistui: %s" % (" ".join(argumentit), tulos.stderr.strip()))
    return tulos.stdout.strip()


def kuole(viesti):
    print("VIRHE: %s" % viesti)
    sys.exit(1)


def kytketyt():
    """adb:n näkemät laitteet tilassa `device`, serial kerrallaan."""
    return [
        r.split("\t")[0]
        for r in adb("devices").splitlines()[1:]
        if r.strip() and r.endswith("device")
    ]


def laitteen_tunniste(serial):
    """Laitteen oma sarjanumero ja malli. Sama fyysinen laite USB:llä ja langattomasti
    näkyy adb:lle kahtena serialina, ja tämä on se jolla ne tunnistetaan samaksi."""
    ulos = subprocess.run(
        [adb_polku(), "-s", serial, "shell", "getprop ro.serialno; getprop ro.product.model"],
        capture_output=True, text=True, timeout=30,
    ).stdout.split()
    return (ulos[0] if ulos else "?", " ".join(ulos[1:]) if len(ulos) > 1 else "?")


def laite(valinta=None):
    """
    Yksi laite, joko ainoa kytketty tai `--laite`lla valittu.

    Kaksi serialia ei ole virhe jos ne ovat sama laite (USB ja langaton rinnakkain), ja
    silloin USB voittaa koska se ei riipu verkosta. Kaksi eri laitetta ilman valintaa on
    yhä virhe, koska silloin ei tiedä kumpaa nauhoitetaan.
    """
    serialit = kytketyt()
    if not serialit:
        kuole("laitetta ei ole kytketty (adb devices on tyhjä). Langaton: `yhdista`.")
    if valinta:
        osumat = [s for s in serialit if valinta in s]
        if len(osumat) != 1:
            osumat = [
                s for s in serialit
                if valinta.lower() in " ".join(laitteen_tunniste(s)).lower()
            ]
        if len(osumat) != 1:
            kuole("--laite %r ei osu tasan yhteen: %s" % (valinta, ", ".join(serialit)))
        return osumat[0]
    if len(serialit) == 1:
        return serialit[0]
    fyysiset = {}
    for s in serialit:
        sarja, _ = laitteen_tunniste(s)
        # USB-serial ei sisällä kaksoispistettä, langaton on `ip:portti`.
        if sarja not in fyysiset or ":" not in s:
            fyysiset[sarja] = s
    if len(fyysiset) == 1:
        return next(iter(fyysiset.values()))
    rivit = ["  %s  %s" % (s, " ".join(laitteen_tunniste(s))) for s in serialit]
    kuole("kytkettynä on %d laitetta, valitse `--laite`:\n%s" % (len(fyysiset), "\n".join(rivit)))


def portin_haltija():
    """
    Kuka kuuntelee porttia. Palauttaa listan (pid, komentorivi).

    Tämä on ansan oma tarkistus. Windows sallii `SO_REUSEADDR`illa kaksi kuuntelijaa
    samaan porttiin, joten toisen proxyn käynnistyminen **ei** kaadu virheeseen vaan
    jää hiljaa varjoon. Portin varaus on siis luettava erikseen eikä pääteltävä siitä
    että oma käynnistys onnistui.
    """
    if os.name != "nt":
        return []
    komento = (
        "Get-NetTCPConnection -State Listen -LocalPort %d -ErrorAction SilentlyContinue"
        " | Select-Object -ExpandProperty OwningProcess -Unique" % PORTTI
    )
    tulos = subprocess.run(
        ["powershell", "-NoProfile", "-Command", komento],
        capture_output=True,
        text=True,
        timeout=60,
    )
    haltijat = []
    for pid in tulos.stdout.split():
        if not pid.isdigit():
            continue
        rivi = subprocess.run(
            [
                "powershell",
                "-NoProfile",
                "-Command",
                "(Get-CimInstance Win32_Process -Filter \"ProcessId=%s\").CommandLine" % pid,
            ],
            capture_output=True,
            text=True,
            timeout=60,
        ).stdout.strip()
        haltijat.append((int(pid), rivi))
    return haltijat


def lan_osoite():
    """
    Koneen osoite siinä verkossa jossa tabletti on. Luetaan tabletin reitityksestä eikä
    koneen liitäntälistalta: kone tuntee viisi osoitetta joista neljä on linkkilokaaleja,
    ja oikean valitseminen niistä olisi arvaus. Tabletti kertoo aliverkon.
    """
    # wlan-liitännän numero vaihtelee laitteittain: tabletilla wlan0, Pixel 8a:lla wlan1
    # (mitattu 28.8.2026). Siksi haetaan mikä tahansa wlan-reitti eikä vakioitua nimeä.
    reitti = adb("shell", "ip", "route")
    osuma = re.search(r"(\d+\.\d+\.\d+)\.\d+/24 dev wlan\d+", reitti)
    if not osuma:
        kuole("tabletin wlan-reittiä ei löytynyt: %s" % reitti.replace("\n", " "))
    aliverkko = osuma.group(1)
    for _, _, _, _, osoitetiedot in socket.getaddrinfo(
        socket.gethostname(), None, socket.AF_INET
    ):
        osoite = osoitetiedot[0]
        if osoite.startswith(aliverkko + "."):
            return osoite
    kuole(
        "kone ei ole tabletin aliverkossa %s.0/24. Onko tabletti eri wifissä tai "
        "mobiilidatalla?" % aliverkko
    )


def tsv_polku(hakemisto):
    return os.path.join(hakemisto, "istunto.tsv")


def rivimaara(hakemisto):
    polku = tsv_polku(hakemisto)
    if not os.path.exists(polku):
        return 0
    with open(polku, encoding="utf-8") as f:
        return max(0, sum(1 for _ in f) - 1)


def jo_kaytossa(hakemisto):
    """Onko hakemisto jonkin aiemman session koti. Palauttaa (rivit, sivut) tai None.

    Mitattu 9.9.2026: `aloita --hakemisto raakasivut/sessio-9-9-yo` meni hakemistoon
    jossa oli jo saman yön aiempi sessio, ja jätti sinne yhden rivin ennen kuin virhe
    huomattiin. Kahden session sekoittuminen ei näy mistään jälkikäteen, koska rivit
    ovat samassa `istunto.tsv`:ssä samassa muodossa, ja luvut lasketaan tiedostosta.

    Kohteena on olemassa oleva rivi ja tiedosto eikä aikomus, joten tämä on portti
    eikä ohje. Väärä hälytys on poissuljettu: hakemisto joko on tyhjä tai ei ole.
    """
    if not os.path.isdir(hakemisto):
        return None
    rivit = rivimaara(hakemisto)
    sivut = len([n for n in os.listdir(hakemisto) if n.endswith(".html")])
    if rivit == 0 and sivut == 0:
        return None
    return rivit, sivut


def koeta_putki(hakemisto, osoite):
    """
    Todennus: tabletti hakee sivun, ja rivin on ilmestyttävä **tähän** lokiin.

    Paluuarvo on kolmikko (onnistui, vastauskoodi, uusia rivejä). Vastauskoodi yksin ei
    riitä todisteeksi, koska 200 tulee myös silloin kun pyyntö meni jonkun toisen proxyn
    kautta tai suoraan sivustolle. Vasta lokirivi kertoo että se meni tämän läpi.
    """
    ennen = rivimaara(hakemisto)
    koodi = adb(
        "shell",
        "curl -s -m 15 -x http://%s:%d %s -o /dev/null -w '%%{http_code}'"
        % (osoite, PORTTI, KOETINOSOITE),
        tarkista=False,
    ).strip()
    if not koodi or "not found" in koodi:
        # Pixelissä ei ole curlia (mitattu 28.8.2026), mutta nc on. Pyyntö kirjoitetaan
        # proxylle käsin, ja koodi luetaan vastauksen statusriviltä.
        vastaus = adb(
            "shell",
            "printf 'GET %s HTTP/1.0\\r\\nHost: dailygammon.com\\r\\n\\r\\n'"
            " | nc -w 15 %s %d | head -1" % (KOETINOSOITE, osoite, PORTTI),
            tarkista=False,
        ).strip()
        osuma = re.search(r"HTTP/[\d.]+ (\d{3})", vastaus)
        koodi = osuma.group(1) if osuma else vastaus
    for _ in range(10):
        jalkeen = rivimaara(hakemisto)
        if jalkeen > ennen:
            return True, koodi, jalkeen - ennen
        time.sleep(0.5)
    return False, koodi, 0


def laitteen_proxy():
    arvo = adb("shell", "settings", "get", "global", "http_proxy").strip()
    return "" if arvo in (":0", "null", "") else arvo


def hakemisto_prosessista(haltijat):
    """
    Mihin hakemistoon käynnissä oleva proxy oikeasti kirjoittaa.

    Luetaan prosessin omalta komentoriviltä eikä oletuksesta, koska juuri oletus petti
    23.8.2026: `aloita` sai `--hakemisto`-lipun ja `lopeta` ajettiin ilman, jolloin
    yhteenveto laski oletushakemiston 908 riviä sen session 22 sivun sijaan. Komento
    onnistui ja raportoi oikean näköisesti väärästä paikasta, eli sama sokea koetin
    jonka takia tämä tiedosto ylipäätään kirjoitettiin.

    Palauttaa None jos kuuntelijaa ei ole tai komentoriviä ei saatu. Silloin hakemistoa
    ei voi tietää, ja se sanotaan ääneen sen sijaan että arvattaisiin.
    """
    for _, komento in haltijat:
        if not komento:
            continue
        osuma = re.search(r'--hakemisto\s+("[^"]+"|\S+)', komento)
        if osuma:
            return os.path.abspath(osuma.group(1).strip('"'))
    return None


def valitse_hakemisto(args, haltijat):
    """
    Kumpi voittaa, lippu vai käynnissä oleva prosessi. Palauttaa (hakemisto, lähde).

    Nimenomainen lippu voittaa aina, koska sillä luetaan myös päättyneitä sessioita.
    Ristiriita on kuitenkin se tilanne josta halutaan tietää, joten se sanotaan.
    """
    prosessista = hakemisto_prosessista(haltijat)
    if args.hakemisto:
        annettu = os.path.abspath(args.hakemisto)
        if prosessista and prosessista != annettu:
            print(
                "HUOM: käynnissä oleva proxy kirjoittaa hakemistoon %s, mutta lippu "
                "osoittaa hakemistoon %s. Luvut ovat lipun hakemistosta." % (prosessista, annettu)
            )
        return annettu, "lippu"
    if prosessista:
        return prosessista, "käynnissä oleva proxy"
    return os.path.abspath(OLETUSHAKEMISTO), "oletus"


def naytto_paalla():
    """
    Onko näyttö hereillä juuri nyt.

    **Tämä on nauhan ainoa hiljainen ansa, ja se on mitattu kahdesti** (8.9.2026, kaksi
    perakkaista sessiota). `screenrecord` käynnistyy virheittä myös silloin kun näyttö on
    pois, kirjoittaa tiedoston ja päättyy nollan kokoiseen mp4:ään. Mikään paluuarvo ei
    kerro siitä, joten ehto on kysyttävä etukäteen eikä pääteltävä jälkeenpäin.
    """
    for rivi in adb("shell", "dumpsys", "power").splitlines():
        if "mWakefulness=" in rivi:
            return rivi.split("mWakefulness=")[1].strip().split()[0] == "Awake"
    return False


def nauha_kaynnissa():
    """Pyöriikö laitteella `screenrecord` juuri nyt."""
    return bool(adb("shell", "pgrep", "-f", "screenrecord", tarkista=False).strip())


def nauhat_laitteella():
    """Laitteelle kertyneet pätkät nimineen ja tavuineen, vanhin ensin."""
    tuloste = adb("shell", "ls", "-l", NAUHAKANSIO, tarkista=False)
    rivit = []
    for rivi in tuloste.splitlines():
        osat = rivi.split()
        if len(osat) >= 5 and osat[-1].endswith(".mp4"):
            try:
                rivit.append((osat[-1], int(osat[-5])))
            except ValueError:
                rivit.append((osat[-1], 0))
    return sorted(rivit)


def nauha_kaynnista():
    """
    Käynnistää ketjutetun nauhoituksen laitteella ja todentaa että se kirjoittaa.

    **Silmukka asuu laitteella eikä tässä prosessissa**, ja se on tarkoituksellista: pätkän
    yläraja on 180 s, joten viiden minuutin sessio tarvitsee kaksi vaihtoa, eikä kumpikaan
    saa jäädä kiinni siitä onko PC:n päässä ikkuna auki. Vaihdon kohdalle jää alle sekunnin
    katko; se on nauhan hinta eikä vika, ja se kirjataan koska sen huomaa vasta katsomalla.
    """
    if not naytto_paalla():
        # Herätys on turvallinen: se ei napauta mitään, ja lukitusruutu jää paikalleen.
        # Ilman tätä nauha jäi pois 12.9.2026 klo 22.13, kun tabletti oli nukahtanut
        # ennen aloitusta, ja käynnistys piti tehdä käsin erikseen.
        adb("shell", "input", "keyevent", "KEYCODE_WAKEUP", tarkista=False)
        time.sleep(1)
    if not naytto_paalla():
        return False, "näyttö ei herännyt"

    adb("shell", "rm", "-rf", NAUHAKANSIO, tarkista=False)
    adb("shell", "mkdir", "-p", NAUHAKANSIO)
    adb("shell", "touch", NAUHALIPPU)
    silmukka = (
        "i=1; while [ -f %s ]; do screenrecord --time-limit %d %s/nauha$i.mp4; "
        "i=$((i+1)); done" % (NAUHALIPPU, NAUHAPATKA, NAUHAKANSIO)
    )
    # Kohdistus (`-s`) on annettava myös tässä: ilman sitä `adb shell` kieltäytyy hiljaa kun
    # laitteita on kaksi, ja nauha jäi siitä pois 19.9.2026 klo 18.12 (puhelin USB:ssä,
    # tabletti langattomasti).
    kohdistus = ["-s", LAITE] if LAITE else []
    subprocess.Popen(
        [adb_polku(), *kohdistus, "shell", "nohup sh -c '%s' >/dev/null 2>&1 &" % silmukka],
        stdout=subprocess.DEVNULL,
        stderr=subprocess.DEVNULL,
    )

    # **Koon kasvu ei kelpaa todisteeksi, ja se mitattiin 9.9.2026.** `screenrecord`
    # kirjoittaa mp4:n vasta lopetuksessa: ajon aikana tiedosto oli 40 tavua ja heti
    # `-INT`:n jälkeen 55 kt. Ensimmäinen versio odotti kasvua ja tuomitsi toimivan nauhan
    # tyhjäksi. Se mitä tässä voi todentaa on kaksi asiaa: prosessi pyörii ja tiedosto on
    # luotu. Kolmas ehto eli näytön valveillaolo on kysytty jo ennen käynnistystä, ja se on
    # se ehto joka oikeasti erottaa tyhjän nauhan toimivasta.
    for _ in range(10):
        time.sleep(1)
        if nauhat_laitteella() and nauha_kaynnissa():
            return True, "screenrecord pyörii ja nauha1.mp4 on luotu"
    if not nauha_kaynnissa():
        return False, "screenrecord ei jäänyt pyörimään"
    return False, "nauha pyörii muttei luonut tiedostoa"


def nauha_pysayta(hakemisto):
    """
    Pysäyttää nauhan, vetää pätkät hakemistoon ja tyhjentää laitteen.

    Järjestys on se joka merkitsee. Lippu poistetaan **ennen** keskeytystä, jotta silmukka
    ei käynnistä uutta pätkää sen jälkeen kun edellinen loppui. `-INT` eikä `-KILL`, koska
    mp4:n hakemisto kirjoitetaan vasta lopetuksessa: tapettu `screenrecord` jättää
    tiedoston jota mikään ei osaa avata.
    """
    if not nauha_kaynnissa() and not nauhat_laitteella():
        return None

    adb("shell", "rm", "-f", NAUHALIPPU, tarkista=False)
    adb("shell", "pkill", "-INT", "-f", "screenrecord", tarkista=False)
    for _ in range(15):
        time.sleep(1)
        if not nauha_kaynnissa():
            break

    patkat = nauhat_laitteella()
    if not patkat:
        return (0, 0, None)
    kohde = os.path.join(hakemisto, "nauhat")
    os.makedirs(kohde, exist_ok=True)
    for nimi, _ in patkat:
        adb("pull", "%s/%s" % (NAUHAKANSIO, nimi), os.path.join(kohde, nimi), tarkista=False)
    adb("shell", "rm", "-rf", NAUHAKANSIO, tarkista=False)
    tavut = sum(
        os.path.getsize(os.path.join(kohde, n))
        for n, _ in patkat
        if os.path.exists(os.path.join(kohde, n))
    )
    # Vasta tässä koko kertoo jotain, koska mp4 on nyt suljettu. Alle kymmenen kilotavun
    # pätkä on tyhjä: se syntyy kun näyttö sammui kesken nauhoituksen.
    tyhjat = sum(
        1
        for n, _ in patkat
        if os.path.exists(os.path.join(kohde, n))
        and os.path.getsize(os.path.join(kohde, n)) < 10 * 1024
    )
    return (len(patkat), tavut, kohde, tyhjat)


def vahdin_haltijat():
    """
    Käynnissä olevat SYN-vahdit (pid, komentorivi), luettuna prosessilistasta eikä
    muistista, samasta syystä kuin `portin_haltija`. Vahti ei kuuntele porttia, joten
    se löytyy vain komentoriviltä.
    """
    if os.name != "nt":
        return []
    komento = (
        "Get-CimInstance Win32_Process -Filter \"Name='python.exe'\" | "
        "Where-Object { $_.CommandLine -like '*synvahti.py*' } | "
        "ForEach-Object { '' + $_.ProcessId + ' ' + $_.CommandLine }"
    )
    tulos = subprocess.run(
        ["powershell", "-NoProfile", "-Command", komento],
        capture_output=True,
        text=True,
        timeout=60,
    )
    haltijat = []
    for rivi in tulos.stdout.splitlines():
        osat = rivi.strip().split(" ", 1)
        if osat and osat[0].isdigit():
            haltijat.append((int(osat[0]), osat[1] if len(osat) > 1 else ""))
    return haltijat


def vahti_kaynnista(hakemisto):
    """
    SYN-vahti samaan hakemistoon (18.9.2026, `Next Game`n 502 paikannettiin pudonneeseen
    SYNiin). Se avaa TCP-yhteyden sivustolle kahden sekunnin välein eikä lähetä HTTP:tä,
    joten jono ei kulu. Loki `synvahti.tsv` on vertailurivi proxyn 502:lle: kertoo oliko
    reitti poikki vai putosiko yksi SYN. Palauttaa (pid, ip) tai (None, syy).
    """
    loki = os.path.join(hakemisto, "synvahti.log")
    with open(loki, "a", encoding="utf-8") as lokitiedosto:
        prosessi = subprocess.Popen(
            [
                sys.executable,
                os.path.join(JUURI, "tyokalut", "synvahti.py"),
                "--hakemisto",
                hakemisto,
            ],
            cwd=JUURI,
            stdout=lokitiedosto,
            stderr=subprocess.STDOUT,
        )
    time.sleep(1.5)
    if prosessi.poll() is not None:
        return None, "vahti sammui heti, ks. %s" % loki
    ip = ""
    try:
        with open(loki, encoding="utf-8") as f:
            for rivi in f:
                if rivi.startswith("SYN-vahti:"):
                    ip = rivi.split("=", 1)[1].split(",", 1)[0].strip()
    except OSError:
        pass
    return prosessi.pid, ip


def vahdin_yhteenveto(hakemisto):
    """Rivit, pisin kesto ja epäonnistuneet `synvahti.tsv`:stä. None jos tiedostoa ei ole."""
    polku = os.path.join(hakemisto, "synvahti.tsv")
    if not os.path.exists(polku):
        return None
    rivit = 0
    pisin = 0
    viat = []
    with open(polku, encoding="utf-8") as f:
        next(f, None)
        for rivi in f:
            osat = rivi.rstrip("\n").split("\t")
            if len(osat) < 5:
                continue
            rivit += 1
            kesto = int(osat[3]) if osat[3].isdigit() else 0
            pisin = max(pisin, kesto)
            if osat[4] != "ok":
                viat.append((osat[0], kesto, osat[4]))
    return rivit, pisin, viat


def aloita(args):
    hakemisto = os.path.abspath(args.hakemisto or OLETUSHAKEMISTO)
    print("Laite: %s (%s)" % (LAITE, " ".join(laitteen_tunniste(LAITE))))

    haltijat = portin_haltija()
    if haltijat:
        print("Portti %d on jo varattu:" % PORTTI)
        for pid, komento in haltijat:
            print("  PID %d  %s" % (pid, komento or "(komentoriviä ei saatu)"))
        kuole(
            "aiempi proxy on yhä päällä. Aja ensin `lopeta`, tai sammuta prosessi. "
            "Kaksi kuuntelijaa ei kaadu vaan jakaa liikenteen hiljaa."
        )

    varattu = jo_kaytossa(hakemisto)
    if varattu and not args.jatka:
        rivit, sivut = varattu
        kuole(
            "hakemistossa %s on jo %d riviä ja %d sivua, eli se on jonkin aiemman session "
            "koti. Kaksi sessiota samassa istunto.tsv:ssä sekoittuu eikä erotu jälkikäteen. "
            "Valitse uusi nimi (esim. sama pääte numerolla) tai jatka tätä sessiota "
            "lipulla --jatka." % (hakemisto, rivit, sivut)
        )

    os.makedirs(hakemisto, exist_ok=True)
    loki = os.path.join(hakemisto, "proxy.log")
    with open(loki, "a", encoding="utf-8") as lokitiedosto:
        prosessi = subprocess.Popen(
            [
                sys.executable,
                os.path.join(JUURI, "tyokalut", "proxy.py"),
                "--osoite",
                "0.0.0.0",
                "--portti",
                str(PORTTI),
                "--hakemisto",
                hakemisto,
            ],
            cwd=JUURI,
            stdout=lokitiedosto,
            stderr=subprocess.STDOUT,
        )
    time.sleep(1.5)
    if prosessi.poll() is not None:
        kuole("proxy sammui heti käynnistyksen jälkeen, ks. %s" % loki)
    print("Proxy: PID %d, hakemisto %s" % (prosessi.pid, hakemisto))

    osoite = lan_osoite()
    adb("shell", "settings", "put", "global", "http_proxy", "%s:%d" % (osoite, PORTTI))
    asetettu = laitteen_proxy()
    if asetettu != "%s:%d" % (osoite, PORTTI):
        kuole("tabletin proxy-asetus ei tarttunut, arvo on nyt %r" % asetettu)
    print("Tabletti: http_proxy = %s" % asetettu)

    onnistui, koodi, uusia = koeta_putki(hakemisto, osoite)
    if not onnistui:
        kuole(
            "tabletin pyyntö vastasi %s mutta ei jättänyt riviä lokiin. Putki EI ole "
            "päällä: liikenne meni jotain muuta reittiä." % (koodi or "tyhjää")
        )
    print("Todennus: tabletin pyyntö näkyy lokissa (%s, %d uutta riviä)." % (koodi, uusia))

    if args.ei_nauhaa:
        print("Nauha: ei käynnistetty (--ei-nauhaa).")
    else:
        onnistui, syy = nauha_kaynnista()
        if onnistui:
            print("Nauha: käynnissä, %s." % syy)
        else:
            # Ei `kuole`: proxy on tässä vaiheessa päällä ja todennettu, ja sivut ovat se
            # osa jota ei saa jalkeenpain. Nauha on lisätodiste, joten sen puute sanotaan
            # ääneen ja sessio jatkuu.
            print("Nauha: EI käynnissa (%s). Sivut tallentuvat silti." % syy)
    if args.ei_vahtia:
        print("SYN-vahti: ei käynnistetty (--ei-vahtia).")
    else:
        vanhat = vahdin_haltijat()
        if vanhat:
            print("SYN-vahti: jo käynnissä (PID %s), ei toista." % ", ".join(str(p) for p, _ in vanhat))
        else:
            pid, tieto = vahti_kaynnista(hakemisto)
            if pid:
                print("SYN-vahti: käynnissä, PID %d, sivusto %s, loki synvahti.tsv." % (pid, tieto or "?"))
            else:
                print("SYN-vahti: EI käynnissä (%s). Sivut tallentuvat silti." % tieto)
    print("")
    print("Putki on päällä. Sivuja lokissa yhteensä: %d" % rivimaara(hakemisto))
    print("Muista `lopeta` ennen kuin tabletti lähtee tästä verkosta.")


def tila(args):
    """
    Kertoo tilan **nyt**, ei sitä mitä `aloita` aikoinaan raportoi. Tämä on se komento
    joka vastaa kysymykseen 'onko talteenotto mukana', eikä siihen saa vastata muistista.
    """
    haltijat = portin_haltija()
    hakemisto, lahde = valitse_hakemisto(args, haltijat)
    laitteella = laitteen_proxy()

    print("Proxy portissa %d: %s" % (PORTTI, "kyllä" if haltijat else "EI"))
    for pid, komento in haltijat:
        print("  PID %d  %s" % (pid, komento or ""))
    if len(haltijat) > 1:
        print("  HUOM: kuuntelijoita on %d. Liikenne jakautuu, ks. tiedoston "
              "alun ansakuvaus." % len(haltijat))
    print("Tabletin http_proxy: %s" % (laitteella or "EI asetettu"))
    print("Lokin rivejä (%s, %s): %d" % (hakemisto, lahde, rivimaara(hakemisto)))
    patkat = nauhat_laitteella()
    if nauha_kaynnissa():
        print("Nauha: käynnissä, %d pätkää, %d Mt laitteella"
              % (len(patkat), sum(t for _, t in patkat) // (1024 * 1024)))
    elif patkat:
        print("Nauha: EI käynnissä, mutta laitteella on %d pätkää hakematta" % len(patkat))
    else:
        print("Nauha: ei käynnissä")
    vahdit = vahdin_haltijat()
    yhteenveto = vahdin_yhteenveto(hakemisto)
    if vahdit and yhteenveto:
        rivit, pisin, viat = yhteenveto
        print("SYN-vahti: käynnissä (PID %s), %d kättelyä, pisin %d ms, vikoja %d"
              % (", ".join(str(p) for p, _ in vahdit), rivit, pisin, len(viat)))
    elif vahdit:
        print("SYN-vahti: käynnissä, ei vielä rivejä")
    else:
        print("SYN-vahti: ei käynnissä")

    if not haltijat or not laitteella:
        print("")
        print("Talteenotto EI ole päällä.")
        sys.exit(1)

    osoite = laitteella.split(":")[0]
    onnistui, koodi, _ = koeta_putki(hakemisto, osoite)
    print("")
    if onnistui:
        print("Talteenotto on päällä ja todennettu juuri nyt (%s)." % koodi)
    else:
        print("Talteenotto EI ole päällä: pyyntö vastasi %s muttei jättänyt riviä "
              "tähän lokiin." % (koodi or "tyhjää"))
        sys.exit(1)


def lopeta(args):
    # Haltijat luetaan ennen sammutusta, koska hakemisto on kiinni juuri siinä
    # prosessissa joka ollaan tappamassa. Sammutuksen jälkeen sitä ei saa mistään.
    haltijat = portin_haltija()
    hakemisto, lahde = valitse_hakemisto(args, haltijat)
    rivit = rivimaara(hakemisto)

    # Nauha ensin, koska sen vetaminen kayttää laitetta ja hakemisto on jo tiedossa.
    nauha = nauha_pysayta(hakemisto)

    if laitteen_proxy():
        adb("shell", "settings", "put", "global", "http_proxy", ":0")
        print("Tabletin proxy purettu (%s)." % (laitteen_proxy() or ":0"))
    else:
        print("Tabletilla ei ollut proxy-asetusta.")

    for pid, _ in haltijat:
        subprocess.run(
            ["powershell", "-NoProfile", "-Command", "Stop-Process -Id %d -Force" % pid],
            capture_output=True,
            text=True,
        )
        print("Proxy PID %d sammutettu." % pid)
    if not haltijat:
        print("Portissa %d ei ollut kuuntelijaa." % PORTTI)

    for pid, _ in vahdin_haltijat():
        subprocess.run(
            ["powershell", "-NoProfile", "-Command", "Stop-Process -Id %d -Force" % pid],
            capture_output=True,
            text=True,
        )
        print("SYN-vahti PID %d sammutettu." % pid)
    yhteenveto = vahdin_yhteenveto(hakemisto)
    if yhteenveto:
        rivit, pisin, viat = yhteenveto
        print("SYN-vahti: %d kättelyä, pisin %d ms, vikoja %d." % (rivit, pisin, len(viat)))
        for aika, kesto, tulos in viat[:10]:
            print("  %s %d ms %s" % (aika, kesto, tulos[:80]))

    sivut = len([n for n in os.listdir(hakemisto) if n.endswith(".html")]) if os.path.isdir(
        hakemisto
    ) else 0
    print("")
    print("Sessio talletti %d riviä ja %d sivua: %s (%s)" % (rivit, sivut, hakemisto, lahde))
    if nauha is None:
        print("Nauhaa ei ollut.")
    elif nauha[0] == 0:
        print("Nauha oli päällä muttei jättänyt yhtään pätkää.")
    else:
        print("Nauha: %d pätkää, %.1f Mt, %s (versioimatta)"
              % (nauha[0], nauha[1] / (1024.0 * 1024.0), nauha[2]))
        if nauha[3]:
            print("HUOM: %d pätkää jäi tyhjäksi, eli näyttö sammui kesken nauhoituksen."
                  % nauha[3])
    if lahde == "oletus":
        print(
            "HUOM: kuuntelijaa ei ollut, joten hakemisto on oletus eikä luettu prosessilta. "
            "Jos sessio ajettiin `--hakemisto`-lipulla, luvut ovat väärästä paikasta."
        )


def siivoa(args):
    """
    Poistaa vanhat nauhat ja jättää kaiken muun.

    **Vain `.mp4`, ja se on koko rajaus.** Nauha on ainoa tiedostolaji joka kasvaa
    kymmeniä megatavuja sessiossa, ja se on myös ainoa jota luetaan käytännössä vain
    saman illan aikana. Sivut, kuvat ja logcat jäävät koskematta: ne ovat pieniä, ja juuri
    ne ovat se aineisto johon palataan viikkojen päästä (`LUEMINUT.md`:n taulukko osoittaa
    niihin nimellä).

    **Ikäraja on lyhyt tarkoituksella** (Tommin tarkennus 9.9.2026): *"en halua
    sessiokohtaisten nauhojen hautausmaata, vaan ne ovat todiste joka vähentävät
    lisäpromptausta"*. Nauhan arvo on tuoreudessa: se katsotaan saman session aikana, jolloin
    Tommin ei tarvitse selittää mitä ruudulla näkyi. Kun havainto on kirjattu
    `LUEMINUT.md`:hen, kirjaus on se joka jää ja nauha on painolastia. Kahden vuorokauden
    raja jättää tilaa seuraavan päivän jälkityölle muttei arkistolle.

    **Kuivaharjoitus on oletus.** Poisto on peruuttamaton eikä nauhaa voi hakea uudelleen:
    se näyttää hetken joka on jo menneisyyttä. Komento luetteloi siis ensin ja poistaa
    vasta `--tee-se`llä. Sama syy kuin sivujen hakemisessa, toisin päin luettuna.

    **Käynnissä olevan session hakemistoon ei kosketa**, vaikka ikäehto täyttyisi. Kesken
    nauhoituksen vedetty pätkä on juuri se tiedosto jota ollaan kirjoittamassa.
    """
    juuri = os.path.join(JUURI, "raakasivut")
    if not os.path.isdir(juuri):
        kuole("hakemistoa %s ei ole." % juuri)

    raja = time.time() - args.vanhemmat_kuin * 86400
    haltijat = portin_haltija()
    kaynnissa = {hakemisto_prosessista(haltijat)} if haltijat else set()
    kaynnissa.discard(None)

    loydot = []
    ohitettu_kaynnissa = 0
    for kansio, _, tiedostot in os.walk(juuri):
        if os.path.basename(kansio) != "nauhat":
            continue
        sessiohakemisto = os.path.dirname(kansio)
        for nimi in tiedostot:
            if not nimi.lower().endswith(".mp4"):
                continue
            polku = os.path.join(kansio, nimi)
            if os.path.getmtime(polku) >= raja:
                continue
            if os.path.abspath(sessiohakemisto) in {os.path.abspath(k) for k in kaynnissa}:
                ohitettu_kaynnissa += 1
                continue
            loydot.append((polku, os.path.getsize(polku), os.path.getmtime(polku)))

    if ohitettu_kaynnissa:
        print("Ohitettu %d tiedostoa: sessio on yhä käynnissä siinä hakemistossa."
              % ohitettu_kaynnissa)

    if not loydot:
        print("Ei yli %d vuorokauden ikäisiä nauhoja. Mitään ei poistettu."
              % args.vanhemmat_kuin)
        return

    loydot.sort(key=lambda r: r[2])
    tavut = sum(koko for _, koko, _ in loydot)
    for polku, koko, muokattu in loydot:
        print("  %-58s %6.1f Mt  %s"
              % (os.path.relpath(polku, JUURI), koko / (1024.0 * 1024.0),
                 time.strftime("%d.%m.%Y", time.localtime(muokattu))))
    print("")
    print("%d nauhaa, %.1f Mt, vanhempia kuin %d vrk."
          % (len(loydot), tavut / (1024.0 * 1024.0), args.vanhemmat_kuin))

    if not args.tee_se:
        print("Mitään ei poistettu. Poista nämä lipulla --tee-se.")
        return

    poistettu = 0
    for polku, _, _ in loydot:
        try:
            os.remove(polku)
            poistettu += 1
        except OSError as virhe:
            print("  EI POISTETTU %s: %s" % (os.path.relpath(polku, JUURI), virhe))
    print("Poistettu %d nauhaa, %.1f Mt vapautui."
          % (poistettu, tavut / (1024.0 * 1024.0)))


def mdns_osoitteet():
    """Langattoman adb:n mainostamat `ip:portti`-osoitteet lähiverkossa, jos adb:n mDNS
    toimii. Tyhjä lista ei todista poissaoloa: Windowsin adb ei aina näe mainoksia."""
    ulos = adb("mdns", "services", tarkista=False)
    return re.findall(r"_adb-tls-connect\._tcp\.?\s+(\d+\.\d+\.\d+\.\d+:\d+)", ulos)


def yhdista(args):
    """
    Langaton adb kahdessa vaiheessa, ja kumpikin todennetaan `adb devices`ista.

    Parikytkentä (`--pari ip:portti --koodi NNNNNN`) tehdään kerran per laite per kone.
    Laitteen `Wireless debugging` › `Pair device with pairing code` näyttää osoitteen ja
    koodin, ja se portti on **eri** kuin yhdistysportti. Yhdistys (`--osoite ip:portti`)
    tehdään joka kerta, koska portti vaihtuu kun asetus kytketään päälle. Ilman `--osoite`a
    yritetään mDNS:ää ja hyväksytään vain yksi osuma.
    """
    if args.pari:
        if not args.koodi:
            kuole("--pari tarvitsee --koodi (kuusi numeroa laitteen ruudulta).")
        ulos = subprocess.run(
            [adb_polku(), "pair", args.pari, args.koodi],
            capture_output=True, text=True, timeout=60,
        )
        teksti = (ulos.stdout + ulos.stderr).strip()
        print("Parikytkentä: %s" % teksti)
        if "Successfully paired" not in teksti:
            kuole("parikytkentä ei onnistunut. Tarkista osoite, portti ja koodi ruudulta.")

    osoite = args.osoite
    if not osoite:
        # Jo kiinni oleva langaton yhteys kelpaa sellaisenaan. mDNS ei mainosta laitetta
        # joka on jo yhdistetty (mitattu 12.9.2026 klo 22.13), joten ilman tätä komento
        # kaatui vaikka `adb devices` näytti laitteen.
        langattomat = [s for s in kytketyt() if ":" in s]
        if len(langattomat) == 1:
            osoite = langattomat[0]
            print("Langaton yhteys on jo kiinni: %s" % osoite)
        elif langattomat:
            kuole("kiinni on %d langatonta laitetta, anna --osoite: %s"
                  % (len(langattomat), ", ".join(langattomat)))
    if not osoite:
        loydetyt = mdns_osoitteet()
        if len(loydetyt) == 1:
            osoite = loydetyt[0]
            print("mDNS löysi osoitteen %s" % osoite)
        elif loydetyt:
            kuole("mDNS löysi %d osoitetta, anna --osoite: %s"
                  % (len(loydetyt), ", ".join(loydetyt)))
        else:
            kuole("anna --osoite ip:portti (laitteen Wireless debugging -ruudulta); "
                  "mDNS ei löytänyt mitään.")

    ennen = set(kytketyt())
    ulos = subprocess.run(
        [adb_polku(), "connect", osoite], capture_output=True, text=True, timeout=60
    )
    print("Yhdistys: %s" % (ulos.stdout + ulos.stderr).strip())
    # Todennus luetaan laiteluettelosta eikä connectin tekstistä, koska `connect` sanoo
    # `connected to` myös silloin kun laite jää tilaan `offline` tai `unauthorized`.
    jalkeen = kytketyt()
    if osoite not in jalkeen:
        kuole("%s ei ole tilassa `device`. `adb devices` näyttää: %s"
              % (osoite, ", ".join(jalkeen) or "(tyhjä)"))
    sarja, malli = laitteen_tunniste(osoite)
    print("Laite vastaa langattomasti: %s, sarja %s, malli %s." % (osoite, sarja, malli))
    if osoite not in ennen and any(":" not in s for s in jalkeen):
        print("Huomio: myös USB-yhteys on kiinni. `aloita` valitsee sen ellei `--laite` "
              "sano toisin.")


def main():
    jasennin = argparse.ArgumentParser(description=__doc__)
    alikomennot = jasennin.add_subparsers(dest="komento", required=True)
    for nimi, funktio in (("aloita", aloita), ("tila", tila), ("lopeta", lopeta)):
        ali = alikomennot.add_parser(nimi)
        # Lippu on alikomennossa eikä juuressa, jotta se kelpaa siinä järjestyksessä
        # jossa sen luontevasti kirjoittaa: `sessio.py aloita --hakemisto ...`.
        ali.add_argument(
            "--laite",
            default=None,
            help=(
                "mikä laite kun kytkettynä on useampi: osa adb-serialista, ip-osoitteesta, "
                "sarjanumerosta tai mallinimestä. Sama laite USB:llä ja langattomasti ei "
                "vaadi tätä, USB voittaa."
            ),
        )
        ali.add_argument(
            "--hakemisto",
            default=None,
            help=(
                "minne sivut kirjoitetaan. `aloita` ilman lippua käyttää hakemistoa "
                "raakasivut/sessio; `tila` ja `lopeta` ilman lippua lukevat hakemiston "
                "käynnissä olevan proxyn komentoriviltä, eivät oletuksesta."
            ),
        )
        if nimi == "aloita":
            ali.add_argument(
                "--ei-nauhaa",
                action="store_true",
                help=(
                    "älä nauhoita ruutua. Oletus on että nauha tulee mukana (Tommin päätös "
                    "9.9.2026): sitä ei voi hakea jälkikäteen, ja se on ainoa todiste siitä "
                    "mitä ruudulla luki."
                ),
            )
            ali.add_argument(
                "--ei-vahtia",
                action="store_true",
                help=(
                    "älä käynnistä SYN-vahtia. Oletus on että vahti tulee mukana (18.9.2026): "
                    "se avaa TCP-yhteyden sivustolle kahden sekunnin välein ilman HTTP:tä ja "
                    "kirjaa keston synvahti.tsv:hen, jotta proxyn 502 saa vertailurivin."
                ),
            )
            ali.add_argument(
                "--jatka",
                action="store_true",
                help=(
                    "kirjoita hakemistoon jossa on jo aiemman session sivuja. Ilman tätä "
                    "`aloita` kieltäytyy, koska kaksi sessiota samassa istunto.tsv:ssä ei "
                    "erotu jälkikäteen (mitattu 9.9.2026)."
                ),
            )
        else:
            ali.set_defaults(ei_nauhaa=False, jatka=False, ei_vahtia=False)
        ali.set_defaults(func=funktio)

    langaton = alikomennot.add_parser("yhdista")
    langaton.add_argument(
        "--osoite", default=None, metavar="IP:PORTTI",
        help="yhdistysosoite laitteen Wireless debugging -ruudulta. Ilman: mDNS.",
    )
    langaton.add_argument(
        "--pari", default=None, metavar="IP:PORTTI",
        help="parikytkennän osoite (eri portti kuin yhdistyksen), kerran per laite.",
    )
    langaton.add_argument("--koodi", default=None, help="parikytkennän kuusinumeroinen koodi.")
    langaton.set_defaults(func=yhdista, laite=None)

    puhdistus = alikomennot.add_parser("siivoa")
    puhdistus.add_argument("--laite", default=None, help="mikä laite, ks. `aloita --help`.")
    puhdistus.add_argument(
        "--vanhemmat-kuin",
        type=int,
        default=2,
        metavar="VRK",
        dest="vanhemmat_kuin",
        help="ikäraja vuorokausina, oletus 2.",
    )
    puhdistus.add_argument(
        "--tee-se",
        action="store_true",
        dest="tee_se",
        help="poista oikeasti. Ilman tätä komento vain luetteloi.",
    )
    puhdistus.set_defaults(func=siivoa, hakemisto=None, ei_nauhaa=False, jatka=False)

    args = jasennin.parse_args()
    if args.func is not yhdista:
        global LAITE
        LAITE = laite(args.laite)
    args.func(args)


if __name__ == "__main__":
    main()
