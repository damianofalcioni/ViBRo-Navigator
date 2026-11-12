package vibro.navigator.android.brouter;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.Nullable;

/** Reconciles a restored pending picker with results handled after the saved snapshot. */
public final class AndroidBRouterPickerRequest {
    private final Context context;
    private final String key;
    private int requestCode;
    private long sequence;

    public AndroidBRouterPickerRequest(Context context, String key) {
        this.context = context;
        this.key = key;
    }

    public boolean restore(@Nullable Bundle state) {
        if (state == null) {
            return false;
        }
        requestCode = state.getInt(key);
        sequence = state.getLong(key + "_sequence");
        if (isPending() && sequence > 0 && sequence <= completed().getLong(key, 0)) {
            requestCode = 0;
            return true;
        }
        return false;
    }

    public void begin(int requestCode) {
        this.requestCode = requestCode;
        sequence = completed().getLong(key, 0) + 1;
    }

    public boolean isPending() {
        return requestCode != 0;
    }

    public boolean matches(int code) {
        return isPending() && requestCode == code;
    }

    public void complete() {
        SharedPreferences completed = completed();
        if (sequence > completed.getLong(key, 0)) {
            completed.edit().putLong(key, sequence).apply();
        }
        requestCode = 0;
    }

    public void save(Bundle state) {
        state.putInt(key, requestCode);
        state.putLong(key + "_sequence", sequence);
    }

    private SharedPreferences completed() {
        return context.getSharedPreferences("brouter_folder_requests", Context.MODE_PRIVATE);
    }
}
