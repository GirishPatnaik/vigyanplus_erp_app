# Vigyan International Junior College — Android ERP

An offline-first Android app for **VIGYAN INTERNATIONAL JUNIOR COLLEGE**, Koraput, Odisha
— built with Kotlin, Jetpack Compose, and Room (local SQLite).

## Modules (from project tree)

1. **Login & Security** — Administrator/Staff login, PBKDF2-hashed passwords, audit log
2. **Dashboard** — live student counts, gender split, today's attendance snapshot, quick actions
3. **Student Management** — add/edit/search/filter students, soft-delete recycle bin
4. **Student Photo** — capture/select a photo per student, shown on profile and list
5. **CSV Import** — downloadable template, bulk import with per-row validation and an error report
6. **Attendance** — mark present/absent by class/batch/section, per-student history, daily % on dashboard
7. **Reports** — student counts by gender/status, CSV export
8. **Academic Structure** — Academic Year, Course, Stream, Class, Batch, Section (all editable)
9. **Master Settings** — the above, plus College Details (name, address, phone, email, board)
10. **Database & Backup** — manual backup, export to Downloads/Drive, restore from a `.db` file

## Tech stack

- Kotlin + Jetpack Compose (Material 3)
- Room (SQLite) for all local data — no server required
- Navigation Compose
- Coil for photo loading
- OpenCSV for import/export
- AndroidX DataStore for session state

## First run

- Default login: **admin / admin123** — change this immediately from a real device
  (a "change password" flow can be wired to `AuthRepository.changePassword` on the settings screen).
- Sample Academic Year, Course/Stream, Class, and Section entries are seeded automatically on first launch —
  edit or delete them from **Master Settings**.

## Opening the project

1. Install **Android Studio** (Koala or newer recommended).
2. `File → Open` and select the `vigyan_erp` folder (the one containing `settings.gradle.kts`).
3. Let Android Studio sync Gradle — on first sync it will download the Gradle wrapper jar and all
   dependencies automatically (needs internet once).
4. Run on an emulator or a physical device (`minSdk 24`, i.e. Android 7.0+).

## Release builds & signing

See **[RELEASE.md](RELEASE.md)** for the full guide — versioning convention, how to build a
signed release APK/AAB, and how the included keystore is set up (`keystore/vigyan_release.keystore`,
kept out of git via `.gitignore`). Debug and release builds can be installed side-by-side on the
same device (debug shows as "Vigyan Jr. College (Debug)").

## Building the APK without installing anything (GitHub Actions)

If you don't want to install Android Studio, this repo includes a ready-to-use GitHub Actions
workflow that builds the APK in the cloud and lets you download it — see
**[.github/workflows/build-debug-apk.yml](.github/workflows/build-debug-apk.yml)** and the
step-by-step in RELEASE.md.

## Pushing this project to GitHub

This project was generated in a sandboxed environment without live internet access, so it
could not be pushed automatically. Run these commands yourself from a terminal that has
internet and `git` installed, from inside the unzipped `vigyan_erp` folder:

```bash
cd vigyan_erp

# 1. Initialize git (skip if already a repo)
git init
git add .
git commit -m "Initial commit: Vigyan International Junior College ERP Android app"

# 2. Create a new repository on GitHub first (via github.com/new), then:
git branch -M main
git remote add origin https://github.com/<your-username>/<your-repo-name>.git
git push -u origin main
```

If you'd rather use the GitHub CLI (`gh`) to create the repo in one step:

```bash
cd vigyan_erp
git init && git add . && git commit -m "Initial commit: Vigyan International Junior College ERP Android app"
gh repo create vigyan-international-jr-college-erp --public --source=. --remote=origin --push
```

## Notes for further development

- The app is fully offline; all data lives in a local Room/SQLite database at
  `Android/data/com.vigyan.juniorcollege/databases/vigyan_erp.db` on the device.
- Attendance marking currently defaults to today's date; a date picker can be added to
  `AttendanceScreen` to mark/edit attendance for past dates.
- `CsvRepository` validates duplicate admission numbers and missing required fields; extend the
  validation rules there as your admission process evolves.

## Implemented in this update

- **Aadhaar encryption** — `util/AadhaarCrypto.kt` encrypts Aadhaar numbers with AES/GCM using a
  key held in the Android Keystore (non-extractable, even with root). The Add/Edit student screen
  collects and encrypts it on save; the profile screen shows it masked (`XXXX XXXX 1234`).
- **Change Password** — new screen at Routes.CHANGE_PASSWORD (also in the drawer), verifies the
  current password before setting a new one, and logs the change to the audit log.
- **Automatic backup** — `work/AutoBackupWorker.kt` + `AutoBackupScheduler.kt` schedule a daily
  WorkManager job (battery-aware, `KEEP` policy so it won't reset on every app launch) that copies
  the live database into the backups folder and prunes anything past the 10 most recent backups.

