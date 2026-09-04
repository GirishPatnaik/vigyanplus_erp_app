# Release Guide — Vigyan International Junior College ERP

## 1. What's included for signing

A release keystore has already been generated for this app and lives at:

```
keystore/vigyan_release.keystore
keystore.properties          (credentials Gradle reads — see below)
```

**⚠️ Keep both of these safe and back them up somewhere outside this project
(e.g. a password manager or an encrypted drive).** Neither is committed to git
(they're in `.gitignore`) — if you lose the keystore, you can never publish an
update to the same app listing again; you'd have to publish as a brand-new app.

Current signing identity:
- **Alias:** `vigyan_jrcollege`
- **Validity:** 30 years (until 2056)
- **Owner:** Vigyan International Junior College, Koraput, Odisha

If you ever move to a new machine, just copy `keystore/vigyan_release.keystore`
and `keystore.properties` into the project root — nothing else needs to change.

If `keystore.properties` is ever missing (e.g. a teammate clones the repo
without it), release builds still compile — they just come out **unsigned**.
Copy `keystore.properties.template` to `keystore.properties` and fill in real
values to enable signing again.

## 2. Versioning convention

In `app/build.gradle.kts`:

```kotlin
versionCode = 1        // integer, MUST increase by at least 1 on every release
versionName = "1.0.0"  // human-readable, MAJOR.MINOR.PATCH
```

Bump both together before every release build:
- **PATCH** (1.0.0 → 1.0.1): bug fixes, no new features
- **MINOR** (1.0.1 → 1.1.0): new features, backward compatible
- **MAJOR** (1.x.x → 2.0.0): breaking changes (e.g. a database schema change
  that isn't backward compatible, a big UI overhaul)

`versionCode` must strictly increase every time you upload a new build to the
Play Store, even for the same `versionName` — the Play Console will reject a
build with a `versionCode` it's already seen.

## 3. Building a signed release

From a terminal with internet access (needed once, to download Gradle/dependencies):

```bash
cd vigyan_erp

# Signed release APK (for direct install / sharing outside the Play Store)
./gradlew assembleRelease
# Output: app/build/outputs/apk/release/app-release.apk

# Signed release AAB (Android App Bundle — what the Play Store requires)
./gradlew bundleRelease
# Output: app/build/outputs/bundle/release/app-release.aab
```

Or in Android Studio: **Build → Generate Signed App Bundle / APK**, choose the
existing keystore at `keystore/vigyan_release.keystore` when prompted.

The release build type has `isMinifyEnabled = true` and `isShrinkResources = true`
turned on (code shrinking + resource shrinking via R8), with ProGuard rules in
`app/proguard-rules.pro` covering Room, OpenCSV, WorkManager workers, and
Keystore-backed crypto so nothing gets stripped that's needed at runtime.

## 4. Debug vs. release side-by-side

Debug builds now use `applicationIdSuffix = ".debug"` and show as
**"Vigyan Jr. College (Debug)"** on the home screen — so you can install a
debug build on the same device as a released version without one overwriting
the other, useful for testing a new feature against real production data
patterns before shipping it.

## 5. App icon

The launcher icon was regenerated to respect Android's adaptive-icon "safe
zone" (~62% of the canvas) so the crest doesn't get clipped on circular or
squircle icon masks on different phone launchers. A 512×512 hi-res copy for
a future Play Store listing is at `store_assets/play_store_icon_512.png`.

## 6. Before your first real Play Store upload

- [ ] Bump `versionCode`/`versionName` if this isn't truly v1.0.0 anymore
- [ ] Test the **release** build specifically (not just debug) — minification
      can occasionally break reflection-based code paths that debug builds don't hit
- [ ] Fill in real contact details on **Master Settings → College Details**
      before demoing/handing off, since they seed as placeholders
- [ ] Change the default `admin` / `admin123` login on the device before go-live
- [ ] Decide on a short privacy policy (required by Play Store) — this app
      stores Aadhaar numbers and student data locally on-device only, which is
      worth stating explicitly

## 7. Building the APK entirely from the GitHub website (no software install)

### ⚠️ Read this first if you upload files via the GitHub website (not `git`)

`.gitignore` only protects you when using the `git` command line. If you're
dragging files into GitHub's **"Add file → Upload files"** button instead,
`.gitignore` does nothing — you could accidentally publish your signing
keystore publicly. **Never upload these three to GitHub, ever:**

- `keystore/` folder (contains `vigyan_release.keystore`)
- `keystore.properties`
- `keystore_base64_for_github_secret.txt`

Keep them only on your own computer. The only *new* thing you need to add to
your GitHub repo for this step is the `.github/workflows/` folder (below) —
everything keystore-related stays local, and only gets pasted into GitHub's
**Secrets** form (which is encrypted and separate from your repo's files) if
you choose to set up signed release builds.

Two workflow files are included at `.github/workflows/`. GitHub runs these on
its own servers — you just click a button and download the finished APK.

### Get a working APK right now (debug build, no setup needed)

This one needs nothing extra — it's already wired up.

1. On GitHub, open your repo → the **Actions** tab.
2. If prompted, click **"I understand my workflows, go ahead and enable them"**.
3. In the left sidebar, click **Build Debug APK**.
4. Click **Run workflow** (dropdown on the right) → **Run workflow** (green button).
5. Wait ~2–4 minutes for the run to go green ✅.
6. Click into the completed run → scroll to **Artifacts** at the bottom →
   download **vigyan-erp-debug-apk** (a zip containing `app-debug.apk`).
7. Unzip it, send `app-debug.apk` to your Android phone (email, Google Drive,
   WhatsApp, USB — any way you like), open it, and tap **Install**. You'll
   need to allow "Install from this source" the first time Android asks.
8. This workflow also re-runs automatically every time you push new code to
   `main`, so future updates just need a re-download from the latest run.

This debug APK is fully functional — it's unsigned, which only matters if you
plan to publish on the Play Store (which needs the *signed release* build below).

### Get a signed release APK (needed for Play Store; optional for personal use)

This needs your keystore added as GitHub "secrets" first — one-time setup:

1. On your computer (wherever you unzipped this project), find
   `keystore_base64_for_github_secret.txt` — it's a copy of your keystore
   file encoded as text so it can be pasted into GitHub's secrets form.
   (If you don't have this file, see the note below.)
2. On GitHub: repo → **Settings** tab → **Secrets and variables** →
   **Actions** → **New repository secret**. Create these four, one at a time:
   - `KEYSTORE_BASE64` → paste the entire contents of that `.txt` file
   - `KEYSTORE_PASSWORD` → `Vigyan@Koraput2026`
   - `KEY_ALIAS` → `vigyan_jrcollege`
   - `KEY_PASSWORD` → `Vigyan@Koraput2026`
3. Go to the **Actions** tab → **Build Signed Release APK** → **Run workflow**.
4. Once green, download the **vigyan-erp-release-apk** artifact the same way as above.

*(If `keystore_base64_for_github_secret.txt` wasn't included in your download,
you can regenerate it from `keystore/vigyan_release.keystore` using any
base64 encoder, or ask for it to be regenerated.)*

**Security note:** GitHub secrets are encrypted and never shown in logs, but
treat them as sensitive — anyone with write access to the repo could add a
workflow step that prints them. Only use this on a repo you control.
