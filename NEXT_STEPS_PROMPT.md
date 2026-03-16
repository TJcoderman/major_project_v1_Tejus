# Future LLM Implementation Prompt — SecureBank Project

> **Instructions**: Copy this entire file as your first message to a new LLM session (Claude, GPT, etc.) working on this project. It contains everything needed to continue development.

---

## Context

You are working on **SecureBank**, an Android app (Kotlin/Jetpack Compose) that detects session hijacking using behavioral biometrics (keystroke dynamics, touch patterns, motion sensors). The project includes a Python ML pipeline for training and a pure Kotlin on-device inference engine.

**Read `SKILLS.md` first** — it contains the full project map, file locations, patterns, and gotchas.

**Current state**: The ML model is trained (MLP, 90.8% accuracy, 97.9% AUC) and integrated into the Android app via a hybrid Z-score + ML detection system in `BehaviorAnalyzer.kt`. The app compiles with zero IDE diagnostics but has not been tested on a real device yet.

---

## Tasks to Implement (in priority order)

### TASK 1: Fix Plaintext Password Storage (CRITICAL SECURITY)

**Problem**: `data/model/UserModels.kt` has a field called `passwordHash` that stores passwords in **plaintext**. `SecureBankDatabase.kt` seeds mock users with plaintext strings.

**What to do**:
1. Read `data/model/UserModels.kt` and find the `User` data class
2. Read `data/local/SecureBankDatabase.kt` and find the seed data callback
3. Read `data/repository/UserRepository.kt` to find the `authenticate()` method
4. Implement PBKDF2 password hashing:
   - Create a utility `SecurityUtils.kt` in the `domain/` package
   - Add `fun hashPassword(password: String, salt: ByteArray): String` using `PBKDF2WithHmacSHA256`, 10000 iterations, 256-bit key
   - Add `fun generateSalt(): ByteArray` (16 bytes from SecureRandom)
   - Add `fun verifyPassword(password: String, salt: ByteArray, expectedHash: String): Boolean`
5. Add a `salt` field to the `User` entity (this is a schema change — bump DB version)
6. Update `SecureBankDatabase` seed data to hash the mock passwords
7. Update `UserRepository.authenticate()` to verify using hash comparison instead of plaintext
8. **IMPORTANT**: Since the DB uses `fallbackToDestructiveMigration()`, the schema change will wipe data — this is acceptable for a research prototype

**Files to modify**: `UserModels.kt`, `SecureBankDatabase.kt`, `UserRepository.kt`
**Files to create**: `domain/SecurityUtils.kt`

---

### TASK 2: Write Unit Tests for ML Components

**Problem**: Zero tests exist. The ML inference pipeline is the most critical path to validate.

**What to do**:
1. Check `app/build.gradle.kts` for existing test dependencies (JUnit, Coroutines Test should be there)
2. Create test files in `app/src/test/java/com/securebank/app/domain/`:

**Test file 1: `MLModelInferenceTest.kt`**
- Test that `loadModel()` returns true when model JSON is available (you'll need to mock the Android Context or use Robolectric)
- Test that `predict()` returns a value in [0.0, 1.0] range
- Test that `classify()` returns `(true, confidence)` when genuine probability > 0.5
- Test that `classify()` returns `(false, confidence)` when genuine probability <= 0.5
- Test that `getFeatureNames()` returns exactly 124 feature names
- Test that `getExpectedFeatureCount()` returns 124

**Test file 2: `MLFeatureExtractorTest.kt`**
- Create mock `PinKeystrokeEvent`, `TouchData`, `MotionData` lists
- Test that `setEnrollmentBaseline()` makes `hasEnrollmentBaseline()` return true
- Test that `computeDeviationFeatures()` returns null before baseline is set
- Test that `computeDeviationFeatures()` returns a FloatArray of size matching featureOrder
- Test that `clearBaseline()` makes `hasEnrollmentBaseline()` return false
- Test deviation math: if session == enrollment, all absolute deviations should be ~0

**Test file 3: `BehaviorAnalyzerTest.kt`**
- Test that `isMLReady()` returns false before initialization
- Test hybrid blending math: verify `(1-0.6)*zScore + 0.6*mlScore` formula
- Test that `reset()` clears all state

**Run with**: `./gradlew testDebugUnitTest`

---

### TASK 3: Add ML Status Indicator to Banking Dashboard UI

**Problem**: The user has no visibility into whether ML detection is active or falling back to Z-score only.

**What to do**:
1. Read `ui/viewmodel/BankingViewModel.kt` to understand the current risk display flow
2. Read `ui/screens/` to find the banking dashboard composable
3. Add an ML status indicator that shows:
   - "ML Active" (green) when `behaviorAnalyzer.isMLReady()` is true
   - "Statistical Only" (yellow) when ML is not ready (fallback mode)
4. Expose `isMLReady` as a `StateFlow<Boolean>` from BankingViewModel
5. Add a small chip/badge next to the existing risk score display
6. Keep it minimal — just a text indicator, no new screens

**Files to modify**: `BankingViewModel.kt`, the banking dashboard screen composable
**Pattern**: Follow existing StateFlow pattern used by `currentRiskLevel`

---

### TASK 4: Verify BankingViewModel Periodic Assessment Loop

**Problem**: `BehaviorAnalyzer.performRiskAssessment()` should be called every 15 seconds during active banking sessions. Need to verify this loop exists and works with the new hybrid system.

**What to do**:
1. Read `ui/viewmodel/BankingViewModel.kt` fully
2. Look for a periodic coroutine/timer that calls `performRiskAssessment()`
3. If it exists: verify it passes the correct `sessionId` and handles the returned `RiskAssessment`
4. If it does NOT exist: implement it:
   - In `init{}` or on session start, launch a coroutine in `viewModelScope`
   - Use `while(isActive) { delay(15_000); behaviorAnalyzer.performRiskAssessment(sessionId) }`
   - Collect `behaviorAnalyzer.riskAssessment` SharedFlow to update UI state
   - Cancel on logout/session end
5. Verify that `updateInitialBaseline()` is called after the first few seconds to update touch/motion defaults

---

### TASK 5: Add Database Encryption with SQLCipher

**Problem**: Behavioral biometric data is stored unencrypted in Room DB. This is sensitive PII.

**What to do**:
1. Add SQLCipher dependency to `app/build.gradle.kts`:
   ```
   implementation("net.zetetic:android-database-sqlcipher:4.5.4")
   implementation("androidx.sqlite:sqlite-ktx:2.4.0")
   ```
2. Read `data/local/SecureBankDatabase.kt` — find `getInstance()` method
3. Replace the Room builder with SQLCipher-backed builder:
   ```kotlin
   val passphrase = SQLiteDatabase.getBytes("your-key-derivation-here".toCharArray())
   val factory = SupportFactory(passphrase)
   Room.databaseBuilder(context, SecureBankDatabase::class.java, DB_NAME)
       .openHelperFactory(factory)
       .build()
   ```
4. For key management, use `AndroidKeyStore` to generate/store the encryption key — do NOT hardcode it
5. Create a `KeyStoreManager.kt` in `domain/` that handles key generation and retrieval
6. **WARNING**: This will make existing unencrypted databases unreadable — the destructive migration fallback handles this

**Files to modify**: `build.gradle.kts`, `SecureBankDatabase.kt`
**Files to create**: `domain/KeyStoreManager.kt`

---

### TASK 6: Add Session Timeout and Rate Limiting

**Problem**: No session timeout means an idle authenticated session stays open forever. No rate limiting on login means brute-force is possible.

**Session timeout**:
1. Read `ui/viewmodel/BankingViewModel.kt`
2. Add an inactivity timer (5 minutes default):
   - Track last user interaction timestamp
   - Launch a coroutine that checks every 30 seconds
   - If `currentTime - lastInteraction > TIMEOUT_MS`, trigger auto-logout
   - Reset timer on any touch/keystroke event
3. Show a warning dialog 30 seconds before timeout
4. On timeout, call `authViewModel.logout()` equivalent

**Rate limiting**:
1. Read `ui/viewmodel/AuthViewModel.kt`
2. Add login attempt tracking:
   - Max 5 failed attempts in 5 minutes
   - After limit: show "Too many attempts. Try again in X minutes"
   - Store attempt timestamps in a simple list (in-memory is fine for prototype)
   - Disable the login button during lockout
3. Reset counter on successful login

**Files to modify**: `AuthViewModel.kt`, `BankingViewModel.kt`, possibly the login screen composable

---

### TASK 7: Add Screenshot Prevention and Root Detection

**Screenshot prevention**:
1. Read `MainActivity.kt`
2. Add `window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)` in `onCreate()`
3. This prevents screenshots and screen recording of the banking app

**Root detection** (basic):
1. Create `domain/DeviceSecurityChecker.kt`
2. Check for common root indicators:
   - `su` binary existence (`/system/bin/su`, `/system/xbin/su`)
   - Build tags containing "test-keys"
   - Presence of root management apps (Magisk, SuperSU)
3. Return a `DeviceSecurityStatus` (SECURE, ROOTED, EMULATOR)
4. Show a warning banner on the banking screen if device is rooted (don't block — just warn)
5. Include the security status in risk assessment as an additional factor

---

### TASK 8: Collect More Real Participant Data

**Problem**: Only 8 real participants (P01-P08). The synthetic data helps but a research paper needs more real data (target: 20-30 participants).

**What to do**:
1. The experiment protocol is already built in the app (ExperimentViewModel + ExperimentScreens)
2. Recruit participants and run them through the existing experiment flow:
   - Standard PIN: `382946` (defined in `PinConfig`)
   - 3 enrollment sessions, 3 genuine sessions, 3 impostor sessions per participant
3. Export data via the built-in CSV exporter
4. Place exported CSVs in `research/synthetic_data/P09/`, `P10/`, etc. (or `collected_data/`)
5. Re-run `cd research && PYTHONIOENCODING=utf-8 python ml_model.py` to retrain with new data
6. Copy the new model JSON to `app/src/main/assets/ml/`

**This is a manual task for the project owner, not an LLM task.**

---

### TASK 9: Update Research Paper with ML Results

**Problem**: The research paper (IEEE format, likely in `docs/` or project root) needs to reflect the new ML integration.

**What to add to the paper**:
1. **Methodology section**: Describe the hybrid detection architecture (Z-score + MLP)
2. **Data augmentation section**: Describe synthetic data generation (Ornstein-Uhlenbeck process for motion, cross-participant impostor sessions, per-digit rhythm offsets)
3. **Results table**:
   | Model | Accuracy | AUC | EER |
   |-------|----------|-----|-----|
   | Random Forest | 90.8% | 97.9% | 8.7% |
   | MLP (deployed) | 87.6% | 93.8% | — |
   | One-Class SVM | — | — | — |
4. **Architecture diagram**: Login → Keystroke/Touch/Motion collection → Feature extraction (124 enrollment-relative deviations) → MLP inference → Hybrid blending with Z-score → Risk level → Security recommendation
5. **On-device inference section**: Explain the pure Kotlin approach (no TFLite), JSON weight export, ~565KB model size, matrix multiplication forward pass
6. **Limitations**: Synthetic data dominates training set, software keyboard dwell time estimation, small real participant pool

---

### TASK 10: End-to-End Integration Test on Device

**After all code changes above**, run this verification checklist on a physical Android device:

1. **Build**: `./gradlew assembleDebug` — must succeed
2. **Install**: `adb install app/build/outputs/apk/debug/app-debug.apk`
3. **Login flow**: Enter credentials, check Logcat for:
   - `"ML model loaded successfully (124 features)"`
   - `"ML enrollment baseline set (X keystrokes, Y touches, Z motion samples)"`
4. **Banking session**: Navigate around, wait 15+ seconds, check Logcat for:
   - `"ML assessment: genuine=true, confidence=0.XX, risk=0.XXX"`
5. **Risk escalation**: Have a different person use the app after login — risk score should increase
6. **Logout + re-login**: Verify ML state resets properly
7. **Session timeout**: Leave app idle for 5 minutes — should auto-logout (after Task 6)
8. **Rate limiting**: Enter wrong password 5 times — should lock out (after Task 6)

---

## Important Notes for the Implementing LLM

- **Always read `SKILLS.md` first** — it has the file map and patterns
- **Always read a file before modifying it** — understand existing code
- **Run `./gradlew assembleDebug`** after changes to verify compilation (requires Android SDK 34)
- **This is a Kotlin/Compose project** — use Hilt for DI, StateFlow for state, coroutines for async
- **Don't add TFLite** — the project deliberately uses pure Kotlin inference to avoid the dependency
- **Don't modify the ML model weights** (`behavioral_auth_model.json`) — retrain via Python if needed
- **The database uses destructive migration** — schema changes wipe data, which is acceptable
- **Preserve the hybrid detection approach** — Z-score + ML blending, with graceful fallback to Z-score only when ML is unavailable
- **Check `docs/security-audit.md`** for the full security recommendations list (20 items)
