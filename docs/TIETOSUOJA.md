# Tietosuojaseloste

**Tila: julkaistu 6.9.2026 osoitteessa <https://tommihaa.github.io/dg-android-privacy/>,
yhteysosoite lisätty 23.9.2026.** Seloste asuu omassa julkisessa repossa `tommihaa/dg-android-privacy`
(yksi `index.html`, GitHub Pages `master`-branchista), koska tätä repoa ei avata sen takia
ja portfoliorepo on henkilön pinta eikä sovelluksen (`Kaanon/LEVITYS.md` §6.1 kohta 4).
Tämä tiedosto on selosteen lähde ja perustelu; julkinen sivu on kopio, ja muutos tehdään
ensin tähän ja sitten sinne. Yhteysosoite on `no.jopas@gmail.com` (Tommin valinta A 23.9.2026),
sama kuin kaupan kehittäjän sähköposti.

Nimi on `tietosuojaseloste` eikä `tietoturvaseloste`. Ne ovat eri asioita: tietoturva on
sitä miten tiedot suojataan, tietosuoja sitä mitä tietoja kerätään ja mihin ne menevät.
Kauppa vaatii jälkimmäisen, ja tämä dokumentti vastaa siihen.

## Miksi tämä on lyhyt

Seloste on lyhyt koska sovellus on. Kolme asiaa tekevät siitä lyhyen, ja kaikki kolme ovat
tarkistettavissa lähdekoodista eivätkä ole lupauksia.

**Sovelluksessa ei ole yhtään kolmannen osapuolen SDK:ta.** Ei analytiikkaa, ei mainoksia,
ei kaatumisraportointia. Riippuvuudet ovat AndroidX, Compose, OkHttp, Jsoup ja Room.

**Sovellus ottaa yhteyden vain `dailygammon.com`iin.** Ei omaa palvelinta, koska sellaista
ei ole olemassa.

**Oikeuksia on neljä** (julkaisubuildin yhdistetty manifesti 27.9.2026): `INTERNET` ja
`ACCESS_NETWORK_STATE` sekä sovelluslukon `USE_BIOMETRIC` ja `USE_FINGERPRINT`, jotka tuo
`androidx.biometric`. Lukko on oletuksena pois, ja tunnistuksen tekee järjestelmä: sovellus saa
vain tuloksen. Ei sijaintia eikä yhteystietoja. Tiedostoista vain ne jotka käyttäjä itse
valitsee: viennin tiedosto ja listojen alle näytettävien kuvien kansio.

## Mitä selosteessa on sanottava, koska se ei ole ilmeistä

**Viestiarkisto on Androidin Auto Backupissa.** Se tarkoittaa että arkisto voi kopioitua
käyttäjän Google-tilille, jos laitteen varmuuskopiointi on päällä. Tämä on käyttäjän ja
Googlen välinen asia eikä sovelluksen, mutta se on **tiedonsiirto pois laitteelta**, ja
siksi se sanotaan ääneen. Tunnukset on jätetty kopion ulkopuolelle
(`data_extraction_rules.xml`).

**Tunnukset ovat salattuina laitteella.** AES-GCM, avain Androidin Keystoressa
(`SharedPrefsCredentialsStore`, `AesGcmCipher`, `KeystoreKey`). Ne lähetetään vain
DailyGammonin omalle kirjautumissivulle, eikä niitä kirjoiteta lokiin missään.

**Yhteys sivustoon on salaamaton, ja se sanotaan ääneen** (Tommin päätös 21.9.2026:
*"pelaajille kannattaa painottaa tätä, siis viestittelyssä sama riski kuin
dailygammon-selaimella pelattuna"*). Sivusto ei tarjoa HTTPS:ää
(`network_security_config.xml`, `docs/KOHDE.md`), joten kirjautuminen, siirrot ja viestit
kulkevat selkokielisinä molempiin suuntiin. Sovellus ei voi korjata sitä, koska salaus vaatii
palvelimen. Sama asia on App Helpin kohdassa A.2 ja kaupan listauksessa (`docs/KAUPPA.md`).

**Yhteysosoite on Tommin valitsema.** Kauppa vaatii toimivan yhteystiedon. Tommi valitsi
23.9.2026 oman osoitteensa `no.jopas@gmail.com` erillisen sovellusosoitteen sijaan, tietäen
että osoite on käytössä muuallakin ja näkyy julkisesti.

## Seloste

Alla oleva teksti on se joka menee julkiseen osoitteeseen. Se on englanniksi, koska
käyttöliittymä on englanniksi (`CLAUDE.md`).

```
Privacy Policy

DG Android (unofficial DailyGammon client)
Last updated: <date>

This app is a third-party client for dailygammon.com. It is not affiliated with
DailyGammon or its operators.

What the app stores on your device

- Your DailyGammon username and password, so you do not have to type them every
  time. They are encrypted with a key held in the Android Keystore and are never
  written to logs. They are sent only to dailygammon.com, over the site's own
  sign-in form, and nowhere else.

- The messages you receive and send on DailyGammon, in a database on your device.
  DailyGammon does not keep messages on its server, so this local archive is the
  only copy. Keeping it is the reason this app exists.

- Your own settings for the app, such as board appearance and which optional helps
  you have switched on.

- Notes you write for yourself in the app, such as reminders and marked positions,
  and the app's own bookkeeping: which matches it has already shown you and when
  its connection to the site dropped. All of it stays on the device.

Where your data goes

- To dailygammon.com, and nowhere else. The app has no server of its own, and it
  sends nothing to the developer.

- Unencrypted. dailygammon.com does not offer HTTPS, so everything the app exchanges
  with the site travels in the clear: your sign-in, your moves and your messages,
  both sent and received. This is the same as using DailyGammon in a browser; the
  app cannot add encryption the site does not provide. Anyone able to watch the
  network you are on can read that traffic.

- If you have Android's backup switched on for this app, your message archive may
  be copied to your own Google account by the operating system. That is a copy you
  control through your Google account, not something the app uploads. Your
  DailyGammon username and password are deliberately excluded from that backup.

- If you use the app's Export or Back up features, the app writes a file to the
  location you choose. Where that file then goes is up to you.

What the app does not do

- No analytics, no advertising, no tracking, and no third-party components that
  could do any of those.

- No background fetching. The app contacts DailyGammon only while you are using it
  and only when you ask it to load something.

- No account of any kind with the developer. There is nothing to sign up for.

Permissions

The app asks for internet access, for the ability to see whether the device is
online and for the use of the device's fingerprint or PIN check. The last is used
only if you turn on the lock in Settings. Android does the check, and the app
learns only whether it passed. Nothing else.

Children

The app is not directed at children and collects nothing about anyone.

Deleting your data

Uninstalling the app removes the archive, your settings and your stored
credentials from the device. Files you exported yourself, and any backup held in
your Google account, are removed the way you normally remove those.

Changes

If this policy changes, the date at the top changes with it.

Contact

Questions about this policy or the app: no.jopas@gmail.com
```

## Mitä selosteeseen lisättiin jälkikäteen

**Muistiinpanot ja kirjanpito (21.9.2026).** Kannassa on viestien lisäksi neljä taulua
joita 6.9. teksti ei nimennyt: `reminders`, `marked_positions`, `seen_matches` ja
`connection_drops` (`data/src/main/kotlin/fi/tommi/dg/data/db/`). Kaksi ensimmäistä ovat
käyttäjän omia merkintöjä ja kaksi jälkimmäistä sovelluksen kirjanpitoa, eikä mikään niistä
lähde laitteelta muuten kuin Auto Backupin mukana (`data_extraction_rules.xml` ottaa koko
`dg.db`:n). Seloste sanoo nyt sen yhdellä kohdalla. Lisäys tuli julkistuksen työjonon
tarkistuksesta, jossa seloste luettiin kannan tauluja vasten eikä muistista.

## Mitä selosteessa ei sanota, ja miksi

**Ei GDPR-sanastoa eikä rekisterinpitäjä-muotoiluja.** Sovellus ei kerää mitään eikä lähetä
mitään kenellekään, joten rekisteriä ei synny. Muodollinen sanasto antaisi vaikutelman
käsittelystä jota ei tapahdu.

**Ei lupausta siitä ettei sivusto kerää mitään.** Seloste puhuu tästä sovelluksesta.
DailyGammon on erillinen palvelu, jolla on omat käytäntönsä, eikä tämä dokumentti voi
puhua sen puolesta.

**Ei mainintaa yhden laitteen rajauksesta.** Se on käytettävyysrajaus eikä tietosuoja-asia,
ja se kuuluu kaupan kuvaustekstiin (`docs/JULKAISU.md`).
