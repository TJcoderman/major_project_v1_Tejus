# Security Audit & Improvement Recommendations

## Current Security Posture

### What's Done Well
1. **Behavioral biometrics layer** - Multi-modal (keystroke + touch + motion) creates a strong composite signal
2. **Z-score based anomaly detection** - Statistically sound approach with configurable thresholds
3. **Exponential smoothing** - Prevents false positives from single anomalous readings
4. **Real-time + periodic assessment** - Motion data triggers instant response; full analysis every 15s
5. **Graduated response** - Warning -> Re-auth -> Force logout (not just binary)
6. **Backup disabled** - `android:allowBackup="false"` in manifest
7. **ProGuard enabled** for release builds
8. **Data extraction rules** configured

### CRITICAL Vulnerabilities (Must Fix)

#### 1. PLAINTEXT PASSWORDS (Severity: CRITICAL)
**File**: `data/local/SecureBankDatabase.kt:87-107`
```kotlin
passwordHash = "demo123"  // NOT HASHED at all
```
**Fix**: Use BCrypt or Argon2 hashing. At minimum, use Android's `MessageDigest` with SHA-256 + salt.

#### 2. NO DATABASE ENCRYPTION (Severity: HIGH)
Room database is stored as plaintext SQLite on device filesystem.
**Fix**: Use SQLCipher with Room (`net.zetetic:android-database-sqlcipher`) or Android's encrypted shared preferences for sensitive fields.

#### 3. NO SESSION TOKEN MANAGEMENT (Severity: HIGH)
Sessions are just UUIDs stored in ViewModel memory. No expiration, no signed tokens, no server-side validation.
**Fix**: Implement JWT-like session tokens with HMAC signatures, expiration timestamps, and rotation.

#### 4. HARDCODED PIN (Severity: MEDIUM)
**File**: `data/model/ExperimentModels.kt:87`
```kotlin
val DEFAULT_PIN = "382946"
```
This is acceptable for research but must be removed for any production use.

#### 5. NO RATE LIMITING ON LOGIN (Severity: MEDIUM)
Unlimited login attempts with no delay, lockout, or captcha.
**Fix**: Implement exponential backoff after 3 failed attempts. Lock after 10 attempts.

#### 6. NO SECURE KEY STORAGE (Severity: MEDIUM)
Despite including `androidx.security:security-crypto`, it's not actually used anywhere.
**Fix**: Use `EncryptedSharedPreferences` for session tokens and sensitive config.

### HIGH-PRIORITY Improvements

#### 7. Add Certificate Pinning (for future network layer)
When adding server communication, implement certificate pinning to prevent MITM attacks.

#### 8. Add Root/Jailbreak Detection
Use SafetyNet/Play Integrity API to detect compromised devices.
```kotlin
// Example: Check for root indicators
fun isDeviceRooted(): Boolean {
    val paths = listOf("/system/bin/su", "/system/xbin/su", "/sbin/su")
    return paths.any { File(it).exists() }
}
```

#### 9. Add Screenshot Prevention
Prevent screen capture of sensitive banking screens.
```kotlin
// In MainActivity or per-screen
window.setFlags(
    WindowManager.LayoutParams.FLAG_SECURE,
    WindowManager.LayoutParams.FLAG_SECURE
)
```

#### 10. Add Biometric Authentication for Re-auth
When the system detects anomalies, use Android BiometricPrompt instead of password re-entry.

#### 11. Memory Protection
Sensitive strings (passwords, PINs) should be cleared from memory after use.
```kotlin
// Use CharArray instead of String for passwords
// Arrays can be zeroed; Strings stay in the string pool
```

#### 12. Anti-Tampering
Add integrity checks:
- APK signature verification at runtime
- Detect debugger attachment (`Debug.isDebuggerConnected()`)
- Code obfuscation beyond ProGuard (consider R8 full mode or DexGuard)

### MEDIUM-PRIORITY Improvements

#### 13. Session Timeout
No session expiration exists. Add:
- Idle timeout (5 minutes of no interaction)
- Absolute timeout (30 minutes max session)
- Background timeout (1 minute when app goes to background)

#### 14. Keystroke Data Residuals
Keystroke and behavioral data is never automatically cleaned up. Old data accumulates.
**Fix**: The `cleanupOldData()` method exists but is never called. Wire it to a periodic WorkManager job.

#### 15. Export Data Protection
CSV exports to device storage are unencrypted and contain behavioral fingerprints.
**Fix**: Encrypt exports or at minimum require authentication before export.

#### 16. Input Validation
Transfer fields have basic validation but no:
- Account number format validation
- SQL injection prevention (Room handles this, but raw queries should be audited)
- Amount overflow/underflow checks

#### 17. Logging in Production
Toast messages show security state to the user. Ensure debug toasts and `debugMode` default to `false` in release builds.

### ML-Specific Security

#### 18. Model Poisoning Prevention
If the enrollment baseline can be poisoned (attacker performs enrollment), the entire system is compromised. Consider:
- Multi-session enrollment (require N sessions across different days)
- Anomaly detection during enrollment itself
- Admin verification of enrollment quality

#### 19. Adversarial Attack Resistance
An informed attacker could train to mimic a victim's behavioral patterns. Countermeasures:
- Use high-entropy features that are hard to consciously replicate (gyroscope micro-patterns, touch pressure)
- Temporal drift detection (behavioral profiles evolve; the baseline should update gradually)
- Multi-factor: combine behavioral biometrics with traditional 2FA

#### 20. Data Privacy
Behavioral data is highly personal (can identify individuals). Ensure:
- Data minimization (don't store raw data longer than needed)
- Anonymization of research exports
- GDPR/CCPA compliance if deployed outside research context
