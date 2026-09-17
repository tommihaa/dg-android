# DG Android

An unofficial Android client for [DailyGammon](http://www.dailygammon.com/), the
turn-based backgammon site that has been running since 2001. The site has no public API,
so the app signs in with your own account and reads the same HTML pages a browser would.
It is not affiliated with DailyGammon.

The app is written in Kotlin with Jetpack Compose and has no third-party SDKs: no
analytics, no ads, no crash reporting. It talks to `dailygammon.com` and to nothing else.
Its privacy policy is at <https://tommihaa.github.io/dg-android-privacy/>.

## Why it exists

Two things separate it from playing in a browser or in DG Mobile, and they are the reason
the project was started.

**Your messages are kept on this device.** DailyGammon's own help says it plainly:
*"Messages are not saved on this server."* Every message is shown once and then gone. The
app saves each one before it appears on screen, so the conversation of a match survives.
Moves survive on the site anyway; the app stores exactly the half the site does not.

**A dropped connection does not sign you out.** The session is renewed quietly, a cold
start in flight mode opens signed in, and a board already loaded stays on screen across the
gap.

## What else is different from the site

This list is the same as the *What is different from the DailyGammon site* group in the
app's own Help screen, which is kept in step with the code by a test.

- You can sort the match list your way.
- Your taps stay on this device until you press Submit Move.
- The app can place checkers for you (forced plays and greedy bear-off), but never sends
  them.
- A tournament row names the opponent and the round.
- You can filter, export, back up and import your archive.
- Each DailyGammon account on this device has its own archive.
- Reminders for a game live on this device.
- The board can look different from the browser.
- Join, Sign up, Double, Accept and forum posts ask you to confirm first.
- You can mark a position to look at after the match, and share the match as `.mat` or as
  `.sgf` for GNU Backgammon.
- The board tells you when an action may not have arrived.

## What the app never does

It never sends anything to DailyGammon without a press of yours. It never fetches in the
background: no timer, no notifications. It reads only the pages you open. It keeps what it
keeps on this device only.

## Building

Requirements: JDK 21 and the Android SDK (API 36). Android Studio's bundled JDK works.

```
gradlew :app:assembleDebug
```

`sdk.dir` goes in `local.properties`, written with forward slashes (see
`local.properties.example`). A signed release build needs `keystore.properties`; see
`keystore.properties.example`. Neither file is versioned.

Tests run without a device or network:

```
gradlew test
```

The `core-*` modules are plain JVM Kotlin on purpose: the HTML parsing, which is where the
risk is, runs under JUnit against saved copies of real pages. The `data` module runs Room
under Robolectric.

`gradlew :core-net:liveTest` runs against the real site and needs credentials in
`local.properties`. Note that DailyGammon has no HTTPS at all, so the password travels
unencrypted, in this app exactly as in a browser.

## Layout

| Module | Type | Contents |
|---|---|---|
| `core-domain` | JVM | Models. No dependencies, no knowledge of the network or of HTML. |
| `core-scrape` | JVM | Jsoup parsing and page recognition. |
| `core-net` | JVM | OkHttp, cookie storage, session renewal. |
| `data` | Android | Room: message archive, outgoing action queue, reminders and marks. |
| `app` | Android | Compose UI, sign-in and the match list. |

The documents under `docs/` are the project's working notes and are in Finnish. They hold
what was measured about the site (`KOHDE.md`), how the modules fit together
(`ARKKITEHTUURI.md`), the screens (`UI.md`) and how the Help screen is kept truthful
(`OHJE.md`). Comments in the code and in these documents also point to files that are
not in this repository (`SUBSTANSSI.md`, `AVOIMET.md` and others): those are the private
working notes, and the pointers are left as they are rather than rewritten.

## Status

Not in the Play Store, and there are no release builds here. If you build it yourself, you
are using your own DailyGammon account through an unofficial client, at your own risk.

## License

GPL-3.0. See `LICENSE`.
