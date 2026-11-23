package vibro.navigator.android.brouter;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import androidx.core.content.ContextCompat;

/** Keeps the active overlay current when removable storage becomes available or disappears. */
public final class AndroidBRouterVolumeChanges {
    private AndroidBRouterVolumeChanges() { }

    public static Runnable observe(Context context, Runnable onChange) {
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_MEDIA_MOUNTED);
        filter.addAction(Intent.ACTION_MEDIA_UNMOUNTED);
        filter.addAction(Intent.ACTION_MEDIA_EJECT);
        filter.addDataScheme("file");
        BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context ignored, Intent intent) {
                onChange.run();
            }
        };
        ContextCompat.registerReceiver(context, receiver, filter, ContextCompat.RECEIVER_EXPORTED);
        return () -> context.unregisterReceiver(receiver);
    }
}
