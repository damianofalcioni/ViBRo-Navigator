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
    public interface ExitListener {
        void onExit(@NonNull Context appContext);
    }

    @Nullable
    private static BroadcastReceiver carModeExitReceiver;
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

    public static void registerExitReceiver(@NonNull Context context, @NonNull ExitListener listener) {
        Context appContext = context.getApplicationContext();
        if (!isActive(appContext) || carModeExitReceiver != null) {
            return;
        }
        BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context receiverContext, Intent intent) {
                if (UiModeManager.ACTION_EXIT_CAR_MODE.equals(intent.getAction())) {
                    listener.onExit(receiverContext.getApplicationContext());
                }
            }
        };
        ContextCompat.registerReceiver(
                appContext,
                receiver,
                new IntentFilter(UiModeManager.ACTION_EXIT_CAR_MODE),
                ContextCompat.RECEIVER_NOT_EXPORTED
        );
        carModeExitReceiver = receiver;
    }

    public static void unregisterExitReceiver(@NonNull Context context) {
        BroadcastReceiver receiver = carModeExitReceiver;
        carModeExitReceiver = null;
        if (receiver != null) {
            context.getApplicationContext().unregisterReceiver(receiver);
        }
    }

    public static void setActiveForTest(@Nullable Boolean active) {
        carModeActiveForTest = active;
    }

    static void dispatchExitForTest(@NonNull Context context) {
        BroadcastReceiver receiver = carModeExitReceiver;
        if (receiver != null) {
            receiver.onReceive(
                    context.getApplicationContext(),
                    new Intent(UiModeManager.ACTION_EXIT_CAR_MODE)
            );
        }
    }
}
