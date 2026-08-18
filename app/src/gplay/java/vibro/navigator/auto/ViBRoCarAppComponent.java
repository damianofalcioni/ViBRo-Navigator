package vibro.navigator.auto;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import vibro.navigator.android.automotive.AndroidCarModeMonitor;

public final class ViBRoCarAppComponent {
    private static boolean disablePendingUntilHostDisconnect;

    private ViBRoCarAppComponent() {
    }

    @NonNull
    public static String state(@NonNull Context context) {
        return componentEnabledStateName(packageManager(context).getComponentEnabledSetting(componentName(context)));
    }

    public static void configure(@NonNull Context context, boolean enabled) {
        Context appContext = context.getApplicationContext();
        if (enabled) {
            cancelPendingDisable(appContext);
            setComponentEnabled(appContext, true);
            return;
        }
        requestDisable(appContext);
    }

    static void onSessionDestroyed(@NonNull Context context) {
        applyPendingDisableIfSafe(context.getApplicationContext());
    }

    static void onCarModeExited(@NonNull Context context) {
        applyPendingDisableIfSafe(context.getApplicationContext());
    }

    static void resetPendingDisableForTest(@NonNull Context context) {
        cancelPendingDisable(context.getApplicationContext());
        AndroidCarModeMonitor.setActiveForTest(null);
    }

    static void setCarModeActiveForTest(@Nullable Boolean active) {
        AndroidCarModeMonitor.setActiveForTest(active);
    }

    private static void requestDisable(@NonNull Context appContext) {
        if (shouldDeferDisable(appContext)) {
            disablePendingUntilHostDisconnect = true;
            registerCarModeExitReceiver(appContext);
            return;
        }
        disableNow(appContext);
    }

    private static void applyPendingDisableIfSafe(@NonNull Context appContext) {
        if (!disablePendingUntilHostDisconnect) {
            return;
        }
        if (shouldDeferDisable(appContext)) {
            registerCarModeExitReceiver(appContext);
            return;
        }
        disableNow(appContext);
    }

    private static boolean shouldDeferDisable(@NonNull Context context) {
        return ViBRoCarAppService.hasActiveSession() || AndroidCarModeMonitor.isActive(context);
    }

    private static void disableNow(@NonNull Context appContext) {
        cancelPendingDisable(appContext);
        setComponentEnabled(appContext, false);
    }

    private static void cancelPendingDisable(@NonNull Context appContext) {
        disablePendingUntilHostDisconnect = false;
        AndroidCarModeMonitor.unregisterExitReceiver(appContext);
    }

    private static void registerCarModeExitReceiver(@NonNull Context appContext) {
        AndroidCarModeMonitor.registerExitReceiver(appContext, ViBRoCarAppComponent::onCarModeExited);
    }

    private static void setComponentEnabled(@NonNull Context context, boolean enabled) {
        int state = enabled
                ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED
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
