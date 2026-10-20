# Screens and navigation

Numa is a single-activity app. Five bottom tabs are navigation roots (switching a tab resets the back
stack); every other screen is pushed on top and closed with the back arrow or system back.

| Route | Purpose | Reached from |
|---|---|---|
| `Welcome` | Three-page introduction | App start (signed out) |
| `SignIn` | Create the local profile (name, email) | Welcome |
| `Home` (tab) | Balance, quick actions, expiring numbers, unread messages, recent activity | After sign-in |
| `Esim` (tab) | Search destinations, open My eSIMs | Tab bar |
| `EsimCountry` | Plans for one destination | Esim |
| `EsimPlanDetail` | Plan details, purchase with credits, install steps | EsimCountry |
| `MyEsims` | Purchased plans, install status, expiry, remove | Home, Profile, Esim, plan purchase |
| `Numbers` (tab) | Owned numbers with remaining term | Tab bar |
| `GetNumber` | Search countries | Numbers, Home |
| `PickNumber` | Choose a number, purchase with credits | GetNumber |
| `NumberDetail` | Rename, renew, release, jump to messages or dialer | Numbers, Home |
| `Messages` | Conversation list | Numbers, Home, Profile |
| `Thread` | Message history and composer | Messages, NumberDetail |
| `Calls` (tab) | Call history, clear history | Tab bar |
| `Dialer` | Keypad with caller-ID selection | Calls, Home, NumberDetail |
| `InCall` | Call attempt screen | Dialer, Calls |
| `Wallet` | Balance, add credits, transactions | Home, Profile, purchase errors |
| `Profile` (tab) | Edit profile, shortcuts | Tab bar |
| `Settings` | Theme, notifications, integration status, sign out, delete data | Profile |

## Main flows

1. **Onboarding** — Welcome → create profile → Home. The session and data survive app restarts.
2. **Get a number** — Numbers → Get number → country → pick number → purchase (credits) → Numbers. A
   welcome conversation is created; the number is valid for 30 days and can be renewed or released.
3. **Messaging** — Numbers/Home → Messages → conversation → compose. Messages are validated, stored locally and
   shown with timestamps; they are not delivered over a carrier.
4. **Travel eSIM** — eSIM → destination → plan → purchase (credits) → My eSIMs → mark as installed (starts the
   validity period) → expiry countdown.
5. **Wallet** — Home/Profile → Wallet → add credits (quick amounts or custom, 1–500) → transaction ledger.
6. **Dialling** — Calls/Home → Dialer → choose caller ID → call attempt screen → history entry.
