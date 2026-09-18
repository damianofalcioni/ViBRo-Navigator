package vibro.navigator.dispatch;

import androidx.annotation.NonNull;

public interface TaskScheduler {
    void post(@NonNull Runnable runnable);

    default void postDelayed(@NonNull Runnable runnable, long delayMs) {
        post(runnable);
    }

    default void postAnimationFrame(@NonNull Runnable runnable) {
        postDelayed(runnable, 16L);
    }

    default void cancelAnimationFrame(@NonNull Runnable runnable) {
        removeCallbacks(runnable);
    }

    default void removeCallbacks(@NonNull Runnable runnable) {
    }
}
