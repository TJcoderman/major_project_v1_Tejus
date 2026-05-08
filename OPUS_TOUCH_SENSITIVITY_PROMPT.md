# Opus Implementation Prompt: Stronger Behavioral Anomaly Detection

You are working in the Android/Kotlin project:

`C:\Users\Tejus Kapoor\Desktop\secure_banking\major_project_v1_Tejus`

Goal: make behavioral anomaly detection more advanced, more sensitive, and more sustainable, especially for touch/pressure and resting/device-state changes. Gyroscope/orientation already feels responsive, but touch/pressure currently feels subtle and often fails to trigger alerts even when the user presses unusually hard or interacts abnormally.

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
   - If a true enrollment baseline is unavailable, keep defaults clearly marked as provisional and use conservative thresholds until enough genuine samples exist.
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

Constraints:

- Preserve existing public behavior unless directly related to anomaly sensitivity.
- Do not delete or revert unrelated dirty files or generated artifacts.
- Keep changes scoped to source files and tests.
- Prefer small, explainable heuristics over a brittle giant rewrite.
- Keep debug explainability updated so the UI can show why risk changed.
- The app should still build with `./gradlew.bat assembleDebug`.

Expected outcome:

- Aggressive touch pressure, long holds, abnormal touch area/proxy, and unusual gesture rhythm should be visible in risk explainability quickly.
- Touch-only severe behavior should be able to produce at least `MEDIUM` or `HIGH` risk depending on severity/persistence.
- Gyro responsiveness should remain intact.
- Rest/device-state changes should be based on recent state, not whole-session state.
- The design should be sustainable: calibrated per modality, not just globally lowering thresholds.
