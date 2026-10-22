package android.iocl.dac_collector.Utility;

import android.content.Context;

import java.io.PrintWriter;
import java.io.StringWriter;

/** Persists the most recent fatal exception so the crash-process UI can display it. */
public final class CrashReportStore {
    private static final String PREFERENCES_NAME = "fatal_crash_report";
    private static final String KEY_REPORT = "report";

    private CrashReportStore() {
    }

    public static void save(final Context context, final Thread thread, final Throwable throwable) {
        final StringWriter stackTrace = new StringWriter();
        final PrintWriter writer = new PrintWriter(stackTrace);
        writer.println("Thread: " + thread.getName());
        throwable.printStackTrace(writer);
        writer.close();

        // commit() is intentional: the app process is about to terminate.
        context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_REPORT, stackTrace.toString())
                .commit();
    }

    public static String get(final Context context) {
        return context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
                .getString(KEY_REPORT, null);
    }

    public static void clear(final Context context) {
        context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
                .edit()
                .remove(KEY_REPORT)
                .commit();
    }
}
