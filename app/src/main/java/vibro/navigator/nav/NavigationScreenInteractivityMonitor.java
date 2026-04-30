package vibro.navigator.nav;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.PowerManager;

import androidx.annotation.NonNull;

final class NavigationScreenInteractivityMonitor {

    interface Listener {
        void onScreenInteractiveChanged(boolean interactive);
    }

    private final Context context;
    private final Listener listener;
    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null || intent.getAction() == null) {
                return;
            }
            if (Intent.ACTION_SCREEN_OFF.equals(intent.getAction())) {
                listener.onScreenInteractiveChanged(false);
            } else if (Intent.ACTION_SCREEN_ON.equals(intent.getAction())) {
                listener.onScreenInteractiveChanged(true);
            }
        }
    };
    private boolean registered;

    NavigationScreenInteractivityMonitor(
            @NonNull Context context,
            @NonNull Listener listener
    ) {
        this.context = context;
        this.listener = listener;
    }

    boolean start() {
        if (!registered) {
            IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_OFF);
            filter.addAction(Intent.ACTION_SCREEN_ON);
            context.registerReceiver(receiver, filter);
            registered = true;
        }
        return isScreenInteractive();
    }

    void stop() {
        if (!registered) {
            return;
        }
        registered = false;
        context.unregisterReceiver(receiver);
    }

    private boolean isScreenInteractive() {
        PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        return powerManager == null || powerManager.isInteractive();
    }

}
