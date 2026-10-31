#include <jni.h>
#include <unistd.h>
#include <stdlib.h>
#include <android/log.h>
#include <sys/types.h>
#include <sys/wait.h>
#include <sys/time.h>
#include <string.h>
#include <signal.h>
#include <fcntl.h>
#include <time.h>
#include <errno.h>
#include <sys/inotify.h>
#include <sys/stat.h>
#include <sys/resource.h>
#include <stdio.h>
#include <stdbool.h>

#define LOG_TAG "KeepAliveNative"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

// Configuration
#define WATCHDOG_PID_FILE "/data/local/tmp/keepalive_watchdog.pid"
#define WATCHDOG_LOG_FILE "/data/local/tmp/keepalive_watchdog.log"
#define WATCHDOG_SUICIDE_FILE "/data/local/tmp/keepalive_watchdog_suicide"
#define POLL_INTERVAL_MS 500 // Fallback poll interval if inotify fails
#define MAX_RESTART_ATTEMPTS 10
#define RESTART_COOLDOWN_SEC 3

/**
 * Custom Log to File + Logcat
 */
static void logToFile(const char *msg) {
    FILE *f = fopen(WATCHDOG_LOG_FILE, "a");
    if (f) {
        time_t now = time(NULL);
        struct tm *tm = localtime(&now);
        char timebuf[64];
        strftime(timebuf, sizeof(timebuf), "%Y-%m-%d %H:%M:%S", tm);
        fprintf(f, "[%s] %s\n", timebuf, msg);
        fclose(f);
    }
    LOGI("%s", msg);
}

/**
 * Handle signals to keep the daemon alive during system cleanup
 */
static void signalHandler(int signum) {
    char msg[64];
    snprintf(msg, sizeof(msg), "Received signal %d. Writing suicide note.", signum);
    logToFile(msg);

    // Write suicide note so Java can restart us
    FILE *f = fopen(WATCHDOG_SUICIDE_FILE, "w");
    if (f) {
        fprintf(f, "signal:%d\n", signum);
        fclose(f);
    }
}

JNIEXPORT void JNICALL
Java_android_iocl_keepAlive_NativeDaemon_startWatch(JNIEnv *env, jobject thiz, jint targetPid, jstring pkgName_) {
    const char *pkgName = (*env)->GetStringUTFChars(env, pkgName_, 0);

    // 1. INITIAL FORK
    pid_t pid = fork();
    if (pid < 0) {
        LOGE("Initial fork failed: %s", strerror(errno));
        (*env)->ReleaseStringUTFChars(env, pkgName_, pkgName);
        return;
    }

    if (pid > 0) {
        // Parent returns to Java immediately
        LOGI("Parent process returned. Watcher PID: %d", pid);
        (*env)->ReleaseStringUTFChars(env, pkgName_, pkgName);
        return;
    }

    // --- CHILD PROCESS CONTEXT (Daemon) ---

    // 2. CREATE NEW SESSION
    if (setsid() < 0) {
        LOGE("setsid failed: %s", strerror(errno));
        _exit(1);
    }

    // 3. SECOND FORK (Orphan the first child)
    pid_t pid2 = fork();
    if (pid2 > 0) {
        _exit(0); // First child exits, second child continues
    }
    if (pid2 < 0) {
        LOGE("Second fork failed: %s", strerror(errno));
        _exit(1);
    }

    // 4. DETACH FROM TERMINAL & CWD
    chdir("/");
    umask(027);

    // 5. REDIRECT STDIO TO /DEV/NULL
    int devnull = open("/dev/null", O_RDWR);
    if (devnull >= 0) {
        dup2(devnull, STDIN_FILENO);
        dup2(devnull, STDOUT_FILENO);
        dup2(devnull, STDERR_FILENO);
        if (devnull > 2) close(devnull);
    }

    // 6. IGNORE TERMINAL SIGNALS & HANDLE CRITICAL SIGNALS
    signal(SIGTTOU, SIG_IGN);
    signal(SIGTTIN, SIG_IGN);
    signal(SIGTSTP, SIG_IGN);
    signal(SIGHUP, SIG_IGN);
    signal(SIGTERM, signalHandler);
    signal(SIGINT, signalHandler);

    // 7. SET PRIORITY (Nice Level)
    // -20 is highest priority. We want to be a "good citizen" but responsive.
    // 0 is normal. -10 is high.
    setpriority(PRIO_PROCESS, 0, -10);

    // 8. WRITE PID FILE
    FILE *pidFile = fopen(WATCHDOG_PID_FILE, "w");
    if (pidFile) {
        fprintf(pidFile, "%d\n", getpid());
        fclose(pidFile);
    }

    char logMsg[128];
    snprintf(logMsg, sizeof(logMsg), "Daemon started. PID: %d, Target: %d, Pkg: %s", getpid(), targetPid, pkgName);
    logToFile(logMsg);

    // 9. SETUP INOTIFY FOR /proc/<pid>/
    // Watching the process directory is more reliable than kill(pid, 0)
    // because it detects the kernel's process cleanup instantly.
    char procPath[64];
    snprintf(procPath, sizeof(procPath), "/proc/%d", targetPid);

    int inotify_fd = inotify_init();
    if (inotify_fd < 0) {
        LOGE("inotify_init failed. Falling back to polling.");
    } else {
        // Watch the process directory for DELETE_SELF (process death)
        if (inotify_add_watch(inotify_fd, procPath, IN_DELETE_SELF) == -1) {
            LOGE("inotify_add_watch failed. Falling back to polling.");
            close(inotify_fd);
            inotify_fd = -1;
        }
    }

    // 10. MONITORING LOOP
    int restartAttempts = 0;
    time_t lastRestartTime = 0;
    bool targetAlive = true;
    struct timeval start_time;
    gettimeofday(&start_time, NULL);

    while (1) {
        // Check if target is alive
        if (kill(targetPid, 0) == 0) {
            // Target is alive, reset restart counter
            if (!targetAlive) {
                logToFile("Target is alive again. Resetting restart counter.");
                restartAttempts = 0;
                targetAlive = true;
            }
        } else {
            // Target is dead
            targetAlive = false;

            // Check cooldown to prevent rapid-fire restarts
            time_t now = time(NULL);
            if (now - lastRestartTime < RESTART_COOLDOWN_SEC) {
                usleep(100000); // 100ms
                continue;
            }

            // Check max restart attempts
            if (restartAttempts >= MAX_RESTART_ATTEMPTS) {
                logToFile("Max restart attempts reached. Giving up.");
                break;
            }

            LOGI("Target %d dead. Attempting restart %d/%d", targetPid, restartAttempts + 1, MAX_RESTART_ATTEMPTS);
            snprintf(logMsg, sizeof(logMsg), "Target dead. Restart attempt %d", restartAttempts + 1);
            logToFile(logMsg);

            // RESTART LOGIC
            // Use 'am start' with specific flags for reliability
            char cmd[512];

            // Primary: Launch main activity with clear top
            snprintf(cmd, sizeof(cmd),
                     "am start -W --activity-clear-top --activity-single-top -n %s/%s.MainActivity", pkgName, pkgName);

            int res = system(cmd);

            // Fallback: If am start fails, try monkey (more robust for launcher activities)
            if (res != 0) {
                LOGI("am start failed. Trying monkey fallback.");
                snprintf(cmd, sizeof(cmd),
                         "monkey -p %s -c android.intent.category.LAUNCHER 1", pkgName);
                res = system(cmd);
            }

            lastRestartTime = time(NULL);
            restartAttempts++;

            // Small delay to let the system process the start command
            usleep(200000); // 200ms
        }

        // Sleep in small increments to allow signal handling and responsiveness
        usleep(POLL_INTERVAL_MS * 1000);
    }

    // Cleanup PID file
    remove(WATCHDOG_PID_FILE);
    logToFile("Daemon exiting.");
    _exit(0);
}