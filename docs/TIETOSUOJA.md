# Tietosuojaseloste

**Tila: julkaistu 6.9.2026 osoitteessa <https://tommihaa.github.io/dg-android-privacy/>,
yhteysosoite puuttuu.** Seloste asuu omassa julkisessa repossa `tommihaa/dg-android-privacy`
(yksi `index.html`, GitHub Pages `master`-branchista), koska tätä repoa ei avata sen takia
ja portfoliorepo on henkilön pinta eikä sovelluksen (`Kaanon/LEVITYS.md` §6.1 kohta 4).
Tämä tiedosto on selosteen lähde ja perustelu; julkinen sivu on kopio, ja muutos tehdään
ensin tähän ja sitten sinne. Sivulla `Contact`-kohdassa lukee toistaiseksi että osoite
lisätään ennen kaupan listausta, ja osoitteen valinta on Tommin.

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

**Oikeuksia on kaksi:** `INTERNET` ja `ACCESS_NETWORK_STATE`. Ei sijaintia, ei yhteystietoja,
ei tiedostoja lukuun ottamatta sitä yhtä tiedostoa jonka käyttäjä itse valitsee viennille.

## Mitä selosteessa on sanottava, koska se ei ole ilmeistä

**Viestiarkisto on Androidin Auto Backupissa.** Se tarkoittaa että arkisto voi kopioitua
käyttäjän Google-tilille, jos laitteen varmuuskopiointi on päällä. Tämä on käyttäjän ja
Googlen välinen asia eikä sovelluksen, mutta se on **tiedonsiirto pois laitteelta**, ja
siksi se sanotaan ääneen. Tunnukset on jätetty kopion ulkopuolelle
(`data_extraction_rules.xml`).

**Tunnukset ovat salattuina laitteella.** AES-GCM, avain Androidin Keystoressa
(`SharedPrefsCredentialsStore`, `AesGcmCipher`, `KeystoreKey`). Ne lähetetään vain
DailyGammonin omalle kirjautumissivulle, eikä niitä kirjoiteta lokiin missään.

**Yhteysosoite on Tommin päätettävä.** Kauppa vaatii toimivan yhteystiedon. Alla on
`<contact>`, ja siihen tulee osoite jonka Tommi valitsee näkyväksi. Sitä ei valita hänen
puolestaan.

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

Where your data goes

- To dailygammon.com, and nowhere else. The app has no server of its own, and it
  sends nothing to the developer.

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

The app asks for internet access and for the ability to see whether the device is
online. Nothing else.

Children

The app is not directed at children and collects nothing about anyone.

Deleting your data

Uninstalling the app removes the archive, your settings and your stored
credentials from the device. Files you exported yourself, and any backup held in
your Google account, are removed the way you normally remove those.

Changes

If this policy changes, the date at the top changes with it.

Contact

<contact>
```

## Mitä selosteessa ei sanota, ja miksi

**Ei GDPR-sanastoa eikä rekisterinpitäjä-muotoiluja.** Sovellus ei kerää mitään eikä lähetä
mitään kenellekään, joten rekisteriä ei synny. Muodollinen sanasto antaisi vaikutelman
käsittelystä jota ei tapahdu.

**Ei lupausta siitä ettei sivusto kerää mitään.** Seloste puhuu tästä sovelluksesta.
DailyGammon on erillinen palvelu, jolla on omat käytäntönsä, eikä tämä dokumentti voi
puhua sen puolesta.

**Ei mainintaa yhden laitteen rajauksesta.** Se on käytettävyysrajaus eikä tietosuoja-asia,
ja se kuuluu kaupan kuvaustekstiin (`docs/JULKAISU.md`).
