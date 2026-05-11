# Opus Implementation Prompt: Stronger Behavioral Anomaly Detection

You are working in the Android/Kotlin project:

`C:\Users\Tejus Kapoor\Desktop\secure_banking\major_project_v1_Tejus`

Goal: make behavioral anomaly detection more advanced, more sensitive, and more sustainable, especially for touch/pressure and resting/device-state changes. Gyroscope/orientation already feels responsive, but touch/pressure currently feels subtle and often fails to trigger alerts even when the user presses unusually hard or interacts abnormally.

Important product direction:

- Move behavioral baseline creation to signup/account creation instead of silently learning from the protected banking session.
- Support multiple users creating new accounts in the demo app.
- During signup, collect banking details, create a PIN/password, and run a guided behavioral enrollment flow.
- Store each user's behavioral profile as an immutable or mostly immutable user-level baseline.
- During login and all banking screens, compare live behavior against that user's stored enrollment baseline.
- If behavior differs enough, prompt the user to verify identity ("Are you the same user?") or force logout for high/critical risk.
- Keep the existing gyroscope/orientation real-time path because it works well; add touch/pressure/swipe/hold detection around it.
- Feed enrollment data into the ML baseline where feasible, especially through the PIN/custom pad path, so ML is actually usable in the banking demo and not only in the experiment module.

Important context discovered in the code:

- Motion works well because `BankingViewModel` calls `BehaviorAnalyzer.processRealTimeMotion()` on every motion sample before DB batching.
- Touch has no real-time path. Touch events are only saved, then evaluated after an initial 10-second delay and then every 15 seconds.
- Touch statistical scoring in `BehaviorAnalyzer.analyzeTouchPatterns()` only uses recent average pressure and recent average swipe velocity.
- The collector already records richer touch data: pressure, size, duration, velocity, acceleration, hold duration, touch area, gesture type, and coordinates.
- Pressure is clipped to `0f..1f`, touch size is hardcoded to `1f` because Compose does not expose size directly, and touch area is basically pressure times `1f`.
- A touch-only anomaly cannot realistically trigger medium/high risk because touch weight is `0.30`, overall medium threshold is `0.40`, and the weighted risk is smoothed. A maxed touch-only deviation starts around `0.09` after smoothing from low risk.
- Banking login skips ML enrollment because it uses password login, not the custom PIN pad. So live banking is usually Z-score only.
- ML touch features omit pressure, touch area, and hold duration.
- Touch/motion baselines are updated from the same live session after 10 seconds, so an impostor can contaminate the baseline if switching happens early.
- Device state changes such as HELD_IN_HAND to ON_TABLE are deliberately low severity, and recent state uses the all-session most common state.

Major UX / account-flow change requested:

1. Add or improve signup/account creation.
   - Let a demo user create a new account from the app UI.
   - Capture normal banking identity fields needed by the current app model, such as full name, username, password/PIN, account number or generated account number, starting balance, and any existing required fields.
   - Validate duplicate usernames/account numbers.
   - Persist the user so multiple accounts can exist and be logged into separately.
   - Keep existing seeded demo users working.

2. Add guided behavioral enrollment during signup.
   - After account details are entered, guide the user through a short calibration/enrollment flow before account creation is considered complete.
   - Capture:
     - PIN entry rhythm through the custom PIN pad where possible.
     - Touch pressure, touch duration, hold duration, touch area/proxy, tap rhythm, and gesture mix.
     - Swipe speed, acceleration, and direction consistency.
     - Gyro/accelerometer holding posture, pitch/roll, and device state over a few seconds.
   - Suggested enrollment tasks:
     - enter the chosen PIN 2-3 times,
     - tap several on-screen targets/buttons,
     - perform 2-3 swipes in a guided area,
     - hold the phone normally for 3-5 seconds.
   - Store the enrollment data as the user's baseline profile.
   - Do not let the protected session overwrite this baseline silently.

3. Use the signup baseline for demo users too.
   - Demo/seed users should either:
     - have precomputed baseline values, or
     - be routed once through the same enrollment calibration before behavioral security is shown as fully ready.
   - Avoid pretending ML is ready if no real enrollment baseline exists.

4. Feed enrollment data to ML baseline.
   - Use the signup/enrollment PIN, touch, and motion samples to call or enable `BehaviorAnalyzer.setMLEnrollmentBaseline(...)`.
   - Prefer using real `PinKeystrokeEvent` data from the custom PIN pad instead of synthetic keystrokes.
   - Make the banking demo capable of ML enrollment by using PIN/custom pad during signup/login where reasonable.
   - If the full ML model cannot be retrained in this pass, still store the data and wire the in-app baseline path cleanly, while keeping inference compatibility.

Relevant files:

- `app/src/main/java/com/securebank/app/domain/BehaviorAnalyzer.kt`
- `app/src/main/java/com/securebank/app/ui/viewmodel/BankingViewModel.kt`
- `app/src/main/java/com/securebank/app/sensor/TouchDataCollector.kt`
- `app/src/main/java/com/securebank/app/ui/components/TouchCaptureWrapper.kt`
- `app/src/main/java/com/securebank/app/data/repository/BehavioralRepository.kt`
- `app/src/main/java/com/securebank/app/data/local/dao/BehavioralDao.kt`
- `app/src/main/java/com/securebank/app/domain/MLFeatureExtractor.kt`
- `app/src/main/java/com/securebank/app/domain/FeatureExtractor.kt`
- `research/ml_model.py`
- `research/synthetic_data_generator.py`

Implement a robust improvement with these priorities:

1. Add immediate real-time touch scoring, analogous to `processRealTimeMotion()`.
   - Add a `BehaviorAnalyzer.processRealTimeTouch(touchData: TouchData): SecurityRecommendation`.
   - Call it in `BankingViewModel` when `touchDataCollector.touchEvents` emits, right after saving/updating live touch state.
   - It should detect large single-event deviations in pressure, touch area, duration, hold duration, velocity, acceleration, and gesture type.
   - It should update current risk with a faster but controlled smoothing factor so obvious touch anomalies visibly affect risk without causing noisy false positives.
   - It should return `SHOW_WARNING`, `REQUEST_REAUTHENTICATION`, or `FORCE_LOGOUT` only when severity and persistence justify it.

2. Replace weak touch scoring with multi-feature touch scoring.
   - Do not rely only on average pressure and swipe velocity.
   - Use a recent window of touches and compute mean/std or robust MAD-style deviation for:
     pressure, touchArea, duration, holdDuration, velocity, acceleration, tap ratio, swipe ratio, long-press ratio, and inter-touch interval.
   - Keep sample gates, but allow immediate scoring after 1-3 suspicious touches for strong anomalies.
   - Avoid a design where touch-only anomalies can never cross medium/high thresholds.
   - Add per-modality escalation: a severe touch anomaly should be able to trigger at least medium/high without waiting for keystroke or motion corroboration.

3. Improve baseline handling.
   - Avoid contaminating touch/motion baseline with the protected live session.
   - Create baseline during signup/enrollment and associate it with the user account, not just the current session.
   - Load the correct user's baseline after login and compare all live data against it.
   - If a true enrollment baseline is unavailable, keep defaults clearly marked as provisional and use conservative thresholds until the user completes enrollment.
   - Do not silently learn the first 10 seconds of a potentially switched user as baseline.
   - If schema changes are too large, implement a minimally invasive baseline stats object inside `BehaviorAnalyzer` first, with clear TODOs/tests.

4. Fix recent device-state analysis.
   - Add DAO/repository support for most-common device state within a recent limited window, not the full session.
   - Raise sustained rest/state-change detection sensitivity while requiring persistence to avoid false positives.
   - HELD_IN_HAND -> ON_TABLE or STATIONARY should not always be only `0.1`; severity should depend on duration/persistence and surrounding motion.

5. Improve ML feature parity only if feasible in this pass.
   - Add touch pressure, touch area, hold duration, long press ratio, and richer gesture mix to Kotlin `MLFeatureExtractor`.
   - Mirror those features in `research/ml_model.py`.
   - Update `research/synthetic_data_generator.py` so synthetic pressure/size are not constant `1.0`; generate realistic participant-specific distributions.
   - If retraining/model assets are out of scope, leave ML changes staged behind clear compatibility guards and do not break current inference.

6. Add tests where practical.
   - Add focused unit tests for touch risk scoring:
     normal touch remains low,
     extreme pressure/hold duration raises risk,
     repeated abnormal touches escalate,
     old smoothing does not hide severe touch-only anomaly.
   - Add repository/DAO test or query-level test for recent device-state windowing if the existing test setup supports Room.
   - Add signup/enrollment tests where practical:
     new user can be created,
     duplicate username/account validation works,
     enrollment baseline is persisted,
     login loads the correct user's baseline,
     ML ready state is false until baseline data exists and true when enough enrollment data is provided.

Constraints:

- Preserve existing public behavior unless directly related to anomaly sensitivity.
- Do not delete or revert unrelated dirty files or generated artifacts.
- Keep changes scoped to source files and tests.
- Prefer small, explainable heuristics over a brittle giant rewrite.
- Keep debug explainability updated so the UI can show why risk changed.
- Do not claim ML is active unless the in-app enrollment baseline is actually available.
- Keep seeded/demo users functional while adding multi-user signup.
- The app should still build with `./gradlew.bat assembleDebug`.

Expected outcome:

- Aggressive touch pressure, long holds, abnormal touch area/proxy, and unusual gesture rhythm should be visible in risk explainability quickly.
- Touch-only severe behavior should be able to produce at least `MEDIUM` or `HIGH` risk depending on severity/persistence.
- Gyro responsiveness should remain intact.
- Rest/device-state changes should be based on recent state, not whole-session state.
- The design should be sustainable: calibrated per modality, not just globally lowering thresholds.
- New demo users can create accounts, complete behavioral enrollment, and get user-specific anomaly detection.
- Live sessions compare against the signup/enrollment baseline, not a baseline learned from the same protected session.
- ML baseline is fed from real enrollment data where feasible, and UI readiness accurately reflects whether ML/statistical baselines exist.
