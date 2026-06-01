package vibro.navigator.android.foreground;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.PowerManager;

import androidx.annotation.NonNull;

import vibro.navigator.nav.foreground.NavigationScreenInteractivityMonitor;

public final class AndroidScreenInteractivityMonitor implements NavigationScreenInteractivityMonitor {

    @NonNull
    private final Context context;
    @NonNull
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

    public AndroidScreenInteractivityMonitor(
            @NonNull Context context,
            @NonNull Listener listener
    ) {
        this.context = context;
        this.listener = listener;
    }

    @Override
    public boolean start() {
        if (!registered) {
            IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_OFF);
            filter.addAction(Intent.ACTION_SCREEN_ON);
            context.registerReceiver(receiver, filter);
            registered = true;
        }
        return isScreenInteractive();
    }

    @Override
    public void stop() {
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
