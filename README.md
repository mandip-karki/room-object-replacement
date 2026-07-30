# RoomSwap — Multi-Tenant Room Object Replacement App

Android app (Kotlin + Jetpack Compose) + Firebase backend that lets a product
company onboard client businesses, whose staff/customers photograph a room and
preview a chosen product swapped into it via AI segmentation + inpainting.

Three tiers: **Super Admin** (product company) → **Client Admin** (tenant
business) → **Sub-Account** (end user). See role/permission details and data
model in the original build spec.

## Status

**Phase 1 (Foundation) — scaffolded, not yet buildable end-to-end.**

This repo currently contains:
- `app/` — Android project skeleton (Gradle + Compose + Navigation), login
  screen, role-based routing, Firestore/Storage/Functions repositories, and
  stub screens for all three roles.
- `functions/` — Firebase Cloud Functions (TypeScript) implementing the
  `replace` / `getReplacementJob` API contract, Firestore-backed job queue,
  and a `createSubAccount` callable that sets Auth custom claims. **Compiles
  cleanly** (`npm run build` in `functions/`) — verified in this environment.
- `firestore.rules`, `storage.rules`, `firebase.json` — tenant isolation
  enforced server-side (every rule checks `request.auth.token.companyId`,
  never a client-supplied value).

**Not done yet, and blocked on manual setup below:** the Android app has not
been compiled (no JDK/Android SDK available in this environment), no Firebase
project exists yet, and no fal.ai account/key exists yet. Nothing has real
credentials wired in.

## Manual setup checklist (you need to do this — I can't create accounts or run GUI installers for you)

### 1. Local tooling
- [ ] Install **Android Studio** (Kotlin + Gradle + Android SDK come bundled): https://developer.android.com/studio
- [ ] Open this folder (`Image Replacement`) in Android Studio and let it sync Gradle — this will download the Android SDK components declared in `app/build.gradle.kts` (compileSdk 34, minSdk 26).
- [ ] Install **Node.js 20** if you don't already have it (this environment has Node 24, which works for `npm install`/`tsc` but Cloud Functions deploys expect Node 20 — see `functions/package.json` `engines`).
- [ ] Install the **Firebase CLI**: `npm install -g firebase-tools`, then `firebase login`.

### 2. Firebase project
- [ ] Create a project at https://console.firebase.google.com
- [ ] Enable **Authentication** → Email/Password sign-in method
- [ ] Enable **Firestore** (production mode)
- [ ] Enable **Storage**
- [ ] Enable **Cloud Functions** (requires the Blaze pay-as-you-go plan — Spark's free tier doesn't allow outbound network calls, which the `replace` function needs to reach fal.ai)
- [ ] Register an Android app in the Firebase console with applicationId `com.roomswap.app`, download `google-services.json`, and place it at `app/google-services.json` (already gitignored — never commit it)
- [ ] Copy `.firebaserc.example` → `.firebaserc` and fill in your project ID
- [ ] Set custom claims (`role`, `companyId`) on your first Super Admin user manually (e.g. via a one-off Admin SDK script or the Firebase console's Cloud Shell) — everything after that (Client Admins, Sub-Accounts) is created through the app/`createSubAccount` function, which sets claims for you.

### 3. fal.ai
- [ ] Create an account at https://fal.ai and generate an API key
- [ ] Set it as a Cloud Functions secret (don't put it in source): `firebase functions:secrets:set FAL_API_KEY`
- [ ] **Verify the model endpoint slugs in `functions/src/fal.ts`** (`fal-ai/sam2/image` for point-prompted segmentation, `fal-ai/flux-lora/inpainting` for compositing) against fal.ai's current model catalog before relying on them — pick these based on the spec's suggestion (SAM for masking, FLUX/SD for inpainting) but I could not verify the exact live endpoint IDs from this environment.

### 4. Local secrets
- [ ] Copy `local.properties.example` → `local.properties` and point `sdk.dir` at your Android SDK install (Android Studio usually writes this for you automatically on first sync)
- [ ] Copy `functions/.env.example` → `functions/.env` for local emulator use only (production uses the Functions secret above, not `.env`)

None of the above files with real secrets (`google-services.json`, `.firebaserc`, `local.properties`, `functions/.env`) are tracked by git — see `.gitignore`.

## Running locally once the above is done

```bash
# Backend: Firebase emulators (Auth, Firestore, Storage, Functions)
cd functions
npm install
npm run serve

# App: open the repo root in Android Studio, select a device/emulator, Run
```

## Project layout

```
app/src/main/java/com/roomswap/app/
  auth/            login screen, AuthViewModel, AuthRepository (Firebase Auth)
  data/model/      Company, User, Product, RoomPhoto, ReplacementJob
  data/repository/ Firestore/Functions-backed repositories
  navigation/      Routes + role-based NavHost
  ui/superadmin/    company list, product catalog manager
  ui/clientadmin/   sub-account manager
  ui/subaccount/    room photo capture, product picker, result screen

functions/src/
  index.ts         exports
  replace.ts       replace / getReplacementJob callables (the /replace API contract)
  fal.ts           fal.ai segmentation + inpainting client
  users.ts         createSubAccount (sets Auth custom claims server-side)
  types.ts         shared types
```

## What's next (Phase 2+)

Per the original build order: admin flows (product catalog upload UI, real
sub-account management UI), the actual camera/photo-picker and tap-to-select
UI, product picker wiring, result screen polish, job history, and a Super
Admin usage dashboard. The screens for these exist as stubs with `TODO`
comments marking exactly what's missing.
