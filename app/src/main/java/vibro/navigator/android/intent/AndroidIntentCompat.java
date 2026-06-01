package vibro.navigator.android.intent;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Build;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

public final class AndroidIntentCompat {
    private AndroidIntentCompat() {
    }

    public static void disableAutoLaunchSingleChoice(@NonNull Intent chooser) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            chooser.putExtra(Intent.EXTRA_AUTO_LAUNCH_SINGLE_CHOICE, false);
        }
    }

    @NonNull
    public static List<Intent> explicitActivityTargets(@NonNull Context context, @NonNull Intent actionView) {
        List<ResolveInfo> handlers = queryViewHandlers(context, actionView);
        List<Intent> targets = new ArrayList<>(handlers.size());
        for (ResolveInfo handler : handlers) {
            ActivityInfo activityInfo = handler.activityInfo;
            if (activityInfo == null || activityInfo.packageName == null || activityInfo.name == null) {
                continue;
            }
            targets.add(new Intent(actionView).setComponent(new ComponentName(
                    activityInfo.packageName,
                    activityInfo.name
            )));
        }
        return targets;
    }

    @NonNull
    private static List<ResolveInfo> queryViewHandlers(@NonNull Context context, @NonNull Intent actionView) {
        PackageManager packageManager = context.getPackageManager();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return packageManager.queryIntentActivities(
                    actionView,
                    PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_DEFAULT_ONLY)
            );
        }
        return packageManager.queryIntentActivities(actionView, PackageManager.MATCH_DEFAULT_ONLY);
    }
}
