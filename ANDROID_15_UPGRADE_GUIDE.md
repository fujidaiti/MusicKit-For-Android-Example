# Android MusicKit SDK - Android 15 Upgrade Guide

This document outlines all changes made to upgrade the Android MusicKit SDK test app from Android API 28 to Android 15 (API 35).

## Table of Contents
- [Overview](#overview)
- [Build System Updates](#build-system-updates)
- [Manifest Changes](#manifest-changes)
- [Code Modernization](#code-modernization)
- [Layout File Updates](#layout-file-updates)
- [Testing](#testing)

---

## Overview

**Original Version:**
- Android API Level: 28 (Android 9)
- Gradle Plugin: 3.4.1
- Build Tools: 28.0.3
- Java: 8

**Upgraded Version:**
- Android API Level: 35 (Android 15)
- Gradle Plugin: 8.5.2
- Build Tools: 34.0.0
- Java: 17
- Gradle: 8.7

---

## Build System Updates

### 1. Updated `build.gradle`

**Location:** `/build.gradle`

**Why:** Android Gradle Plugin 8.x is required to support Android 15 (API 35) and includes critical bug fixes, performance improvements, and new build features. The old AGP 3.4.1 from 2019 doesn't support modern Android versions.

**Key Changes Explained:**

| Change | Reason |
|--------|--------|
| **jcenter() → mavenCentral()** | JCenter was shut down in 2021. All artifacts must come from Maven Central now. |
| **AGP 3.4.1 → 8.5.2** | Required for Android 15 support. AGP 8.x requires Java 17 and Gradle 8.x. |
| **namespace declaration** | AGP 8+ moved package declaration from AndroidManifest.xml to build.gradle for better modularity. |
| **Java 8 → Java 17** | AGP 8.x requires Java 17. Many modern Android APIs also require newer Java features. |
| **compileSdk 28 → 35** | Must match or exceed targetSdk. Needed to access Android 15 APIs and features. |
| **targetSdk 28 → 35** | Apps targeting old SDKs get compatibility behaviors that may hide bugs. Google Play requires recent targetSdk. |
| **Removed dexOptions** | Deprecated in AGP 4.2. Modern AGP automatically optimizes DEX compilation. |

```gradle
buildscript {
    repositories {
        google()
        mavenCentral()  // Replaced jcenter()
    }
    dependencies {
        classpath 'com.android.tools.build:gradle:8.5.2'  // Was: 3.4.1
    }
}

repositories {
    google()
    mavenCentral()  // Replaced jcenter()
    flatDir {
        dirs 'src/main/libs'
    }
}

android {
    namespace 'com.apple.android.music.sdk.testapp'  // New: Required for AGP 8+
    buildToolsVersion '34.0.0'  // Was: 28.0.3
    compileSdkVersion 35  // Was: 28

    compileOptions {
        sourceCompatibility JavaVersion.VERSION_17  // Was: VERSION_1_8
        targetCompatibility JavaVersion.VERSION_17
    }

    defaultConfig {
        applicationId 'com.apple.android.music.sdk.testapp'
        versionCode 1
        versionName '1.0'
        minSdkVersion 21
        targetSdkVersion 35  // Was: 28
    }

    // Removed deprecated dexOptions block
}

dependencies {
    annotationProcessor 'androidx.annotation:annotation:1.8.0'  // Was: 1.1.0
    implementation 'androidx.appcompat:appcompat:1.6.1'  // New
    implementation 'androidx.activity:activity:1.8.2'  // New
    implementation 'androidx.fragment:fragment:1.6.2'  // New
    implementation 'androidx.media:media:1.7.0'  // Was: 1.0.0
    implementation 'com.google.android.material:material:1.11.0'  // Was: 1.1.0-alpha07
    implementation 'com.squareup.picasso:picasso:2.71828'  // Was: 2.5.2

    implementation (name: 'mediaplayback-release-1.1.1', ext: 'aar', group: 'com.apple.android.music', version: '1.1.1')
    implementation (name: 'musickitauth-release-1.1.2', ext: 'aar', group: 'com.apple.android.music', version: '1.1.2')
}
```

### 2. Created `gradle.properties`

**Location:** `/gradle.properties`

**Why:** AndroidX is required for modern Android development. The `android.useAndroidX=true` flag is mandatory when using AndroidX dependencies (all modern libraries). Without it, the build will fail with package resolution errors.

```properties
# Enable AndroidX
android.useAndroidX=true

# Automatically convert third-party libraries to use AndroidX
android.enableJetifier=true

# Enable incremental annotation processing
android.enableIncrementalAnnotationProcessing=true
```

### 3. Created Gradle Wrapper

**Location:** `/gradle/wrapper/gradle-wrapper.properties`

**Why:** Gradle wrapper ensures everyone uses the same Gradle version, avoiding "works on my machine" issues. AGP 8.5.2 requires Gradle 8.2+ for compatibility.

```properties
distributionBase=GRADLE_USER_HOME
distributionPath=wrapper/dists
distributionUrl=https\://services.gradle.org/distributions/gradle-8.7-bin.zip
zipStoreBase=GRADLE_USER_HOME
zipStorePath=wrapper/dists
```

Also created `/gradlew` script and made it executable.

---

## Manifest Changes

### 1. Updated `AndroidManifest.xml`

**Location:** `/src/main/AndroidManifest.xml`

#### Removed package attribute

**Why:** AGP 8+ requires the package/namespace to be declared in `build.gradle` instead of `AndroidManifest.xml`. This provides better separation of concerns and prevents conflicts in multi-module projects.

```xml
<!-- OLD: package attribute removed (now in build.gradle as namespace) -->
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools">
```

#### Added Package Visibility Queries (Critical for Android 11+)

**Why:** Android 11 (API 30) introduced package visibility restrictions for privacy and security. Apps can no longer see all installed apps by default. Without the `<queries>` element, `PackageManager.getPackageInfo("com.apple.android.music")` throws `NameNotFoundException` even when Apple Music is installed. This is THE most critical fix for making the SDK work on modern Android.
```xml
<!-- Required to detect if Apple Music app is installed on Android 11+ -->
<queries>
    <!-- Apple Music app package -->
    <package android:name="com.apple.android.music" />

    <!-- Intent to launch Apple Music -->
    <intent>
        <action android:name="android.intent.action.VIEW" />
        <data android:scheme="musicsdk" />
    </intent>

    <!-- Market intent for fallback -->
    <intent>
        <action android:name="android.intent.action.VIEW" />
        <data android:scheme="market" />
    </intent>
</queries>
```

#### Added New Permissions

**Why:**
- `FOREGROUND_SERVICE_MEDIA_PLAYBACK`: Required by Android 14+ (API 34) for foreground services that play media. Without it, the service crashes when calling `startForeground()`.
- `INTERNET`: Required for the SDK to communicate with Apple Music servers. Previously might have worked through implicit permissions, now must be explicit.

```xml
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK" />
<uses-permission android:name="android.permission.INTERNET" />
```

#### Updated Component Declarations

**Why:**
- `android:exported="true"`: Required by Android 12+ (API 31) for all components with intent filters. The manifest merger fails without explicit declaration.
- `android:foregroundServiceType="mediaPlayback"`: Required by Android 14+ (API 34) to declare what type of foreground service this is. Improves user privacy by letting the system enforce appropriate restrictions.
- AndroidX migration: `android.support.v4.media.session.MediaButtonReceiver` no longer exists in modern AndroidX.
```xml
<!-- MainActivity -->
<activity android:name=".activity.MainActivity"
    android:screenOrientation="portrait"
    android:exported="true">  <!-- Added: Required for Android 12+ -->
    <intent-filter>
        <action android:name="android.intent.action.MAIN"/>
        <category android:name="android.intent.category.LAUNCHER"/>
    </intent-filter>
</activity>

<!-- MediaPlaybackService -->
<service android:name=".service.MediaPlaybackService"
    android:exported="true"
    android:foregroundServiceType="mediaPlayback">  <!-- Added: Required for Android 14+ -->
    <intent-filter>
        <action android:name="android.media.browse.MediaBrowserService"/>
    </intent-filter>
</service>

<!-- MediaButtonReceiver - Migrated to AndroidX -->
<receiver android:name="androidx.media.session.MediaButtonReceiver"  <!-- Was: android.support.v4 -->
    android:exported="true">
    <intent-filter>
        <action android:name="android.intent.action.MEDIA_BUTTON" />
    </intent-filter>
</receiver>

<!-- Apple SDK Activity -->
<activity android:name="com.apple.android.sdk.authentication.SDKUriHandlerActivity"
    android:exported="true"
    tools:replace="android:exported" />
```

---

## Code Modernization

### 1. Replaced AsyncTask (Deprecated in API 30)

**File:** `/src/main/java/com/apple/android/music/sdk/testapp/util/LocalMediaProvider.java`

**Why:** AsyncTask was deprecated in Android 11 (API 30) and removed in API 31 due to:
- Poor handling of configuration changes (memory leaks)
- Difficult lifecycle management
- Hidden threading issues
- No way to cancel properly

Modern alternatives like `ExecutorService` + `Handler` provide better control, testability, and lifecycle safety.

**Before:**
```java
private static class DataLoader extends AsyncTask<Void, Void, Void> {
    @Override
    protected Void doInBackground(Void... params) {
        // ...
    }
}
new DataLoader(applicationContext, parentId, result).execute();
```

**After:**
```java
private final ExecutorService executorService;
private final Handler mainHandler;

public LocalMediaProvider(Context context) {
    applicationContext = context.getApplicationContext();
    executorService = Executors.newSingleThreadExecutor();
    mainHandler = new Handler(Looper.getMainLooper());
}

private static class DataLoader implements Runnable {
    private final Handler mainHandler;

    @Override
    public void run() {
        try {
            final List<MediaBrowserCompat.MediaItem> items = readItemsFromFile(applicationContext, fileName);
            mainHandler.post(() -> result.sendResult(items));
        } catch (IOException e) {
            mainHandler.post(() -> result.sendResult(Collections.emptyList()));
        }
    }
}

executorService.execute(new DataLoader(applicationContext, parentId, result, mainHandler));
```

### 2. Migrated Fragment API

**Files:**
- `/src/main/java/com/apple/android/music/sdk/testapp/activity/MainActivity.java`
- All fragment files in `/src/main/java/com/apple/android/music/sdk/testapp/fragment/`

**Why:** The old `android.app.Fragment` API was deprecated in Android 9 (API 28) and removed in later versions because:
- Poor lifecycle handling
- Inconsistent behavior across Android versions
- No support for modern features like Activity Result API

AndroidX Fragments (`androidx.fragment.app.Fragment`) provide better lifecycle management, ViewModel integration, and are actively maintained.

**Before:**
```java
import android.app.Fragment;
import android.app.FragmentTransaction;

getFragmentManager().beginTransaction()...
```

**After:**
```java
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

getSupportFragmentManager().beginTransaction()...
```

### 3. Replaced Switch Statements with If-Else (R.id no longer final)

**Files:**
- `MainActivity.java`
- `NowPlayingFragment.java`
- `BrowseFragment.java`

**Why:** Starting with Android Gradle Plugin 8.0, resource IDs (R.id.*) are no longer `final static` constants - they're generated as regular `static` fields. This change:
- Improves build performance (incremental builds don't need to recompile everything when resources change)
- Reduces APK size
- But breaks `switch` statements which require compile-time constants

Solution: Use if-else chains instead of switch statements for resource IDs.

**Before:**
```java
@Override
public boolean onNavigationItemSelected(@NonNull MenuItem item) {
    switch (item.getItemId()) {
        case R.id.navitem_browse:
            navigateToBrowse(null, null, true);
            return true;
        case R.id.navitem_now_playing:
            navigateToNowPlaying(true);
            return true;
    }
    return false;
}
```

**After:**
```java
@Override
public boolean onNavigationItemSelected(@NonNull MenuItem item) {
    int itemId = item.getItemId();
    if (itemId == R.id.navitem_browse) {
        navigateToBrowse(null, null, true);
        return true;
    } else if (itemId == R.id.navitem_now_playing) {
        navigateToNowPlaying(true);
        return true;
    }
    return false;
}
```

### 4. Updated Picasso API

**Files:**
- `MediaBrowserAdapter.java`
- `QueueItemAdapter.java`
- `MediaSessionManager.java`

**Why:** Picasso 2.71828 (updated from 2.5.2) introduced breaking API changes:
- `Picasso.with(context)` → `Picasso.get()`: Changed to singleton pattern for better performance and memory management
- `onBitmapFailed(Drawable)` → `onBitmapFailed(Exception, Drawable)`: Added exception parameter to help debug image loading failures
- Better OkHttp integration and bug fixes for modern Android

**Before:**
```java
Picasso.with(context).load(url).into(target);

// Target interface
@Override
public void onBitmapFailed(Drawable drawable) { }
```

**After:**
```java
Picasso.get().load(url).into(target);

// Target interface
@Override
public void onBitmapFailed(Exception e, Drawable drawable) { }
```

### 5. Removed RemoteException Handling

**Files:**
- `MainActivity.java`
- `PlaybackNotificationManager.java`

**Why:** In newer versions of AndroidX Media, the `MediaControllerCompat` constructor no longer throws `RemoteException`. The internal implementation was refactored to handle remote exceptions internally. Keeping the try-catch causes a compilation error: "exception RemoteException is never thrown in body of corresponding try statement."

**Before:**
```java
try {
    mediaController = new MediaControllerCompat(this, mediaBrowser.getSessionToken());
} catch (RemoteException e) {
    // Handle exception
}
```

**After:**
```java
// MediaControllerCompat constructor no longer throws RemoteException
mediaController = new MediaControllerCompat(this, mediaBrowser.getSessionToken());
```

### 6. Migrated to Activity Result API

**File:** `/src/main/java/com/apple/android/music/sdk/testapp/fragment/SettingsFragment.java`

**Why:** `startActivityForResult()` and `onActivityResult()` were deprecated in Android 10 (API 29) due to:
- Poor lifecycle handling (results could arrive after fragment/activity is destroyed)
- No type safety (manual request code management)
- Difficult to test
- No way to enforce contracts

The new Activity Result API provides:
- Type-safe contracts
- Better lifecycle awareness
- Cleaner code with lambdas
- Separation of concerns (registration vs. launching)

**Before:**
```java
private static final int REQUESTCODE_APPLEMUSIC_AUTH = 3456;

startActivityForResult(intent, REQUESTCODE_APPLEMUSIC_AUTH);

@Override
public void onActivityResult(int requestCode, int resultCode, Intent data) {
    if (requestCode == REQUESTCODE_APPLEMUSIC_AUTH) {
        // Handle result
    }
}
```

**After:**
```java
// Import new APIs
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

// Register launcher in onCreate()
private ActivityResultLauncher<Intent> authLauncher;

@Override
public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    authLauncher = registerForActivityResult(
        new ActivityResultContracts.StartActivityForResult(),
        result -> {
            handleAuthenticationResult(result.getData());
        }
    );
}

// Launch activity
authLauncher.launch(intent);

// Handle result
private void handleAuthenticationResult(Intent data) {
    TokenResult tokenResult = authenticationManager.handleTokenResult(data);
    // Process result
}
```

### 7. Fixed PendingIntent Flags (Android 12+)

**File:** `/src/main/java/com/apple/android/music/sdk/testapp/service/PlaybackNotificationManager.java`

**Why:** Android 12 (API 31) requires explicit mutability flags for `PendingIntent` for security reasons:
- Prevents malicious apps from hijacking PendingIntents
- Forces developers to explicitly declare intent behavior
- Without `FLAG_IMMUTABLE` or `FLAG_MUTABLE`, the app crashes with `IllegalArgumentException`

`FLAG_IMMUTABLE` is used here because media notification intents don't need to be modified after creation (they're read-only).

**Before:**
```java
return PendingIntent.getService(context, mediaKeyCode, intent, 0);
```

**After:**
```java
return PendingIntent.getService(context, mediaKeyCode, intent,
    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
```

---

## Layout File Updates

### Replaced androidx.legacy.widget.Space with Space

**Files:**
- `/src/main/res/layout/list_item_media.xml`
- `/src/main/res/layout/fragment_now_playing.xml`
- `/src/main/res/layout/fragment_settings.xml`
- `/src/main/res/layout/list_item_up_next.xml`

**Why:** `androidx.legacy.widget.Space` was removed from AndroidX. It was a temporary compatibility shim during the AndroidX migration. The standard `android.widget.Space` (just `<Space>`) is the correct widget to use. Without this change, the app crashes at runtime with `ClassNotFoundException: androidx.legacy.widget.Space`.

**Before:**
```xml
<androidx.legacy.widget.Space
    android:layout_width="16dp"
    android:layout_height="match_parent"/>
```

**After:**
```xml
<Space
    android:layout_width="16dp"
    android:layout_height="match_parent"/>
```

---

## Testing

### Build the App
```bash
./gradlew clean assembleDebug
```

### Install on Device
```bash
adb install -r app/build/outputs/apk/sdk/debug/app-sdk-debug.apk
```

### Prerequisites
1. **Apple Music app must be installed** on the test device
2. **Valid Apple Music JWT Developer Token** must be configured in `src/main/res/values/strings.xml`

### Key Test Points
- ✅ App launches successfully
- ✅ Fragment navigation works
- ✅ Settings screen loads
- ✅ "Request Access" button triggers Apple Music authentication
- ✅ Apple Music app is detected (no `PackageManager.NameNotFoundException`)
- ✅ Media playback controls work
- ✅ No deprecation warnings in build

---

## Important Notes

### 1. Package Visibility (Critical!)
The `<queries>` element in AndroidManifest.xml is **critical** for Android 11+. Without it, the SDK cannot detect if Apple Music is installed, even if it is.

### 2. Apple SDK AAR Compatibility
The Apple MusicKit AAR libraries are from 2021 and were built for API 28. They still work on Android 15 with these changes, but extensive testing is recommended.

### 3. Media Package Names
The `androidx.media:media` library maintains backward compatibility by keeping the old `android.support.v4.media.*` package names. Do NOT try to change these to `androidx.media.*` - they don't exist!

### 4. Java 17 Requirement
Android Gradle Plugin 8.x requires Java 17. Make sure your development environment has JDK 17 installed.

---

## Summary of Key Changes

| Category | Changes |
|----------|---------|
| **Build System** | AGP 3.4.1 → 8.5.2, Gradle 8.7, Java 17, AndroidX enabled |
| **SDK Versions** | compileSdk 28 → 35, targetSdk 28 → 35 |
| **Manifest** | Added `<queries>`, foreground service types, `android:exported` |
| **Deprecated APIs** | Replaced AsyncTask, Fragment API, startActivityForResult |
| **Dependencies** | Updated all androidx libraries to latest versions |
| **Layouts** | Replaced androidx.legacy.widget.Space with Space |
| **Picasso** | Updated to 2.71828, API changes |
| **PendingIntent** | Added FLAG_IMMUTABLE for Android 12+ |

---

## References

- [Android 15 Behavior Changes](https://developer.android.com/about/versions/15/behavior-changes-15)
- [Package Visibility in Android 11+](https://developer.android.com/training/package-visibility)
- [Activity Result API](https://developer.android.com/training/basics/intents/result)
- [Foreground Service Types](https://developer.android.com/develop/background-work/services/fg-service-types)
- [Migration Guide (Japanese)](https://techblog.ldi.co.jp/entry/2025/08/20/162918)

---

**Last Updated:** 2025-10-19
**Gradle Version:** 8.7
**Android Gradle Plugin:** 8.5.2
**Target SDK:** 35 (Android 15)
