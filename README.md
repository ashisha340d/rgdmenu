# VSK Orders

A multi-user Android app for logging and tracking catering/event orders in real time, across four fixed **Stations** (Mangarh, Vrindavan, Barsana, Mussoorie):

- **Board**: a single structured view of Today's orders followed by Upcoming orders. Any assigned member can add/edit/delete an order.
- **History**: past orders, most recent first.
- **Acknowledge**: any member can acknowledge an order; the app tracks who has.
- **Voice notes**: record a short voice note on an order (like a WhatsApp voice message) and play it back. Off by default — see setup step 4.
- **Forward to WhatsApp**: turn an order into a formatted text message and hand it to WhatsApp's share sheet.
- **Admin**: mark an order Done, and set the final per-item quantity (e.g. turning "Coffee" into "Coffee 5 liter — 200 glass").
- **Super Admin**: approves new users and assigns them to one or more Stations, sets their role (User / Admin / Super Admin).

## Onboarding flow

1. Sign up with email + password.
2. Wait for a Super Admin to approve you and assign you to a Station (or Stations).
3. Once approved, set a 4-digit PIN.
4. From then on, opening the app just asks for the PIN (Firebase stays signed in behind the scenes) instead of your email/password every time. "Forgot PIN?" clears it and lets you set a new one.
5. You land on your Station's Board (or a picker, if you're assigned to more than one Station).

## How it's built

- Native Kotlin, no Compose.
- **Firebase Authentication** (email/password).
- **Cloud Firestore** for real-time sync.
- **Firebase Storage** for voice notes.

## ⚠️ One-time setup required: Firebase project

The repo ships with a **placeholder** `app/google-services.json` so CI can compile the app — but it doesn't point at a real backend, so nothing actually works until you swap it for your own project's config. Do this once, from your phone or any browser:

1. Go to the [Firebase console](https://console.firebase.google.com/) and create a new project (free "Spark" plan is enough — voice notes are small, so Storage should stay within the free tier for a while).
2. **Build → Authentication → Get started**, enable the **Email/Password** sign-in provider.
3. **Build → Firestore Database → Create database**, database ID `(default)`, pick any region, and choose **Start in test mode** — that applies working rules for you. (Production mode denies every read and write until you publish rules by hand, which looks exactly like a broken app.)
4. **Skip Storage.** Cloud Storage for Firebase now requires the paid Blaze plan, so voice notes are turned off by default (`Constants.VOICE_NOTES_ENABLED = false`). Everything else runs on the free Spark plan. To enable voice notes later: upgrade to Blaze, create the Storage bucket, publish [`storage.rules`](storage.rules), and flip that flag to `true`.
5. **Project settings → General**, under "Your apps", click the Android icon to register an app with package name **`com.vsk.orders`**. Download the generated `google-services.json`.
6. Replace `app/google-services.json` in this repo with the one you downloaded, then push — CI rebuilds against your real backend.
7. **Firestore Database → Rules** (the tab across the top of the Firestore page, not an item in the left menu): paste the contents of [`firestore.rules`](firestore.rules) and Publish. Test mode is fine to start with, but it expires after 30 days and lets any signed-in user do anything — these rules enforce the actual Super Admin / station permissions.

## Getting set up as Super Admin + demo accounts

`ashisha340d@gmail.com` is hardcoded (in `Constants.kt` and mirrored in `firestore.rules`) as a bootstrap Super Admin — the first time that email signs up in the app, it's auto-approved as Super Admin and skips straight to PIN setup, no one has to approve it.

Once signed in as that Super Admin, open the toolbar menu → **Manage Users** → toolbar → **Seed Demo Data**. This creates the four Station documents and pre-approves `dummy1@gmail.com` / `dummy2@gmail.com` with PIN **1234**, assigned to all four stations, role User.

Those two demo accounts still need to actually **sign up once** in the app (any password they like) to create the real Firebase Auth account — after that first sign-up, since their Firestore profile is already approved with the PIN preset, they go straight to PIN unlock (enter `1234`) and land on the Board.

## Data model (Firestore)

```
users/{email}
  status: pending | approved
  role: user | admin | superadmin
  stations: [stationId...]
  pinHash, createdAt

stations/{stationId}      # mangarh | vrindavan | barsana | mussoorie
  name

stations/{stationId}/orders/{orderId}
  eventType, location, contactName, pax, orderTimeMillis,
  items: [{ name, qty, adminQty }],
  createdBy, createdAt, acknowledgedBy: [email...],
  done, doneBy, voiceNoteUrl
```

Voice note files live in Storage at `stations/{stationId}/orders/{orderId}/voice.m4a`.

## Getting the APK on your phone

Every push triggers `.github/workflows/build-apk.yml`, which builds a debug APK and publishes it as a GitHub Release.

1. Push/merge to this repo.
2. Open the **Releases** tab.
3. On your phone, open the latest release in a browser and download `app-debug.apk`.
4. Allow "install unknown apps" for your browser the first time, then open the downloaded file to install.

## Local build (if you ever have Android Studio / SDK available)

```
./gradlew assembleDebug
```

Output APK: `app/build/outputs/apk/debug/app-debug.apk`

## Known limitations (MVP)

- Roles are global per user (Admin/Super Admin), not scoped per-station — an Admin can mark any order Done on any station they're assigned to.
- Firestore rules restrict `done`/`doneBy` to Admins, but a member could still write arbitrary values to an item's `adminQty` via the raw Firestore API (the app UI itself only allows Admins to edit it). Fine for a small trusted group.
- Station assignment/creation is Super-Admin-only from inside the app (`Manage Users`); there's no in-app UI to rename or add new Stations beyond the four seeded ones — edit `Constants.SEED_STATIONS` and rerun seeding, or add a document directly in the Firestore console.
- PIN reset ("Forgot PIN?") requires still being signed into Firebase on that device — it's a local convenience lock, not real authentication.
