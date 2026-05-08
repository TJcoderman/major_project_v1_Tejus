# SecureBank Audit Fix Prompt

You are working in this Android/Kotlin project:

`C:\Users\Tejus Kapoor\Desktop\secure_banking\major_project_v1_Tejus`

Goal: make the college demo credible for a mobile-banking session-hijacking prevention project. The app is a test/demo app, not a real banking app, but it should behave consistently with its claims, avoid obvious security theater, and not look vibe-coded under review.

Before editing, run:

```powershell
.\gradlew.bat testDebugUnitTest
git status --short
```

Do not revert unrelated user changes. Ignore generated Gradle/build artifacts except for repository hygiene tasks explicitly listed below.

## Highest Priority Bugs

1. Enforce security decisions instead of only displaying them.
   - `SecurityRecommendation.FORCE_LOGOUT` is documented as "Immediately terminate session", but `BankingViewModel.handleRiskAssessment()` only sets `_showSecurityAlert = true`.
   - `SecurityAlertDialog` always offers "Continue Session", even for critical risk.
   - Fix by modeling alert severity/action explicitly. Critical risk must stop collectors, clear auth/session state, and navigate to login automatically or show a non-dismissible logout-only screen. High risk should request re-authentication, not a simple dismiss.
   - Files: `BankingViewModel.kt`, `NavGraph.kt`, `SecurityComponents.kt`, `BehavioralData.kt`.

2. Fix lifecycle monitoring gaps.
   - `MainActivity` stops `sensorDataCollector` and `touchDataCollector` on pause, but nothing restarts them on resume.
   - Because `BankingViewModel.isCollecting` remains true, even a later `startBehavioralCollection()` call would return early.
   - Move lifecycle handling into the view model or add `pauseBehavioralCollection()` / `resumeBehavioralCollection()` that correctly restarts collectors for the active session. Add a debug event so reviewers can see monitoring resumed.
   - Files: `MainActivity.kt`, `BankingViewModel.kt`.

3. Stop duplicate behavioral database writes.
   - `AuthViewModel` collects `keystrokeCollector.keystrokeEvents` forever and saves every emitted event, even after login.
   - `BankingViewModel` also collects and saves keystrokes after login. This can duplicate non-baseline keystrokes.
   - `BankingViewModel` does not store/cancel the keystroke collection job, so repeated sessions can add additional collectors.
   - `ExperimentViewModel.startSession()` starts sensor and keystroke collection jobs but does not retain/cancel them between sessions.
   - Fix by storing all collection jobs, cancelling them on stop/reset/logout, and filtering events by active `sessionId` and baseline mode where appropriate.
   - Files: `AuthViewModel.kt`, `BankingViewModel.kt`, `ExperimentViewModel.kt`, `KeystrokeCollector.kt`.

4. Make the ML path honest and functional.
   - `AuthViewModel` creates `PinKeystrokeEvent`s from username/password typing using `index % 10`, not real PIN digits.
   - `setMLEnrollmentBaseline()` is called before touch/motion collection has meaningful login data, so the ML baseline is incomplete.
   - `BehaviorAnalyzer.performMLAssessment()` uses `keystrokeCollector.getBaselineKeystrokes()` as the current session PIN data, so ML compares baseline-like data against itself instead of current user behavior.
   - `MLModelInference.classify()` fails open with `Pair(true, 0f)` if prediction fails.
   - Fix by either:
     - using the existing `CustomPinPad` for login/enrollment and collecting current comparable PIN features during protected actions, or
     - disabling ML in the banking demo until a proper experiment enrollment profile exists, while keeping the statistical detector active.
   - Failed model inference should be neutral/unavailable, not "genuine".
   - Files: `AuthViewModel.kt`, `BehaviorAnalyzer.kt`, `MLFeatureExtractor.kt`, `MLModelInference.kt`, `CustomPinPad.kt`, login UI as needed.

5. Use recent windows and sample gates for risk analysis.
   - Constants such as `MIN_KEYSTROKE_SAMPLES`, `MIN_TOUCH_SAMPLES`, `MIN_MOTION_SAMPLES`, and `ANALYSIS_WINDOW` exist but are mostly unused.
   - Current DAOs return all-session averages, so anomalies are diluted over time.
   - Touch/motion baselines are built from the same live session after 10 seconds, then the same all-session data is used for future comparisons.
   - Add DAO methods for recent non-baseline samples and sample counts. Risk assessment should compare an immutable baseline window against the latest window, and skip/mark signals as "insufficient data" until enough samples exist.
   - Files: `BehavioralDao.kt`, `BehavioralRepository.kt`, `BehaviorAnalyzer.kt`.

## Banking Logic Bugs

6. Make transfers transactionally correct.
   - `UserRepository.transferFunds()` debits sender but never validates that the recipient account exists.
   - It does not credit the recipient or create a receiver transaction.
   - Debit and transaction insert are not atomic.
   - The UI also subtracts local balance after repository success, which can drift from Room state.
   - Fix with a Room `@Transaction` operation or repository-level transaction support. Validate recipient, block self-transfer, enforce amount format/limits, and represent money as paise/Long or BigDecimal-style integer units rather than `Double`.
   - Files: `UserDao.kt`, `UserRepository.kt`, `BankingViewModel.kt`, `TransferScreen.kt`, models if needed.

7. Add a real re-authentication flow.
   - High risk currently only shows a dialog.
   - Implement a lightweight PIN/password re-auth screen/dialog. On success, reset or lower risk and continue. On failure or timeout, logout.
   - Keep the demo simple but make the security response real.

## Security / Privacy Credibility

8. Do not claim security mechanisms that are not implemented.
   - `androidx.security:security-crypto` is included but not used.
   - Room database is plaintext.
   - Demo passwords are stored as plaintext in a field named `passwordHash`.
   - Either implement basic demo-safe hashing for seeded users and clearly label the database as demo-local, or rename/comment honestly. Do not imply real banking-grade storage unless you add proper encryption.
   - Files: `SecureBankDatabase.kt`, `UserModels.kt`, `UserDao.kt`, `UserRepository.kt`, README/report docs.

9. Remove destructive migration from the normal path.
   - `SecureBankDatabase` uses `.fallbackToDestructiveMigration()`.
   - Add proper Room migrations from schema 1 to 2 or gate destructive migration behind debug-only/demo reset behavior.
   - Files: `SecureBankDatabase.kt`, `app/schemas/...`.

10. Protect exported behavioral data.
   - `collected_data/` contains participant-like behavioral CSVs. The `.gitignore` currently has `collected_data/` commented out.
   - Keep a tiny anonymized sample if needed, but do not track bulk/sensitive data. Update `.gitignore`.
   - Data export CSV should escape string fields and clearly indicate export location.
   - Files: `.gitignore`, `DataExporter.kt`, `collected_data/`.

## UI / UX Polish

11. Fix mojibake / encoding damage.
   - Many strings show corrupted characters such as `âš`, `â€¢`, `â‚¹`, `âœ“`, and box-drawing artifacts in README.
   - Replace with clean ASCII or valid UTF-8. Prefer ASCII in code strings if unsure.
   - Files include `README.md`, `LoginScreen.kt`, `DashboardScreen.kt`, `TransferScreen.kt`, `CustomPinPad.kt`, `SecurityComponents.kt`, `ExperimentScreens.kt`, `BehaviorAnalyzer.kt`, `MLFeatureExtractor.kt`, `BehavioralData.kt`.

12. Hide or frame debug/demo controls professionally.
   - `debugMode` defaults to true and the dashboard exposes reviewer simulation controls directly.
   - For a college demo, keep them, but label them clearly as "Demo Controls" and make debug mode opt-in from a toolbar/dev toggle. The first impression should be a banking app, not an internal console.
   - Files: `BankingViewModel.kt`, `DashboardScreen.kt`, `SecurityComponents.kt`.

13. Remove dead-looking UI.
   - Dashboard "History" button is a TODO.
   - "See All" does nothing.
   - Either implement simple history screens or remove/disable these controls with a polished unavailable state.
   - Files: `DashboardScreen.kt`, `NavGraph.kt`.

14. App naming should be submission-ready.
   - `strings.xml` has `app_name` as "SecureBank Dev".
   - Rename to "SecureBank" or "SecureBank Demo" consistently.

15. Make security alert copy match behavior.
   - If critical: no "Continue Session".
   - If high: say "Verify Identity" and actually verify.
   - If medium: prefer in-app banner over debug-only toast so behavior is visible in demo mode.

## Research / Report Credibility

16. Align docs with actual implementation.
   - README claims forced logout and real-time continuous monitoring; current app does not fully enforce that.
   - README says "rolling averages" / "analysis window" conceptually, but implementation uses broad session averages.
   - Paper/report discuss 8 real + 50 synthetic participants; keep that disclosure, but avoid overstating generalization.
   - If the app ML path is not fixed, docs must say the deployed banking demo uses statistical detection plus demo simulations, while the experiment module trains/evaluates ML offline.

17. Avoid model-metric confusion.
   - `behavioral_auth_model.json` contains `training_accuracy: 0.9815` and `training_samples: 379`.
   - Do not present training accuracy as deployed validation accuracy. The paper reports deployed MLP accuracy around 87.60%; keep docs consistent.

18. Validate the synthetic-data evaluation.
   - If time allows, add a note or script option for participant-grouped validation, because random session-level cross-validation can leak participant identity patterns between folds.
   - Files: `research/ml_model.py`, reports.

## Repository Hygiene

19. Stop tracking generated/local files.
   - `.gradle/`, `.idea/`, `app/build/`, and `local.properties` appear tracked despite `.gitignore`.
   - Remove them from git index while preserving local files:
     `git rm -r --cached .gradle .idea app/build local.properties`
   - Commit only source, docs, schemas, small deterministic assets, and intended research artifacts.

20. Add meaningful tests.
   - Current test coverage is basically `GsonTest`.
   - Add unit tests for:
     - `BehaviorAnalyzer` recommendation thresholds and forced logout behavior.
     - ML inference invalid-input behavior.
     - transfer validation and atomic balance updates.
     - keystroke collector start/stop/session filtering.
   - Keep tests deterministic and runnable with `.\gradlew.bat testDebugUnitTest`.

## Acceptance Criteria

The project is ready after:

- `.\gradlew.bat testDebugUnitTest` passes.
- A debug APK builds.
- Login, dashboard, transfer, risk warning, re-auth, and critical logout can be demonstrated.
- Background/resume does not silently kill monitoring.
- Behavioral event counts do not double after logout/login or experiment sessions.
- README and reports no longer contain garbled text or claims contradicted by the app.
- Git status does not show generated build artifacts as tracked changes.
- The demo still clearly communicates the core idea: behavioral drift after login increases risk and can interrupt/terminate the session.
