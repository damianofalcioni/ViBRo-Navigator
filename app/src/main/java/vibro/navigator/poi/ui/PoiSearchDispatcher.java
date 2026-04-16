package vibro.navigator.poi.ui;

import androidx.annotation.NonNull;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

final class PoiSearchDispatcher {

    private static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(2);

    private PoiSearchDispatcher() {
    }

    @NonNull
    static Future<?> submit(@NonNull Runnable runnable) {
        return EXECUTOR.submit(runnable);
    }
}
