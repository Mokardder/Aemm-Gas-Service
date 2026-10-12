#include <jni.h>
#include <unistd.h>
#include <stdlib.h>
#include <stdio.h>
#include <dirent.h>
#include <fcntl.h>
#include <string.h>
#include <errno.h>
#include <signal.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <sys/wait.h>
#include <sys/ioctl.h>
#include <sys/mman.h>
#include <sys/prctl.h>
#include <sys/resource.h>
#include <sys/syscall.h>
#include <android/log.h>
#include <pthread.h>
#include <linux/types.h>

#define LOG_TAG "KeepAliveNative"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

#define BINDER_DEVICE "/dev/binder"
#define MAX_BINDER_THREADS 4
#define OOM_SCORE_ADJ_MIN -1000

// Binder structures
struct binder_object_header {
    __u32 type;
};

struct flat_binder_object {
    struct binder_object_header hdr;
    __u32 flags;
    union {
        __u64 binder;
        __u32 handle;
    };
    __u64 cookie;
};

struct binder_write_read {
    __u64 write_size;
    __u64 write_consumed;
    __u64 write_buffer;
    __u64 read_size;
    __u64 read_consumed;
    __u64 read_buffer;
};

// Binder ioctl commands - use sys/ioctl.h macros
#define BINDER_WRITE_READ          _IOWR('b', 1, struct binder_write_read)
#define BINDER_SET_MAX_THREADS     _IOW('b', 5, __u32)
#define BINDER_SET_CONTEXT_MGR_EXT _IOW('b', 13, struct flat_binder_object)

// Binder types
#define BINDER_TYPE_BINDER        1
#define FLAT_BINDER_FLAG_ACCEPTS_FDS 0x100

// Global state
static volatile int keep_running = 1;
static volatile int binder_thread_count = 0;
static pthread_mutex_t binder_mutex = PTHREAD_MUTEX_INITIALIZER;
static int binder_fd = -1;

// Binder thread tracking
typedef struct {
    int id;
    pthread_t thread;
    volatile int active;
} binder_thread_t;

static binder_thread_t binder_threads[MAX_BINDER_THREADS];

// Syscall wrapper for ioctl
static inline int binder_ioctl(int fd, unsigned long request, void *arg) {
    return syscall(__NR_ioctl, fd, request, arg);
}

// Set OOM score to minimum
static void adjust_oom_score(int pid) {
    char path[256];
    snprintf(path, sizeof(path), "/proc/%d/oom_score_adj", pid);
    int fd = open(path, O_WRONLY);
    if (fd >= 0) {
        write(fd, "-1000", 5);
        close(fd);
    }
}

// Set realtime priority
static void set_realtime_priority() {
    struct sched_param param;
    param.sched_priority = 99;
    syscall(__NR_sched_setscheduler, 0, SCHED_FIFO, &param);
}

// Lock memory to prevent swapping
static void lock_memory_pages() {
    syscall(__NR_mlockall, MCL_CURRENT | MCL_FUTURE);
}

// Ignore termination signals
static void ignore_signals() {
    struct sigaction sa;
    memset(&sa, 0, sizeof(sa));
    sa.sa_handler = SIG_IGN;
    sigaction(SIGTERM, &sa, NULL);
    sigaction(SIGINT, &sa, NULL);
    sigaction(SIGHUP, &sa, NULL);
    sigaction(SIGQUIT, &sa, NULL);
}

// Binder polling thread
static void* binder_poll_thread(void* arg) {
    int thread_id = (int)(long)arg;
    binder_threads[thread_id].active = 1;

    binder_thread_count++;

    LOGI("Binder thread %d started", thread_id);

    while (keep_running && binder_threads[thread_id].active) {
        struct binder_write_read bwr;
        memset(&bwr, 0, sizeof(bwr));

        unsigned char buffer[1024];
        bwr.read_size = sizeof(buffer);
        bwr.read_buffer = (__u64)buffer;

        binder_ioctl(binder_fd, BINDER_WRITE_READ, &bwr);
        usleep(10000);
    }

    binder_thread_count--;

    return NULL;
}

// Initialize binder hardening
static void init_binder() {
    binder_fd = open(BINDER_DEVICE, O_RDWR | O_CLOEXEC);
    if (binder_fd < 0) {
        LOGE("Binder open failed: %s", strerror(errno));
        return;
    }

    LOGI("Binder opened successfully (fd=%d)", binder_fd);

    // Set max threads
    __u32 max_threads = MAX_BINDER_THREADS;
    if (binder_ioctl(binder_fd, BINDER_SET_MAX_THREADS, &max_threads) < 0) {
        LOGE("Failed to set max binder threads");
    } else {
        LOGI("Binder max threads set to %d", MAX_BINDER_THREADS);
    }

    // Register as context manager (best effort)
    struct flat_binder_object obj;
    memset(&obj, 0, sizeof(obj));
    obj.hdr.type = BINDER_TYPE_BINDER;
    obj.flags = FLAT_BINDER_FLAG_ACCEPTS_FDS;
    obj.binder = 0;
    obj.cookie = 0;

    if (binder_ioctl(binder_fd, BINDER_SET_CONTEXT_MGR_EXT, &obj) < 0) {
        LOGI("Context manager registration skipped (requires root)");
    } else {
        LOGI("Registered as binder context manager");
    }

    // Start binder polling threads
    for (int i = 0; i < MAX_BINDER_THREADS; i++) {
        binder_threads[i].id = i;
        binder_threads[i].active = 0;
        pthread_create(&binder_threads[i].thread, NULL, binder_poll_thread, (void*)(long)i);
    }
}

// Watchdog for binder threads
static void* watchdog_thread(void* arg) {
    while (keep_running) {
        int active = binder_thread_count;

        if (active < MAX_BINDER_THREADS) {
            LOGE("Binder threads depleted (%d/%d), respawning...", active, MAX_BINDER_THREADS);
            for (int i = 0; i < MAX_BINDER_THREADS; i++) {
                if (!binder_threads[i].active) {
                    pthread_create(&binder_threads[i].thread, NULL,
                                   binder_poll_thread, (void*)(long)i);
                }
            }
        }

        adjust_oom_score(getpid());
        sleep(1);
    }
    return NULL;
}

// Find PID by package name
static pid_t find_pid_by_package(const char *pkg_name) {
    DIR *proc = opendir("/proc");
    if (!proc) return -1;

    struct dirent *entry;
    pid_t result = -1;

    while ((entry = readdir(proc)) != NULL) {
        if (entry->d_type != DT_DIR) continue;

        char *endptr;
        long pid_l = strtol(entry->d_name, &endptr, 10);
        if (*endptr != '\0' || pid_l <= 0) continue;

        char cmdline_path[256];
        snprintf(cmdline_path, sizeof(cmdline_path), "/proc/%ld/cmdline", pid_l);

        FILE *fp = fopen(cmdline_path, "r");
        if (!fp) continue;

        char cmdline[512] = {0};
        size_t len = fread(cmdline, 1, sizeof(cmdline) - 1, fp);
        fclose(fp);

        if (len == 0) continue;

        if (strstr(cmdline, pkg_name) != NULL) {
            result = (pid_t)pid_l;
            break;
        }
    }
    closedir(proc);
    return result;
}

// Check if process is alive
static int is_process_alive(pid_t pid) {
    if (pid <= 0) return 0;
    if (kill(pid, 0) == 0) return 1;

    char path[64];
    snprintf(path, sizeof(path), "/proc/%d", pid);
    return (access(path, F_OK) == 0);
}

// Restart the service
static int restart_service(const char *pkg_name) {
    char cmd[512];

    snprintf(cmd, sizeof(cmd), "am force-stop %s 2>/dev/null", pkg_name);
    system(cmd);
    usleep(200000);

    snprintf(cmd, sizeof(cmd), "pkill -9 -f %s 2>/dev/null", pkg_name);
    system(cmd);
    usleep(100000);

    snprintf(cmd, sizeof(cmd),
             "am startservice --user 0 %s/.ServiceA 2>/dev/null", pkg_name);
    return system(cmd);
}

// Proper daemonization
static void daemonize() {
    pid_t pid = fork();
    if (pid < 0) _exit(1);
    if (pid > 0) _exit(0);

    setsid();

    pid = fork();
    if (pid < 0) _exit(1);
    if (pid > 0) _exit(0);

    chdir("/");
    umask(0);

    for (int fd = sysconf(_SC_OPEN_MAX); fd > 2; fd--) {
        if (fd != binder_fd) close(fd);
    }

    int devnull = open("/dev/null", O_RDWR);
    if (devnull >= 0) {
        dup2(devnull, 0);
        dup2(devnull, 1);
        dup2(devnull, 2);
        if (devnull > 2) close(devnull);
    }
}

/**
 * Main JNI entry point
 */
JNIEXPORT void JNICALL
Java_android_iocl_keepAlive_NativeDaemon_startWatch(JNIEnv *env, jobject thiz,
        jint targetPid,
jstring pkgName_) {
const char *pkgName = (*env)->GetStringUTFChars(env, pkgName_, 0);

char pkg_copy[256];
strncpy(pkg_copy, pkgName, sizeof(pkg_copy) - 1);
pkg_copy[sizeof(pkg_copy) - 1] = '\0';

init_binder();

pid_t pid = fork();
if (pid < 0) {
LOGE("Fork failed");
(*env)->ReleaseStringUTFChars(env, pkgName_, pkgName);
return;
}

if (pid == 0) {
(*env)->ReleaseStringUTFChars(env, pkgName_, pkgName);

daemonize();

ignore_signals();
set_realtime_priority();
lock_memory_pages();
adjust_oom_score(getpid());
prctl(PR_SET_NAME, "system_server", 0, 0, 0);

pthread_t watchdog;
pthread_create(&watchdog, NULL, watchdog_thread, NULL);
pthread_detach(watchdog);

LOGI("Watcher child started for PID: %d (hardened)", targetPid);

int crash_count = 0;
pid_t current_pid = targetPid;

while (keep_running) {
if (kill(current_pid, 0) != 0) {
LOGE("Target process %d dead (crash #%d). Restarting %s",
     current_pid, ++crash_count, pkg_copy);

restart_service(pkg_copy);

usleep(500000);
current_pid = find_pid_by_package(pkg_copy);

if (current_pid > 0) {
LOGI("Service restarted with PID: %d", current_pid);
adjust_oom_score(current_pid);
} else {
LOGE("Failed to find restarted process");
usleep(1000000);
}

if (crash_count > 2) {
usleep(100000);
}
continue;
}

sleep(2);
}

_exit(0);
}

adjust_oom_score(pid);
(*env)->ReleaseStringUTFChars(env, pkgName_, pkgName);
}