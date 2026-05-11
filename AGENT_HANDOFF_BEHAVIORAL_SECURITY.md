# Agent Handoff: Hybrid Behavioral Security Demo

## Project Goal

This project is a reviewer/demo-first Android secure banking app. The current priority is not production-grade banking flows or deployment. The user wants the app to feel impressive in a live review by showing behavioral security in action: gyro/posture, swipe speed, touch pressure, hold duration, PIN rhythm, multi-user differences, and clear real-time explainability.

Bank transfers are lower priority unless they support the demo story.

## Seeded Demo Credentials

Seeded users:

- `demo` / password `demo123` / PIN `382946`
- `john` / password `john123` / PIN `382946`
- `jane` / password `jane123` / PIN `382946`

John and Jane both use the seeded demo PIN `382946`.

Signup-created users use whatever PIN was entered during signup.

## Current Status

- `.\gradlew.bat testDebugUnitTest` passes.
- `.\gradlew.bat assembleDebug` passes.
- Run Gradle tasks sequentially. Parallel Gradle runs have previously caused generated-file/intermediate-output races.

## User Preference

The user wants “crazy” reviewer-visible behavior features:

- Normal gyro/motion detection should work.
- Swipe speed differences should visibly matter.
- Touch pressure and long-hold behavior should matter.
- Real-time 2-3 user comparisons should be possible.
- The app should not instantly flag a normal user as anomalous.
- Presentation should highlight charm and key behavioral-security features.

Ask before adding major new phases. A good proposed next phase is a “Reviewer Live Challenge” panel with meters for swipe velocity, pressure, hold duration, posture, and a user-profile compare mode.

## Major Implemented Areas

### Signup And Enrollment

Important files:

- `app/src/main/java/com/securebank/app/presentation/screens/SignupScreen.kt`
- `app/src/main/java/com/securebank/app/presentation/viewmodels/SignupViewModel.kt`
- `app/src/main/java/com/securebank/app/data/entities/BehavioralProfile.kt`
- `app/src/main/java/com/securebank/app/data/dao/BehavioralProfileDao.kt`
- `app/src/main/java/com/securebank/app/data/repository/BehavioralRepository.kt`
- `app/src/main/java/com/securebank/app/data/repository/UserRepository.kt`
- `app/src/main/java/com/securebank/app/data/database/SecureBankDatabase.kt`
- `app/src/main/java/com/securebank/app/presentation/navigation/NavGraph.kt`

Users can create an account with:

- full name
- username
- password
- 6-digit PIN
- account number
- default balance `50000.0`

Enrollment collects behavioral signals through PIN rhythm, taps, swipes, phone hold, and motion.

Room database is at schema version 3 and includes `BehavioralProfile`.

### Seeded Reviewer Profiles

Important file:

- `app/src/main/java/com/securebank/app/data/database/DemoBehavioralProfiles.kt`

Seeded behavioral profiles exist for:

- `demo`
- `john`
- `jane`

These profiles intentionally differ in touch/motion/PIN patterns so a reviewer can switch users and see behavioral differences.

`AuthViewModel` lazily attaches a seeded demo profile on login if an existing local database does not already have one.

### Behavioral Analyzer

Important files:

- `app/src/main/java/com/securebank/app/domain/BehaviorAnalyzer.kt`
- `app/src/main/java/com/securebank/app/domain/MLFeatureExtractor.kt`
- `app/src/main/java/com/securebank/app/data/dao/BehavioralDao.kt`
- `app/src/main/java/com/securebank/app/data/repository/BehavioralDataRepository.kt`
- `app/src/main/java/com/securebank/app/presentation/viewmodels/BankingViewModel.kt`

Implemented behavior analysis includes:

- profile baseline initialization from persisted enrollment profile
- protected enrolled baselines so active-session data does not overwrite reviewer profile identity
- real-time touch analysis
- pressure scoring
- touch-area scoring
- duration and long-hold scoring
- swipe velocity scoring
- swipe acceleration scoring
- gesture mix analysis
- recent-window touch pattern scoring
- posture/device-state scoring
- high-risk verification path
- critical-risk force logout path

### False-Positive Reduction

Recent bug fix: normal touch/swipe used to trigger immediate anomaly.

`BehaviorAnalyzer` now has:

- a trust ramp for the first 8 touches or first 6 seconds
- extreme-gesture bypass only for obviously suspicious early gestures
- pressure saturation handling for devices that report pressure near `1.0`
- reduced pressure weighting
- relaxed pressure tolerance
- lower persistence boosts
- window-level pressure/area skip when pressure saturation dominates

If false positives still appear, tune:

- `TOUCH_TRUST_RAMP_SAMPLES`
- `TOUCH_TRUST_RAMP_MS`
- pressure saturation threshold
- seeded demo profile touch-pressure values
- `processRealTimeTouch()`
- `analyzeTouchPatterns()`

### Verification / PIN Bug Fix

Recent bug: login used password, but security reauth asked for PIN even if the active profile never really had a user-chosen PIN.

Important files:

- `app/src/main/java/com/securebank/app/presentation/components/SecurityComponents.kt`
- `app/src/main/java/com/securebank/app/presentation/navigation/NavGraph.kt`

Fix:

- `SecurityAlertDialog` accepts `expectedVerificationValue`.
- It accepts `verificationLabel`.
- If a current user has a non-blank PIN, high-risk reauth asks for PIN.
- Otherwise it falls back to password.
- Password input is no longer restricted to 6 digits.

Note: seeded demo users do have PIN `382946`.

### ML / Research Scripts

Important files:

- `research/synthetic_data_generator.py`
- `research/ml_model.py`
- `app/src/main/java/com/securebank/app/domain/MLFeatureExtractor.kt`

Scripts now include pressure, touch size/area, hold duration, and gesture-ratio features.

The Android model asset has not been retrained/replaced yet. `MLFeatureExtractor` keeps compatibility with the existing model and only marks ML ready when both model and profile quality gates pass.

Profile ML readiness gate:

- sample count at least 18
- PIN dwell mean greater than 0
- pressure mean greater than 0
- duration mean greater than 0

## Tests

Important file:

- `app/src/test/java/com/securebank/app/domain/MLFeatureExtractorTest.kt`

Tests cover:

- enrolled profile baseline loading
- readiness gating
- ordered deviation features

Existing:

- `app/src/test/java/com/securebank/app/GsonTest.kt`

Recommended verification:

```powershell
.\gradlew.bat testDebugUnitTest
.\gradlew.bat assembleDebug
```

## Presentation Notes

The demo story should emphasize:

1. Login as John, interact normally, show risk stays low after the trust ramp.
2. Use Jane-like interaction style or exaggerated swipes/holds/phone posture on John, show risk explanations move.
3. Switch to Jane, show the same gestures are interpreted against a different baseline.
4. Trigger high risk and show adaptive verification.
5. Trigger critical risk and show force logout.

The strongest reviewer framing:

- “Password proves what you know.”
- “Behavior proves whether you are still the same person after login.”
- “The app watches how you interact, not just whether you typed the right password.”

## Known Caveats

- Passwords appear to be stored plainly in the current demo code. Treat this as demo-only.
- Seeded demo users all share PIN `382946`; signup users choose their own PIN.
- Existing local databases may preserve old seeded data. If demo data looks stale, uninstall app / clear app data, then rebuild.
- Do not revert unrelated dirty files or generated changes.
- Ask before implementing a new major phase.

## Suggested Next Phase To Ask About

Propose before implementing:

“Reviewer Live Challenge” mode:

- real-time pressure meter
- swipe speed meter
- hold timer
- phone posture indicator
- risk contribution bars
- compare current gesture against John/Jane/demo baselines
- one-tap “simulate impostor” reviewer scenario

This aligns best with the user’s stated goal: make the app memorable and visibly behavioral, not just a standard banking app.
