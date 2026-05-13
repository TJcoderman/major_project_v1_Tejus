# ML Viva Questions & Answers — SecureBank Behavioral Authentication

> Grounded in **your actual codebase**: `BehaviorAnalyzer.kt`, `MLFeatureExtractor.kt`, `MLModelInference.kt`, `ml_model.py`, `synthetic_data_generator.py`.

---

## 1 — Architecture & Design

### Q1. What ML model did you use and why?

**A.** We use a **Multi-Layer Perceptron (MLP)** neural network with architecture **124 → 128 → 64 → 32 → 1**.
- Hidden layers use **ReLU** activation; output uses **Sigmoid** for binary probability.
- We chose MLP over CNN/RNN because our features are **fixed-length statistical summaries** (mean, std, ratios), not sequential raw signals — so a fully-connected network is the natural fit.
- We also evaluated **Random Forest** and **One-Class SVM** in `ml_model.py` for comparison. MLP gave the best F1/AUC balance.
- The model is exported as **JSON weights** (not TFLite/ONNX) and runs via pure Kotlin matrix math in `MLModelInference.kt`, avoiding heavy framework dependencies on-device.

### Q2. Why not use a CNN or LSTM for time-series touch data?

**A.** Our pipeline **pre-computes statistical features** (mean, std, skewness, kurtosis, percentiles, ratios) from raw sensor streams *before* feeding them to the model. This is a deliberate design choice:
- Statistical summaries are **rotation/translation invariant** — they don't depend on screen position.
- They're **fixed-length** regardless of session duration.
- They're **explainable** — we can tell the user *which* feature deviated.
- A CNN/LSTM would need raw time-series of variable length, more training data, and would be a black box. For a security-critical banking app, explainability matters.

### Q3. Explain your hybrid detection approach.

**A.** We combine **two independent anomaly detection signals** in `BehaviorAnalyzer.kt`:

1. **Z-Score Statistical Analysis** — Computes per-feature deviation from the enrolled baseline using standard deviations. Weighted across keystroke (35%), touch (30%), motion (35%) modalities. Works immediately with zero ML dependency.

2. **ML Model Inference** — The trained MLP classifies the session as genuine/impostor using 124 enrollment-relative deviation features.

**Blending formula** (line ~625 of BehaviorAnalyzer.kt):
```
finalRisk = (1 - ML_WEIGHT) × zScoreRisk + ML_WEIGHT × mlRisk
```
Where `ML_WEIGHT = 0.6` — giving ML majority since it was trained on a richer feature set, but keeping Z-score as a safety net when ML data is insufficient.

### Q4. Why a hybrid approach instead of pure ML?

**A.** Three reasons:
1. **Cold-start problem** — ML needs sufficient session data (PIN keystrokes + touches + motion) before it can run. Z-score works from the first touch event.
2. **Graceful degradation** — If the model fails to load or input is invalid, Z-score still provides protection.
3. **Complementary signals** — Z-score catches *individual* feature spikes in real-time; ML captures *multivariate patterns* that Z-score misses (e.g., subtle correlated shifts across pressure + velocity + rhythm simultaneously).

---

## 2 — Feature Engineering

### Q5. How many features does your model use and what are they?

**A.** **124 features** organized into three categories of **deviation features**:

For each of ~40 raw features, we compute three derived features:
- `dev_{feature}_abs` — Absolute deviation from enrollment
- `dev_{feature}_rel` — Relative (percentage) deviation
- `raw_{feature}` — Current session value

Plus 4 aggregate features: `overall_abs_deviation`, `overall_rel_deviation`, `max_abs_deviation`, `max_rel_deviation`.

**Raw feature categories:**
| Category | Features | Count |
|----------|----------|-------|
| PIN Keystroke | dwell mean/std/median/Q25/Q75/skew/kurtosis/IQR, flight mean/std/median/IQR, touch x/y mean/std, touch size mean/std, per-digit rhythm (d0-d5) | 24 |
| Touch | pressure mean/std, area mean/std, hold mean/std, duration mean/std, velocity mean/std/max, accel mean/std, tap/swipe/long_press ratios, count | 17 |
| Motion | accel magnitude mean/std, gyro magnitude mean/std, accel x/y/z mean | 7 |

### Q6. What are "enrollment-relative deviation features"?

**A.** Instead of feeding raw behavioral values to the model, we compute **how much the current session deviates from the user's own enrollment baseline**. This is implemented in `MLFeatureExtractor.computeDeviationMap()`:

```
For each feature key:
  dev_abs = |session_value - enrollment_value|
  dev_rel = |session_value - enrollment_value| / |enrollment_value|
  raw     = session_value
```

**Why this matters:** Raw features vary wildly between users (one person types fast, another slow). Deviation features normalize this — a genuine user will have *low* deviation from their own enrollment, an impostor will have *high* deviation. This gives the model a clear, user-independent signal.

### Q7. What is Z-score normalization and where do you use it?

**A.** Z-score (StandardScaler) transforms each feature to have mean=0 and std=1:
```
z = (x - μ) / σ
```
We use it in **two places**:
1. **Training** (`ml_model.py` line 266): `StandardScaler().fit_transform(X)` — the scaler's mean/scale parameters are saved in the model JSON.
2. **Inference** (`MLModelInference.kt` line 121-129): Before the forward pass, each feature is standardized using the saved scaler parameters.

This prevents features with large magnitudes (e.g., velocity in px/s ~500) from dominating features with small magnitudes (e.g., pressure ~0.5).

### Q8. Explain skewness and kurtosis — why are they useful?

**A.** Both are computed for PIN dwell times in `MLFeatureExtractor.kt`:
- **Skewness** measures asymmetry of the distribution. A genuine user has a consistent typing rhythm (low skewness); an impostor unfamiliar with the PIN may have erratic timing (high skewness).
- **Kurtosis** measures "tailedness". High kurtosis means occasional extreme outliers (hesitation on unfamiliar digits). We use excess kurtosis (subtract 3) so a normal distribution = 0.

These capture **rhythm consistency** that mean/std alone miss.

---

## 3 — Training Pipeline

### Q9. How did you train the model? What data did you use?

**A.** The training pipeline is in `research/ml_model.py`:
1. **Data generation**: `synthetic_data_generator.py` creates realistic behavioral profiles for N participants, each with enrollment, genuine, and impostor sessions.
2. **Feature extraction**: For each participant, enrollment features become the baseline; genuine/impostor session features are compared against it to produce deviation features.
3. **Training**: 5-fold stratified cross-validation with class balancing.
4. **Export**: The final MLP is retrained on all data and exported as JSON weights + scaler params to `behavioral_auth_model.json`.

### Q10. What is stratified K-fold cross-validation?

**A.** We use **5-fold stratified CV** (`StratifiedKFold` in `ml_model.py` line 270):
- The dataset is split into 5 folds, maintaining the same genuine/impostor ratio in each fold.
- Each fold serves as the test set once while the other 4 are used for training.
- This gives us **5 independent accuracy estimates** and avoids the optimistic bias of a single train/test split.
- We use `cross_val_predict` to get out-of-fold predictions for every sample, enabling honest metric computation.

### Q11. How do you handle class imbalance?

**A.** Multiple strategies:
- **Random Forest**: `class_weight="balanced"` automatically upweights the minority class.
- **Synthetic data generator**: Generates controlled ratios of genuine vs impostor sessions.
- **Stratified CV**: Preserves class distribution in every fold.
- **Threshold tuning**: The model's decision threshold (default 0.5) can be adjusted based on the ROC curve to balance FAR vs FRR.

### Q12. What metrics do you use to evaluate the model?

**A.** Computed in `compute_metrics()` of `ml_model.py`:

| Metric | What it measures |
|--------|-----------------|
| **Accuracy** | Overall correct predictions |
| **Precision** | Of predicted genuine, how many are truly genuine |
| **Recall** | Of truly genuine, how many are correctly identified |
| **F1 Score** | Harmonic mean of precision and recall |
| **AUC-ROC** | Area under ROC curve — threshold-independent performance |
| **EER** | Equal Error Rate — where FAR = FRR (lower is better) |
| **Confusion Matrix** | TP, TN, FP, FN counts |

**For a banking app, FRR matters most** — falsely locking out a legitimate user is worse UX than a false alarm. But FAR matters for security — letting an impostor through is dangerous.

### Q13. What is EER and why is it important for biometrics?

**A.** **Equal Error Rate** is the point where:
- **FAR** (False Accept Rate) = **FRR** (False Reject Rate)

It's computed by finding the threshold where the FAR and FRR curves intersect on the DET curve. Lower EER = better system. It's the standard biometric evaluation metric because:
- It's **threshold-independent** — gives a single number to compare systems
- It captures the **security-usability tradeoff** — a system that rejects everyone has 0% FAR but 100% FRR

In our system: `eer_idx = np.nanargmin(np.abs(far - frr))` (ml_model.py line 326).

---

## 4 — On-Device Inference

### Q14. How does the model run on the Android device?

**A.** Pure Kotlin matrix multiplication in `MLModelInference.kt`:

1. **Load**: Parse `behavioral_auth_model.json` from assets (578 KB) using Gson.
2. **Standardize**: Apply saved scaler means/scales to input features (Z-score).
3. **Forward pass**: For each layer, compute `output = ReLU(W^T × input + bias)`, final layer uses Sigmoid.
4. **Classify**: If sigmoid output ≥ threshold (0.5) → genuine, else → impostor.

**No TensorFlow, ONNX, or any ML framework required.** This keeps the APK small and avoids native library compatibility issues.

### Q15. Why JSON weights instead of TFLite?

**A.** Three reasons:
1. **Zero dependencies** — No TensorFlow Lite SDK (~5MB), no native `.so` files for different ABIs.
2. **Python version independence** — TFLite export often breaks with specific Python/TF version combinations. JSON works everywhere.
3. **Debuggability** — We can inspect every weight, bias, and scaler parameter in plaintext. During development, we could verify that the Kotlin forward pass exactly matches Python's `mlp.predict_proba()`.

The tradeoff is ~578KB JSON vs ~200KB TFLite, and slightly slower inference. But for our 124-feature, 4-layer network, inference takes <1ms — negligible.

### Q16. Walk through the inference pipeline end-to-end.

**A.** When a risk assessment triggers (every ~15-30 seconds during banking):

```
1. BehaviorAnalyzer.performRiskAssessment()
   │
2. Gather recent session data from Room DB
   │  → Last 200 touches, 500 motion samples, 50 keystrokes
   │
3. MLFeatureExtractor.computeDeviationFeatures()
   │  a. extractRawFeatures(sessionData) → 40 raw features
   │  b. computeDeviationMap(enrollment, session) → 124 deviation features
   │  c. Order features by model's feature_names list
   │
4. MLModelInference.predict(features)
   │  a. Z-score standardize using saved scaler
   │  b. Forward pass: 124→128→64→32→1
   │  c. Return sigmoid probability [0,1]
   │
5. classify() → (isGenuine: Boolean, confidence: Float)
   │
6. Convert to risk: genuine+high_conf → low risk, impostor+high_conf → high risk
   │
7. Blend: finalRisk = 0.4 × zScoreRisk + 0.6 × mlRisk
```

---

## 5 — Behavioral Biometrics Specifics

### Q17. What behavioral modalities do you capture?

**A.** Three modalities:

| Modality | Sensor | Features | Collector |
|----------|--------|----------|-----------|
| **Keystroke Dynamics** | Soft keyboard events | Dwell time, flight time, per-digit rhythm | `KeystrokeCollector.kt` |
| **Touch Dynamics** | Pointer events | Pressure, area, velocity, acceleration, hold duration, gesture ratios | `TouchDataCollector.kt` |
| **Motion Patterns** | Accelerometer + Gyroscope | Device orientation (pitch/roll), movement magnitude, device state | `SensorDataCollector.kt` |

### Q18. What is dwell time vs flight time?

**A.**
- **Dwell time**: How long a key is held down (ACTION_DOWN → ACTION_UP). Measures finger pressure habit.
- **Flight time**: Time between releasing one key and pressing the next. Measures typing rhythm/speed.

Flight time is **3× more important** in our weighting (`0.75 vs 0.25` in BehaviorAnalyzer line 800) because Android soft keyboards don't reliably report dwell time, but inter-key timing is consistent and hard to mimic.

### Q19. How do you handle the enrollment/baseline problem?

**A.** Two-phase approach:

1. **Enrollment** (signup): Guided calibration collects 3 PIN entries, 10 taps + 3 long-press holds, 8 directional swipes, and 6 seconds of motion data. This builds a `BehavioralProfile` stored in Room DB.

2. **Runtime**: When the user logs in, the profile is loaded as the baseline. The `MLFeatureExtractor.setEnrollmentBaseline(profile)` reconstructs the enrollment feature vector from stored statistics. Current session data is compared against this baseline.

### Q20. What is the "trust ramp" in real-time touch processing?

**A.** The first few touches after login are unreliable — the user may be adjusting grip, finding their bearings. The trust ramp (in `processRealTimeTouch()`) **suppresses risk scoring** for the first 8 touch samples or 6 seconds, whichever comes later. Only clearly extreme events (hold >1200ms, velocity >3.2× baseline) bypass the ramp. This prevents false alarms at session start.

---

## 6 — Security & Risk

### Q21. How is the risk score computed and what actions does it trigger?

**A.** Risk score is a float [0.0, 1.0]:

| Score Range | Risk Level | Action |
|-------------|-----------|--------|
| 0.0 – 0.2 | LOW | Continue normally |
| 0.2 – 0.4 | MEDIUM | Show warning toast |
| 0.4 – 0.6 | HIGH | Request re-authentication |
| 0.6 – 1.0 | CRITICAL | Force logout |

The Z-score analysis uses **weighted contributions**: keystroke (35%), touch (30%), motion (35%). Each is normalized so 3σ deviation maps to 100% risk.

### Q22. Can an attacker fool the system by mimicking behavior?

**A.** It's extremely difficult because:
- **Multi-modal**: An attacker would need to simultaneously match keystroke rhythm, touch pressure, swipe velocity, AND device holding angle.
- **Unconscious behavior**: Touch pressure and micro-movements are largely involuntary and not observable by an attacker.
- **124 features**: Even if some features are matched, the multivariate pattern across all 124 deviation features is nearly impossible to replicate.
- **Continuous**: Unlike one-time biometrics (fingerprint), we monitor continuously throughout the session.

### Q23. What happens if the ML model fails to load?

**A.** Graceful degradation in `MLModelInference.kt`:
- `predict()` returns `-1f` → `classify()` returns `null`
- `performMLAssessment()` returns `null`
- The blending formula falls back to **Z-score only**: `finalRisk = zScoreRisk`
- The app continues functioning with statistical-only detection
- `mlReadyState` StateFlow is set to `false`, visible in the debug UI

---

## 7 — Data & Privacy

### Q24. Where is the behavioral data stored?

**A.** Everything stays **on-device** in a Room SQLite database (`SecureBankDatabase`):
- `keystroke_data` — raw keystroke events
- `touch_data` — touch interactions
- `motion_data` — sensor readings
- `behavioral_profiles` — enrollment baselines
- `behavioral_sessions` — session aggregates

**No data leaves the device.** The ML model runs locally. There is no cloud component.

### Q25. How do you handle sensor data that Android doesn't reliably provide?

**A.** Key challenges and solutions:
- **Touch pressure**: Many devices report 0.0 or 1.0 (saturated). We detect saturation (`saturatedPressureSamples >= 3`) and **downweight pressure** in risk scoring when it's unreliable.
- **Touch size**: Compose doesn't expose size directly, so we pass `1f` and rely on `pressure × size` as a proxy for contact area.
- **Multi-sample averaging**: The recent enrollment rework collects pressure/size samples throughout the entire touch lifecycle (not just start/end), averaging up to 32 samples via `pressureSamples` list in `TouchDataCollector`.

---

## 8 — Model Comparison & Justification

### Q26. You trained Random Forest, MLP, and One-Class SVM — compare them.

**A.**

| Aspect | Random Forest | MLP | One-Class SVM |
|--------|--------------|-----|---------------|
| **Type** | Ensemble of decision trees | Neural network | Anomaly detection |
| **Training** | Needs genuine + impostor | Needs genuine + impostor | Trains on genuine only |
| **Interpretability** | High (feature importance) | Low (black box) | Low |
| **Performance** | Good accuracy, robust | Best F1/AUC | Highest FRR |
| **On-device** | Hard (300 trees) | Easy (matrix math) | Hard (support vectors) |

We **deployed MLP** because: best metric balance, simplest on-device inference (just matrix multiply), and smallest model size.

### Q27. What is One-Class SVM and when would you prefer it?

**A.** One-Class SVM learns the boundary of "normal" (genuine) behavior and flags anything outside as anomalous. It's trained on **genuine data only** — no impostor samples needed.

**Prefer it when:**
- You have no impostor data (real-world cold start)
- You want to detect novel/unseen attack patterns

**We didn't deploy it because:** It had higher FRR (more false rejections of genuine users), and we had synthetic impostor data available for supervised training.

### Q28. What is the role of `class_weight="balanced"` in Random Forest?

**A.** It automatically adjusts class weights inversely proportional to class frequencies:
```
weight_class = n_samples / (n_classes × n_samples_class)
```
This prevents the model from being biased toward the majority class. Without it, if we have 70% genuine and 30% impostor samples, the model could achieve 70% accuracy by always predicting "genuine" — useless for security.

---

## 9 — Advanced / Research Questions

### Q29. How would you improve the system with more time?

**A.**
1. **Federated learning** — Train across users without sharing raw data.
2. **Temporal modeling** — Use LSTM on raw touch sequences for richer patterns.
3. **Adaptive thresholds** — Per-user risk thresholds based on their behavioral consistency.
4. **Transfer learning** — Pre-train on public behavioral datasets, fine-tune on enrollment.
5. **Continuous enrollment** — Gradually update the baseline as the user's behavior naturally evolves (aging, new phone).

### Q30. What is the difference between verification and identification?

**A.**
- **Verification** (1:1): "Is this the claimed user?" Compare against one enrolled profile. This is what we do.
- **Identification** (1:N): "Who is this user?" Compare against all enrolled profiles. We don't do this — the user has already logged in with credentials.

### Q31. What is FAR vs FRR and the tradeoff?

**A.**
- **FAR (False Accept Rate)**: Impostor incorrectly accepted as genuine. Security risk.
- **FRR (False Reject Rate)**: Genuine user incorrectly rejected. Usability risk.

**Tradeoff**: Lowering the threshold increases FAR but decreases FRR (more permissive). Raising it does the opposite. Our threshold is 0.5, but in production you'd tune it based on:
- Banking apps: Lower threshold → favor security (accept some FRR)
- Social apps: Higher threshold → favor usability (accept some FAR)

### Q32. Explain the forward pass of your neural network mathematically.

**A.** For input vector **x** ∈ ℝ¹²⁴:

```
Layer 1: h₁ = ReLU(W₁ᵀx + b₁)     where W₁ ∈ ℝ¹²⁴ˣ¹²⁸, h₁ ∈ ℝ¹²⁸
Layer 2: h₂ = ReLU(W₂ᵀh₁ + b₂)    where W₂ ∈ ℝ¹²⁸ˣ⁶⁴,  h₂ ∈ ℝ⁶⁴
Layer 3: h₃ = ReLU(W₃ᵀh₂ + b₃)    where W₃ ∈ ℝ⁶⁴ˣ³²,   h₃ ∈ ℝ³²
Layer 4: ŷ  = σ(W₄ᵀh₃ + b₄)       where W₄ ∈ ℝ³²ˣ¹,    ŷ ∈ [0,1]

ReLU(z) = max(0, z)
σ(z)    = 1 / (1 + e⁻ᶻ)
```

Total parameters: 124×128 + 128 + 128×64 + 64 + 64×32 + 32 + 32×1 + 1 = **26,529**

### Q33. Why ReLU activation and not Sigmoid for hidden layers?

**A.**
- **Vanishing gradient**: Sigmoid saturates at 0 and 1, making gradients near-zero for deep layers → slow training. ReLU has constant gradient (1) for positive values.
- **Computational speed**: ReLU is just `max(0, x)` — much cheaper than `1/(1+e^-x)`.
- **Sparsity**: ReLU outputs zero for negative inputs, creating sparse activations that act as implicit feature selection.

Sigmoid is used only on the **output layer** because we need a probability in [0, 1].

### Q34. What is early stopping and why do you use it?

**A.** In `ml_model.py` line 287: `early_stopping=True, validation_fraction=0.15`.

During training, 15% of data is held as validation set. Training stops when validation loss hasn't improved for 10 consecutive epochs. This prevents **overfitting** — the model won't memorize the training data and will generalize better to unseen users.

### Q35. What is the L2 regularization (`alpha`) parameter?

**A.** `alpha=0.0005` in the MLP configuration. It adds a penalty term to the loss function:
```
Loss = CrossEntropy + α × Σ(w²)
```
This discourages large weights, reducing overfitting. Small alpha (0.0005) provides mild regularization without hurting the model's ability to learn complex patterns.

---

## 10 — Quick-Fire Conceptual Questions

### Q36. Supervised vs Unsupervised — which is your approach?
**A.** **Supervised** — We have labeled data (genuine=1, impostor=0) and train a binary classifier.

### Q37. What loss function does your MLP use?
**A.** **Binary Cross-Entropy** (log loss): `L = -[y·log(ŷ) + (1-y)·log(1-ŷ)]`. Standard for binary classification with sigmoid output.

### Q38. What optimizer does your MLP use?
**A.** **Adam** (Adaptive Moment Estimation) — combines momentum and RMSprop. It adapts learning rates per-parameter, converges faster than vanilla SGD.

### Q39. What is the curse of dimensionality — is 124 features too many?
**A.** 124 features is moderate, not excessive. We mitigate dimensionality issues by: (1) removing constant features, (2) StandardScaler normalization, (3) regularization (alpha), (4) having sufficient training samples. The MLP's hidden layers also act as automatic feature compression (124 → 128 → 64 → 32).

### Q40. Can behavioral biometrics fully replace passwords?
**A.** Not yet. Behavioral biometrics are best as a **second factor** or **continuous authentication layer**. They can't replace passwords because: (1) behavior changes with injury, stress, environment; (2) initial enrollment requires an identity anchor (password/PIN); (3) regulatory requirements still mandate knowledge-based factors for financial apps.
