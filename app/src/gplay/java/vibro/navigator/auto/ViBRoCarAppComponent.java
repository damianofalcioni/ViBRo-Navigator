package vibro.navigator.auto;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;

import androidx.annotation.NonNull;

public final class ViBRoCarAppComponent {
    private static boolean disablePendingAfterSession;

    private ViBRoCarAppComponent() {
    }

    @NonNull
    public static String state(@NonNull Context context) {
        return componentEnabledStateName(packageManager(context).getComponentEnabledSetting(componentName(context)));
    }

    public static void configure(@NonNull Context context, boolean enabled) {
        Context appContext = context.getApplicationContext();
        if (enabled) {
            disablePendingAfterSession = false;
            setComponentEnabled(appContext, true);
            return;
        }
        if (ViBRoCarAppService.hasActiveSession()) {
            disablePendingAfterSession = true;
            return;
        }
        setComponentEnabled(appContext, false);
    }

    static void onSessionDestroyed(@NonNull Context context) {
        if (!disablePendingAfterSession) {
            return;
        }
        disablePendingAfterSession = false;
        setComponentEnabled(context.getApplicationContext(), false);
    }

    static void resetPendingDisableForTest() {
        disablePendingAfterSession = false;
    }

    private static void setComponentEnabled(@NonNull Context context, boolean enabled) {
        int state = enabled
                ? PackageManager.COMPONENT_ENABLED_STATE_DEFAULT
                : PackageManager.COMPONENT_ENABLED_STATE_DISABLED;
        packageManager(context).setComponentEnabledSetting(
                componentName(context),
                state,
                PackageManager.DONT_KILL_APP
        );
    }

    @NonNull
    private static ComponentName componentName(@NonNull Context context) {
        return new ComponentName(context, ViBRoCarAppService.class);
    }

    @NonNull
    private static PackageManager packageManager(@NonNull Context context) {
        return context.getPackageManager();
    }

    @NonNull
    private static String componentEnabledStateName(int state) {
        switch (state) {
            case PackageManager.COMPONENT_ENABLED_STATE_DEFAULT:
                return "default";
            case PackageManager.COMPONENT_ENABLED_STATE_ENABLED:
                return "enabled";
            case PackageManager.COMPONENT_ENABLED_STATE_DISABLED:
                return "disabled";
            case PackageManager.COMPONENT_ENABLED_STATE_DISABLED_USER:
                return "disabled_user";
            case PackageManager.COMPONENT_ENABLED_STATE_DISABLED_UNTIL_USED:
                return "disabled_until_used";
            default:
                return "unknown(" + state + ")";
        }
    }
}
