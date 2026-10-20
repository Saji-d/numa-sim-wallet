# Implementation Report — Numa (first version)

Status: **installable demo prototype with mock data**. Not production-ready; no real telecom functionality.

## Screens implemented (19 routes)
Welcome (3-page intro), Sign-in (demo), Home, eSIM store, eSIM country plans, eSIM plan detail,
My eSIMs, Numbers, Get number (country list), Pick number, Number detail, Messages, Thread,
Calls, Dialer, In-call, Wallet, Profile, Settings.

## Flows
- Welcome → demo sign-in → Home (bottom tabs: Home, eSIM, Numbers, Calls, Profile).
- Get number → country → pick number → purchase with credits (insufficient-credit error → Wallet) → Numbers.
- Number detail → release (confirm dialog); welcome SMS thread created on purchase; reply in thread.
- eSIM: country → plan → buy with credits → demo QR/install steps → My eSIMs.
- Dialer (caller-ID from owned numbers) → in-call (timer) → call logged in history.
- Wallet: demo top-up, transaction list. Settings: local toggles, sign out (resets state).

## Mocked (everything)
Authentication, credits/payments, number catalogue and purchase, SMS, VoIP calls, eSIM plans/QR/activation,
call history seed data. State is in memory only and resets when the app process dies.

## Evidence-based accuracy
`docs/SCREEN_INVENTORY.md`: only feature lists and the registration flow were confirmed from store/review
text. The tab bar, screen hierarchy and layouts of the real app were **not** verified (no app screenshots
or installed reference app were inspected). The layouts here are therefore informed guesses, not a verified clone.

## Build / test results (actual)
- `./gradlew :app:assembleDebug` — BUILD SUCCESSFUL (first build, 2m47s). Debug APK ~9.6 MB.
- Installed on the existing `vc_pixel6` emulator (API image, software rendering): install Success,
  app launched, process stayed alive, no AndroidRuntime errors in logcat, welcome/intro screens rendered.
- NOT tested: the remaining screens/flows on a device (emulator System UI hung under software rendering),
  no unit/UI tests exist, no lint run, no physical device.

## Known limitations
- Only core Material icons (no extended set); no custom app icon or brand assets.
- Welcome "Skip" jumps to the last intro page, not to sign-in.
- Status-bar icons are light-on-light on the welcome screen (no edge-to-edge handling).
- No persistence, no accessibility audit, no tablet layouts, no dark-mode review.

## Remaining work (priority)
1. Click-through QA of all flows on a real device/accelerated emulator; fix findings.
2. Verify the reference app's real layout from screenshots; adjust navigation/screens.
3. App icon, branding, edge-to-edge, accessibility.
4. Persistence (Room/DataStore), unit + UI tests.
5. Real backends (auth, payments, number/eSIM providers) — requires legitimate provider agreements.
