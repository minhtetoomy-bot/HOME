HOME Messenger 2.0.0 — reference-design Firebase MVP

This package uses the user's supplied HOME design artwork as the visual reference for:
- Splash
- Phone login
- OTP
- Create account
- Create password
- Account created
- Home
- Me/Profile

Functional core:
- Firebase Phone Authentication (real SMS when Firebase billing/Phone Auth requirements are satisfied)
- OTP verification
- Profile creation saved to Realtime Database
- SHA-256 phone index for contact lookup
- Contact lookup by phone
- Deterministic 1-to-1 chat rooms
- Realtime text messages
- Chats / Contacts / Me navigation
- Sign out
- Edit profile name

Important:
- Real Phone Auth SMS can require Cloud Billing and Firebase phone-auth configuration.
- Firebase Storage is not required for the text-chat core. Photo cloud upload is intentionally not claimed as complete.
- The reference artwork is used as the visual source; interactive controls are overlaid so the flow remains functional.
- This environment does not contain a full Android SDK, so the APK must be compiled by GitHub Actions or another Android build environment.

Firebase setup:
1. Android package: com.home.app
2. Enable Authentication > Phone.
3. Configure SMS region policy for the countries you need.
4. Add SHA-1 and SHA-256 fingerprints for the release/debug signing keys used to build the app.
5. Keep Realtime Database rules locked down using database-rules.json.
6. For real SMS, configure Firebase/Google Cloud billing as required by Firebase Phone Authentication.

Build:
GitHub Actions workflow is included at .github/workflows/build-apk.yml.
It builds app-debug.apk and uploads it as an artifact named HOME-design-exact-debug-apk.
