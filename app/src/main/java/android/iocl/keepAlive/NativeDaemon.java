package android.iocl.keepAlive;



public class NativeDaemon {
    static {
        System.loadLibrary("keepalive");
    }

    // Call this from ServiceA/ServiceB
    public native void startWatch(int targetPid, String packageName);
}
