# DoomSQL - Firebase Authentication & Account Configuration Guide

This guide walks you through setting up **Firebase Authentication** (Google Sign-In via Android Credential Manager + Email/Password) and optional cloud database sync for **DoomSQL**.

---

## Architecture Summary
- **100% Offline-First:** DoomSQL works fully without an account. There is **no sign-in wall**.
- **Optional Account:** Users can optionally sign in from **Settings > Account** to prepare for cloud backups and cross-device sync.
- **Modern Credential Manager:** Uses `androidx.credentials` + `GetGoogleIdOption`. The deprecated `GoogleSignInClient` is NOT used.
- **Google Play Compliant:** Supports both in-app account deletion (preserving or wiping local progress) and external web deletion.

---

## Step 1: Create a Firebase Project

1. Navigate to the [Firebase Console](https://console.firebase.google.com/).
2. Click **Add project** (or select an existing Google Cloud project).
3. Name your project (e.g., `DoomSQL`) and complete the creation wizard (Google Analytics is optional).

---

## Step 2: Register the Android App in Firebase

1. In the Firebase project overview, click the **Android** icon (or go to **Project Settings > General > Your apps > Add app**).
2. Enter the following details:
   - **Android package name (CRITICAL):**
     ```
     com.manish.doomsql
     ```
     *(This MUST match the `applicationId` in `app/build.gradle.kts`)*
   - **App nickname:** `DoomSQL`
   - **Debug signing certificate SHA-1 and SHA-256:**
     You **must** register certificate fingerprints for Google Sign-In to work.

### Debug Certificate Fingerprints for this project:
- **SHA-1:**
  ```
  A5:41:0E:41:18:3A:65:4D:71:17:69:59:6F:9D:1E:76:74:C7:99:FF
  ```
- **SHA-256:**
  ```
  55:B3:7F:AB:02:27:4A:47:EE:B8:5F:03:04:B4:6F:20:A2:D7:9C:24:41:F5:16:75:37:ED:00:E0:F6:6C:9E:70
  ```

> **Note for Production / Release Builds:**
> When you generate your production release APK or Google Play App Signing key, extract its fingerprints using:
> ```bash
> keytool -list -v -keystore my-upload-key.jks -alias upload
> ```
> Add the production SHA-1 and SHA-256 fingerprints to your Firebase app under **Project settings > General > Your apps**.

3. Click **Register app**.

---

## Step 3: Download & Place `google-services.json`

1. Download the generated `google-services.json` file from Firebase.
2. Place it in the app module directory:
   ```
   app/google-services.json
   ```
3. The build system will automatically parse this file and generate internal client IDs, project credentials, and resource configurations.

---

## Step 4: Enable Authentication Providers in Firebase Console

1. In the Firebase console left menu, go to **Build > Authentication**.
2. Click **Get Started**.
3. Under the **Sign-in method** tab:

### 1. Enable Email/Password
- Click **Email/Password**.
- Switch **Enable** to ON.
- Click **Save**.

### 2. Enable Google Sign-In
- Click **Google**.
- Switch **Enable** to ON.
- Choose a **Project support email** from the dropdown.
- Click **Save**.
- Under the Google provider configuration, Firebase automatically provisions a **Web SDK configuration (Web client ID)**.

---

## Step 5: Environment Variables & Web Client ID

### Automatic Detection (Default)
When you add `app/google-services.json`, the Google Services Gradle plugin automatically creates a resource named `@string/default_web_client_id`. DoomSQL's `FirebaseAuthRepository` automatically detects and uses this key.

### Manual Override via `.env` (Optional)
If you wish to explicitly specify the Web Client ID:
1. In Firebase Console, go to **Authentication > Sign-in method > Google > Web SDK configuration**.
2. Copy the **Web client ID** (ends with `.apps.googleusercontent.com`).
3. In your project root, open `.env` (or configure via the AI Studio Secrets panel):
   ```env
   WEB_CLIENT_ID=your_web_client_id_here.apps.googleusercontent.com
   ```
4. DoomSQL will inject this into `BuildConfig.WEB_CLIENT_ID` via the Secrets Gradle Plugin.

---

## Step 6: Cloud Database (Firestore) Sync (Optional)

DoomSQL currently stores all problem statistics, draft queries, streaks, and progress locally in **Room** (SQLite) on the device.

If you want to sync progress to the cloud across devices:
1. In Firebase Console, go to **Build > Firestore Database** and click **Create database**.
2. Start in test mode or production mode with rules:
   ```javascript
   rules_version = '2';
   service cloud.firestore {
     match /databases/{database}/documents {
       match /users/{userId}/{document=**} {
         allow read, write: if request.auth != null && request.auth.uid == userId;
       }
     }
   }
   ```
3. In `app/build.gradle.kts`, uncomment:
   ```kotlin
   implementation(libs.firebase.firestore)
   ```
4. Sync local Room entities with Firestore under `/users/{uid}/progress/{questionId}` when `currentUser` is authenticated.

---

## Step 7: Google Play Account Deletion Requirement

Google Play requires that any app offering account creation must allow users to delete their account:
1. **In-App Deletion:** DoomSQL includes a **"Delete Account"** button in **Settings > Account** with a confirmation dialog. It permanently deletes the user's Firebase Auth record. A checkbox allows the user to choose whether to also reset local offline progress.
2. **Web Deletion URL:** Google Play also requires an external web link where users can request account deletion outside the app.
   - Open `app/src/main/java/com/manish/doomsql/config/AppLinks.kt`.
   - Update `ACCOUNT_DELETION_URL` with your website's URL (e.g., `https://yourdomain.com/doomsql/delete-account`):
     ```kotlin
     const val ACCOUNT_DELETION_URL = "https://yourdomain.com/doomsql/delete-account"
     ```
   - Provide this same URL in the Google Play Console under **Data safety > Account deletion**.
