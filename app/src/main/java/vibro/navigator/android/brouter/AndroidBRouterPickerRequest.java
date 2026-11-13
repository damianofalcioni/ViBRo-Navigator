package vibro.navigator.android.brouter;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.Nullable;

import java.util.UUID;

/** Reconciles a restored pending picker with results handled after the saved snapshot. */
public final class AndroidBRouterPickerRequest {
    private static final int COMPLETION_HISTORY_LENGTH = 64;

    public enum Completion {
        ADVANCE, RETRY
    }

    private final Context context;
    private final String key;
    private int requestCode;
    private String token;

    public AndroidBRouterPickerRequest(Context context, String key) {
        this.context = context;
        this.key = key;
    }

    @Nullable
    public Completion restore(@Nullable Bundle state) {
        if (state == null) {
            return null;
        }
        requestCode = state.getInt(key);
        token = state.getString(key + "_token");
        if (isPending() && token != null) {
            return restoreCompletion();
        }
        return null;
    }

    @Nullable
    private Completion restoreCompletion() {
        for (String entry : completionHistory()) {
            if (entry.startsWith(token + ":")) {
                requestCode = 0;
                return Completion.valueOf(entry.substring(token.length() + 1));
            }
        }
        return null;
    }

    public void begin(int requestCode) {
        this.requestCode = requestCode;
        token = UUID.randomUUID().toString();
    }

    public boolean isPending() {
        return requestCode != 0;
    }

    public boolean matches(int code) {
        return isPending() && requestCode == code;
    }

    public void complete(Completion completion) {
        if (token != null) {
            saveCompletion(completion);
        }
        requestCode = 0;
        token = null;
    }

    private void saveCompletion(Completion completion) {
        StringBuilder history = new StringBuilder(token).append(':').append(completion);
        String[] previous = completionHistory();
        for (int i = 0; i < Math.min(previous.length, COMPLETION_HISTORY_LENGTH - 1); i++) {
            history.append(',').append(previous[i]);
        }
        context.getSharedPreferences("brouter_folder_requests", Context.MODE_PRIVATE)
                .edit().putString(key + "_completed", history.toString()).apply();
    }

    public void save(Bundle state) {
        state.putInt(key, requestCode);
        state.putString(key + "_token", token);
    }

    private String[] completionHistory() {
        return context.getSharedPreferences("brouter_folder_requests", Context.MODE_PRIVATE)
                .getString(key + "_completed", "").split(",");
    }
}
