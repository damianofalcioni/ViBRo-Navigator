package vibro.navigator.nav.service;


import vibro.navigator.nav.foreground.NavigationForegroundController;
import vibro.navigator.nav.foreground.NavigationForegroundCoordinator;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public final class NavigationServiceForegroundHost implements NavigationForegroundCoordinator.Host {
    public interface ForegroundControllerSupplier {
        @Nullable
        NavigationForegroundController get();
    }

    private final ForegroundControllerSupplier foregroundControllerSupplier;
    private final Runnable foregroundPromotion;
    private final Runnable navigationStop;
    private final Runnable serviceStop;

    public NavigationServiceForegroundHost(
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
