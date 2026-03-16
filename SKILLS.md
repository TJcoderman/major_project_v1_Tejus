# SKILLS.md - LLM Reference Guide for SecureBank Project

> This file helps future LLMs (Claude, GPT, Copilot, etc.) quickly understand and work with this codebase.

## Project Identity
- **Name**: SecureBank - Behavioral Biometric Authentication for Mobile Banking
- **Goal**: Detect session hijacking using keystroke dynamics, touch patterns, and motion sensors
- **Type**: Android app (Kotlin/Compose) + Python ML pipeline
- **Stage**: Research prototype with data from 8 real participants + 50 synthetic, ML model integrated on-device

## Quick Start for an LLM

### "I need to modify the Android app"
- Entry: `app/src/main/java/com/securebank/app/MainActivity.kt`
- DI: Hilt-based, all providers in `di/AppModule.kt`
- DB: Room, schema at `app/schemas/`, entities in `data/model/`
- Navigation: `ui/navigation/NavGraph.kt` (Compose Navigation)
- Build: `./gradlew assembleDebug` (requires Android SDK 34)

### "I need to modify behavioral data collection"
- Keystroke: `sensor/KeystrokeCollector.kt` (dwell/flight time)
- Touch: `sensor/TouchDataCollector.kt` (tap/swipe classification)
- Motion: `sensor/SensorDataCollector.kt` (accel + gyro with noise filter)
- All collectors are `@Singleton` injected via Hilt
- Data stored via `data/repository/BehavioralRepository.kt` -> Room DAOs

### "I need to modify the risk scoring / anomaly detection"
- Core engine: `domain/BehaviorAnalyzer.kt`
- **HYBRID system**: Z-score deviation (original) + ML neural network (MLP)
- Blending: `(1 - ML_WEIGHT) * Z-score + ML_WEIGHT * ML prediction` where ML_WEIGHT=0.6
- Z-score weights: Keystroke 35%, Touch 30%, Motion 35%
- ML model: 124 enrollment-relative deviation features -> 128->64->32->1 MLP (sigmoid)
- Real-time motion scoring + periodic full assessment (15s interval)
- Thresholds: LOW<0.2, MEDIUM<0.4, HIGH<0.6, CRITICAL<0.8
- ML components: `MLModelInference.kt` (neural net forward pass), `MLFeatureExtractor.kt` (deviation features)
- Model weights: `app/src/main/assets/ml/behavioral_auth_model.json` (~565 KB)

### "I need to modify the ML pipeline"
- Main script: `research/ml_model.py`
- Uses enrollment-relative deviation features (not raw features)
- Models: RandomForest, MLP, One-Class SVM (compared automatically)
- Best result: RandomForest 90.8% accuracy, 97.9% AUC, 8.7% EER
- Data: `research/synthetic_data/` (58 participants: P01-P08 real + P09-P58 synthetic)
- Synthetic generator: `research/synthetic_data_generator.py` (50 participants, 3 genuine + 3 impostor sessions each)
- Android export: `research/results/android_model/behavioral_auth_model.json` (MLP weights as JSON)
- Run: `cd research && PYTHONIOENCODING=utf-8 python ml_model.py`
- Retrain + export: runs automatically, copies to `app/src/main/assets/ml/`

### "I need to modify the feature extraction"
- **On-device ML (Kotlin)**: `domain/MLFeatureExtractor.kt` -> 124 enrollment-relative deviation features (matches Python pipeline exactly)
- **On-device legacy (Kotlin)**: `domain/FeatureExtractor.kt` -> 52 raw features (kept for backwards compat, not used by ML)
- **Offline training (Python)**: `research/ml_model.py::extract_raw_features()` + `compute_deviation_features()` -> 124 deviation features
- Feature alignment is maintained: MLFeatureExtractor.kt mirrors the Python pipeline, and uses model's `feature_names` list for correct ordering

### "I need to add a new screen"
1. Create screen composable in `ui/screens/`
2. Add route to `ui/navigation/NavGraph.kt` (sealed class `Screen`)
3. Create ViewModel in `ui/viewmodel/` with `@HiltViewModel`
4. Wire in NavGraph composable

### "I need to modify the experiment protocol"
- Models: `data/model/ExperimentModels.kt`
- ViewModel: `ui/viewmodel/ExperimentViewModel.kt`
- Screens: `ui/screens/ExperimentScreens.kt`
- Export: `data/export/DataExporter.kt`
- Standard PIN: `382946` (PinConfig)
- Tasks: PIN_ENTRY -> TEXT_TYPING -> TOUCH_INTERACTION -> FREE_BROWSING

## Key File Map

| Purpose | File |
|---------|------|
| App entry | `MainActivity.kt` |
| Login flow | `ui/viewmodel/AuthViewModel.kt` |
| Banking + monitoring | `ui/viewmodel/BankingViewModel.kt` |
| Anomaly detection (hybrid) | `domain/BehaviorAnalyzer.kt` |
| ML neural net inference | `domain/MLModelInference.kt` |
| ML feature extraction | `domain/MLFeatureExtractor.kt` |
| Legacy feature extraction | `domain/FeatureExtractor.kt` |
| Keystroke capture | `sensor/KeystrokeCollector.kt` |
| Touch capture | `sensor/TouchDataCollector.kt` |
| Motion capture | `sensor/SensorDataCollector.kt` |
| Data persistence | `data/repository/BehavioralRepository.kt` |
| CSV export | `data/export/DataExporter.kt` |
| ML training | `research/ml_model.py` |
| Synthetic data gen | `research/synthetic_data_generator.py` |
| ML model weights | `app/src/main/assets/ml/behavioral_auth_model.json` |
| Data models | `data/model/BehavioralData.kt` |
| Experiment protocol | `data/model/ExperimentModels.kt` |
| DB schema | `data/local/SecureBankDatabase.kt` |
| DI setup | `di/AppModule.kt` |
| Navigation | `ui/navigation/NavGraph.kt` |

## Common Patterns in This Codebase

1. **StateFlow for UI state**: All ViewModels use `MutableStateFlow` + `asStateFlow()`
2. **SharedFlow for events**: Collectors emit data via `MutableSharedFlow(extraBufferCapacity=100)`
3. **callbackFlow for sensors**: `SensorDataCollector` wraps Android `SensorEventListener` in a Flow
4. **Hilt injection**: Constructor injection with `@Inject`, `@Singleton`, `@HiltViewModel`
5. **Repository pattern**: ViewModels -> Repositories -> DAOs -> Room DB
6. **CSV export**: `FileWriter` with `appendLine()` for each data type

## Critical Gotchas

1. **Passwords are stored in PLAINTEXT** in Room DB (`passwordHash` field is not actually hashed). Must fix before any real deployment.
2. **Software keyboard dwell times are ESTIMATED at 80ms** (constant). Only the custom PIN pad captures real dwell times. The ML model should weight PIN keystrokes more heavily.
3. **Feature extractors**: `MLFeatureExtractor.kt` matches the Python pipeline (124 deviation features). The legacy `FeatureExtractor.kt` (52 raw features) is NOT used by the ML path — kept only for fallback Z-score analysis.
4. **Database uses `fallbackToDestructiveMigration()`** - schema changes wipe all data.
5. **No network layer exists**. All data is local. Export is to device filesystem CSV files.
6. **ML model uses pure Kotlin inference** (no TFLite/ONNX dependency). The model weights are stored as JSON in `assets/ml/` and loaded via Gson. Forward pass is matrix multiplication in `MLModelInference.kt`.
7. **ML enrollment baseline requires >= 6 PIN keystrokes and >= 50 motion samples**. If insufficient data at login, the system falls back to Z-score only (graceful degradation).
8. **Retraining workflow**: Run `python research/ml_model.py` -> copies JSON to `app/src/main/assets/ml/`. Rebuild app to include updated model.

## Testing

- Unit test dependencies present (JUnit, Coroutines Test, Espresso)
- No tests are currently written
- To test: `./gradlew testDebugUnitTest`
