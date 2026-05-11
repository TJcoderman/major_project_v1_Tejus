# GAMMA AI PRESENTATION PROMPT

> Copy-paste the section below into Gamma.app → "Paste in text" option.

---

## PASTE THIS INTO GAMMA ↓

Create a professional, visually stunning B.Tech Major Project presentation with a dark theme, modern gradients (deep blue to purple), and sleek typography. Use icons, diagrams, and data visualizations wherever possible. The presentation should look like a research conference talk — premium, technical, and impressive. Use 16:9 aspect ratio.

---

**Title Slide:**
Title: "SecureBank: A Hybrid Statistical & Neural Network Framework for Continuous Behavioral Authentication on Mobile Banking Platforms"
Subtitle: Major Project Presentation | B.Tech Computer Science & Engineering
University: Graphic Era (Deemed to be University), Dehradun
Team Members: Tejus Kapoor, Tanush Malhotra, Bela Diwan, Gautam Nautiyal
Department: Computer Science & Engineering

---

**Slide 2 — The Problem:**
Heading: "What Happens After Login?"
Content: Banking apps authenticate you ONCE at login. After that — nothing. The session stays open, and whoever picks up the phone is trusted implicitly. The ECB reported a 23% year-on-year rise in unauthorized mobile transactions (2022). FBI's IC3 flagged the same trend in 2023. The dominant model is "gate-and-forget" — verify once, trust blindly until timeout. This creates a massive security gap for session hijacking attacks.
Visual: Show a timeline — Login (authenticated) → gap → anyone can use → session expires. Highlight the unprotected gap in red.

---

**Slide 3 — Our Solution:**
Heading: "SecureBank — Continuous Behavioral Authentication"
Content: SecureBank is a working Android banking app that silently monitors 3 behavioral channels throughout the session: (1) PIN Keystroke Timing — captured through a custom numeric pad, (2) Touch Interaction Patterns — taps, swipes, scrolls intercepted via Compose's pointer input, (3) Device Orientation — accelerometer and gyroscope data for phone-holding posture. A hybrid detection engine runs entirely on-device with ZERO server dependency. If behavior doesn't match → graduated security response from warning to forced logout.
Visual: Three icons for Keystroke, Touch, Motion feeding into a shield icon labeled "Continuous Protection."

---

**Slide 4 — System Architecture:**
Heading: "5-Layer MVVM Architecture"
Content: Show a layered architecture diagram:
- Layer 1 (Top): UI Layer — Jetpack Compose + Material 3
- Layer 2: ViewModel Layer — AuthVM, BankingVM, ExperimentVM
- Layer 3: Domain Layer — BehaviorAnalyzer, ML Inference, Feature Extractor
- Layer 4: Data Layer — Room SQLite + Repositories
- Layer 5 (Bottom): Sensor Layer — Keystroke | Touch | Accelerometer | Gyroscope
Side box: MLP Model (JSON weights) feeding into Domain Layer.
Arrows showing data flowing upward: events → data flows → risk scores → UI state.
Tech Stack: Kotlin, Jetpack Compose, Hilt DI, Room DB, Coroutines & Flow, Material 3. Min SDK 26, Target SDK 34.

---

**Slide 5 — Behavioral Data Collection:**
Heading: "Three Behavioral Channels"
Content: Create 3 columns:
Column 1 — Keystroke Dynamics: Dwell Time (how long a key is held), Flight Time (gap between key releases and next press), Per-digit rhythm patterns, Custom PIN pad captures real timestamps (stock Android keyboard doesn't expose key events).
Column 2 — Touch Patterns: Touch classification — TAP (<30dp displacement), SWIPE (>50dp), SCROLL, LONG_PRESS. Features: pressure, velocity, acceleration, contact area, duration. Tap-to-total-event ratio (key discriminator).
Column 3 — Motion Sensors: Accelerometer + Gyroscope at ~60ms intervals. 10-sample moving average filter for noise reduction. Pitch & Roll angles computed. Device state inference: ON_TABLE, HELD_IN_HAND, WALKING, STATIONARY.

---

**Slide 6 — The Soft Keyboard Challenge:**
Heading: "Android's Biggest Limitation"
Content: Android's stock keyboard (Gboard, Samsung Keyboard) does NOT expose key-down/key-up events to apps. Only the resulting text reaches the app layer. Our solution: For general text input → fixed 80ms dwell time estimate. For PIN entry → Custom numeric pad built in Jetpack Compose that intercepts MotionEvent.ACTION_DOWN and ACTION_UP directly, capturing real dwell times, flight times, per-digit (x,y) coordinates, and contact area. This honest limitation actually led to our most surprising finding.
Visual: Split comparison — Stock Keyboard (estimated, less accurate) vs Custom PIN Pad (real timestamps, high accuracy).

---

**Slide 7 — Feature Engineering (The Breakthrough):**
Heading: "From 42 Raw Features to 124 Deviation Features"
Content: Early iterations fed raw session statistics directly → poor accuracy. Models were separating users by inherent baseline differences, NOT distinguishing genuine vs impostor. The breakthrough: Enrollment-Relative Deviation Features. For each feature i: Absolute deviation = |session_i - enrollment_i|, Relative deviation = |session_i - enrollment_i| / |enrollment_i|, Plus raw session value retained. Result: 3 × 40 features + 4 aggregate stats = 124-dimensional vector. This single design change → +12 percentage points accuracy improvement.
Visual: Diagram showing Enrollment Session → Raw Features (42-dim) and Test Session → Raw Features (42-dim) both feeding into Deviation Computation → 124-dim Feature Vector.

---

**Slide 8 — Machine Learning Pipeline:**
Heading: "Three Classifiers Compared"
Content: Table showing:
| Model | Accuracy | Precision | Recall | F1 | AUC | EER |
| Random Forest | 89.71% | 95.18% | 83.62% | 89.04% | 0.979 | 8.71% |
| MLP (Deployed) | 87.60% | 89.37% | 85.23% | 87.25% | 0.938 | 14.51% |
| One-Class SVM | 82.06% | 80.72% | 84.10% | 82.37% | 0.908 | 23.76% |
Random Forest Configuration: 300 trees, max_depth=12, balanced class weights.
MLP Architecture: 124 → 128 → 64 → 32 → 1 (sigmoid), ReLU activations, Adam optimizer, 26,273 parameters.
One-Class SVM: RBF kernel, nu=0.1, trained on genuine sessions only.
Evaluation: 5-fold stratified cross-validation.

---

**Slide 9 — Synthetic Data Generation:**
Heading: "From 8 Real to 58 Participants"
Content: Only 8 real participants — too small for reliable training. Solution: Calibrated synthetic data generator producing 50 additional participants.
Technique 1: Dirichlet Blending — each synthetic profile blends 2-3 real profiles with random weights.
Technique 2: Age-Adjusted Motor Noise — based on published ergonomics research (Ketcham & Stelmach, 2002). Young (18-25): 15% faster typing. Older (46-60): 35-45% slower.
Technique 3: Ornstein-Uhlenbeck Process — physics-based mean-reverting random walk for realistic accelerometer traces (not simple Gaussian noise).
Technique 4: Impostor sessions use a genuinely different participant's profile, NOT noised copies.
Final dataset: 379 sessions (174 genuine + 205 impostor).

---

**Slide 10 — Hybrid Detection Engine:**
Heading: "Dual-Track Risk Assessment"
Content: Show a flow diagram:
Live Session Data splits into two parallel paths:
Path 1 — Z-Score Track: Compares rolling session averages against enrollment baseline. Formula: R_Z = 0.35 × keystroke_deviation + 0.30 × touch_deviation + 0.35 × motion_deviation. Available immediately from session start.
Path 2 — ML (MLP) Track: Activates after 6+ keystrokes and 50+ motion samples. Constructs 124-dim deviation vector → StandardScaler → Forward pass → Sigmoid probability. Both tracks feed into Score Fusion: R = 0.4 × R_Z + 0.6 × R_ML. Then Exponential Moving Average (α=0.3) for smoothing. Then maps to 4 graduated risk levels.

---

**Slide 11 — Graduated Security Response:**
Heading: "4 Risk Levels — Not Binary Pass/Fail"
Content: Show 4 color-coded levels:
GREEN — LOW (0-20%): Continue normally. Silent monitoring.
YELLOW — MEDIUM (20-40%): Warning banner displayed. User alerted.
ORANGE — HIGH (40-60%): Re-authentication demanded. Must verify identity.
RED — CRITICAL (60-100%): Force logout immediately. Device vibrates. Session terminated.
Key point: One-Strike Policy — if ANY single metric deviates by 5+ standard deviations (e.g., phone held upside down), immediately flagged as CRITICAL.
Inspired by credit card fraud detection — graduated escalation, not binary blocking.

---

**Slide 12 — The Surprising Finding:**
Heading: "Motion > Keystrokes — What We Didn't Expect"
Content: Show Top 10 Feature Importance (Random Forest Gini Importance) as a horizontal bar chart:
1. dev_motion_accel_y_mean_abs — 0.0907
2. dev_motion_accel_z_mean_rel — 0.0775
3. dev_motion_accel_y_mean_rel — 0.0749
4. dev_motion_accel_z_mean_abs — 0.0731
5. dev_touch_tap_ratio_rel — 0.0603
6. dev_motion_accel_mag_mean_abs — 0.0585
7. dev_touch_tap_ratio_abs — 0.0447
8. dev_motion_accel_mag_mean_rel — 0.0381
9. dev_motion_accel_x_mean_abs — 0.0257
10. overall_rel_deviation — 0.0212
Key insight: 7 of top 10 features are from MOTION sensors. Phone-holding posture is an unconscious motor habit — invisible to external observers and nearly impossible to deliberately replicate. Keystroke features appear around rank 12-40.

---

**Slide 13 — Zero-Dependency On-Device ML:**
Heading: "Pure Kotlin Neural Network — No TensorFlow Required"
Content: Most Android ML apps use TensorFlow Lite (adds 5-15MB native libraries) or ONNX Runtime. Our approach: Model trained in Python (scikit-learn MLP) → Weights exported as JSON file (~450KB) → Loaded in Android via Gson → Forward pass = 4 matrix-vector multiplications in pure Kotlin. No native libraries, no ABI-specific builds, no model format conversion. MLP: 26,273 parameters, 124→128→64→32→1. Inference latency: <1ms on Snapdragon 680. Full analysis cycle (DB query + feature extraction + inference + fusion + EMA): ~200ms. Analysis runs every 15 seconds — completely imperceptible to user.

---

**Slide 14 — On-Device Performance:**
Heading: "Real-World Performance Metrics"
Content: Show key performance metrics:
- Inference Latency: <1ms (MLP forward pass)
- Full Analysis Cycle: ~200ms (includes DB query, feature extraction, inference, fusion)
- Analysis Interval: Every 15 seconds
- Model File Size: ~450KB JSON
- Battery Impact: Negligible (no participant reported drain)
- Tested Devices: Redmi Note 11 (Snapdragon 680), Samsung Galaxy A52, Pixel 6a
- No network calls required — 100% on-device processing
- Zero user-perceptible lag during normal banking operations

---

**Slide 15 — Security Audit (Honest Assessment):**
Heading: "Known Limitations — Research Prototype"
Content: What we did well: Multi-modal behavioral biometrics, Z-score + ML hybrid, graduated response, no network dependency, ProGuard enabled.
Known vulnerabilities (documented by us): Passwords stored in PLAINTEXT (not hashed — must use BCrypt/Argon2 for production), No database encryption (need SQLCipher), No session token management (UUIDs in ViewModel memory), No rate limiting on login, CSV exports are unencrypted.
Why this is okay: This is a research prototype demonstrating the behavioral authentication concept. Production deployment would require addressing all documented security gaps. We documented these ourselves — proving we understand security engineering.

---

**Slide 16 — Adversarial Resilience:**
Heading: "Why Behavioral Mimicry is Extremely Difficult"
Content: Attack 1 — Enrollment Poisoning: If attacker enrolls their own behavior, all legitimate sessions get flagged. Defense needed: Multi-session enrollment across different days + external identity verification.
Attack 2 — Behavioral Mimicry: Typing rhythm can be observed and partially imitated. BUT our system requires simultaneously matching: typing rhythm + touch pressure + swipe velocity + tap-to-swipe ratio + phone holding angle (accelerometer). The most important features (accelerometer tilt) are: unconscious motor habits, NOT externally observable, nearly impossible to consciously replicate. An attacker would need to hold the phone at exactly the same angle while typing with the same rhythm and touching with the same pressure.

---

**Slide 17 — Comparison with Literature:**
Heading: "How We Compare to Published Research"
Content: Our EER (8.71%) falls within the 5-12% range reported by Killourhy & Maxion (2009) for fixed-text keystroke dynamics on hardware keyboards. Our setting is MORE challenging: mobile soft keyboard (not hardware), multi-modal fusion, cross-session evaluation (not within-session). AUC of 0.979 is consistent with strongest results surveyed by Zheng et al. (2020) and Patel et al. (2016). Key references: Monrose & Rubin (1997) — Keystroke dynamics pioneer. Frank et al. (2013) — Touchalytics. Sitová et al. (2016) — HMOG hand movement features. Bo et al. (2013) — SilentSense.

---

**Slide 18 — Future Work:**
Heading: "Roadmap — What's Next"
Content: 5 planned improvements:
1. Larger User Study: 30+ real participants to reduce synthetic data dependency.
2. Adaptive Baselines: Handle behavioral drift over time (new phone case, posture changes) with gradually updating enrollment profiles.
3. Temporal Sequence Models: LSTM or Transformers on raw event streams to capture ordering patterns lost by pre-aggregation.
4. BiometricPrompt Integration: Use Android's native fingerprint/face recognition for re-authentication challenges instead of custom UI.
5. Formal Usability Study: Measure false rejection rates experienced by legitimate users and assess if security prompts feel acceptable or disruptive.

---

**Slide 19 — Tech Stack Summary:**
Heading: "Technologies Used"
Content: Show as a grid/icon layout:
Android Layer: Kotlin 1.9+, Jetpack Compose, Material Design 3, Hilt (Dagger), Room SQLite, Kotlin Coroutines & Flow, Compose Navigation, AndroidX Security Crypto, Gradle KTS with KSP. Min SDK 26 (Android 8.0), Target SDK 34 (Android 14).
ML/Research Layer: Python 3, scikit-learn (Random Forest, MLP, OCSVM), pandas, numpy, scipy, matplotlib. Custom synthetic data generator. JSON model export pipeline.

---

**Slide 20 — Thank You:**
Heading: "Thank You"
Subtitle: "SecureBank — Because Authentication Shouldn't End at Login"
Content: Team Members with roles:
- Tejus Kapoor — System Architecture & Integration Lead
- Tanush Malhotra — Sensor Data Collection & Experiment Module
- Bela Diwan — ML Pipeline & Feature Engineering
- Gautam Nautiyal — Anomaly Detection & Security UI
Guide: Dr./Prof. [Add Guide Name]
Department of Computer Science & Engineering, Graphic Era (Deemed to be University), Dehradun
GitHub Repository: [Add Link]
Questions?

---

END OF PROMPT.
