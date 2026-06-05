package vibro.navigator.nav.location;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

import vibro.navigator.logging.AppLogger;

final class NavigationLocationUpdateRequester {

    interface LegacyUpdatesRemover {
        void removeLegacyUpdates();
    }

    interface AvailabilityDescriptor {
        @NonNull
        String describeAvailability();
    }

    private static final String TAG = "NavLocation";

    @NonNull
    private final NavigationLocationProvider providerAccess;
    @NonNull
    private final FusedLocationUpdateClient fusedLocationUpdateClient;
    @NonNull
    private final NavigationGnssTracker gnssStatusTracker;
    @NonNull
    private final LegacyUpdatesRemover legacyUpdatesRemover;
    @NonNull
    private final AvailabilityDescriptor availabilityDescriptor;

    NavigationLocationUpdateRequester(
            @NonNull NavigationLocationProvider providerAccess,
            @NonNull FusedLocationUpdateClient fusedLocationUpdateClient,
            @NonNull NavigationGnssTracker gnssStatusTracker,
            @NonNull LegacyUpdatesRemover legacyUpdatesRemover,
            @NonNull AvailabilityDescriptor availabilityDescriptor
    ) {
        this.providerAccess = providerAccess;
        this.fusedLocationUpdateClient = fusedLocationUpdateClient;
        this.gnssStatusTracker = gnssStatusTracker;
        this.legacyUpdatesRemover = legacyUpdatesRemover;
        this.availabilityDescriptor = availabilityDescriptor;
    }

    @NonNull
    Result request(
            long minTimeMs,
            boolean useFusedLocation,
            long lastRequestedLocationMinTimeMs,
            @Nullable String lastRequestedProvider
    ) {
        boolean fineGranted = providerAccess.hasFineLocationPermission();
        boolean coarseGranted = providerAccess.hasCoarseLocationPermission();
        if (!NavigationLocationProviders.hasAnyLocationPermission(fineGranted, coarseGranted)) {
            AppLogger.w(TAG, "Location permission unavailable, cannot request updates");
            return Result.clearActiveRequest();
        }

        List<String> legacyProviders = providerAccess.enabledPermittedProviders(fineGranted, coarseGranted);
        String desiredProviderSummary = providerSummary(useFusedLocation, legacyProviders);
        if (desiredProviderSummary == null) {
            AppLogger.w(TAG, "No enabled location provider available for updates "
                    + availabilityDescriptor.describeAvailability());
            return Result.clearActiveRequest();
        }
        if (NavigationLocationProviders.shouldReuseActiveLocationRequest(
                minTimeMs,
                desiredProviderSummary,
                lastRequestedLocationMinTimeMs,
                lastRequestedProvider
        )) {
            return Result.active(desiredProviderSummary);
        }

        fusedLocationUpdateClient.removeUpdates();
        legacyUpdatesRemover.removeLegacyUpdates();
        boolean fusedRequested = requestFusedUpdates(useFusedLocation, minTimeMs, fineGranted, coarseGranted);
        List<String> requestedLegacyProviders = providerAccess.requestProviderUpdates(legacyProviders, minTimeMs);
        String activeProviderSummary = providerSummary(fusedRequested, requestedLegacyProviders);
        if (activeProviderSummary == null) {
            AppLogger.w(TAG, "Failed to request location updates from permitted providers "
                    + availabilityDescriptor.describeAvailability());
            return Result.clearActiveRequest();
        }

        updateGnssStatus(requestedLegacyProviders);
        AppLogger.i(TAG, "Requested location updates provider="
                + activeProviderSummary
                + " minTimeMs="
                + minTimeMs);
        return Result.active(activeProviderSummary);
    }

    private boolean requestFusedUpdates(
            boolean useFusedLocation,
            long minTimeMs,
            boolean fineGranted,
            boolean coarseGranted
    ) {
        if (!useFusedLocation) {
            return false;
        }
        if (fusedLocationUpdateClient.requestUpdates(minTimeMs, fineGranted, coarseGranted)) {
            return true;
        }
        AppLogger.w(TAG, "Fused location unavailable, continuing with legacy providers "
                + fusedLocationUpdateClient.describeAvailability());
        return false;
    }

    private void updateGnssStatus(@NonNull List<String> requestedLegacyProviders) {
        if (requestedLegacyProviders.isEmpty()) {
            gnssStatusTracker.reset();
            return;
        }
        gnssStatusTracker.updateForRequestedProviders(requestedLegacyProviders);
    }

    @Nullable
    private static String providerSummary(boolean includeFused, @NonNull List<String> legacyProviders) {
        List<String> providers = new ArrayList<>(legacyProviders.size() + 1);
        if (includeFused) {
            providers.add(LiveLocationCoordinator.FUSED_PROVIDER);
        }
        providers.addAll(legacyProviders);
        return NavigationLocationProviders.join(providers);
    }

    static final class Result {
        private final boolean clearActiveRequest;
        @Nullable
        private final String activeProviderSummary;

        private Result(boolean clearActiveRequest, @Nullable String activeProviderSummary) {
            this.clearActiveRequest = clearActiveRequest;
            this.activeProviderSummary = activeProviderSummary;
        }

        @NonNull
        static Result unchanged() {
            return new Result(false, null);
        }

        @NonNull
        static Result clearActiveRequest() {
            return new Result(true, null);
        }

        @NonNull
        static Result active(@NonNull String activeProviderSummary) {
            return new Result(false, activeProviderSummary);
        }

        boolean shouldClearActiveRequest() {
            return clearActiveRequest;
        }

        boolean hasActiveRequest() {
            return activeProviderSummary != null;
        }

        @NonNull
        String activeProviderSummary() {
            if (activeProviderSummary == null) {
                throw new IllegalStateException("No active provider summary is available");
            }
            return activeProviderSummary;
        }
    }
}
