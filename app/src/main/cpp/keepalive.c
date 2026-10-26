#include <jni.h>
#include <unistd.h>
#include <stdlib.h>
#include <android/log.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <sys/wait.h>
#include <string.h>
#include <fcntl.h>

#define LOG_TAG "KeepAliveNative"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

JNIEXPORT void JNICALL
Java_android_iocl_keepAlive_NativeDaemon_startWatch(JNIEnv *env, jobject thiz, jint targetPid, jstring pkgName_) {
    const char *pkgName = (*env)->GetStringUTFChars(env, pkgName_, 0);

    // Daemonize: Double Fork
    pid_t pid = fork();
    if (pid < 0) {
        LOGE("Fork 1 failed");
        (*env)->ReleaseStringUTFChars(env, pkgName_, pkgName);
        return;
    }
    if (pid > 0) {
        // Parent returns to Java
        (*env)->ReleaseStringUTFChars(env, pkgName_, pkgName);
        return;
    }

    // Child 1: Become session leader
    if (setsid() < 0) {
        LOGE("setsid failed");
        _exit(1);
    }

    // Child 1: Fork again to prevent reacquiring controlling terminal
    pid_t pid2 = fork();
    if (pid2 > 0) {
        _exit(0); // Child 1 exits
    }
    if (pid2 < 0) {
        LOGE("Fork 2 failed");
        _exit(1);
    }

    // Now we are the Grandchild (Daemon)
    chdir("/");
    umask(027);

    // Optional: Redirect stdio to /dev/null to avoid blocking
    int devnull = open("/dev/null", O_RDWR);
    if (devnull >= 0) {
        dup2(devnull, STDIN_FILENO);
        dup2(devnull, STDOUT_FILENO);
        dup2(devnull, STDERR_FILENO);
        if (devnull > 2) close(devnull);
    }

    LOGI("Daemon watcher active for PID %d, Pkg: %s", targetPid, pkgName);

    // Monitor Loop
    // Use a short sleep for responsiveness
    while (1) {
        // kill(pid, 0) returns 0 if process exists, -1 if not (or no permission)
        if (kill(targetPid, 0) != 0) {
            LOGI("Target %d is dead. Relaunching.", targetPid);

            // Construct the launch command.
            // Using 'am start' with the main activity is standard.
            // If you know the exact main activity, use it. 
            // Otherwise, use the LAUNCHER intent filter approach.
            char cmd[512];
            
            // Attempt to launch the main launcher activity.
            // If your app has a specific entry point, hardcode it.
            // Generic approach: am start -a android.intent.action.MAIN -c android.intent.category.LAUNCHER
            // But 'am start' needs a component name usually. 
            // Let's assume the main activity is in the same package or passed.
            // For simplicity, we'll use a generic main activity guess or you should pass it.
            
            // BETTER: Pass the main activity class name from Java
            // For now, using a placeholder that works for most apps if they follow convention
            // OR use 'monkey' which is more robust for "just launch the app"
            
            // Monkey is often safer for "restart the app" as it finds the launcher activity
            snprintf(cmd, sizeof(cmd),
                     "monkey -p %s -c android.intent.category.LAUNCHER 1", pkgName);

            int res = system(cmd);
            LOGI("Relaunch command finished with code %d", res);
            
            // Exit the watcher. The app is restarted. 
            // If you want the watcher to stay alive to monitor the NEW pid,
            // you'd need to parse the new PID, which is complex in C++.
            // Usually, restarting the app triggers the Java code to call startWatch again 
            // if you have an Application class that initializes on startup.
            break;
        }
        sleep(1);
    }
    
    _exit(0);
}