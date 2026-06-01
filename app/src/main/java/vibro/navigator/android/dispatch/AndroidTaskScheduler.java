package vibro.navigator.android.dispatch;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import vibro.navigator.dispatch.TaskScheduler;

public final class AndroidTaskScheduler implements TaskScheduler {
    @NonNull
    private final Handler handler;

    public AndroidTaskScheduler(@NonNull Handler handler) {
        this.handler = handler;
    }

    @NonNull
    public static AndroidTaskScheduler main() {
        return new AndroidTaskScheduler(new Handler(Looper.getMainLooper()));
    }

    @Override
    public void post(@NonNull Runnable runnable) {
        handler.post(runnable);
    }

    @Override
    public void postDelayed(@NonNull Runnable runnable, long delayMs) {
        handler.postDelayed(runnable, delayMs);
    }

    @Override
    public void removeCallbacks(@NonNull Runnable runnable) {
        handler.removeCallbacks(runnable);
    }
}
