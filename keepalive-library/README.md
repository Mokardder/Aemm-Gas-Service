# Android KeepAlive native library

This module builds the existing KeepAlive JNI code as an Android AAR. The native C implementation and JNI wrapper are copied unchanged from the app's KeepAlive implementation. Only the standalone Android library build configuration and integration documentation are new.

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

It contains `libkeepalive.so` for `armeabi-v7a` and `arm64-v8a`, as well as the `android.iocl.keepAlive.NativeDaemon` class.

## Service classes and manifest

The AAR contains the native watcher and `NativeDaemon`; it does **not** contain the app's `ServiceA` or `ServiceB` service classes. Those classes are app-specific and currently depend on the host app's `NotificationHelper` and notification-channel configuration. The host app must provide its own services, or separately adapt/copy those service classes and their dependencies.

In the current app implementation:

- `ServiceA` starts a foreground notification, calls `NativeDaemon.startWatch(Process.myPid(), getPackageName())`, and starts `ServiceB`.
- `ServiceB` starts its own foreground notification and starts `ServiceA`. Both services return `START_STICKY` and start the other service from `onDestroy()`.
- `ServiceA.start(context)` is the provided convenience entry point; it uses `ContextCompat.startForegroundService`.

The current app registers its service classes in the **app** manifest (inside `<application>`) like this:

```xml
<service
    android:name="android.iocl.keepAlive.ServiceA"
    android:enabled="true"
    android:exported="false"
    android:stopWithTask="false" />
<service
    android:name="android.iocl.keepAlive.ServiceB"
    android:enabled="true"
    android:exported="false" />
```

For another app, register the fully qualified names of the service classes it actually provides; these names are examples from this repository, not classes included in the AAR. For foreground services, the host app should also declare the applicable permission at the manifest root:

```xml
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
```

On Android 13 and later, request `POST_NOTIFICATIONS` at runtime if the app needs to show notifications in the notification drawer. Add any foreground-service type permissions and manifest types required by the app's target SDK and actual service function. Create the notification channel used by the services before calling `startForeground`; notification channels are app-specific and are not supplied by this AAR.

When the host app has included a `ServiceA` class with the same API, start it from an appropriate user-visible app flow, for example:

```java
ServiceA.start(context);
```

Starting a foreground service is subject to Android's background-start restrictions. The host app is responsible for choosing a user-initiated start flow, requesting notification permission when applicable, providing its notification channel, and handling start failures. Declaring these services alone does not make them part of the AAR.

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

For continuous background work, call it from a user-visible foreground service and follow the target Android version's foreground-service and notification requirements. This AAR does not declare app-specific services, notifications, or permissions.

## Existing implementation constraints

- The native implementation launches the fixed component `<package>/.ServiceA` with `am start --user 0`. The consuming app must provide a launchable `ServiceA` activity if it expects that command to resolve. A service named `ServiceA` is not an activity. The current application defines `ServiceA` as a service, so its present component is not a valid activity target for this command.
- The component name is fixed in the existing native implementation and cannot be configured through the current JNI method. Consequently, integration into another app requires that app to provide the expected activity name.
- The host app's `ServiceA` and `ServiceB` are not included in this AAR; the manifest snippet above only registers service classes that the host app actually provides.
- Android versions and device manufacturers may restrict background process execution, shell commands, or relaunching an app after its process is killed. The watcher is best-effort and cannot guarantee that Android will keep or restart an app.
- The current JNI API does not expose a stop/cleanup method for the forked watcher.

Use only with clear user consent and a visible foreground-service notification. Do not use this library to hide ongoing activity or bypass Android background-execution controls.
