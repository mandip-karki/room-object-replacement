# RoomSwap — Multi-Tenant Room Object Replacement App

Android app (Kotlin + Jetpack Compose) + Supabase backend that lets a product
company onboard client businesses, whose staff/customers photograph a room and
preview a chosen product swapped into it via AI.

Three tiers: **Super Admin** (product company) → **Client Admin** (tenant
business) → **Sub-Account** (end user). See role/permission details and data
model in the original build spec.

## Stack (changed from the original spec, at your request)

The original spec suggested Firebase + fal.ai. This repo instead uses:
- **Supabase** instead of Firebase — Postgres + Auth + Storage + Edge Functions,
  open source, self-hostable, and its free tier doesn't require a credit card
  (Firebase's Cloud Functions require the Blaze plan, which does).
- **Hugging Face** (free Inference Providers tier) instead of fal.ai — one HF
  token, no separate paid account.

**The trade-off to know about:** Hugging Face's free tier has no pipeline that
takes a mask + a reference product photo and composites them pixel-for-pixel
(that's what fal.ai's flux-lora/inpainting gave us). Instead, the `replace`
Edge Function:
1. Captions the tapped region of the room photo (e.g. "a hardwood floor") using
   BLIP image captioning.
2. Captions the chosen replacement product photo (e.g. "gray marble hexagon
   tile") the same way.
3. Sends the *whole* room photo to FLUX.1-Kontext-dev (an instruction-based
   image editor) with a text prompt: "Replace the {tapped label} with
   {product label}, keep everything else unchanged, photorealistic."

This is genuinely free and needs no account beyond a single Hugging Face
token, but the result is guided by a *description* of the product, not an
exact copy of its photo. Test this against real room photos before assuming
it's good enough — if the quality isn't there, the fallback is paying for
fal.ai (or another reference-image-conditioned inpainting API) for this one
step only, everything else in this stack stays the same.

## Status

**Phase 1 (Foundation) — live.** The Supabase project is created, schema is
pushed, both Edge Functions are deployed, and the Android app is wired to the
real project. One manual step remains: a Hugging Face token (see below) —
without it, `replace` will insert the job row and then fail the AI step with
a clear "HF_TOKEN is not configured" error.

- **Supabase project**: `RoomSwap`, ref `fvmlxyzpemyfplzlxncl`, org `RoomSwap`
  (`trjztrcditsjypbnvbvh`), region `us-east-1` —
  https://supabase.com/dashboard/project/fvmlxyzpemyfplzlxncl. Created via the
  Supabase CLI using a personal access token you provided; the CLI session
  used to set this up is not persisted anywhere beyond this local machine's
  `supabase` CLI config. The database password was generated locally and is
  not recorded in this repo or README — if you need it later (e.g. for a
  direct Postgres connection), reset it from the dashboard's Database
  settings rather than trying to recover it.
- `app/` — Android project skeleton (Gradle + Compose + Navigation), login
  screen, role-based routing, Postgrest/Storage/Functions repositories via
  supabase-kt, and stub screens for all three roles. **Compiles and packages
  cleanly** (`gradlew assembleDebug` produces
  `app/build/outputs/apk/debug/app-debug.apk`) — verified in this environment,
  now with the real project's URL/anon key in `local.properties` (gitignored).
- `supabase/functions/` — **deployed**: `replace` (segmentation-free
  caption+edit pipeline above) and `create-sub-account` (sets up a new
  Sub-Account under the calling Client Admin's own company). Also
  **type-checks cleanly** (`deno check`) — verified in this environment.
- `supabase/migrations/0001_init.sql` — **applied** to the live database.
  Postgres schema + Row Level Security policies enforcing tenant isolation
  (every policy checks the caller's own `company_id` via a `security
  definer` helper function reading `profiles`, never a client-supplied
  value) plus Storage bucket policies.
- `SUPABASE_URL`, `SUPABASE_ANON_KEY`, and `SUPABASE_SERVICE_ROLE_KEY` are
  **auto-injected into every Edge Function by the Supabase platform** —
  the `SUPABASE_` prefix is reserved and can't be set manually
  (`supabase secrets set` rejects it), which is why `supabaseAdmin.ts` just
  reads `Deno.env.get(...)` for these without any setup step.

### Local toolchain (already installed on this machine)

To verify the app actually compiles, a command-line Android toolchain and
Android Studio were installed here:
- Eclipse Temurin JDK 17 → `C:\Program Files\Eclipse Adoptium\jdk-17.0.20.8-hotspot`
- Android Studio → `C:\Program Files\Android\Android Studio`
- Android SDK (platform-tools, platforms 34/36, build-tools 34–36) → `C:\Android\Sdk`
- Deno (for type-checking the Edge Functions) → installed via winget
- The project has its own Gradle wrapper (`gradlew`/`gradlew.bat`, committed to git) pinned to Gradle 8.11.1, so building doesn't need anything installed globally beyond a JDK.
- Firebase CLI was installed via npm earlier in this project's history but is no longer needed — Supabase CLI (`npm install -g supabase`) is what you'll want instead; it wasn't installed here since it needs an interactive `supabase login` you must run yourself.

### 1. Local tooling — done
Supabase CLI installed (`npm install -g supabase`) and authenticated with the
access token you provided. If you want to run `supabase` commands yourself
later from a different machine, install the CLI there and run
`supabase login` (opens a browser under your own account).

### 2. Supabase project — done
Project created, schema pushed, both Edge Functions deployed, `local.properties`
filled in with the real URL/anon key. See the Status section above for the
project ref and dashboard link. One thing left here:
- [ ] **Create your first Super Admin.** Sign up a user (via the dashboard's
      Auth panel, or `supabase.auth.signUpWith(Email)` from the app once you
      run it), then in the SQL editor:
      ```sql
      insert into companies (name, type, created_by) values ('Your Company', 'main', '<the new user''s uid>');
      insert into profiles (id, company_id, role, email) values ('<uid>', '<the company id you just inserted>', 'super_admin', '<their email>');
      ```
      Everything after that (Client Admins, Sub-Accounts) is created through
      the app / `create-sub-account` function.

### 3. Hugging Face — needs your token
- [ ] Create a free account at https://huggingface.co
- [ ] Generate a **fine-grained token** with "Make calls to Inference Providers" permission: https://huggingface.co/settings/tokens/new?ownUserPermissions=inference.serverless.write&tokenType=fineGrained
- [ ] Give me the token value and I'll run `supabase secrets set HF_TOKEN=hf_...` — or run it yourself if you'd rather not paste it here
- [ ] Test the pipeline against a few real room photos before trusting it — model IDs in `supabase/functions/_shared/hf.ts` (`Salesforce/blip-image-captioning-large`, `black-forest-labs/FLUX.1-Kontext-dev`) were current as of this writing, but Hugging Face's provider routing shifts over time, so re-check https://huggingface.co/docs/api-inference/tasks/image-to-image if calls start failing.

### 4. Local secrets — done
`local.properties` has the real `sdk.dir`, `supabase.url`, and `supabase.anonKey`.

None of the above files with real secrets (`local.properties`, Supabase function secrets) are tracked by git — see `.gitignore`.

## Running locally

Backend is already linked, pushed, and deployed (see Status above). If you
change `supabase/migrations/` or `supabase/functions/` later, re-run:

```bash
supabase db push
supabase functions deploy replace
supabase functions deploy create-sub-account
```

For the app:

```bash
.\gradlew.bat assembleDebug
# APK lands at app\build\outputs\apk\debug\app-debug.apk

# Or open the repo root in Android Studio, select a device/emulator, Run
```

## Project layout

```
app/src/main/java/com/roomswap/app/
  SupabaseClientProvider.kt  single SupabaseClient (Auth, Postgrest, Storage, Functions)
  auth/                      login screen, AuthViewModel, AuthRepository
  data/model/                Company, User, Product, RoomPhoto, ReplacementJob (kotlinx.serialization)
  data/repository/           Postgrest/Storage/Functions-backed repositories
  navigation/                Routes + role-based NavHost
  ui/superadmin/             company list, product catalog manager
  ui/clientadmin/            sub-account manager
  ui/subaccount/             room photo capture, product picker, result screen

supabase/
  migrations/0001_init.sql  schema, RLS policies, storage buckets/policies
  functions/replace/        caption tapped region + product, edit room photo (FLUX.1-Kontext-dev)
  functions/create-sub-account/  Client Admin creates a Sub-Account under their own company
  functions/_shared/hf.ts   Hugging Face captioning + image-editing client
```

## What's next (Phase 2+)

Per the original build order: admin flows (product catalog upload UI, real
sub-account management UI), the actual camera/photo-picker and tap-to-select
UI (including cropping a small region around the tap for captioning — see the
TODO in `RoomPhotoScreen.kt`), product picker wiring, result screen polish,
job history, and a Super Admin usage dashboard. The screens for these exist
as stubs with `TODO` comments marking exactly what's missing.
