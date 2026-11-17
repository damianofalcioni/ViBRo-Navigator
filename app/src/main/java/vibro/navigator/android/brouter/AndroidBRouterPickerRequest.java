package vibro.navigator.android.brouter;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.core.util.Consumer;

import java.util.UUID;

/** Reconciles a restored pending picker with results handled after the saved snapshot. */
public final class AndroidBRouterPickerRequest {
    private static final int COMPLETION_HISTORY_LENGTH = 64;
    private static final String VALIDATING = "VALIDATING:";
    private static final String PROCESS_VALIDATION = VALIDATING + UUID.randomUUID();

    public enum Completion {
        ADVANCE, RETRY, CANCELED
    }

    private final Context context;
    private final String key;
    private int requestCode;
    private String token;
    private SharedPreferences.OnSharedPreferenceChangeListener listener;

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
                return restoreOutcome(entry.substring(token.length() + 1));
            }
        }
        return null;
    }

    @Nullable
    private Completion restoreOutcome(String outcome) {
        if (PROCESS_VALIDATION.equals(outcome)) {
            return null;
        }
        requestCode = 0;
        token = null;
        return outcome.startsWith(VALIDATING) ? Completion.RETRY : Completion.valueOf(outcome);
    }

    public void begin(int requestCode) {
        stopWatching();
        this.requestCode = requestCode;
        token = UUID.randomUUID().toString();
    }

    public boolean isPending() {
        return requestCode != 0;
    }

    public boolean matches(int code) {
        return isPending() && requestCode == code;
    }

    public void beginValidation() {
        if (token != null) {
            saveOutcome(token, PROCESS_VALIDATION);
        }
    }

    public void complete(Completion completion) {
        String completedToken = token;
        requestCode = 0;
        token = null;
        stopWatching();
        if (completedToken != null) {
            saveOutcome(completedToken, completion.name());
        }
    }

    public void watch(Consumer<Completion> onCompletion) {
        if (isPending() && token != null) {
            listener = (preferences, changedKey) -> {
                Completion completion = keyWithCompletion().equals(changedKey) ? restoreCompletion() : null;
                if (completion != null) {
                    stopWatching();
                    onCompletion.accept(completion);
                }
            };
            preferences().registerOnSharedPreferenceChangeListener(listener);
        }
    }

    public void stopWatching() {
        if (listener != null) {
            preferences().unregisterOnSharedPreferenceChangeListener(listener);
            listener = null;
        }
    }

    private void saveOutcome(String requestToken, String outcome) {
        StringBuilder history = new StringBuilder(requestToken).append(':').append(outcome);
        String[] previous = completionHistory();
        int added = 1;
        for (String entry : previous) {
            if (added < COMPLETION_HISTORY_LENGTH && !entry.startsWith(requestToken + ":")) {
                history.append(',').append(entry);
                added++;
            }
        }
        preferences().edit().putString(keyWithCompletion(), history.toString()).apply();
    }

    public void save(Bundle state) {
        state.putInt(key, requestCode);
        state.putString(key + "_token", token);
    }

    private String[] completionHistory() {
        return preferences().getString(keyWithCompletion(), "").split(",");
    }

    private String keyWithCompletion() {
        return key + "_completed";
    }

    private SharedPreferences preferences() {
        return context.getSharedPreferences("brouter_folder_requests", Context.MODE_PRIVATE);
    }
}
