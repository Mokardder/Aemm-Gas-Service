#include <jni.h>
#include <unistd.h>
#include <stdlib.h>
#include <android/log.h>
#include <sys/types.h>
#include <sys/wait.h>
#include <string.h>

#define LOG_TAG "KeepAliveNative"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

/**
 * Simple keep-alive: forks a child process to monitor the parent PID.
 * If the parent dies, restart the app with am command.
 */
JNIEXPORT void JNICALL
Java_android_iocl_keepAlive_NativeDaemon_startWatch(JNIEnv *env, jobject thiz, jint targetPid, jstring pkgName_) {
    const char *pkgName = (*env)->GetStringUTFChars(env, pkgName_, 0);

    pid_t pid = fork();
    if (pid < 0) {
        LOGE("Fork failed");
        (*env)->ReleaseStringUTFChars(env, pkgName_, pkgName);
        return;
    }

    if (pid == 0) {
        // Child process loop
        LOGI("Watcher child started for PID: %d", targetPid);
        while (1) {
            // Check if target is alive
            if (kill(targetPid, 0) != 0) {
                LOGE("Target process %d dead. Restarting %s", targetPid, pkgName);

                char cmd[256];
                snprintf(cmd, sizeof(cmd),
                         "am start --user 0 -n %s/.ServiceA", pkgName);

                int res = system(cmd);
                LOGI("Restart cmd result: %d", res);
                break;
            }
            sleep(5);
        }
        _exit(0);
    }

    (*env)->ReleaseStringUTFChars(env, pkgName_, pkgName);
}
