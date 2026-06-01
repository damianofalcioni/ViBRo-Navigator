package vibro.navigator.dispatch;

import androidx.annotation.NonNull;

public interface TaskScheduler {
    void post(@NonNull Runnable runnable);

    default void postDelayed(@NonNull Runnable runnable, long delayMs) {
        post(runnable);
    }

    default void removeCallbacks(@NonNull Runnable runnable) {
    }
}
