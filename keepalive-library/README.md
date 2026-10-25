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
- Android versions and device manufacturers may restrict background process execution, shell commands, or relaunching an app after its process is killed. The watcher is best-effort and cannot guarantee that Android will keep or restart an app.
- The current JNI API does not expose a stop/cleanup method for the forked watcher.

Use only with clear user consent and a visible foreground-service notification. Do not use this library to hide ongoing activity or bypass Android background-execution controls.
