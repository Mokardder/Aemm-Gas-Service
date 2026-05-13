package android.iocl.dac_collector.Utility;

public class SettingsTracker {
    private static boolean waiting = false;

    public static void markOpened() {
        waiting = true;
    }

    public static boolean consume() {
        if (waiting) {
            waiting = false;
            return true;
        }
        return false;
    }
}