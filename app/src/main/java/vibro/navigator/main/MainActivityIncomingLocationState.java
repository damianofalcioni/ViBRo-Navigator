package vibro.navigator.main;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

final class MainActivityIncomingLocationState {
    static final String APPLIED = "incomingLocationApplied";

    private MainActivityIncomingLocationState() {
    }

    static void save(@NonNull Bundle state, @Nullable Intent intent) {
        state.putBoolean(APPLIED, intent != null && intent.getBooleanExtra(APPLIED, false));
    }

    static void restore(@Nullable Bundle state, @Nullable Intent intent) {
        if (state != null && intent != null) {
            intent.putExtra(APPLIED, state.getBoolean(APPLIED, false));
        }
    }
}
