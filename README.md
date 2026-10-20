# Numa — virtual number & travel eSIM app (UI prototype)

A Kotlin + Jetpack Compose Android prototype that structurally mirrors the screen
layout and main flows of a virtual-number / travel-eSIM app (reference: *Numero eSIM*,
see `docs/SCREEN_INVENTORY.md` for what is confirmed vs assumed).

> **Demo only.** All data is in-memory mock data. No real calls, SMS, payments,
> number provisioning or eSIM activation. Sign-in is a demo form with no authentication.

## Screens
Welcome/onboarding, demo sign-in, Home, eSIM store (country → plans → plan detail → purchase),
My eSIMs, Numbers (get number → pick → purchase, detail, release), Messages + thread,
Calls history, Dialer, In-call, Wallet (credits/transactions), Profile, Settings.

## Build
```
./gradlew :app:assembleDebug
```
APK: `app/build/outputs/apk/debug/app-debug.apk` (JDK 17, Gradle 8.11.1, AGP 8.7.3, compileSdk 35).
