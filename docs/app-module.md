# `app/` - Android Application Module

## Directory Structure

```
app/src/main/java/com/securebank/app/
├── MainActivity.kt              # Entry point, sets up Compose + Hilt
├── SecureBankApplication.kt     # Hilt Application class
├── data/
│   ├── export/
│   │   └── DataExporter.kt      # CSV export engine for ML training
│   ├── local/
│   │   ├── dao/                  # Room DAO interfaces
│   │   │   ├── BehavioralDao.kt  # Keystroke, Touch, Motion, Session DAOs
│   │   │   └── UserDao.kt       # User & Transaction DAOs
│   │   └── SecureBankDatabase.kt # Room DB definition, seeding
│   ├── model/
│   │   ├── BehavioralData.kt    # KeystrokeData, TouchData, MotionData, Sessions, Risk models
│   │   ├── ExperimentModels.kt  # Participant, Session types, PIN config, prompts
│   │   └── UserModels.kt       # User, Transaction, AuthState
│   └── repository/
│       ├── BehavioralRepository.kt  # Keystroke/Touch/Motion/Session CRUD
│       └── UserRepository.kt       # Auth + banking operations
├── di/
│   └── AppModule.kt            # Hilt DI module (DB + DAO providers)
├── domain/
│   ├── BehaviorAnalyzer.kt     # Core anomaly detection engine (Z-score based)
│   └── FeatureExtractor.kt     # 52-feature vector extraction for ML
├── sensor/
│   ├── KeystrokeCollector.kt   # Dwell time + flight time capture
│   ├── SensorDataCollector.kt  # Accelerometer + gyroscope (moving avg filter)
│   └── TouchDataCollector.kt   # Tap/swipe/scroll classification + metrics
└── ui/
    ├── components/
    │   ├── CustomPinPad.kt     # PIN keypad with real dwell time capture
    │   ├── SecurityComponents.kt # Alert dialogs, risk indicators
    │   └── TouchCaptureWrapper.kt # Compose pointer input wrapper
    ├── navigation/
    │   └── NavGraph.kt         # Navigation routes + screen wiring
    ├── screens/
    │   ├── DashboardScreen.kt  # Balance, transactions, risk display, debug panel
    │   ├── ExperimentScreens.kt # Experiment hub + session screens
    │   ├── LoginScreen.kt      # Login with keystroke baseline capture
    │   └── TransferScreen.kt   # Fund transfer with behavioral monitoring
    ├── theme/
    │   └── Theme.kt            # Material 3 theme
    └── viewmodel/
        ├── AuthViewModel.kt      # Login/logout + baseline initialization
        ├── BankingViewModel.kt    # Banking ops + continuous monitoring
        └── ExperimentViewModel.kt # Experiment workflow management
```

## Key Concepts

### Behavioral Data Collection (3 modalities)
1. **Keystroke Dynamics**: Dwell time (key hold duration) and flight time (inter-key gap). PIN pad gives real hardware-level dwell; software keyboard uses estimated 80ms.
2. **Touch Dynamics**: Tap/swipe classification, pressure, velocity, acceleration, hold duration, spatial entropy.
3. **Motion Sensors**: Accelerometer + gyroscope with moving average noise filter. Calculates pitch/roll/azimuth and infers device state (table, hand-held, walking).

### Risk Scoring Algorithm
- **Weights**: Keystroke 35%, Touch 30%, Motion 35%
- **Method**: Z-score deviation from baseline with exponential smoothing
- **Thresholds**: LOW < 0.2, MEDIUM < 0.4, HIGH < 0.6, CRITICAL < 0.8
- **Real-time**: Motion data triggers instant risk updates; full assessment every 15 seconds

### Feature Vector (52 features)
- Keystroke: 12 features (mean/std dwell, flight, speed, digraphs, CV)
- Touch: 18 features (area, duration, velocity, ratios, entropy, angles)
- Motion: 22 features (accel/gyro per-axis, magnitude, orientation, state transitions)

### Experiment Protocol
- **Enrollment**: 5 PIN attempts + 3 text prompts + touch tasks + free browsing
- **Genuine**: 3 PIN attempts + 2 text prompts (same user)
- **Impostor**: 3 PIN attempts + 2 text prompts (different user)
- Standard PIN: `382946`, standard texts defined in `PromptedTexts`

### Database (Room)
- 6 entities: User, Transaction, KeystrokeData, TouchData, MotionData, BehavioralSession
- Schema version 2, destructive migration enabled
- Seeded with 3 mock users (demo/john/jane) and sample transactions
