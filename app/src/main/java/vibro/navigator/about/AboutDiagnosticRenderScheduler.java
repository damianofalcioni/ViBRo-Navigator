package vibro.navigator.about;

import androidx.annotation.NonNull;

import vibro.navigator.android.dispatch.AndroidTaskScheduler;
import vibro.navigator.dispatch.TaskScheduler;

final class AboutDiagnosticRenderScheduler {

    // A normal post can still run before the first activity draw; keep diagnostics just past launch.
    private static final long INITIAL_DIAGNOSTIC_RENDER_DELAY_MS = 50L;

    @NonNull
    private final TaskScheduler scheduler;
    @NonNull
    private final Runnable renderNow;
    private boolean started;
    private boolean scheduled;
    @NonNull
    private final Runnable deferredRender;

    AboutDiagnosticRenderScheduler(
            @NonNull TaskScheduler scheduler,
            @NonNull Runnable renderNow
    ) {
        this.scheduler = scheduler;
        this.renderNow = renderNow;
        deferredRender = () -> {
            scheduled = false;
            if (started) {
                this.renderNow.run();
            }
        };
    }

    @NonNull
    static AboutDiagnosticRenderScheduler mainThread(@NonNull Runnable renderNow) {
        return new AboutDiagnosticRenderScheduler(AndroidTaskScheduler.main(), renderNow);
    }

    void start() {
        started = true;
        schedule();
    }

    void stop() {
        started = false;
        scheduled = false;
        scheduler.removeCallbacks(deferredRender);
    }

    void schedule() {
        if (!started || scheduled) {
            return;
        }
        scheduled = true;
        scheduler.postDelayed(deferredRender, INITIAL_DIAGNOSTIC_RENDER_DELAY_MS);
    }

    void renderNow() {
        scheduled = false;
        scheduler.removeCallbacks(deferredRender);
        renderNow.run();
    }

    boolean isStarted() {
        return started;
    }
}
