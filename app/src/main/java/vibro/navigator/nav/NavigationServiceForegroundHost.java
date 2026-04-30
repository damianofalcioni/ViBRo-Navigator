package vibro.navigator.nav;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

final class NavigationServiceForegroundHost implements NavigationForegroundCoordinator.Host {
    interface ForegroundControllerSupplier {
        @Nullable
        NavigationForegroundController get();
    }

    private final ForegroundControllerSupplier foregroundControllerSupplier;
    private final Runnable foregroundPromotion;
    private final Runnable navigationStop;
    private final Runnable serviceStop;

    NavigationServiceForegroundHost(
            @NonNull ForegroundControllerSupplier foregroundControllerSupplier,
            @NonNull Runnable foregroundPromotion,
            @NonNull Runnable navigationStop,
            @NonNull Runnable serviceStop
    ) {
        this.foregroundControllerSupplier = foregroundControllerSupplier;
        this.foregroundPromotion = foregroundPromotion;
        this.navigationStop = navigationStop;
        this.serviceStop = serviceStop;
    }

    @Override
    public boolean isOngoingNotificationVisible() {
        @Nullable NavigationForegroundController foregroundController = foregroundControllerSupplier.get();
        return foregroundController != null && foregroundController.isOngoingNotificationVisible();
    }

    @Override
    public void promoteToForeground() {
        foregroundPromotion.run();
    }

    @Override
    public void stopNavigation() {
        navigationStop.run();
    }

    @Override
    public void stopSelf() {
        serviceStop.run();
    }
}
