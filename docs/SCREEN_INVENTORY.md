# Numero eSIM - Screen Inventory (research notes)

Date: 2026-10-20. Method: 2 web searches + 2 page fetches. No app was installed or run.
Tags: CONFIRMED = stated by a cited source. ASSUMED = inferred from typical apps or from the feature list; not seen on a screen.

## Vendor identification
- CONFIRMED: App is "Numero eSIM" (store title varies: "2nd Phone Number", "USA Number & More", "Data & 2nd Number"). App Store id 1249825694.
  Source: https://apps.apple.com/us/app/numero-esim-2nd-phone-number/id1249825694
- CONFIRMED: Developer is Smarteletec S.L. (Smarteletec Sociedad Limitada). Source: same page; also AlternativeTo and APKMirror ("smarteletec-s-l").
- CONFIRMED: Not Aloha Mobile. The "Numero/Aloha" guess is wrong as far as the sources show. Support contacts listed are support@numeroesim.com and support@numero.global.
- CONFIRMED: App Store rating is about 4.0 from roughly 4.1K ratings (search snippet only). Latest version seen: 6.2.25.
- NOT VERIFIED: Google Play listing (not found in search). The Android package is likely com.esim.numero (from a BlueStacks URL). Treat this as unconfirmed.

## Main navigation structure
- NOT VERIFIED: No source describes the tab bar or the screen hierarchy. Every screen below is inferred from the feature list.
- ASSUMED: Bottom tabs. Suggested set: Numbers, Messages, Calls/Dialer, eSIM (Data), Account/Wallet.
- CONFIRMED (named in store copy): a "Free Coins Center" and an in-app "Support Center" exist.
  Source: App Store page above.
- CONFIRMED: Separate message section tied to the selected number, and an in-app dialer.
  Source: https://blog.yesim.tech/?p=30540

## Onboarding / auth
- CONFIRMED: Registration asks for an existing mobile number, then a verification code, then basic info. It does not activate an eSIM by itself.
  Source: https://blog.yesim.tech/?p=30540
- ASSUMED: Splash/intro carousel, country-code picker and OTP entry screen, profile-info form, permission prompts (notifications, contacts, microphone).
- NOT VERIFIED: Social login, email login, guest mode, PIN/biometrics.

## Virtual number purchase
- CONFIRMED: Numbers in 80+ countries and 4000+ cities (store copy). US and UK are the most common. Availability varies by country. Source: yesim review; App Store search snippet.
- CONFIRMED: Two number modes. Receive-only (SMS and calls), or send and receive plus calling (needs credit).
  Source: App Store page.
- CONFIRMED: A dedicated WhatsApp-compatible numbers section. Only numbers in it support WhatsApp. Source: App Store page.
- CONFIRMED: Toll-free numbers, up to 5 lines. Terms are 1, 3 or 12 months, with auto-renewal on by default.
  Source: App Store page.
- CONFIRMED: Free tier. Collecting coins in the Free Coins Center earns a free US number for one month, renewable. These numbers do not support WhatsApp.
- CONFIRMED price points (US App Store IAP): USA Monthly $3.49, USA Annual $18.99. Source: App Store page.
- ASSUMED screens: country list, then city/area-code list, number list with search, number type/plan picker, checkout (IAP / Stripe / Numero Money), "My Numbers" list, number detail with renew/cancel.
- CONFIRMED (user review): activation can take longer for some numbers. Source: App Store page.

## SMS / messaging
- CONFIRMED: Inbox per number. Verification codes arrive by SMS or call, and success depends on country and number type. Source: yesim review.
- CONFIRMED: Outgoing SMS requires credit. Source: App Store page.
- ASSUMED screens: conversation list, thread view, compose, per-number filter, notification settings.

## Calling
- CONFIRMED: VoIP dialer over internet. Local calling plans need a local virtual number. EU plans with UK numbers. Limited-time offers.
  Source: App Store page; yesim review.
- CONFIRMED: Call forwarding to a second number, another number, or voicemail. Hide-second-number and personalised voicemail are listed in a search snippet only.
- ASSUMED screens: dialer keypad, in-call screen, call history, voicemail list, forwarding settings, caller-ID selector.
- NOT VERIFIED: Call recording, contacts integration, rate table screen.

## eSIM plans
- CONFIRMED: Travel eSIMs are local, regional, global (120+ countries) and unlimited. Standard data plans cover 190+ countries (sources disagree: 150+ in an older note). Activation is by QR code or manual details.
  Source: App Store page; yesim review.
- CONFIRMED: Data eSIM is data only. Full eSIM adds a phone number, SMS and calling minutes.
- CONFIRMED: Activation happens in the phone's system settings (add eSIM, scan QR or enter details manually, set as data line). Source: yesim review.
- ASSUMED screens: destination search, country/region/global tabs, plan list (GB/days/price), plan detail, checkout, "My eSIMs" with QR/manual install guide and usage/top-up, install-help walkthrough.
- NOT VERIFIED: Exact usage-tracking UI, top-up flow, device compatibility check.

## Wallet / credits / payments
- CONFIRMED: Credit exists. Recharge IAPs: 1 EUR $0.99, 2 EUR $2.99, 5 EUR $6.99. Packages and a Local Minutes plan are also listed (see the App Store page for prices).
- CONFIRMED: "Numero Money" is earned via referrals (referred friend gets EUR 3 off an eSIM). Payment options named: App Store, Stripe/Numero Money, website.
- CONFIRMED: Free Coins Center (coins earn a free US number).
- NOT VERIFIED: How coins are earned (ads, tasks, daily check-in). It is typical, so treat as ASSUMED.
- ASSUMED screens: balance and top-up, transaction history, referral/invite screen, promo code entry, payment method selector.

## Settings / account / support
- CONFIRMED: Support Center in-app, 24/7 support claimed, subscription cancellation depends on payment channel.
- ASSUMED: Profile, language, notification toggles, linked payment/subscriptions, privacy/terms, delete account, logout, app version.
- NOT VERIFIED: Any actual settings screen layout (no source describes it).

## Reviews and caveats
- Reported pros: fast setup, works for WhatsApp verification, clean UI. Cons: pricier than data-only eSIMs, verification can fail by country, support can be slow, spam calls/texts reported once (2023).
- Several App Store reviews are about other apps (mismatched); ignore.

## What could not be verified
- No screenshots, screen recordings or official help-centre pages were retrieved. The vendor website and help centre were not fetched.
- The Google Play listing was not located.
- Navigation layout, visual design, exact screen names and the post-purchase flows are all unverified.
- Prices and country counts come from store copy and vary by source and region.
- Next step: install the app (or view App Store screenshots) to confirm the tab structure before building screens.

## Sources
- https://apps.apple.com/us/app/numero-esim-2nd-phone-number/id1249825694
- https://blog.yesim.tech/?p=30540
- https://alternativeto.net/software/numero-esim-second-phone-number-and-virtual-sim/about (search snippet only)
- https://apkmirror.com/apk/smarteletec-s-l/numero-esim-virtual-number (search snippet only)
