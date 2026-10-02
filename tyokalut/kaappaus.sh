#!/bin/bash
# Kaappaa tabletin ruudun ja pienentää sen luettavaksi. Git Bashista:
#
#     tyokalut/kaappaus.sh <nimi> [hakemisto]
#
# Tuottaa <hakemisto>/<nimi>.png (täysi, 1752x2800 SM-T970:llä) ja <nimi>_s.png
# (enintään 800x1280), joka riittää lukemiseen ja on murto-osa tokeneista.
#
# Kaksi ansaa jotka tämä kiertää (Kaanon/docs/ymparistofaktat.md):
# - PowerShellin uudelleenohjaus rikkoo binäärin, siksi screencap laitteelle ja adb pull.
# - Git Bash muuntaa /sdcard/x.png Windows-poluksi ennen kuin adb näkee sen, siksi
#   MSYS_NO_PATHCONV=1. Ilman sitä screencap tulostaa käyttöohjeensa ja pull ei löydä mitään.
#
# adb ei ole PATHissa; polku on sama jonka tyokalut/sessio.py etsii.
set -e
NIMI="${1:?anna kaappaukselle nimi}"
HAK="${2:-$PWD}"
ADB="$LOCALAPPDATA/Android/Sdk/platform-tools/adb.exe"
export MSYS_NO_PATHCONV=1
"$ADB" shell screencap -p /sdcard/dg_kaappaus.png
"$ADB" pull /sdcard/dg_kaappaus.png "$HAK/$NIMI.png" >/dev/null
"$ADB" shell rm /sdcard/dg_kaappaus.png
python -c "from PIL import Image; im=Image.open(r'$HAK/$NIMI.png'); print(im.size); im.thumbnail((800,1280)); im.save(r'$HAK/${NIMI}_s.png')"
