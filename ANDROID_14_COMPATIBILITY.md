# Android 14+ Compatibility Updates for SMRS

This document summarizes all changes made to ensure the Student App and Clinic Terminal are fully compatible with Android 14 (API level 34) and later.

## Overview

Both applications have been updated to meet Android 14+ requirements:
- **compileSdk**: 34 (Android 14)
- **targetSdk**: 34 (Android 14)
- **minSdk**: 26 (Android 8.0) - maintains backward compatibility

---

## Student App Updates

### 1. **Dependencies Updated** (`app/build.gradle`)
```gradle
// Updated to latest Android 14+ compatible versions
implementation 'androidx.appcompat:appcompat:1.7.0'           // was 1.6.1
implementation 'androidx.core:core:1.13.1'                    // newly added
implementation 'com.google.android.material:material:1.12.0'  // was 1.11.0
```

**Why**: Latest versions include bug fixes, security patches, and proper Android 14 support.

### 2. **Java Compiler Version** (`app/build.gradle`)
```gradle
compileOptions {
    sourceCompatibility JavaVersion.VERSION_11   // was VERSION_1_8
    targetCompatibility JavaVersion.VERSION_11   // was VERSION_1_8
}
```

**Why**: Java 11 is better optimized for Android 14 and modern Gradle builds.

### 3. **Manifest Permissions** (`app/src/main/AndroidManifest.xml`)
```xml
<!-- Added for Android 14+ notification support -->
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
```

**Why**: Android 14 requires explicit permission for notifications.

### 4. **Runtime NFC Permission Handling** (`NfcTapActivity.java`)

**Added imports:**
```java
import android.content.pm.PackageManager;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
```

**Added permission constants:**
```java
private static final int NFC_PERMISSION_REQUEST_CODE = 100;
private static final String NFC_PERMISSION = "android.permission.NFC";
```

**Permission check in onCreate():**
```java
// Request NFC permission for Android 14+ (API 34+)
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
    if (ContextCompat.checkSelfPermission(this, NFC_PERMISSION)
            != PackageManager.PERMISSION_GRANTED) {
        ActivityCompat.requestPermissions(this,
            new String[]{NFC_PERMISSION},
            NFC_PERMISSION_REQUEST_CODE);
    }
}
```

**Permission result handler:**
```java
@Override
public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    if (requestCode == NFC_PERMISSION_REQUEST_CODE) {
        if (grantResults.length > 0 && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
            setStatus("❌ NFC permission denied. This app requires NFC access.", "#FCEBEB", "#A32D2D");
        }
    }
}
```

**Why**: Android 14 requires runtime permission checks for NFC, even though it's declared in the manifest.

### 5. **Intent Flags for Android 14+** (Multiple files)

**In NfcTapActivity.navigateHome():**
```java
Intent intent = new Intent(this, HomeActivity.class);
intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
startActivity(intent);
```

**In BaseActivity - Re-tap button:**
```java
Intent intent = new Intent(this, NfcTapActivity.class);
intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
startActivity(intent);
```

**In BaseActivity - Bottom navigation:**
```java
Intent intent = new Intent(this, Class.forName(cls));
intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
startActivity(intent);
```

**Why**: Android 14 requires explicit intent flags to prevent app crashes and ensure proper activity stack management.

---

## Clinic Terminal App Updates

### 1. **Dependencies Updated** (`app/build.gradle`)
```gradle
implementation 'androidx.appcompat:appcompat:1.7.0'    // was 1.6.1
implementation 'androidx.core:core:1.13.1'             // newly added
```

**Why**: Same as student app - latest versions with Android 14 support.

### 2. **Existing Android 14+ Compatibility**

The Clinic Terminal already had good Android 14+ practices:

✅ **Proper exported component attributes:**
```xml
<activity android:name=".MainActivity" android:exported="true" />
<service android:name=".NfcHceService" android:exported="true" />
```

✅ **Background threading with ExecutorService:**
```java
private final ExecutorService executor = Executors.newSingleThreadExecutor();
```

✅ **Main thread UI updates with Handler:**
```java
private final Handler mainHandler = new Handler(Looper.getMainLooper());
mainHandler.post(() -> onStudentLoaded(...));
```

✅ **No network calls on main thread** - prevents `NetworkOnMainThreadException`

✅ **Proper error handling** - Shows user-friendly error dialogs

---

## Android 14+ Key Changes Explained

### 1. **Runtime Permissions**
- Android 14 requires runtime permission checks even for permissions already declared in manifest
- NFC permission must be requested at runtime on devices running Android 14+

### 2. **Intent Flags**
- `FLAG_ACTIVITY_NEW_TASK` - Proper task management
- `FLAG_ACTIVITY_CLEAR_TASK` - Clears the task stack
- Required to prevent crashes and ensure proper navigation

### 3. **Exported Components**
- All activities/services with intent-filters must have `android:exported` attribute
- Prevents crashes on Android 12+ when attribute is missing

### 4. **Network on Main Thread**
- Strict Mode enforcement is stricter in Android 14
- All network calls must be on background threads
- Clinic Terminal already handles this correctly

### 5. **Java Version**
- Java 11 support is better optimized for newer Android
- Improves performance and compatibility

---

## Testing Checklist

Before deploying, verify:

- [ ] App launches successfully on Android 14+ device
- [ ] NFC permission request appears on first launch
- [ ] NFC tapping works after granting permission
- [ ] Navigation between screens works smoothly
- [ ] Student data loads correctly from clinic terminal
- [ ] All buttons and interactive elements work
- [ ] No crashes in logcat
- [ ] App works on multiple Android versions (8.0+)

---

## Deployment Notes

### For Google Play Store
- Update `versionCode` if releasing as new version
- Test on actual devices running Android 14+
- Check Android Studio's Lint warnings (should be minimal now)

### For Building
```bash
# Student App
cd student_app
./gradlew clean build

# Clinic Terminal
cd clinic_terminal
./gradlew clean build
```

### Installation
```bash
# Install student app
adb install -r student_app/app/build/outputs/apk/debug/app-debug.apk

# Install clinic terminal app
adb install -r clinic_terminal/app/build/outputs/apk/debug/app-debug.apk
```

---

## Backward Compatibility

✅ All changes maintain backward compatibility with:
- Android 8.0 (API 26, minSdk)
- Android 9, 10, 11, 12, 13
- Android 14+ (API 34, targetSdk)

No existing functionality was removed or modified in breaking ways.

---

## Summary of Changes

| Component | Old | New | Reason |
|-----------|-----|-----|--------|
| compileSdk | 34 | 34 | Already correct |
| targetSdk | 34 | 34 | Already correct |
| AndroidX AppCompat | 1.6.1 | 1.7.0 | Bug fixes & Android 14 support |
| Material Design | 1.11.0 | 1.12.0 | Android 14 improvements |
| Java | VERSION_1_8 | VERSION_11 | Better optimization |
| NFC Permission | Manifest only | Runtime check | Android 14 requirement |
| Intent Flags | Missing/incomplete | Added explicit flags | Android 14 requirement |

---

## References

- [Android 14 Migration Guide](https://developer.android.com/about/versions/14)
- [Jetpack AndroidX Libraries](https://developer.android.com/jetpack)
- [NFC API Documentation](https://developer.android.com/guide/topics/connectivity/nfc)
- [Runtime Permissions Guide](https://developer.android.com/training/permissions/requesting)

