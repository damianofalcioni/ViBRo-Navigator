package vibro.navigator.nav.location;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Collections;
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
        String desiredProviderSummary = desiredProviderSummary(useFusedLocation, legacyProviders);
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
        List<String> requestedLegacyProviders = fusedRequested
                ? Collections.emptyList()
                : providerAccess.requestProviderUpdates(legacyProviders, minTimeMs);
        String activeProviderSummary = fusedRequested
                ? LiveLocationCoordinator.FUSED_PROVIDER
                : NavigationLocationProviders.join(requestedLegacyProviders);
        if (activeProviderSummary == null) {
            AppLogger.w(TAG, "Failed to request location updates from permitted providers "
                    + availabilityDescriptor.describeAvailability());
            return Result.clearActiveRequest();
        }

        updateGnssStatus(legacyProviders);
        AppLogger.i(TAG, "Requested location updates provider="
                + activeProviderSummary
                + " minTimeMs="
                + minTimeMs);
        return Result.active(activeProviderSummary, fusedRequested);
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
    private static String desiredProviderSummary(
            boolean useFusedLocation,
            @NonNull List<String> legacyProviders
    ) {
        return useFusedLocation
                ? LiveLocationCoordinator.FUSED_PROVIDER
                : NavigationLocationProviders.join(legacyProviders);
    }

    static final class Result {
        private final boolean clearActiveRequest;
        private final boolean fusedActive;
        @Nullable
        private final String activeProviderSummary;

        private Result(
                boolean clearActiveRequest,
                @Nullable String activeProviderSummary,
                boolean fusedActive
        ) {
            this.clearActiveRequest = clearActiveRequest;
            this.activeProviderSummary = activeProviderSummary;
            this.fusedActive = fusedActive;
        }

        @NonNull
        static Result clearActiveRequest() {
            return new Result(true, null, false);
        }

        @NonNull
        static Result active(@NonNull String activeProviderSummary) {
            return active(activeProviderSummary, LiveLocationCoordinator.FUSED_PROVIDER.equals(activeProviderSummary));
        }

        @NonNull
        static Result active(@NonNull String activeProviderSummary, boolean fusedActive) {
            return new Result(false, activeProviderSummary, fusedActive);
        }

        boolean shouldClearActiveRequest() {
            return clearActiveRequest;
        }

        boolean hasActiveRequest() {
            return activeProviderSummary != null;
        }

        boolean isFusedActive() {
            return fusedActive;
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
