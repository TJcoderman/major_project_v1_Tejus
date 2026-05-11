# 🎓 SecureBank — Viva Presentation Guide

> **Project**: SecureBank: Hybrid Statistical & Neural Network Framework for Continuous Behavioral Authentication on Mobile Banking
> **Team**: Tejus Kapoor, Tanush Malhotra, Bela Diwan, Gautam Nautiyal
> **University**: Graphic Era (Deemed to be University), Dehradun

---

## 📋 Project Summary (Sabko Yaad Hona Chahiye)

SecureBank ek Android banking app hai jo **login ke baad bhi continuously user ko verify** karti hai — keystroke timing, touch patterns, aur phone holding angle se. Agar koi aur phone uthaye toh system detect karke **auto-logout** kar deta hai.

**Key Numbers jo sabko yaad hone chahiye:**
- 🎯 Random Forest: **89.71% accuracy**, AUC = **0.979**, EER = **8.71%**
- 🧠 On-device MLP: **87.60% accuracy**, AUC = **0.938**
- 📊 **124-dimensional** enrollment-relative deviation feature vector
- 👥 **58 participants** (8 real + 50 synthetic), **379 sessions**
- ⚡ Inference latency: **<1ms** (MLP forward pass), full cycle **~200ms**
- 🏗️ Tech: Kotlin, Jetpack Compose, Hilt, Room, Coroutines, Material 3

---

## 👤 MEMBER 1: TEJUS KAPOOR — Project Lead & Core Architecture

### 🎯 Role: System Architect + Android App Core + Integration Lead

### 📂 Files to Read Deeply:

| File | Kya Hai | Priority |
|------|---------|----------|
| `MainActivity.kt` | App entry point, Hilt setup | ⭐⭐⭐ |
| `SecureBankApplication.kt` | Hilt Application class | ⭐⭐ |
| `di/AppModule.kt` | Dependency Injection — sabko kaise wire kiya | ⭐⭐⭐ |
| `ui/navigation/NavGraph.kt` | Navigation flow — Login → Dashboard → Transfer → Experiment | ⭐⭐⭐ |
| `data/local/SecureBankDatabase.kt` | Room DB schema, tables, migrations | ⭐⭐⭐ |
| `data/repository/BehavioralRepository.kt` | Data access layer — sensors se DB tak | ⭐⭐⭐ |
| `data/repository/UserRepository.kt` | User management, login/signup | ⭐⭐ |
| `data/model/UserModels.kt` | User entity definitions | ⭐⭐ |
| `docs/architecture.md` | Architecture diagram & data flow | ⭐⭐⭐ |
| `README.md` | Project overview | ⭐⭐⭐ |
| `build.gradle.kts` (app-level) | Dependencies list | ⭐⭐ |
| `SKILLS.md` | Full project reference | ⭐⭐⭐ |

### 🗣️ Viva Questions Tejus Should Prepare:

1. **"Project ka architecture explain karo"** → MVVM + Clean Architecture, 5 layers (UI → ViewModel → Domain → Data → Sensor)
2. **"Hilt kya hai aur kyun use kiya?"** → Dependency Injection framework, `@Singleton` collectors, `@HiltViewModel` for ViewModels — testing/modularity ke liye
3. **"Data flow kaise hota hai login se logout tak?"** → Login → Baseline store → Continuous collection → 15s interval analysis → Risk score → Response
4. **"Room database mein kya store hote hain?"** → Users, KeystrokeData, TouchData, MotionData, BehavioralProfile entities
5. **"Navigation kaise handle ki?"** → Compose Navigation with sealed class `Screen`, conditional routing based on auth state
6. **"Coroutines aur Flow kahan use kiye?"** → SharedFlow for sensor events, StateFlow for UI state, viewModelScope for async ops
7. **"Project ka scope kya hai aur limitations?"** → Research prototype, not production. Known: plaintext passwords, no encryption, no network layer

### 💡 Wow Moment for Tejus:
> *"Humne zero external ML dependencies use kiye — no TensorFlow Lite, no ONNX. Pure Kotlin matrix multiplication se neural network inference kiya. JSON mein weights load hote hain aur 4 matrix multiplications se result aata hai. APK size minimal rehti hai."*

---

## 👤 MEMBER 2: TANUSH MALHOTRA — Sensor Layer & Data Collection

### 🎯 Role: Behavioral Data Collection Engine + Experiment Module

### 📂 Files to Read Deeply:

| File | Kya Hai | Priority |
|------|---------|----------|
| `sensor/KeystrokeCollector.kt` | Typing pattern capture — dwell time, flight time | ⭐⭐⭐ |
| `sensor/TouchDataCollector.kt` | Touch events — taps, swipes, pressure, velocity | ⭐⭐⭐ |
| `sensor/SensorDataCollector.kt` | Accelerometer + Gyroscope — pitch, roll, device state | ⭐⭐⭐ |
| `ui/components/CustomPinPad.kt` | Custom PIN keyboard — real dwell time capture | ⭐⭐⭐ |
| `ui/components/TouchCaptureWrapper.kt` | Compose pointerInput wrapper | ⭐⭐⭐ |
| `data/model/BehavioralData.kt` | KeystrokeData, TouchData, MotionData entities | ⭐⭐⭐ |
| `data/model/ExperimentModels.kt` | Experiment session models, PIN config | ⭐⭐ |
| `ui/screens/ExperimentScreens.kt` | Experiment Hub — enrollment, genuine, impostor flows | ⭐⭐⭐ |
| `ui/viewmodel/ExperimentViewModel.kt` | Experiment logic + CSV export trigger | ⭐⭐⭐ |
| `data/export/DataExporter.kt` | CSV export — per-participant files | ⭐⭐⭐ |
| `docs/research-module.md` | CSV formats, data structure | ⭐⭐ |

### 🗣️ Viva Questions Tanush Should Prepare:

1. **"Keystroke dynamics kya hoti hai?"** → Dwell time (key hold duration), Flight time (gap between keys), typing speed — har insaan ka unique pattern hota hai
2. **"Android keyboard se dwell time kaise capture kiya?"** → Nahi kar sakte directly! Stock keyboard key events nahi deta. General text → fixed 80ms estimate. PIN → custom pad se real timestamps
3. **"Touch data kaise classify karte ho?"** → Displacement <30dp + duration <300ms = TAP, displacement >50dp = SWIPE, else SCROLL/LONG_PRESS
4. **"Accelerometer data se kya pata chalta hai?"** → Phone holding angle (pitch/roll), device state (table/hand/walking), ye sabse important feature nikla!
5. **"Moving average filter kyun lagaya?"** → Sensor noise remove karne ke liye — 10-sample window, especially Redmi Note 11 pe gyroscope bahut noisy tha
6. **"Experiment protocol kya hai?"** → Enrollment → 3 Genuine sessions (same person) → 3 Impostor sessions (different person) → CSV export
7. **"Data export kaise hota hai?"** → Per-participant folders (P01-P08), separate CSVs: `pin_keystrokes.csv`, `touches.csv`, `motion.csv`, `metadata.csv`

### 💡 Wow Moment for Tanush:
> *"Sabse interesting finding ye thi ki keystroke dynamics — jisko hum most important samajh rahe the — wo actually top 10 features mein nahi aayi. Accelerometer tilt (phone kaise pakadta hai) sabse powerful discriminator nikla. 7 out of top 10 features motion sensor se hain. Ye isliye kyunki phone holding posture unconscious habit hai — koi consciously replicate nahi kar sakta, aur bahar se observe bhi nahi ho sakta."*

---

## 👤 MEMBER 3: BELA DIWAN — ML Pipeline & Research

### 🎯 Role: Machine Learning Pipeline + Feature Engineering + Research Paper

### 📂 Files to Read Deeply:

| File | Kya Hai | Priority |
|------|---------|----------|
| `research/ml_model.py` | Full ML pipeline — feature extraction, training, evaluation | ⭐⭐⭐ |
| `research/synthetic_data_generator.py` | 50 synthetic participants generator | ⭐⭐⭐ |
| `research/generate_visualizations.py` | ROC curves, feature importance plots | ⭐⭐ |
| `research/results/` (folder) | Results JSONs, CSVs, model comparison | ⭐⭐⭐ |
| `domain/MLFeatureExtractor.kt` | On-device 124-feature extraction (Kotlin mirror of Python) | ⭐⭐⭐ |
| `domain/MLModelInference.kt` | Pure Kotlin MLP forward pass | ⭐⭐⭐ |
| `domain/FeatureExtractor.kt` | Legacy 52 raw features (Z-score path) | ⭐⭐ |
| `research_paper.tex` | IEEE format research paper — Sections 4-5 (Features + ML) | ⭐⭐⭐ |
| `docs/research-module.md` | ML pipeline documentation | ⭐⭐⭐ |

### 🗣️ Viva Questions Bela Should Prepare:

1. **"Feature engineering explain karo"** → 42 raw features → enrollment-relative deviation → 124-dim vector (abs deviation + relative deviation + raw value + 4 aggregates)
2. **"Enrollment-relative deviation kyun?"** → Raw features se model user-specific differences seekhta tha, not genuine vs impostor. Deviation se 12% accuracy jump mila
3. **"3 models kaunse hain aur kyun?"** → Random Forest (best accuracy, feature importance), MLP (deployable on device), One-Class SVM (anomaly detection without impostor data)
4. **"Random Forest kyun best hai par deploy MLP kiya?"** → RF: 300 trees serialize + traverse karna heavy hai mobile pe. MLP: 4 matrix multiplications, ~450KB JSON, <1ms inference
5. **"Synthetic data kaise generate kiya?"** → Dirichlet blending of 2-3 real profiles + age-adjusted motor noise + Ornstein-Uhlenbeck process for realistic accelerometer traces
6. **"EER kya hota hai?"** → Equal Error Rate — jahan FAR = FRR. Lower is better. Humara best: 8.71% (RF)
7. **"Cross-validation kaise kiya?"** → 5-fold stratified, class balance preserved, metrics: Accuracy, Precision, Recall, F1, AUC-ROC, EER
8. **"On-device inference kaise kaam karta hai?"** → JSON se weights load → StandardScaler apply → 4 layers: 124→128→64→32→1, ReLU activations, final sigmoid

### 💡 Wow Moment for Bela:
> *"Humne pure Kotlin mein neural network inference implement kiya — bina kisi TensorFlow ya ONNX dependency ke. Python mein model train hota hai, weights JSON mein export hote hain, aur Kotlin mein simple matrix-vector multiplication se forward pass hota hai. 26,273 parameters, 450KB file, aur Snapdragon 680 pe <1ms mein result. Ye approach APK size minimal rakhti hai aur native library conflicts se bachati hai."*

---

## 👤 MEMBER 4: GAUTAM NAUTIYAL — Anomaly Detection & UI/UX

### 🎯 Role: Hybrid Risk Engine + Security Responses + UI Screens

### 📂 Files to Read Deeply:

| File | Kya Hai | Priority |
|------|---------|----------|
| `domain/BehaviorAnalyzer.kt` | ⭐ CORE FILE — hybrid Z-score + ML fusion, risk levels, EMA smoothing | ⭐⭐⭐ |
| `ui/viewmodel/BankingViewModel.kt` | Banking logic + continuous monitoring integration | ⭐⭐⭐ |
| `ui/viewmodel/AuthViewModel.kt` | Login flow + baseline establishment | ⭐⭐⭐ |
| `ui/screens/LoginScreen.kt` | Login UI + keystroke capture during password | ⭐⭐⭐ |
| `ui/screens/DashboardScreen.kt` | Dashboard + real-time risk indicator + debug panel | ⭐⭐⭐ |
| `ui/screens/TransferScreen.kt` | Transfer flow + continuous monitoring | ⭐⭐ |
| `ui/components/SecurityComponents.kt` | Risk badges, alert dialogs, re-auth prompts | ⭐⭐⭐ |
| `data/model/BehavioralProfile.kt` | Baseline profile structure | ⭐⭐ |
| `data/model/DemoBehavioralProfiles.kt` | Demo/testing profiles | ⭐⭐ |
| `ui/theme/Theme.kt` | Material 3 theme setup | ⭐⭐ |
| `docs/security-audit.md` | Security vulnerabilities & improvements | ⭐⭐⭐ |
| `ui/viewmodel/SignupViewModel.kt` | Signup + behavioral enrollment | ⭐⭐ |
| `ui/screens/SignupScreen.kt` | Signup UI with calibration flow | ⭐⭐ |

### 🗣️ Viva Questions Gautam Should Prepare:

1. **"Risk score kaise calculate hota hai?"** → Hybrid: `R = 0.4 × Z-score + 0.6 × ML`. Z-score = 0.35×keystroke + 0.30×touch + 0.35×motion deviations
2. **"4 risk levels kya hain?"** → LOW (<20%): continue | MEDIUM (20-40%): warning | HIGH (40-60%): re-auth | CRITICAL (60%+): force logout
3. **"Z-score track aur ML track parallel kyun?"** → Z-score turant kaam karta hai (partial data se bhi). ML ko minimum 6 keystrokes + 50 motion samples chahiye. Pehle 15 seconds mein sirf Z-score chalta hai
4. **"Exponential Moving Average kyun lagaya?"** → Single anomalous readings se false positive na aaye. α=0.3, smoothing karta hai risk score ko over time
5. **"One-Strike policy kya hai?"** → Koi bhi single metric 5 standard deviations se zyada deviate kare (e.g., phone ulta pakadna) → immediately CRITICAL flag
6. **"UI mein security kaise show hoti hai?"** → Color-coded risk badge (green→yellow→orange→red), debug panel with live metrics, alert dialogs, vibration on critical
7. **"Known security vulnerabilities kya hain?"** → Plaintext passwords, no DB encryption, no session tokens, no rate limiting — ye research prototype hai, production nahi

### 💡 Wow Moment for Gautam:
> *"System ki graduated response real banking apps se inspired hai. Jaise credit card companies suspicious transaction pe pehle small alert bhejti hain, phir call karti hain, phir card block karti hain — waise hi humara system LOW pe kuch nahi karta, MEDIUM pe warning deta hai, HIGH pe re-authentication maangta hai, aur CRITICAL pe phone vibrate karke force logout kar deta hai. Ye binary pass/fail se kahin zyada user-friendly hai."*

---

## 🔥 COMMON WOW MOMENTS (Sabke Liye)

### 1. 🏆 "Unexpected Finding" — Feature Importance Surprise
> **Sabko bolna chahiye:** "Humne expect kiya tha ki keystroke dynamics sabse important hogi, but accelerometer tilt — matlab phone kaise pakadta hai insaan — wo sabse powerful feature nikla. Top 10 mein 7 features motion sensor se hain. Ye isliye kyunki phone holding posture ek unconscious habit hai — bahar se observe nahi ho sakta aur deliberately replicate karna nearly impossible hai."

### 2. 💻 "Zero-Dependency ML" — Pure Kotlin Inference
> **Bolna:** "Kaafi Android ML projects TensorFlow Lite ya ONNX Runtime use karte hain jo 5-15MB native libraries add karte hain. Humne pure Kotlin mein matrix multiplication implement karke inference kiya — sirf ek 450KB JSON file se. No native dependencies, no compatibility issues."

### 3. 🧪 "Synthetic Data Innovation" — Ornstein-Uhlenbeck Process
> **Bolna:** "Simple Gaussian noise se fake accelerometer data realistic nahi lagta kyunki real sensor data mein temporal autocorrelation hoti hai. Humne Ornstein-Uhlenbeck stochastic process use kiya jo physics-based mean-reverting random walk hai — isse synthetic data ki time-series structure real data jaisi dikhti hai."

### 4. 📱 "The Soft Keyboard Problem" — Honest Limitation
> **Bolna:** "Android ka stock keyboard key-down/key-up events app ko nahi deta. Isliye general typing ke liye hum 80ms fixed dwell time estimate karte hain — ye ek genuine limitation hai. Par PIN entry ke liye humne custom numeric pad banaya jo real timestamps capture karta hai. Ye honesty examiners ko impress karegi."

### 5. 🛡️ "Adversarial Resilience" — Why Mimicry is Hard
> **Bolna:** "Agar koi attacker typing pattern copy kare bhi, toh use simultaneously phone holding angle, touch pressure, swipe velocity, aur tap-to-swipe ratio bhi match karna padega. Aur accelerometer features — jo sabse important hain — wo consciously control karna practically impossible hai."

---

## 🎯 PRESENTATION FLOW (Suggested Order)

### Opening (2 min) — **Tejus**
- Problem statement: "Login ke baad kya? Session hijacking ka real threat"
- ECB/FBI statistics cite karo
- Solution: Continuous behavioral authentication

### Architecture Demo (3 min) — **Tejus**
- 5-layer architecture diagram dikhao
- Tech stack explain karo
- Data flow: Login → Collection → Analysis → Response

### Data Collection Deep Dive (4 min) — **Tanush**
- 3 collectors explain karo with live examples
- Custom PIN pad vs stock keyboard problem
- Experiment protocol — how data was collected from 8 real people
- CSV export and data structure

### ML Pipeline (4 min) — **Bela**
- Feature engineering: Raw 42 → Deviation 124
- Why enrollment-relative features changed everything
- 3 models comparison with numbers
- Synthetic data generation (Dirichlet + O-U process)
- On-device deployment: Python → JSON → Kotlin

### Risk Engine & Security (3 min) — **Gautam**
- Hybrid detection: Z-score + ML fusion
- 4 risk levels with graduated response
- Live demo: normal use → behavior change → risk spike → alert
- Security audit findings (honest limitations)

### Results & Discussion (2 min) — **All together**
- Feature importance surprise (motion > keystrokes)
- Comparison with published research (8.71% EER vs literature's 5-12%)
- Future work: 30+ real participants, LSTM/Transformers, BiometricPrompt

### Q&A — **Respective owners based on topic**

---

## 📊 KEY NUMBERS CHEAT SHEET (Sab Yaad Karo!)

| Metric | Value |
|--------|-------|
| Random Forest Accuracy | 89.71% |
| Random Forest AUC | 0.979 |
| Random Forest EER | 8.71% |
| MLP Accuracy | 87.60% |
| MLP AUC | 0.938 |
| OCSVM Accuracy | 82.06% |
| Feature Dimensions | 124 (from 42 raw) |
| Total Participants | 58 (8 real + 50 synthetic) |
| Total Sessions | 379 |
| MLP Parameters | 26,273 |
| MLP Architecture | 124 → 128 → 64 → 32 → 1 |
| Model File Size | ~450 KB (JSON) |
| Inference Latency | <1ms |
| Full Analysis Cycle | ~200ms |
| Analysis Interval | Every 15 seconds |
| Z-score Weight in Fusion | 40% |
| ML Weight in Fusion | 60% |
| EMA Alpha | 0.3 |
| Risk Thresholds | 0.2 / 0.4 / 0.6 / 0.8 |

---

## ⚠️ TRICKY QUESTIONS & ANSWERS (Sabke Liye)

### Q: "Sirf 8 real participants se kaise reliable results milenge?"
> **A:** "Valid concern hai. Isliye humne calibrated synthetic data generator banaya — Dirichlet blending + age-adjusted noise + O-U process. Par hum acknowledge karte hain ki 30+ real participants wala study next step hai. Ye research prototype hai."

### Q: "TensorFlow Lite kyun nahi use kiya?"
> **A:** "TFLite 5-15MB native libraries add karta hai, ABI-specific builds chahiye, aur model format conversion (`.tflite`) ka overhead hai. Humara MLP sirf 4 layers ka hai — plain matrix multiplication Kotlin mein implement karna simpler, lighter, aur maintenance-free hai."

### Q: "Password plaintext kyun store hai?"
> **A:** "Ye intentionally research prototype hai. Security audit mein humne khud ye document kiya hai as CRITICAL vulnerability. Production mein BCrypt/Argon2 hashing + SQLCipher encryption lagana mandatory hai."

### Q: "Attacker agar behavioral pattern copy kar le?"
> **A:** "Typing rhythm copy possible hai (difficult but possible). Par simultaneously phone holding angle, touch pressure, swipe velocity match karna practically impossible hai — especially accelerometer features jo unconscious motor habits hain aur externally observable nahi hain."

### Q: "Battery impact kya hai?"
> **A:** "Sensors `SENSOR_DELAY_UI` pe chalte hain (~60ms), analysis sirf 15s interval pe hota hai, inference <1ms hai. Kisi participant ne noticeable battery drain report nahi kiya."

---

## 📚 REFERENCES (Important Ones to Remember)

1. **Killourhy & Maxion (2009)** — Keystroke dynamics benchmark, 51 subjects, EER 5-12%
2. **Frank et al. (2013)** — Touchalytics, 30-feature touch representation, EER 2-3%
3. **Bo et al. (2013)** — SilentSense, pressure + contact area, taps vs swipes separate signals
4. **Sitová et al. (2016)** — HMOG, accelerometer + gyroscope for hand movement, EER 7-10%
5. **ECB (2022)** — 23% rise in unauthorized mobile transactions
6. **Ketcham & Stelmach (2002)** — Age-related motor control decline (synthetic data basis)

---

> **Final Tip:** Viva mein confidence se bolo, numbers yaad rakho, aur jab limitation puche toh honestly accept karo — examiners ko sabse zyada ye impress karta hai ki tum apne project ki weaknesses jaante ho aur future work plan hai. 🎯
