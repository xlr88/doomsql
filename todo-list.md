# DoomSQL — To-Do List

Work top to bottom. Tick `[x]` when done. Each phase depends on the one before it.

Repos:
- **doomsql** (private): the Android app
- **doomsql-content** (public): remote questions, served at
  `https://cdn.jsdelivr.net/gh/xlr88/doomsql-content@main/questions/manifest.json`

---

## ✅ Done (Oct 3, 2026)

- [x] Sign-in is Google-only. Docs updated to match.
- [x] App points at `xlr88/doomsql-content` (`RemoteModels.kt` → `DEFAULT_REPO`)
- [x] Automatic question check set to once per 24h (was 5 min)
- [x] Old `questions/` and `tools/` removed from the app repo
- [x] App repo scripts + GitHub check now only cover bundled questions
- [x] Content repo has tooling, guide (`gen_sql_ques.md`) and GitHub check
- [x] 7 new questions from templates (`sql_014`–`sql_020`). Two had their ordering wording fixed.
- [x] Bundled questions raised from 12 → 20 (`index.json` updated)
- [x] Swipe navigation (dev-android-studio) merged into dev-claude; question sync fixed to use doomsql-content
- [x] Debug SHA corrected (49:8A…) — Google sign-in + delete account tested OK
- [x] Code shrinking stays OFF (decision: keep everything working)
- [x] AI generator `ai_gen.py` added to doomsql-content (tested with a fake AI; real API not yet tried)

---

## Phase 1 — Push & verify (do now)

- [x] **Push doomsql-content** questions (20 live)
- [x] Push doomsql-content again (ai_gen, workflows, guide)
- [ ] Check GitHub → **Actions** tab shows a green ✅ for that push
- [x] Open the manifest URL above and confirm it lists **20** questions. (✓ live, version 6)
      If it shows old data or a 404, open
      `https://purge.jsdelivr.net/gh/xlr88/doomsql-content@main/questions/manifest.json` once.
- [x] **Push doomsql** (`dev-claude`)
- [x] Android Studio: **Build → Clean Project**, then Run on phone/emulator
- [ ] Questions tab shows **20** questions with airplane mode ON (fresh install)
- [ ] Airplane mode OFF → Settings → **Check for new questions** → says "You're up to date"
      (bundled and remote are both at 20)
- [ ] Solve "Top Earner in Each Branch" and "Movie Rating Classification" to confirm
      the ordering fix works
- [ ] Run unit tests: `./gradlew testDebugUnitTest` → all pass
- [x] Decision: `dev-claude` is the working branch (not merging to `main`); cut a `release` branch from it for production

## Phase 2 — More questions (target 50+ before launch)

The design doc targets 50–80. All 8 generator templates are used up.

- [x] Get an API key (Gemini has a free tier): https://aistudio.google.com/apikey
- [x] In doomsql-content: `cp .env.example .env`, then put the key in `.env` (never commit it)
- [x] First try: `python3 ai_gen.py dry --e 1 --m 1`. Check it connects and questions look good.
- [x] Then `python3 ai_gen.py man …`, push (25 questions live as of Oct 4)
- [ ] Once trusted: `python3 ai_gen.py auto --e … --m … --h …`
- [ ] Optional hands-free: add the key as a GitHub secret and run the "AI generate questions" Action
      (enable its daily schedule once you're happy with quality)
- [ ] Hand-written questions still work too: drafts in `doomsql-content/drafts/` (see `gen_sql_ques.md`, Option A)
- [ ] Aim for a balance: ~20 EASY / ~20 MEDIUM / ~10 HARD
- [ ] Before launch, copy the final set into the app's
      `app/src/main/assets/questions/` + `index.json` (same ids, same contentVersion)
- [ ] After launch, new questions only need to go into `doomsql-content`

## Phase 3 — Firebase (Google sign-in)

Follow `account_conf.md`.

- [x] Create Firebase project "DoomSQL"
- [x] Add Android app (old id `com.manish.doomsql`) with **debug** SHA-1 + SHA-256 (in `account_conf.md`)
- [x] Download `google-services.json` → put it in `app/` (Gradle was already set up, so no build file changes needed)
- [x] Authentication → enable **Google** only
- [x] **Package renamed to `com.chaduvukondi.firstu` (Oct 4).** Firebase → Project settings → Add app → Android →
      package `com.chaduvukondi.firstu`, same debug SHA-1 + SHA-256 → download the new `google-services.json`
      into `app/` → Clean + Run → re-test Google sign-in. Then remove the old `com.manish.doomsql` app in Firebase.
- [x] Test: Welcome → Continue with Google → signed in → Settings shows email
- [x] Test: Settings → Delete account works
- [x] Test: Sign out → Continue with Google shows the account picker again
- [ ] Know the manual deletion routine (for emails from the web page): Firebase Console → Authentication →
      Users → search the email → ⋮ → Delete account → reply to confirm. Promise on the page: within 7 days.
- [ ] Upload key SHA → Firebase: see Phase 4.3
- [ ] After the first Play upload: add the **Play App Signing** SHA-1/SHA-256 to Firebase too
      (Play Console → Test and release → App integrity → App signing). Otherwise sign-in breaks for Play Store installs.

## Phase 4 — Release setup (no Play account needed)

**Why:** Play only accepts apps signed with *your* private "upload key". You create it once and
use it for every update. **If you lose it, or its passwords, you can't update the app** without
a slow reset through Google support. So: create it once, back it up twice.

Run all commands in the VS Code terminal (zsh), one at a time.

### 4.1 Create the upload key (one time)

- [ ] **Make a folder outside the repo** for keys, so a key can never be committed by accident:
      `mkdir -p ~/keys`
- [ ] **Check keytool exists:** `keytool -help`
      If you get "command not found", use Android Studio's copy instead of `keytool` in the next step:
      `"/Applications/Android Studio.app/Contents/jbr/Contents/Home/bin/keytool"`
- [ ] **Create the key:**
      `keytool -genkey -v -keystore ~/keys/firstu-upload.jks -alias upload -keyalg RSA -keysize 2048 -validity 10000`
  - It asks for a **keystore password**. Pick a strong one and write it down.
  - Name / organisation / city / country: anything sensible (e.g. your name, "Chaduvukondi", Hyderabad, IN).
  - Key password: press **Enter** to use the same password (simplest).
  - The alias **must stay `upload`**. The build file expects that name.
- [ ] **Back it up in 2 places** (e.g. password manager + encrypted cloud drive):
  - the file `~/keys/firstu-upload.jks`
  - the keystore password
  - the alias (`upload`)

### 4.2 Tell the build where the key is

The build reads 3 environment variables (see `signingConfigs` in `app/build.gradle.kts`).

- [ ] Open your shell profile: `open -e ~/.zshrc` (create it if it doesn't exist)
- [ ] Add these 3 lines at the bottom, save, close:
      ```
      export KEYSTORE_PATH="$HOME/keys/firstu-upload.jks"
      export STORE_PASSWORD="your-keystore-password"
      export KEY_PASSWORD="your-keystore-password"
      ```
- [ ] Reload it: `source ~/.zshrc`
- [ ] Check: `echo $KEYSTORE_PATH` prints the path
- [ ] Note: Android Studio opened from the Dock does **not** see these variables. Build release
      versions from the terminal (step 4.4). Debug runs from Android Studio are unaffected.

### 4.3 Add the upload key's fingerprints to Firebase

Without this, Google sign-in fails in release builds (same issue as the debug key earlier).

- [ ] Get the fingerprints: `keytool -list -v -keystore ~/keys/firstu-upload.jks -alias upload`
      (enter the keystore password). Copy the **SHA1** and **SHA256** lines.
- [ ] Firebase Console → Project settings → your Android app (`com.chaduvukondi.firstu`) →
      **Add fingerprint** → paste SHA-1 → **Add fingerprint** → paste SHA-256
- [ ] Download `google-services.json` again → replace `app/google-services.json`

### 4.4 Set the version and build

- [ ] In `app/build.gradle.kts`: `versionCode = 1`, `versionName = "1.0"` is right for the first upload.
      **Every later upload needs a higher `versionCode`** (2, 3, 4…). Play rejects repeats.
- [ ] Go to the repo root: `cd ~/Desktop/curious/doomsql`
- [ ] Build the Play bundle: `./gradlew clean bundleRelease`
      → output: `app/build/outputs/bundle/release/app-release.aab` (this is what you upload to Play)
- [ ] Build an installable copy for testing: `./gradlew assembleRelease`
      → output: `app/build/outputs/apk/release/app-release.apk`
- [ ] If the build fails with a "keystore" / "password" error: run `echo $STORE_PASSWORD` in the
      same terminal. If it's empty, redo step 4.2.

### 4.5 Test the release build on your phone

Release builds can behave differently from debug ones, so test before uploading.

- [ ] Uninstall the debug version from the phone first (the two are signed with different keys, so
      the install fails otherwise)
- [ ] Install: `adb install app/build/outputs/apk/release/app-release.apk`
      (or drag the .apk to the emulator window)
- [ ] Smoke test:
  - [ ] App opens; 20 bundled questions show with airplane mode ON
  - [ ] Solve one question → marked solved, streak updates
  - [ ] Continue with Google → signed in (this proves step 4.3 worked)
  - [ ] Settings → Check for new questions → new questions download
  - [ ] Swipe between questions works
  - [ ] Sign out → sign in again → account picker shows
- [ ] All good → Phase 4 done. The `.aab` from 4.4 is ready for Play Console (Phase 5).

### Already done in this phase
- [x] Host the 3 required pages on GitHub Pages repo **xlr88/xlr88.github.io** (wording fixed Oct 4: no cloud sync claims):
  - [x] Privacy policy: https://xlr88.github.io/privacy.html
  - [x] Terms of service: https://xlr88.github.io/terms.html
  - [x] Account deletion: https://xlr88.github.io/delete-account.html
- [x] Update `app/src/main/java/com/chaduvukondi/firstu/config/AppLinks.kt` (support email + 3 URLs)
- [x] `*.jks` / `*.keystore` added to `.gitignore`

## Phase 5 — Play Console (needs a Google Play developer account)

- [ ] Register at https://play.google.com/console/signup: one-time US$25, identity verification can take a few days, so start early

- [ ] Create the app in Play Console (name, default language, free)
- [ ] Store listing: short + full description, 512×512 icon, 1024×500 feature graphic,
      at least 2 phone screenshots
- [ ] **Data safety** form: Google account (email, name), purchase history (tips),
      app works offline, data deletion supported (paste `ACCOUNT_DELETION_URL`)
- [ ] Content rating questionnaire
- [ ] Target audience (13+ or 18+; avoids the extra rules that apply to apps for children)
- [ ] Privacy policy URL
- [ ] **Closed testing**: new personal developer accounts must run a closed test with at least
      12 testers for 14 days before production access. Recruit testers early.
- [ ] Upload the `.aab` to closed testing → fix feedback → apply for production

## Phase 6 — Tips (Play Billing)

Follow `billing_setup.md`. Needs the app uploaded to Play Console first.

- [ ] Set up the merchant/payments profile + bank account
- [ ] Create 4 in-app products: `tip_small` ₹29, `tip_medium` ₹79, `tip_large` ₹199, `tip_hero` ₹499
- [ ] Add your Google account under **License testing**
- [ ] Test each tip with the test card → Supporter badge appears

## Later / nice-to-have

- [x] Package id changed to `com.chaduvukondi.firstu` before launch (it can never change after publishing); app name stays "DoomSQL".
- [ ] Company-tagged questions
- [ ] Cloud sync of progress via Firestore (see `account_conf.md` Step 6)
- [ ] Activity heatmap / calendar view
