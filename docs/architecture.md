# SecureBank - System Architecture

## Project Overview

SecureBank is an Android banking application that uses **behavioral biometrics** to detect session hijacking in real-time. It continuously captures keystroke dynamics, touch patterns, and device motion to build a behavioral fingerprint of the authentic user, then flags anomalies when an impostor takes over.

## High-Level Architecture

```
+--------------------------------------------------+
|                  Android App (Kotlin)             |
|                                                    |
|  +-----------+   +-----------+   +-------------+  |
|  |  UI Layer |   |  Domain   |   |  Data Layer |  |
|  | (Compose) |-->|  (Logic)  |-->|  (Room DB)  |  |
|  +-----------+   +-----------+   +-------------+  |
|        |               |               |           |
|  +-----v-----+   +----v------+   +----v--------+  |
|  | ViewModels|   | Behavior  |   | Repositories |  |
|  |           |   | Analyzer  |   |              |  |
|  +-----------+   +-----------+   +--------------+  |
|        |               ^                           |
|  +-----v---------------+------------------------+  |
|  |            Sensor Layer (Collectors)           |  |
|  |  Keystroke | Touch | Accelerometer | Gyroscope|  |
|  +-----------------------------------------------+  |
+--------------------------------------------------+
        |
        | CSV Export
        v
+--------------------------------------------------+
|            Research/ML Pipeline (Python)           |
|                                                    |
|  Feature Extraction --> Model Training --> Eval   |
|  (52 features)         (RF, MLP, OCSVM)           |
+--------------------------------------------------+
```

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Language | Kotlin 1.9+ |
| UI Framework | Jetpack Compose + Material 3 |
| DI | Hilt (Dagger) |
| Database | Room (SQLite) |
| Navigation | Compose Navigation |
| Async | Kotlin Coroutines + Flow |
| Security | AndroidX Security Crypto |
| ML Pipeline | Python (scikit-learn, pandas, numpy, scipy) |
| Build | Gradle KTS, KSP |
| Min SDK | 26 (Android 8.0) |
| Target SDK | 34 (Android 14) |

## Data Flow

1. **Login Phase**: User types credentials -> KeystrokeCollector captures dwell/flight times -> stored as **baseline**
2. **Active Session**: Continuous collection of keystrokes, touches, and motion sensor data
3. **Risk Assessment**: BehaviorAnalyzer compares current data against baseline every 15 seconds
4. **Response**: LOW -> continue | MEDIUM -> warning | HIGH -> re-auth | CRITICAL -> force logout
5. **Experiment Mode**: Controlled data collection with enrollment/genuine/impostor sessions
6. **Export**: CSV files per participant for offline ML training

## Module Dependency Graph

```
SecureBankApplication
  └── MainActivity
       └── NavGraph
            ├── LoginScreen      -> AuthViewModel
            ├── DashboardScreen  -> BankingViewModel
            ├── TransferScreen   -> BankingViewModel
            └── ExperimentHub    -> ExperimentViewModel
                                      |
All ViewModels depend on:             v
  ├── BehavioralRepository (Room DAOs)
  ├── UserRepository
  ├── BehaviorAnalyzer (domain logic)
  ├── FeatureExtractor (52-feature vector)
  └── Sensor Collectors (Keystroke, Touch, Motion)
```
