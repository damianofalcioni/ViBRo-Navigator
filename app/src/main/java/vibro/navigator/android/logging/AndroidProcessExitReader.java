package vibro.navigator.android.logging;

import android.app.ActivityManager;
import android.app.ApplicationExitInfo;
import android.content.Context;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import java.util.List;

import vibro.navigator.logging.AppProcessExit;

public final class AndroidProcessExitReader {
    // ApplicationExitInfo's Android 11 reason codes are stable from 0 through 13.
    private static final String[] BASE_REASON_NAMES = {
            "UNKNOWN", "EXIT_SELF", "SIGNALED", "LOW_MEMORY", "CRASH", "CRASH_NATIVE",
            "ANR", "INITIALIZATION_FAILURE", "PERMISSION_CHANGE", "EXCESSIVE_RESOURCE_USAGE",
            "USER_REQUESTED", "USER_STOPPED", "DEPENDENCY_DIED", "OTHER"
    };

    private AndroidProcessExitReader() {
    }

    @Nullable
    public static AppProcessExit findPrevious(@NonNull Context context, int pid, long startedAtMs) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R || pid <= 0) {
            return null;
        }
        return findOnAndroid11(context, pid, startedAtMs);
    }

    @Nullable
    @RequiresApi(Build.VERSION_CODES.R)
    private static AppProcessExit findOnAndroid11(@NonNull Context context, int pid, long startedAtMs) {
        ActivityManager manager = context.getSystemService(ActivityManager.class);
        if (manager == null) {
            return null;
        }
        try {
            List<ApplicationExitInfo> exits = manager.getHistoricalProcessExitReasons(
                    context.getPackageName(), pid, 10
            );
            for (ApplicationExitInfo exit : exits) {
                if (exit.getPid() == pid && exit.getTimestamp() >= startedAtMs) {
                    return new AppProcessExit(
                            exit.getTimestamp(),
                            pid,
                            exit.getStatus(),
                            exit.getImportance(),
                            isAnomalous(exit.getReason()),
                            reasonName(exit.getReason()),
                            exit.getDescription()
                    );
                }
            }
        } catch (RuntimeException ignored) {
            // Exit history is best effort and may be unavailable on an OEM build.
        }
        return null;
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private static boolean isAnomalous(int reason) {
        return reason == ApplicationExitInfo.REASON_CRASH
                || reason == ApplicationExitInfo.REASON_CRASH_NATIVE
                || reason == ApplicationExitInfo.REASON_ANR
                || reason == ApplicationExitInfo.REASON_INITIALIZATION_FAILURE
                || reason == ApplicationExitInfo.REASON_EXCESSIVE_RESOURCE_USAGE
                || reason == ApplicationExitInfo.REASON_SIGNALED;
    }

    @NonNull
    @RequiresApi(Build.VERSION_CODES.R)
    private static String reasonName(int reason) {
        return reason >= 0 && reason < BASE_REASON_NAMES.length
                ? BASE_REASON_NAMES[reason]
                : "UNKNOWN(" + reason + ")";
    }
}
