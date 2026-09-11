package vibro.navigator.brouter;

import java.util.concurrent.CancellationException;

final class BRouterStreetReadCancellation {
    private BRouterStreetReadCancellation() {
    }

    static void check() {
        if (Thread.currentThread().isInterrupted()) {
            throw new CancellationException("Street loading cancelled");
        }
    }
}
