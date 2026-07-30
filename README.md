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

**Phase 1 (Foundation) — scaffolded and building. Not yet wired to real Supabase/Hugging Face credentials.**

This repo currently contains:
- `app/` — Android project skeleton (Gradle + Compose + Navigation), login
  screen, role-based routing, Postgrest/Storage/Functions repositories via
  supabase-kt, and stub screens for all three roles. **Compiles and packages
  cleanly** (`gradlew assembleDebug` produces
  `app/build/outputs/apk/debug/app-debug.apk`) — verified in this environment
  with placeholder Supabase URL/key values in `local.properties`.
- `supabase/functions/` — Supabase Edge Functions (Deno/TypeScript)
  implementing the `replace` API (segmentation-free caption+edit pipeline
  above) and `create-sub-account` (sets up a new Sub-Account under the
  calling Client Admin's own company). **Type-checks cleanly** (`deno check`)
  — verified in this environment.
- `supabase/migrations/0001_init.sql` — Postgres schema + Row Level Security
  policies enforcing tenant isolation (every policy checks the caller's own
  `company_id` via a `security definer` helper function reading `profiles`,
  never a client-supplied value) plus Storage bucket policies.

**Not done yet, and blocked on manual setup below:** no Supabase project
exists yet, and no Hugging Face token exists yet. Nothing has real
credentials wired in, so the app builds but can't actually sign in or run a
replacement job until you complete the checklist.

### Local toolchain (already installed on this machine)

To verify the app actually compiles, a command-line Android toolchain and
Android Studio were installed here:
- Eclipse Temurin JDK 17 → `C:\Program Files\Eclipse Adoptium\jdk-17.0.20.8-hotspot`
- Android Studio → `C:\Program Files\Android\Android Studio`
- Android SDK (platform-tools, platforms 34/36, build-tools 34–36) → `C:\Android\Sdk`
- Deno (for type-checking the Edge Functions) → installed via winget
- The project has its own Gradle wrapper (`gradlew`/`gradlew.bat`, committed to git) pinned to Gradle 8.11.1, so building doesn't need anything installed globally beyond a JDK.
- Firebase CLI was installed via npm earlier in this project's history but is no longer needed — Supabase CLI (`npm install -g supabase`) is what you'll want instead; it wasn't installed here since it needs an interactive `supabase login` you must run yourself.

### 1. Local tooling
- [ ] Install the **Supabase CLI**: `npm install -g supabase`, then `supabase login` (opens a browser — this is your own account, not something I can do for you)
- [ ] Install **Node.js 20+** if you don't already have it (for the Supabase CLI and any local scripting)

### 2. Supabase project
- [ ] Create a project at https://supabase.com/dashboard (free tier, no card required)
- [ ] From your project's API settings, copy the **Project URL** and **anon public key**
- [ ] Put them in `local.properties` (copy from `local.properties.example`) as `supabase.url` and `supabase.anonKey` — this file is gitignored, never commit real keys
- [ ] Link the CLI to your project: `supabase link --project-ref <your-project-ref>`
- [ ] Push the schema: `supabase db push` (runs `supabase/migrations/0001_init.sql`)
- [ ] Deploy the Edge Functions: `supabase functions deploy replace` and `supabase functions deploy create-sub-account`
- [ ] Set the Hugging Face token as a function secret (see step 3): `supabase secrets set HF_TOKEN=hf_...`
- [ ] Also set `SUPABASE_SERVICE_ROLE_KEY` as a function secret — Edge Functions need it to bypass RLS for writing job rows and Storage results: `supabase secrets set SUPABASE_SERVICE_ROLE_KEY=<from API settings>` (`SUPABASE_URL` and `SUPABASE_ANON_KEY` are injected automatically by Supabase, no need to set those yourself)
- [ ] Create your first Super Admin manually: sign up a user (e.g. via `supabase.auth.signUpWith(Email)` from a quick script, or the dashboard's Auth panel), then insert a row into `companies` (type `'main'`) and `profiles` (role `'super_admin'`) for that user via the SQL editor — everything after that (Client Admins, Sub-Accounts) is created through the app / `create-sub-account` function.

### 3. Hugging Face
- [ ] Create a free account at https://huggingface.co
- [ ] Generate a **fine-grained token** with "Make calls to Inference Providers" permission: https://huggingface.co/settings/tokens/new?ownUserPermissions=inference.serverless.write&tokenType=fineGrained
- [ ] Set it as a Supabase function secret: `supabase secrets set HF_TOKEN=hf_...`
- [ ] Test the pipeline against a few real room photos before trusting it — model IDs in `supabase/functions/_shared/hf.ts` (`Salesforce/blip-image-captioning-large`, `black-forest-labs/FLUX.1-Kontext-dev`) were current as of this writing, but Hugging Face's provider routing shifts over time, so re-check https://huggingface.co/docs/api-inference/tasks/image-to-image if calls start failing.

### 4. Local secrets
- [ ] Copy `local.properties.example` → `local.properties` (if not already done in step 2) and also point `sdk.dir` at your Android SDK install

None of the above files with real secrets (`local.properties`, Supabase function secrets) are tracked by git — see `.gitignore`.

## Running locally once the above is done

```bash
# Backend: push schema and deploy functions (see checklist above)
supabase link --project-ref <your-project-ref>
supabase db push
supabase functions deploy replace
supabase functions deploy create-sub-account

# App, from the command line (once local.properties has real Supabase values):
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
