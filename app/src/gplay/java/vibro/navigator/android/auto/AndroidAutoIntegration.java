package vibro.navigator.android.auto;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;

import androidx.annotation.NonNull;

import vibro.navigator.auto.ViBRoCarAppService;

public final class AndroidAutoIntegration {
    private AndroidAutoIntegration() {
    }

    public static void configure(@NonNull Context context, boolean enabled) {
        int state = enabled
                ? PackageManager.COMPONENT_ENABLED_STATE_DEFAULT
                : PackageManager.COMPONENT_ENABLED_STATE_DISABLED;
        ComponentName componentName = new ComponentName(context, ViBRoCarAppService.class);
        context.getPackageManager().setComponentEnabledSetting(
                componentName,
                state,
                PackageManager.DONT_KILL_APP
        );
    }
}
