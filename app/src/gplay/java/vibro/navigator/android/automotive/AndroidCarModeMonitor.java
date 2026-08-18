package vibro.navigator.android.automotive;

import android.app.UiModeManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

public final class AndroidCarModeMonitor {
    @Nullable
    private static BroadcastReceiver carModeExitReceiver;
    @Nullable
    private static Context carModeExitReceiverContext;
    @Nullable
    private static Boolean carModeActiveForTest;

    private AndroidCarModeMonitor() {
    }

    public static boolean isActive(@NonNull Context context) {
        if (carModeActiveForTest != null) {
            return carModeActiveForTest;
        }
        UiModeManager uiModeManager = (UiModeManager) context.getSystemService(Context.UI_MODE_SERVICE);
        if (uiModeManager != null) {
            return uiModeManager.getCurrentModeType() == Configuration.UI_MODE_TYPE_CAR;
        }
        return (context.getResources().getConfiguration().uiMode & Configuration.UI_MODE_TYPE_MASK)
                == Configuration.UI_MODE_TYPE_CAR;
    }

    public static void registerExitReceiver(@NonNull Context context, @NonNull Runnable onExit) {
        Context appContext = context.getApplicationContext();
        if (!isActive(appContext) || carModeExitReceiver != null) {
            return;
        }
        BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context receiverContext, Intent intent) {
                if (UiModeManager.ACTION_EXIT_CAR_MODE.equals(intent.getAction())) {
                    onExit.run();
                }
            }
        };
        carModeExitReceiver = receiver;
        carModeExitReceiverContext = appContext;
        ContextCompat.registerReceiver(
                appContext,
                receiver,
                new IntentFilter(UiModeManager.ACTION_EXIT_CAR_MODE),
                ContextCompat.RECEIVER_NOT_EXPORTED
        );
    }

    public static void unregisterExitReceiver() {
        BroadcastReceiver receiver = carModeExitReceiver;
        Context context = carModeExitReceiverContext;
        carModeExitReceiver = null;
        carModeExitReceiverContext = null;
        if (receiver != null && context != null) {
            context.unregisterReceiver(receiver);
        }
    }

    public static void setActiveForTest(@Nullable Boolean active) {
        carModeActiveForTest = active;
    }

    static void dispatchExitForTest() {
        BroadcastReceiver receiver = carModeExitReceiver;
        Context context = carModeExitReceiverContext;
        if (receiver != null && context != null) {
            receiver.onReceive(context, new Intent(UiModeManager.ACTION_EXIT_CAR_MODE));
        }
    }
}
