# SecureBank - Behavioral Continuous Authentication Demo

A mock banking Android application that demonstrates **Continuous Authentication** using behavioral biometrics. This app detects potential session hijacking by analyzing user behavior patterns in real-time.

## 🎯 Project Purpose

This application serves as a **testbed** for researching and demonstrating behavioral authentication techniques. It is **NOT** intended for actual banking use.

### Core Concept
The app continuously monitors user behavior (typing patterns, touch dynamics, device motion) and compares it against a baseline established during login. Significant deviations trigger security alerts, potentially indicating session hijacking.

## 🏗️ Architecture

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose (Material Design 3)
- **Architecture**: MVVM + Clean Architecture
- **Dependency Injection**: Hilt
- **Async Processing**: Coroutines & Flow
- **Local Storage**: Room Database

```
com.securebank.app/
├── data/
│   ├── local/         # Room database & DAOs
│   ├── model/         # Data classes
│   └── repository/    # Data repositories
├── di/                # Hilt modules
├── domain/            # Business logic (BehaviorAnalyzer)
├── sensor/            # Behavioral data collectors
├── ui/
│   ├── components/    # Reusable UI components
│   ├── navigation/    # Navigation graph
│   ├── screens/       # Screen composables
│   ├── theme/         # Material theme
│   └── viewmodel/     # ViewModels
└── MainActivity.kt
```

## 📊 Behavioral Biometrics Captured

### 1. Keystroke Dynamics
- **Dwell Time**: Duration a key is held down
- **Flight Time**: Time between releasing one key and pressing the next
- **Typing Speed**: Characters per minute

### 2. Touch Dynamics
- **Touch Pressure**: Force applied during touch
- **Swipe Velocity**: Speed of swipe gestures
- **Touch Patterns**: Taps, swipes, scrolls, long presses

### 3. Device Motion (Accelerometer & Gyroscope)
- **Device Orientation**: Pitch, roll, azimuth angles
- **Device State**: Held in hand, on table, walking, etc.
- **Movement Patterns**: Filtered sensor data

## 🔐 Anomaly Detection

The `BehaviorAnalyzer` compares current behavior against the login baseline:

```kotlin
Risk Score = (Keystroke Deviation × 0.35) + 
             (Touch Deviation × 0.30) + 
             (Motion Deviation × 0.35)
```

### Risk Levels
| Level | Score Range | Action |
|-------|-------------|--------|
| LOW | 0% - 30% | Continue normally |
| MEDIUM | 30% - 60% | Show warning (debug toast) |
| HIGH | 60% - 80% | Request re-authentication |
| CRITICAL | 80% - 100% | Force logout |

## 🚀 Getting Started

### Prerequisites
- Android Studio Hedgehog (2023.1.1) or later
- JDK 17
- Android SDK 34
- Physical device recommended (for accurate sensor data)

### Build & Run

1. Clone the repository
2. Open in Android Studio
3. Sync Gradle files
4. Run on device/emulator

### Demo Credentials
```
Username: demo
Password: demo123
```

## 📱 App Features

### Login Screen
- Custom keystroke timing capture during password entry
- Establishes behavioral baseline for the session

### Dashboard
- Displays mock account balance and transactions
- Shows real-time risk score indicator
- Debug panel for behavioral metrics

### Transfer Screen
- Simulated fund transfer functionality
- Captures keystroke and touch patterns
- Demonstrates continuous monitoring

## 🔧 Key Components

### SensorDataCollector
Manages accelerometer and gyroscope data collection with:
- Moving average filter for noise reduction
- Device state inference (held, table, walking)
- Efficient batch processing

### TouchDataCollector
Captures touch events via Compose's `pointerInput`:
- Touch classification (tap, swipe, scroll)
- Velocity and acceleration calculation
- Pressure tracking

### KeystrokeCollector
Records typing patterns from text field changes:
- Dwell time estimation for software keyboards
- Flight time between characters
- Baseline storage during login

### BehaviorAnalyzer
Core anomaly detection engine:
- Weighted deviation calculation
- Exponential smoothing for stability
- Configurable thresholds

## ⚙️ Configuration

Adjust thresholds in `BehaviorAnalyzer.kt`:

```kotlin
const val LOW_THRESHOLD = 0.2f       // 20% deviation
const val MEDIUM_THRESHOLD = 0.4f    // 40% deviation
const val HIGH_THRESHOLD = 0.6f      // 60% deviation
const val CRITICAL_THRESHOLD = 0.8f  // 80% deviation
```

## 📝 Edge Cases Handled

1. **Screen Rotation**: Sensor data continuity maintained via Hilt singletons
2. **Background State**: Sensors stopped when app is paused (battery/privacy)
3. **Permissions**: Motion sensors don't require runtime permissions
4. **Noise Filtering**: Moving average filter on accelerometer data

## 🧪 Testing Scenarios

### Simulate Session Hijacking
1. Login with demo credentials
2. Note your typing speed and device position
3. Dramatically change your behavior:
   - Type much faster/slower
   - Hold phone at different angle
   - Use different finger for touches
4. Observe risk score increase in debug panel

## 📄 License

This project is for educational and research purposes only.

## 🤝 Contributing

Contributions welcome! Please focus on:
- Improved ML-based anomaly detection
- Additional behavioral features
- Better noise filtering algorithms
- Cross-session baseline learning

---

**Disclaimer**: This is a demonstration application. Do not use for actual financial transactions.

