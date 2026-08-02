# VSK Orders

A multi-user Android app for logging and tracking catering/event orders in real time — a Dashboard of today's orders by time, a Board where anyone can add/edit/delete orders, per-user Acknowledge, and Admin-only "mark served" + final item quantities.

## How it's built

- Native Kotlin, no Compose — Activities + two Fragments (Dashboard, Board) under a bottom nav.
- **Firebase Authentication** (email/password) for sign-in.
- **Cloud Firestore** for real-time sync — every device sees adds/edits/acknowledgements instantly.
- Multiple **Groups**: each group is its own order board with a short join code (e.g. `VSK4X9`), so different teams/venues stay separate. The person who creates a group is its first Admin.

## ⚠️ One-time setup required: Firebase project

The repo currently ships with a **placeholder** `app/google-services.json` so CI can compile the app — but it doesn't point at a real backend, so sign-in and orders won't actually work until you swap it for your own project's config. Do this once, from your phone or any browser:

1. Go to the [Firebase console](https://console.firebase.google.com/) and create a new project (free "Spark" plan is enough).
2. In the project, click **Build → Authentication → Get started**, enable the **Email/Password** sign-in provider.
3. Click **Build → Firestore Database → Create database**, start in production mode, pick any region.
4. In **Project settings → General**, under "Your apps", click the Android icon to register an app with package name **`com.vsk.orders`**. Download the generated `google-services.json`.
5. Replace `app/google-services.json` in this repo with the one you downloaded (send it to me, or commit it yourself), then push — CI will rebuild against your real backend.
6. In **Firestore Database → Rules**, paste the contents of [`firestore.rules`](firestore.rules) from this repo and click Publish. This is what enforces "members only see their group's orders" and "only Admins can mark served."

Everything else (sign-up, creating/joining groups, adding orders) works entirely from inside the app once those steps are done — no further console work needed.

## Data model (Firestore)

```
groups/{groupCode}
  name, members: [email...], admins: [email...], createdBy, createdAt

groups/{groupCode}/orders/{orderId}
  eventType, location, contactName, pax, orderTimeMillis,
  items: [{ name, qty, adminQty }],
  createdBy, createdAt, acknowledgedBy: [email...], served, servedBy
```

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

- Admin roles are per-group, stored as an email list on the group document — no admin UI to add/remove admins yet (edit the `admins` array directly in Firestore console to promote someone).
- Firestore rules restrict `served`/`servedBy` to Admins, but a technically inclined member could still write arbitrary values to an item's `adminQty` via the raw Firestore API (the app UI itself only allows Admins to edit it). Fine for a small trusted group; tighten later if needed.
