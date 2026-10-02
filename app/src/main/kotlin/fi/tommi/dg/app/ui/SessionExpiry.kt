package fi.tommi.dg.app.ui

/**
 * Tila joka tarkoittaa: **istunto ei enää kelpaa.**
 *
 * Kahdeksan tilaa seitsemässä hierarkiassa sanoo saman asian, ja niiden yhteinen merkitys
 * oli tähän asti olemassa vain kutsupaikan `is`-ehdossa: `MainActivity` toisti saman kolmen
 * rivin käsittelyn (`container.signOut()`, `topModel.signOut()`, `popBackStack()`) kuudessa
 * kohdassa, kukin oman hierarkiansa `SessionExpired`ille kirjoitettuna.
 *
 * **Merkitys on nyt tyyppi, ja käsittely on yksi.** Se on kompositioauditoinnin H1 samassa
 * muodossa kuin [ActingSurface]: sääntö joka toistuu kopioina ei ole sääntö vaan tapa.
 *
 * **Merkintä on lupaus uloskirjauksesta, ei kuvaus istunnosta (täsmennetty 4.9.2026).**
 * `SignOutOnExpiry` tyhjentää tunnukset ja sulkee ruudun, joten merkintä kuuluu vain sille
 * virralle jonka ruutu antaa käsittelijälle. Teon oma tila (`ReplyUiState`,
 * `SettingsSaveResult`, `ProfileActionUiState`, `ResignUiState`, `ExportUiState`) jättää
 * käyttäjän paikalleen ja näyttää virheen, koska sulkeminen veisi kirjoitetun tekstin ja
 * tallentamattoman valinnan. Kysymys uudelle tilalle on siis: **viekö tämä kirjautumiseen**,
 * ei: kertooko tämä katkenneesta istunnosta.
 *
 * Kaksi tilaa kantoi merkintää vastoin tätä sääntöä 1.9.–4.9.2026. Kumpikaan ei muuttanut
 * käytöstä, koska ruudut antavat käsittelijälle vain oman tilansa, ja juuri siksi ne olivat
 * H1:n muoto: merkintä oli oikein siihen asti että joku antaisi virran eteenpäin. Vartija on
 * nyt `SessionExpiryTest`, joka kysyy jokaiselta tilalta saman kysymyksen tyhjentävänä
 * `when`inä.
 *
 * **Mitä tämä ei korjaa, ja se on tietoinen raja.** Uusi näkymämalli voi yhä unohtaa sekä
 * merkinnän että käsittelyn kutsun, ja unohdus näkyisi siinä ettei katkennut istunto vie
 * kirjautumiseen kyseisessä ruudussa. Rakenteellinen loppuratkaisu olisi eri kokoinen työ ja
 * eri päätös: istunnon katkeaminen on `DgResponse.AuthFailed`, ja se voisi olla sovelluksen
 * tason tapahtuma jonka `MainActivity` kuuntelee kerran, jolloin ruutuja ei tarvitsisi
 * kytkeä lainkaan. Se on kirjattu `docs/AVOIMET.md`:hen päätettäväksi eikä tehty tässä.
 */
interface SessionExpiredState
