package vibro.navigator.logging;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Process;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import vibro.navigator.android.logging.AndroidProcessExitReader;

public final class AppExitDiagnostics {
    private static final String TAG = "AppExitDiagnostics";
    private static final String PREFS_NAME = "app_exit_diagnostics";
    private static final String KEY_PROCESS_PID = "process_pid";
    private static final String KEY_PROCESS_STARTED_AT = "process_started_at";
    private static final String KEY_NAVIGATION_ACTIVE = "navigation_active";
    private static final String KEY_NAVIGATION_STARTED_AT = "navigation_started_at";

    interface ExitReader {
        @Nullable AppProcessExit find(@NonNull Context context, int pid, long startedAtMs);
    }

    private AppExitDiagnostics() {
    }

    public static void onProcessStart(@NonNull Context context) {
        onProcessStart(context, Process.myPid(), System.currentTimeMillis(), AndroidProcessExitReader::findPrevious);
    }

    static void onProcessStart(
            @NonNull Context context,
            int currentPid,
            long nowMs,
            @NonNull ExitReader exitReader
    ) {
        SharedPreferences prefs = preferences(context);
        int previousPid = prefs.getInt(KEY_PROCESS_PID, 0);
        long previousStartMs = prefs.getLong(KEY_PROCESS_STARTED_AT, 0L);
        boolean navigationActive = prefs.getBoolean(KEY_NAVIGATION_ACTIVE, false);
        long navigationStartMs = prefs.getLong(KEY_NAVIGATION_STARTED_AT, 0L);
        reportPrevious(context, exitReader, previousPid, previousStartMs, navigationActive, navigationStartMs);
        prefs.edit()
                .putInt(KEY_PROCESS_PID, currentPid)
                .putLong(KEY_PROCESS_STARTED_AT, nowMs)
                .putBoolean(KEY_NAVIGATION_ACTIVE, false)
                .remove(KEY_NAVIGATION_STARTED_AT)
                .commit();
    }

    private static void reportPrevious(
            @NonNull Context context,
            @NonNull ExitReader exitReader,
            int previousPid,
            long previousStartMs,
            boolean navigationActive,
            long navigationStartMs
    ) {
        if (previousPid <= 0 || previousStartMs <= 0L) {
            return;
        }
        AppProcessExit exit = exitReader.find(context, previousPid, previousStartMs);
        String message = previousExitMessage(exit, previousPid, navigationActive, navigationStartMs);
        if (message != null) {
            AppLogger.anomaly(context, TAG, message, null);
        }
    }

    @Nullable
    private static String previousExitMessage(
            @Nullable AppProcessExit exit,
            int previousPid,
            boolean navigationActive,
            long navigationStartMs
    ) {
        if (exit != null) {
            return navigationActive || exit.anomalous
                    ? describeExit(exit, navigationActive, navigationStartMs)
                    : null;
        }
        return navigationActive
                ? "Previous navigation ended without a clean stop; exit cause unavailable"
                        + " previousPid=" + previousPid
                        + " navigationStartedAtEpochMs=" + navigationStartMs
                : null;
    }

    public static void navigationStarted(@NonNull Context context) {
        preferences(context).edit()
                .putBoolean(KEY_NAVIGATION_ACTIVE, true)
                .putLong(KEY_NAVIGATION_STARTED_AT, System.currentTimeMillis())
                .commit();
    }

    public static void navigationStopped(@NonNull Context context) {
        preferences(context).edit()
                .putBoolean(KEY_NAVIGATION_ACTIVE, false)
                .remove(KEY_NAVIGATION_STARTED_AT)
                .commit();
    }

    @NonNull
    private static String describeExit(
            @NonNull AppProcessExit exit,
            boolean navigationActive,
            long navigationStartMs
    ) {
        return (navigationActive ? "Previous process exited during navigation" : "Previous process exited unexpectedly")
                + " exitAtEpochMs=" + exit.timestampMs
                + " pid=" + exit.pid
                + " reason=" + exit.reason
                + " status=" + exit.status
                + " importance=" + exit.importance
                + (navigationActive ? " navigationStartedAtEpochMs=" + navigationStartMs : "")
                + (exit.description == null ? "" : " description=" + exit.description);
    }

    @NonNull
    private static SharedPreferences preferences(@NonNull Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }
}
