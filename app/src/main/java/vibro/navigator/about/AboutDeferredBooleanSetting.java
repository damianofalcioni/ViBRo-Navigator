package vibro.navigator.about;

import android.widget.Switch;

import androidx.annotation.NonNull;

import vibro.navigator.dispatch.TaskScheduler;

final class AboutDeferredBooleanSetting {

    interface Writer {
        void write(boolean enabled);
    }

    // Keep writes past the switch thumb animation so I/O or package-manager work cannot pause it midway.
    private static final long APPLY_DELAY_MS = 300L;

    @NonNull
    private final TaskScheduler scheduler;
    @NonNull
    private final Writer writer;
    @NonNull
    private final Runnable afterWrite;
    @NonNull
    private final Runnable applyPending;

    private boolean pending;
    private boolean pendingValue;
    private boolean rendering;

    AboutDeferredBooleanSetting(
            @NonNull TaskScheduler scheduler,
            @NonNull Writer writer,
            @NonNull Runnable afterWrite
    ) {
        this.scheduler = scheduler;
        this.writer = writer;
        this.afterWrite = afterWrite;
        applyPending = this::applyNow;
    }

    void set(boolean value) {
        if (rendering) {
            return;
        }
        pendingValue = value;
        if (pending) {
            return;
        }
        pending = true;
        scheduler.postDelayed(applyPending, APPLY_DELAY_MS);
    }

    void flush() {
        flush(true);
    }

    void flush(boolean runAfterWrite) {
        if (!pending) {
            return;
        }
        scheduler.removeCallbacks(applyPending);
        applyNow(runAfterWrite);
    }

    void render(@NonNull Switch switchView, boolean storedValue) {
        rendering = true;
        try {
            switchView.setChecked(pending ? pendingValue : storedValue);
        } finally {
            rendering = false;
        }
    }

    private void applyNow() {
        applyNow(true);
    }

    private void applyNow(boolean runAfterWrite) {
        boolean value = pendingValue;
        pending = false;
        writer.write(value);
        if (runAfterWrite) {
            afterWrite.run();
        }
    }
}
