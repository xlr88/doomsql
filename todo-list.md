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
- [ ] Push doomsql-content again: `ai_gen.py`, `.env.example`, `gen_sql_ques.md`, AI workflow, `.gitignore` are still uncommitted
- [ ] Check GitHub → **Actions** tab shows a green ✅ for that push
- [x] Open the manifest URL above and confirm it lists **20** questions. (✓ live, version 6)
      If it shows old data or a 404, open
      `https://purge.jsdelivr.net/gh/xlr88/doomsql-content@main/questions/manifest.json` once.
- [ ] **Push doomsql** (`dev-claude`): review the diff in VS Code, commit, push
- [x] Android Studio: **Build → Clean Project**, then Run on phone/emulator
- [ ] Questions tab shows **20** questions with airplane mode ON (fresh install)
- [ ] Airplane mode OFF → Settings → **Check for new questions** → says "You're up to date"
      (bundled and remote are both at 20)
- [ ] Solve "Top Earner in Each Branch" and "Movie Rating Classification" to confirm
      the ordering fix works
- [ ] Run unit tests: `./gradlew testDebugUnitTest` → all pass
- [ ] Merge `dev-claude` → `main` once the above pass

## Phase 2 — More questions (target 50+ before launch)

The design doc targets 50–80. All 8 generator templates are used up.

- [ ] Get an API key (Gemini has a free tier): https://aistudio.google.com/apikey
- [ ] In doomsql-content: `cp .env.example .env`, then put the key in `.env` (never commit it)
- [ ] First try: `python3 ai_gen.py dry --e 1 --m 1`. Check it connects and questions look good.
- [ ] Then `python3 ai_gen.py man --e 5 --m 5 --h 2`, read `ai_runs/…md`, solve a couple in the app, push
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
- [ ] **Package renamed to `com.chaduvukondi.firstu` (Oct 4).** Firebase → Project settings → Add app → Android →
      package `com.chaduvukondi.firstu`, same debug SHA-1 + SHA-256 → download the new `google-services.json`
      into `app/` → Clean + Run → re-test Google sign-in. Then remove the old `com.manish.doomsql` app in Firebase.
- [x] Test: Welcome → Continue with Google → signed in → Settings shows email
- [x] Test: Settings → Delete account works
- [x] Test: Sign out → Continue with Google shows the account picker again
- [ ] Know the manual deletion routine (for emails from the web page): Firebase Console → Authentication →
      Users → search the email → ⋮ → Delete account → reply to confirm. Promise on the page: within 7 days.
- [ ] After Phase 4: add the **release/upload** and **Play App Signing** SHA keys to Firebase too
      (otherwise sign-in breaks in the Play Store build)

## Phase 4 — Release setup

- [ ] Create an upload key: `keytool -genkey -v -keystore my-upload-key.jks -alias upload -keyalg RSA -keysize 2048 -validity 10000`
- [ ] Store the `.jks` + passwords somewhere safe (password manager). **Never commit it.**
      Losing it means you can't update the app.
- [ ] Set env vars `STORE_PASSWORD` and `KEY_PASSWORD` (and `KEYSTORE_PATH` if not in the repo root)
- [x] Host the 3 required pages on GitHub Pages repo **xlr88/xlr88.github.io** (wording fixed Oct 4: no cloud sync claims):
  - [x] Privacy policy: https://xlr88.github.io/privacy.html
  - [x] Terms of service: https://xlr88.github.io/terms.html
  - [x] Account deletion: https://xlr88.github.io/delete-account.html
- [x] Update `app/src/main/java/com/chaduvukondi/firstu/config/AppLinks.kt`:
  - [x] `SUPPORT_EMAIL` (omkarmaduguri000@proton.me)
  - [x] `PRIVACY_POLICY_URL`
  - [x] `TERMS_URL`
  - [x] `ACCOUNT_DELETION_URL`
- [ ] Bump `versionCode` / `versionName` in `app/build.gradle.kts` for every upload
- [ ] Build: `./gradlew bundleRelease` → `app/build/outputs/bundle/release/app-release.aab`
- [ ] Install the release build on a real phone and smoke-test everything (sign-in, run query, sync)

## Phase 5 — Play Console

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
