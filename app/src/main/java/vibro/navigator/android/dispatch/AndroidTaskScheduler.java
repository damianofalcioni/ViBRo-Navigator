package vibro.navigator.android.dispatch;

import android.os.Handler;
import android.os.Looper;
import android.view.Choreographer;

import androidx.annotation.NonNull;

import java.util.IdentityHashMap;
import java.util.Map;

import vibro.navigator.dispatch.TaskScheduler;

public final class AndroidTaskScheduler implements TaskScheduler {
    @NonNull
    private final Handler handler;
    @NonNull
    private final Map<Runnable, Choreographer.FrameCallback> animationCallbacks = new IdentityHashMap<>();

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
    public void postAnimationFrame(@NonNull Runnable runnable) {
        cancelAnimationFrame(runnable);
        Choreographer.FrameCallback callback = frameTimeNanos -> {
            animationCallbacks.remove(runnable);
            runnable.run();
        };
        animationCallbacks.put(runnable, callback);
        Choreographer.getInstance().postFrameCallback(callback);
    }

    @Override
    public void cancelAnimationFrame(@NonNull Runnable runnable) {
        Choreographer.FrameCallback callback = animationCallbacks.remove(runnable);
        if (callback != null) {
            Choreographer.getInstance().removeFrameCallback(callback);
        }
    }

    @Override
    public void removeCallbacks(@NonNull Runnable runnable) {
        handler.removeCallbacks(runnable);
    }
}
