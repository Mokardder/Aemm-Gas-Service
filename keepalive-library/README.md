# Android KeepAlive native library

This module packages the existing KeepAlive JNI code and the `ServiceA`/`ServiceB` Android service classes as an Android AAR. The native C implementation and JNI wrapper remain unchanged. The service classes and their notification helper are included in the library so a consuming app does not need to copy them.

## Build the AAR

From the repository root, run:

```powershell
.\gradlew.bat :keepalive-library:assembleRelease
```

The AAR is generated at:

```text
keepalive-library/build/outputs/aar/keepalive-library-release.aar
```

The repository also includes a prebuilt copy at `keepalive-library/dist/keepalive-library-release.aar`.

It contains `libkeepalive.so` for `armeabi-v7a` and `arm64-v8a`, the `NativeDaemon`, `ServiceA`, and `ServiceB` classes, and a manifest that declares both services and the foreground-service permission.

## Service classes and manifest

The AAR contains these services:

- `ServiceA` starts a foreground notification, calls `NativeDaemon.startWatch(Process.myPid(), getPackageName())`, and starts `ServiceB`.
- `ServiceB` starts its own foreground notification and starts `ServiceA`. Both services return `START_STICKY` and start the other service from `onDestroy()`.
- `ServiceA.start(context)` starts the foreground service using the appropriate Android API for the OS version.

The AAR manifest automatically adds the following entries to the consuming app's merged manifest; normally, do not duplicate them in the app manifest:

```xml
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />

<application>
    <service
        android:name="android.iocl.keepAlive.ServiceA"
        android:enabled="true"
        android:exported="false"
        android:stopWithTask="false" />
    <service
        android:name="android.iocl.keepAlive.ServiceB"
        android:enabled="true"
        android:exported="false"
        android:stopWithTask="false" />
</application>
```

The library creates its own low-importance `KeepAlive service` notification channel. On Android 13 and later, the host app should request `POST_NOTIFICATIONS` at runtime if it wants notifications to appear in the notification drawer. Apps targeting Android 14 or later must configure a foreground-service type and any associated permission/property appropriate to their use case; the library does not select a type on the host app's behalf.

Start the packaged `ServiceA` from an appropriate user-visible app flow:

```java
android.iocl.keepAlive.ServiceA.start(context);
```

Starting a foreground service is subject to Android's background-start restrictions. The host app is responsible for choosing an allowed user-initiated start flow, requesting notification permission when applicable, and configuring any target-SDK-specific foreground-service type requirements.

## Integrate into another Android app

Copy the AAR into the consuming app's `app/libs/` directory and add it to the app module's Gradle dependencies:

```gradle
dependencies {
    implementation files("libs/keepalive-library-release.aar")
}
```

The consuming app must target Android API 23 or newer and include one of the AAR's supported ABIs. Start the watcher from a service owned by the consuming app:

```java
new android.iocl.keepAlive.NativeDaemon()
        .startWatch(android.os.Process.myPid(), getPackageName());
```

The AAR includes `ServiceA` and `ServiceB`; start them as described above rather than separately starting the JNI watcher if you want the bundled service behavior.

## Existing implementation constraints

- The native implementation launches the fixed component `<package>/.ServiceA` with `am start --user 0`. The bundled `android.iocl.keepAlive.ServiceA` is a service, not an activity, and its fully qualified component is not `<package>/.ServiceA`. Therefore the native relaunch command does not target the bundled service. The native source is intentionally unchanged; process restart through that command is not a working integration path as packaged.
- Android versions and device manufacturers may restrict background process execution, shell commands, or relaunching an app after its process is killed. The watcher is best-effort and cannot guarantee that Android will keep or restart an app.
- The current JNI API does not expose a stop/cleanup method for the forked watcher.

Use only with clear user consent and a visible foreground-service notification. Do not use this library to hide ongoing activity or bypass Android background-execution controls.
