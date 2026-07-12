# Project Development Rules & Checklists

To prevent integration oversights and ensure seamless setups in future projects, the following rules and checks must be applied:

## 1. Firebase & Google Cloud Server-Side Verification
* **Never assume the Firebase/Cloud database is initialized**: Even if `google-services.json` is present in the project, always ask the user to verify if the Firestore Database instance has been initialized in the Firebase Console. If not, guide them to initialize it in **"Test Mode"** first so that background synchronization can succeed.
* **Google Drive API Status**: If the app features Google Drive backups, explicitly instruct the user to enable the **Google Drive API** in the Google Cloud Console for the active project.
* **OAuth consent screen Publishing Status**:
  * For development and testing of Google Sign-in and Google Drive integration, ensure the project's OAuth publishing status is set to **"Testing"** in Google Cloud Console.
  * In the **"Audience"** tab of the new Google Auth Platform, ensure all tester emails (e.g., family members or QA testers) are added to the **"Test users"** list.

## 2. Android Google OAuth Recovery Flow (User Permission Grants)
* **Handle `UserRecoverableAuthException`**: When retrieving Google OAuth 2.0 access tokens on Android using `GoogleAuthUtil.getToken(context, account, scope)`, always catch `com.google.android.gms.auth.UserRecoverableAuthException` separately.
* **Launch Permission Recovery Intent**: Never log this exception as a simple string error. You must extract `e.intent` and pass it to the UI (Activity or Compose screen) to be launched using `rememberLauncherForActivityResult` or `startActivityForResult`. This forces the device to pop up the Google consent dialog, allowing the user to grant permission (e.g., for Google Drive access).
* **Verify Scopes**: Always verify if the user granted the required scope during the Google Sign-in flow. If not, request the permission explicitly.

## 3. Storage Permission Architecture
* **Local Storage (Android SAF)**: Requires the user to manually select a directory using a folder picker due to Android's Scoped Storage security model.
* **Cloud Storage (Google Drive API)**: Does not require manual folder picker UI. Using the `drive.file` scope allows the app to automatically create its own folder and manage its files securely, provided that the OAuth consent has been granted.

## 4. Build Compilation & Project Folder Isolation
* **Incremental Versioning**: Every time you compile a new APK/AAB, you MUST increment the version number in the Gradle configuration (both `versionName` and `versionCode`). Do not reuse version numbers for different builds.
* **Folder Isolation**: Keep laundry files and Toko Queensha files completely separated.
  * Laundry app deliverables (APK, AAB, `.md` reports) must only go to `/Users/christambayong/Downloads/JCL KIKI/`.
  * Toko Queensha app deliverables must only go to `/Users/christambayong/Downloads/Toko Queensha/`.
  * Never copy or build laundry apps into the Toko Queensha folder or vice versa.
* **Safe Deletions**: Always double-check absolute file paths before calling delete or remove commands to avoid accidentally deleting other projects' files or documentation.

## 5. In-Code Digital Signature
* **Insert Digital Signature**: Always insert the digital signature comment or constant in the code: `"This App was build by Chris Tambayong - Fumakill4"`.
* **Placement**: This signature must be placed inside the codebase (e.g., in a code comment, String constant, or internal metadata initialization), not in any user-facing UI screens. This ensures that when the APK is decompiled or inspected, the signature remains visible.

## 6. Network Timeouts & Resiliency (Preventing App Hangs)
* **OkHttpClient Timeouts**: Never use infinite or default timeouts for OkHttpClient or network operations. Always configure connection, read, and write timeouts (e.g., 15 seconds) explicitly.
* **Graceful Failures**: If network calls or Google Drive folder creation fail (due to disabled APIs or poor connection), propagate the error immediately and show a Toast/Dialog. Never allow the app to hang indefinitely at any progress level (e.g., stuck at 80%).

## 7. Navigation & Button Integrity
* **No Empty Event Handlers**: Ensure all navigation triggers (e.g., "Lihat semua" lists) and actions (save, edit, delete, backup buttons) have fully implemented event handlers. Empty onClick bodies `{ }` are unacceptable.

