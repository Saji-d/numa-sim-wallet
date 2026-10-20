# Implementation report — Numa 0.2.0

Status: working local-first Android app. No provider integrations (see "Integration boundaries").

## What works
- **Onboarding and profile:** 3-page intro, profile creation with validation, profile editing, sign-out (data kept), delete-all-data.
- **Numbers:** country search, purchase with credits, 30-day term with countdown, rename, renew, release, expiring-soon prompt.
- **Messaging:** per-number conversations, unread state, validated composer (empty / 1,000-char limit), delete with confirmation.
- **Calling:** keypad dialer with caller-ID selection and number validation, call-attempt screen, history, redial, clear history.
- **Travel eSIM:** destination search, plan comparison, purchase with credits, install steps, My eSIMs with install status and validity countdown, remove.
- **Wallet:** starter credits, quick-add and validated custom top-up (1–500), signed ledger of every spend and top-up.
- **Settings:** system/light/dark theme, notification preferences, integration status list, about.
- **Persistence:** all state is stored as versioned JSON in app-private storage (atomic writes); corrupt or unknown-version files start clean.

## Integration boundaries
| Capability | State |
|---|---|
| Voice calling | Not connected: the call screen says so and logs an attempt |
| SMS delivery | Not connected: messages are stored on the device only |
| eSIM provisioning | Not connected: purchases are recorded locally, no profile or QR is issued |
| Payments | Not connected: credits are a local balance |
| Authentication / backend | None: the profile exists on the device only |

## Architecture changes in 0.2.0
- Replaced the in-memory `Store` object with an injectable `AppStore` plus `StateStorage` (file and in-memory implementations) and a versioned `SnapshotCodec`.
- Timestamps are real epoch values (previously the string "now"); relative formatting lives in `Format.kt`.
- Business rules and validation moved out of the screens into the store; screens display the returned error.
- Removed fabricated data (seeded call history, usage figures, simulated "connected" calls and fake QR code).
- Store lists are exposed as immutable copies. `allowBackup` is disabled and data-extraction rules exclude app data.
- Package renamed to `com.sajid.numa`; original launcher icon added.

## Verification (actual results)
| Check | Result |
|---|---|
| `./gradlew :app:assembleDebug` | BUILD SUCCESSFUL |
| `./gradlew :app:testDebugUnitTest` | 63 tests, 0 failures |
| `./gradlew :app:lintDebug` | 0 errors, 8 warnings (4 dependency-version notices, resource-folder naming, `allowBackup` advisory, monochrome icon, Modifier parameter order) |
| Emulator walkthrough (API image `vc_pixel6`, software rendering) | Exercised manually: profile validation and creation, number purchase, messaging, dialer and call attempt, history, eSIM purchase and install, wallet, theme switch, force-stop and relaunch with state restored |
| Automated UI / instrumented tests | None |
| Physical device | Not tested |

Dependency versions are the same set the project built with earlier; upgrading them was deliberately not part of this pass.

## Known limitations
- No automated UI tests; the manual walkthrough is not repeatable in CI.
- Layout was checked at phone width only (no tablet or landscape review); no accessibility audit beyond content descriptions.
- State is one JSON file rewritten on every change, which is fine at this size but not for large histories.
- Catalogue (countries, numbers, plans) is static sample data bundled with the app.

## Next steps (by value)
1. Provider integrations behind `AppStore`: messaging/voice, payments, eSIM provisioning.
2. Room database and ViewModels once data is loaded from a backend.
3. Compose UI tests and an instrumented navigation test.
4. Accessibility, localisation and tablet layouts.
