# Opus Implementation Prompt: Hybrid ML + Statistical Behavioral Security

You are working in the Android/Kotlin project:

`C:\Users\Tejus Kapoor\Desktop\secure_banking\major_project_v1_Tejus`

Build a stronger behavioral authentication system for the SecureBank demo. The final design must include ML because the project and research paper position this as an ML-based behavioral biometrics system, but it should not rely only on ML for every decision. Use a hybrid design:

- Statistical/heuristic scoring for immediate, explainable, real-time anomalies.
- ML scoring for user-level similarity/impostor detection once a proper enrollment baseline exists.
- A fusion layer that combines both signals into risk levels and security actions.

The goal is not just to lower thresholds. The goal is to make the app genuinely more advanced, more sensitive, and more believable for demo/research evaluation.

## Current Problems To Fix

From code inspection:

- Gyroscope/orientation feels good because motion is processed in real time through `BehaviorAnalyzer.processRealTimeMotion()` from `BankingViewModel`.
- Touch/pressure feels weak because touch events are saved and only evaluated later by periodic assessment.
- Touch statistical scoring currently uses only average pressure and average swipe velocity.
- The collector already records richer touch data: pressure, size, duration, velocity, acceleration, hold duration, touch area, gesture type, coordinates.
- Touch-only anomalies are underweighted: `TOUCH_WEIGHT = 0.30`, medium risk starts at `0.40`, and smoothing makes severe touch-only anomalies stay low.
- Pressure is clipped to `0f..1f`, and Compose touch size is hardcoded to `1f`, so pressure is device-dependent and often saturated.
- Banking login currently skips ML enrollment because it uses password login instead of the custom PIN pad.
- Existing ML features do not sufficiently include pressure, touch area/proxy, hold duration, and richer gesture patterns.
- Touch/motion baseline is currently learned from the same live protected session after 10 seconds, which can contaminate the baseline if the user switches early.
- Device-state/rest detection uses low severity and whole-session most-common state instead of recent-window state.

Relevant files:

- `app/src/main/java/com/securebank/app/domain/BehaviorAnalyzer.kt`
- `app/src/main/java/com/securebank/app/domain/MLFeatureExtractor.kt`
- `app/src/main/java/com/securebank/app/domain/MLModelInference.kt`
- `app/src/main/java/com/securebank/app/domain/FeatureExtractor.kt`
- `app/src/main/java/com/securebank/app/ui/viewmodel/BankingViewModel.kt`
- `app/src/main/java/com/securebank/app/ui/viewmodel/AuthViewModel.kt`
- `app/src/main/java/com/securebank/app/ui/viewmodel/ExperimentViewModel.kt`
- `app/src/main/java/com/securebank/app/sensor/TouchDataCollector.kt`
- `app/src/main/java/com/securebank/app/sensor/SensorDataCollector.kt`
- `app/src/main/java/com/securebank/app/ui/components/TouchCaptureWrapper.kt`
- `app/src/main/java/com/securebank/app/data/model/BehavioralData.kt`
- `app/src/main/java/com/securebank/app/data/model/UserModels.kt`
- `app/src/main/java/com/securebank/app/data/repository/UserRepository.kt`
- `app/src/main/java/com/securebank/app/data/repository/BehavioralRepository.kt`
- `app/src/main/java/com/securebank/app/data/local/dao/BehavioralDao.kt`
- `app/src/main/java/com/securebank/app/data/local/dao/UserDao.kt`
- `research/ml_model.py`
- `research/synthetic_data_generator.py`

## Product Direction

Implement user-level enrollment during signup/account creation.

The demo app should allow multiple users to create accounts. During signup, collect normal banking/account details and a behavioral enrollment profile. Future sessions should compare live behavior against the stored profile for that specific user.

The app should support:

- Seeded/demo users still working.
- New users creating accounts.
- Each user having their own behavioral baseline.
- A guided calibration flow during signup.
- ML baseline readiness based on real enrollment data, not fake status.
- Live risk monitoring after login.
- Security responses:
  - low risk: continue,
  - medium risk: warn or ask "Are you the same user?",
  - high risk: require reauthentication,
  - critical/repeated high risk: force logout.

## Account Signup Requirements

Add or improve signup/account creation.

- Add a route/screen for account creation if missing.
- Let a demo user enter:
  - full name,
  - username,
  - password,
  - PIN,
  - optional/generated account number,
  - starting balance or default balance,
  - any fields required by the existing `User` model.
- Validate:
  - duplicate username,
  - duplicate account number if account numbers are user-controlled,
  - password/PIN minimum requirements,
  - enrollment completion before account is fully active.
- Persist the new user in the existing database/repository pattern.
- Keep seeded users usable.

## Guided Behavioral Enrollment

During signup, guide the user through a short calibration flow. Do not silently learn from the protected banking session.

Capture:

- PIN entry rhythm using the custom PIN pad where possible.
- Touch pressure.
- Touch duration.
- Hold duration.
- Touch area/proxy.
- Tap rhythm.
- Gesture mix.
- Swipe speed.
- Swipe acceleration.
- Swipe direction consistency.
- Gyroscope and accelerometer handling.
- Pitch/roll holding posture.
- Device state over a few seconds.

Suggested guided tasks:

1. Enter the chosen PIN 2-3 times.
2. Tap several on-screen targets/buttons.
3. Perform 2-3 guided swipes in a test area.
4. Hold the phone normally for 3-5 seconds.

Quality gates:

- Require minimum samples before accepting enrollment.
- Reject or ask retry if samples are too inconsistent or too sparse.
- If sensors are unavailable, degrade gracefully and mark motion baseline unavailable.
- Do not mark ML ready until enough enrollment data exists.

## Baseline Storage

Store a user-level behavioral baseline/profile.

Prefer derived statistics instead of raw long-term data where practical:

- pressure mean/std/MAD,
- touch area/proxy mean/std/MAD,
- duration mean/std/MAD,
- hold duration mean/std/MAD,
- velocity mean/std/MAD,
- acceleration mean/std/MAD,
- tap/swipe/long-press ratios,
- inter-touch interval stats,
- PIN dwell/flight stats,
- pitch/roll mean/std,
- gyro magnitude stats,
- accel magnitude stats,
- common/recent device state baseline.

If schema changes are too large for one pass, implement a minimally invasive profile object or table that can be loaded by `BehaviorAnalyzer`, then leave clear TODOs for full normalization.

Important:

- The protected banking session must not overwrite the signup baseline silently.
- Adaptive updates are allowed only after successful verification and should be explicitly separated from the immutable enrollment baseline.
- If no baseline exists, show baseline/ML as "not ready" and use conservative fallback rules.

## ML Requirements

ML must be included meaningfully. The project should not become purely statistical.

Implement ML as a user-similarity/impostor signal based on enrollment baseline versus current session features.

Required ML design:

- Use signup/enrollment samples to call or support `BehaviorAnalyzer.setMLEnrollmentBaseline(...)`.
- Prefer real `PinKeystrokeEvent` data from a custom PIN pad over synthetic keystrokes.
- Compute current-session features from recent PIN/typing, touch, and motion samples.
- Feed enrollment-relative deviation features into the existing ML inference path.
- Expose ML status accurately:
  - model loaded,
  - enrollment baseline available,
  - feature count expected,
  - feature count extracted,
  - prediction,
  - confidence,
  - ML risk.

Feature improvements:

- Add touch pressure features.
- Add touch area/proxy features.
- Add hold duration features.
- Add long-press ratio.
- Add richer gesture mix features.
- Add swipe direction/angle consistency if feasible.
- Add motion magnitude and gyro variability features if not already present.

Research pipeline:

- Update `research/ml_model.py` to mirror any new feature names/order.
- Update `research/synthetic_data_generator.py` so synthetic pressure and size are not constant `1.0`.
- Generate participant-specific pressure/touch-size distributions so ML can actually learn pressure/area differences.
- If model retraining/assets are too large for this pass, keep old model compatibility and clearly gate new ML features until a retrained model exists.

Hybrid fusion:

- Statistical risk catches immediate touch/motion anomalies.
- ML risk contributes when model + enrollment baseline are available.
- If ML is unavailable, the app must still work through statistical scoring, but UI must not pretend ML is active.
- Do not let a confident "genuine" ML prediction fully suppress severe real-time statistical anomalies.
- Do not let one noisy ML prediction force logout without statistical support or repeated evidence unless confidence is extremely high.

## Real-Time Touch Scoring

Add immediate real-time touch scoring similar to motion.

Add:

`BehaviorAnalyzer.processRealTimeTouch(touchData: TouchData): SecurityRecommendation`

Call it inside `BankingViewModel` when `touchDataCollector.touchEvents` emits, after updating live touch state and saving the touch.

It should score:

- pressure,
- touch area/proxy,
- duration,
- hold duration,
- velocity,
- acceleration,
- gesture type,
- abnormal repeated long press,
- abnormal repeated high-pressure taps,
- abnormal swipe speed/gesture rhythm.

Use controlled smoothing/decay:

- Strong one-off anomalies should visibly raise risk.
- Repeated suspicious touches should escalate.
- Normal touches should decay risk gradually.
- Avoid jittery false positives.

Touch-only severe behavior must be able to reach at least `MEDIUM` or `HIGH` depending on severity/persistence. Do not keep a design where touch can never cross action thresholds alone.

## Periodic Touch Scoring

Improve `analyzeTouchPatterns()`.

Do not use only average pressure and swipe velocity. Use a recent window and robust per-feature deviations:

- pressure,
- touch area/proxy,
- duration,
- hold duration,
- velocity,
- acceleration,
- tap ratio,
- swipe ratio,
- long-press ratio,
- inter-touch interval,
- possibly spatial entropy/location consistency.

Use per-modality thresholds instead of one global `LOW_THRESHOLD`.

Prefer robust z-score/MAD where possible because touch values can be noisy.

## Gyroscope And Rest Detection

Keep the existing real-time gyroscope/orientation path.

Improve rest/device-state detection:

- Add DAO/repository support for most-common device state within a recent limited window.
- Do not use whole-session most-common state for recent risk.
- Raise sensitivity for sustained HELD_IN_HAND -> ON_TABLE or HELD_IN_HAND -> STATIONARY changes.
- Require persistence over a few samples/seconds to avoid false positives.
- Keep motion/orientation responsive.

## Security Actions

Use tiered decisions:

- `LOW`: continue monitoring.
- `MEDIUM`: show warning or "Are you the same user?" prompt.
- `HIGH`: require reauthentication/PIN.
- `CRITICAL`: force logout.

For touch:

- one moderate anomaly should not force logout.
- one extreme anomaly may prompt verification.
- repeated severe touch anomalies may force logout.

For motion:

- preserve current strong response for obvious orientation hijack behavior.

For ML:

- ML impostor confidence should increase risk.
- ML confidence should be shown in debug/explainability.
- ML should not be used if enrollment baseline is missing.

## Explainability

Keep or improve debug explainability.

The UI should be able to show:

- statistical risk,
- ML risk,
- final fused risk,
- top contributing modality,
- top anomalous features,
- baseline vs current values,
- whether ML is ready,
- whether baseline is provisional or enrolled,
- recommended action.

This is important for project demonstration and teacher evaluation.

## Tests

Add focused tests where practical.

Minimum desirable tests:

- normal touch remains low risk,
- extreme pressure/hold duration raises risk,
- repeated abnormal touches escalate,
- severe touch-only anomaly is not hidden by smoothing,
- signup creates a new user,
- duplicate username/account validation works,
- enrollment baseline is persisted,
- login loads the correct user's baseline,
- ML ready is false without enrollment data,
- ML ready becomes true when enough enrollment data is available,
- recent device-state query uses recent window.

If Room/instrumented tests are too heavy, add unit tests for pure scoring functions and keep DB tests minimal.

## Constraints

- Preserve existing seeded/demo users.
- Do not delete or revert unrelated dirty files or generated artifacts.
- Keep edits scoped to source files/tests/research scripts needed for this feature.
- Prefer small, explainable heuristics plus ML fusion over a brittle giant rewrite.
- Do not hardcode demo-only fake ML readiness.
- Do not claim ML is active unless model + enrollment baseline are actually available.
- Keep the app buildable with `./gradlew.bat assembleDebug`.
- Keep research scripts compatible or document required retraining steps.

## Expected Outcome

After implementation:

- New users can create accounts.
- Signup includes behavioral enrollment.
- Each user has a stored behavioral profile.
- Live banking sessions compare against the signup/enrollment baseline.
- Gyroscope responsiveness remains strong.
- Touch pressure, long holds, abnormal swipe speed, gesture rhythm, and touch area/proxy affect risk quickly.
- Touch-only severe behavior can trigger verification/high risk.
- ML is meaningfully integrated when enrollment data exists.
- Debug/explainability clearly shows statistical vs ML contribution.
- The project story becomes: "SecureBank enrolls each user during account creation, then continuously verifies identity using hybrid ML and real-time behavioral biometrics across touch, typing, and motion."
