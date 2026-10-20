<div align="center">

# Numa

**A virtual-number & travel-eSIM app, prototyped natively in Kotlin + Jetpack Compose.**

![Platform](https://img.shields.io/badge/platform-Android%208.0%2B-3DDC84?logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-2.1.0-7F52FF?logo=kotlin&logoColor=white)
![Compose](https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285F4?logo=jetpackcompose&logoColor=white)
![Status](https://img.shields.io/badge/status-UI%20prototype-orange)
![Data](https://img.shields.io/badge/data-100%25%20mock-lightgrey)

</div>

> **⚠️ Demo only.** Every number, message, call, eSIM, balance and "payment" in this app is simulated
> with in-memory mock data. **No real calls, SMS, payments, number provisioning or eSIM activation
> take place**, and the sign-in form performs no authentication. Connecting any of that requires
> legitimate telecom/payment provider integrations that this project does not have.

---

## What is this?

Numa is a first-version Android prototype that reproduces the **screen structure, navigation and
major user flows** of a "second phone number + travel eSIM" app in the style of *Numero eSIM*. It is
meant for demonstration and as a starting point for a real product: the UI, navigation and state
flow are in place; the backend is not.

> **How closely does it match the real app?** Only partly verified. Public store and review text
> confirmed the *feature set* (numbers in 80+ countries, SMS inbox, VoIP dialer, travel eSIMs,
> credits/wallet, registration by phone number). The real app's tab bar, screen hierarchy and
> visual layout could **not** be confirmed (no screenshots or installed copy were inspected), so
> layouts here are informed guesses. See [`docs/SCREEN_INVENTORY.md`](docs/SCREEN_INVENTORY.md) for
> what is *confirmed* vs *assumed*. No proprietary assets are used.

## Features

| Area | What you can do (all simulated) |
|---|---|
| **Onboarding** | 3-page intro, demo sign-in with name/email validation |
| **Home** | Credit balance, quick actions, owned numbers, latest eSIM, unread-message shortcut |
| **Virtual numbers** | Search 8 countries, pick a number, buy with credits, view details, release a number |
| **SMS** | Inbox with unread indicators, chat-style threads, reply |
| **Calling** | Keypad dialer with caller-ID selector, in-call screen with timer, call history (incoming / outgoing / missed) |
| **Travel eSIM** | Browse destinations, compare plans (GB / days / price), buy, demo install steps, "My eSIMs" with usage |
| **Wallet** | Credit balance, demo top-up packs, transaction history |
| **Account** | Profile, settings toggles, sign out (resets all state) |

Insufficient-credit, empty, and error states are handled and route the user to the Wallet.

## Screens (19 routes)

`Welcome` · `SignIn` · `Home` · `Esim` · `EsimCountry` · `EsimPlanDetail` · `MyEsims` · `Numbers` ·
`GetNumber` · `PickNumber` · `NumberDetail` · `Messages` · `Thread` · `Calls` · `Dialer` · `InCall` ·
`Wallet` · `Profile` · `Settings`

```
Welcome ─▶ SignIn ─▶ ┌────────────── bottom tabs ──────────────┐
                     │ Home │ eSIM │ Numbers │ Calls │ Profile │
                     └──┬──────┬───────┬─────────┬───────┬─────┘
                        │      │       │         │       └▶ Settings · Wallet · Messages · MyEsims
                        │      │       │         └▶ Dialer ─▶ InCall
                        │      │       └▶ GetNumber ─▶ PickNumber ─▶ (buy) ─▶ NumberDetail ─▶ Thread
                        │      └▶ EsimCountry ─▶ EsimPlanDetail ─▶ (buy) ─▶ MyEsims
                        └▶ Wallet · Messages · Dialer · GetNumber · MyEsims
```

## Architecture

Single-module, single-activity app. No DI framework, no navigation library — deliberately small.

```
app/src/main/java/com/cloneapp/numa/
├── MainActivity.kt     Screen routes (sealed interface), Nav back stack, bottom bar, router
├── Data.kt             Domain models, mock catalogue, in-memory Store (all business rules)
├── Theme.kt            Material 3 theme + shared components (ScreenFrame, AppCard, PrimaryButton, …)
├── ScreensHome.kt      Welcome, SignIn, Home, Profile, Settings
├── ScreensNumbers.kt   Numbers, Messages, GetNumber, PickNumber, NumberDetail, Thread
├── ScreensCalls.kt     Calls, Dialer, InCall
└── ScreensEsim.kt      Esim, EsimCountry, EsimPlanDetail, MyEsims, Wallet
```

**Layers**

- **UI** — stateless-ish `@Composable` screens that take a `Nav` and read from `Store`.
- **Domain/state** — `Store` is a Kotlin `object` holding Compose snapshot state
  (`mutableStateOf` / `mutableStateListOf`); screens recompose automatically. Business rules
  (credit checks, purchases, releases) live here, returning `null` on success or an error string.
- **Navigation** — `Screen` is a sealed interface; `Nav` wraps a `SnapshotStateList<Screen>` back
  stack. Tabs reset the stack; other screens are pushed. System back is handled with `BackHandler`.
  A guard sends signed-out users back to `Welcome`.
- **Data access** — none. There is no network, database or persistence; state resets when the
  process ends.

**Design choices:** one shared theme and component set for visual consistency, a `DEMO` badge on
every simulated result, core Material icons only (no extra dependencies), and a hand-rolled back
stack to keep the dependency list minimal.

## Tech stack

| | |
|---|---|
| Language | Kotlin 2.1.0 |
| UI | Jetpack Compose (BOM 2024.09.00), Material 3 |
| Build | Gradle 8.11.1, Android Gradle Plugin 8.7.3 |
| Java | JDK 17 |
| SDK | `minSdk 26` · `targetSdk 35` · `compileSdk 35` |
| App ID | `com.cloneapp.numa` |

## Build & run

Requirements: JDK 17 and an Android SDK with platform 35 (point `sdk.dir` in `local.properties` at it).

```bash
# Windows: gradlew.bat   |  macOS/Linux: ./gradlew
./gradlew :app:assembleDebug
```

The APK is written to `app/build/outputs/apk/debug/app-debug.apk`.

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.cloneapp.numa/.MainActivity
```

Or open the folder in Android Studio and press **Run**.

## Quality & testing status

| Check | Result |
|---|---|
| `assembleDebug` | ✅ builds successfully |
| Install + launch on an Android emulator | ✅ installs, launches, no crash, onboarding renders |
| Full click-through of every flow | ❌ not yet done (emulator was too slow) |
| Unit / UI tests, lint, accessibility audit | ❌ none yet |
| Real device | ❌ not tested |

## Known limitations

- Everything is mock data; nothing persists across app restarts.
- Intro "Skip" jumps to the last intro page rather than to sign-in.
- Status-bar icons are low-contrast on the welcome screen (no edge-to-edge handling).
- No custom app icon or brand assets; default launcher icon.
- Layouts are informed by public information, not a verified copy of the reference app.

## Roadmap

1. Full click-through QA on a real device; fix findings.
2. Compare against real reference screenshots and adjust navigation/layout.
3. App icon, branding, edge-to-edge, accessibility, dark-mode review.
4. Persistence (Room / DataStore), ViewModels, unit and UI tests.
5. Real backends — auth, payments, number/eSIM providers (requires provider agreements).

## Project docs

- [`docs/SCREEN_INVENTORY.md`](docs/SCREEN_INVENTORY.md) — reference research, confirmed vs assumed
- [`docs/IMPLEMENTATION_REPORT.md`](docs/IMPLEMENTATION_REPORT.md) — what works, what's mocked, build/test results

## Disclaimer

Numa is an independent learning/prototyping project. It is not affiliated with, endorsed by, or
derived from the code or assets of Numero eSIM or its developer. "Numero" is referenced only to
describe the category of app being studied.
