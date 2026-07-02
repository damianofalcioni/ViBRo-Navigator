package vibro.navigator.nav.presentation;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import vibro.navigator.R;
import vibro.navigator.nav.compass.NavCompassStateFactory;
import vibro.navigator.nav.format.NavigationTextResources;
import vibro.navigator.nav.location.NavigationGpsTelemetryFormatter;
import vibro.navigator.nav.location.NavigationLocation;
import vibro.navigator.nav.model.NavGpsStatus;
import vibro.navigator.nav.model.NavGpsTelemetry;
import vibro.navigator.nav.model.NavPauseStatus;
import vibro.navigator.nav.model.NavState;

public final class NavStateResourceComposer {
    private NavStateResourceComposer() {
    }

    @NonNull
    public static NavState waiting(@NonNull NavigationTextResources textResources) {
        String noRoute = textResources.getString(R.string.nav_no_route);
        NavGpsTelemetry gpsTelemetry = defaultGpsTelemetry(textResources);
        return NavStateComposer.create(
                noRoute,
                "",
                "",
                "",
                gpsTelemetry.compactLine,
                NavState.NO_DEADLINE,
                noRoute,
                null,
                false,
                gpsTelemetry
        );
    }

    @NonNull
    public static NavState waitingForLocation(@NonNull NavigationTextResources textResources) {
        return waitingForLocation(textResources, NavState.NO_DEADLINE);
    }

    @NonNull
    public static NavState waitingForLocation(
            @NonNull NavigationTextResources textResources,
            long nextEvaluationDeadlineElapsedMs
    ) {
        NavGpsTelemetry gpsTelemetry = defaultGpsTelemetry(textResources);
        return NavStateComposer.create(
                textResources.getString(R.string.nav_waiting_for_location_title),
                "",
                "",
                "",
                gpsTelemetry.compactLine,
                nextEvaluationDeadlineElapsedMs,
                textResources.getString(R.string.nav_waiting_for_location_body),
                null,
                false,
                gpsTelemetry
        );
    }

    @NonNull
    public static NavState calculatingRoute(@NonNull NavigationTextResources textResources) {
        return calculatingRoute(textResources, NavState.NO_DEADLINE);
    }

    @NonNull
    public static NavState calculatingRoute(
            @NonNull NavigationTextResources textResources,
            long nextEvaluationDeadlineElapsedMs
    ) {
        NavGpsTelemetry gpsTelemetry = defaultGpsTelemetry(textResources);
        return NavStateComposer.create(
                textResources.getString(R.string.nav_calculating_route_title),
                "",
                "",
                "",
                gpsTelemetry.compactLine,
                nextEvaluationDeadlineElapsedMs,
                textResources.getString(R.string.nav_calculating_route_body),
                null,
                false,
                gpsTelemetry
        );
    }

    @NonNull
    public static NavState routeUnavailable(@NonNull NavigationTextResources textResources, @NonNull String detail) {
        return routeUnavailable(textResources, detail, NavState.NO_DEADLINE);
    }

    @NonNull
    public static NavState routeUnavailable(
            @NonNull NavigationTextResources textResources,
            @NonNull String detail,
            long nextEvaluationDeadlineElapsedMs
    ) {
        NavGpsTelemetry gpsTelemetry = defaultGpsTelemetry(textResources);
        return NavStateComposer.create(
                textResources.getString(R.string.nav_route_unavailable_title),
                "",
                "",
                "",
                gpsTelemetry.compactLine,
                nextEvaluationDeadlineElapsedMs,
                textResources.getString(R.string.format_nav_route_unavailable_body, detail),
                null,
                false,
                gpsTelemetry
        );
    }

    @NonNull
    public static NavState withPauseState(
            @NonNull NavigationTextResources textResources,
            @NonNull NavState base,
            boolean paused
    ) {
        String detail = base.routeStatus.progress.detailBlock;
        if (paused) {
            String pauseNotice = textResources.getString(R.string.nav_paused_notice);
            detail = detail.isEmpty() ? pauseNotice : pauseNotice + "\n" + detail;
        }
        return new NavState(
                base.routeStatus.withProgress(base.routeStatus.progress.withDetailBlock(detail)),
                base.gpsStatus,
                new NavPauseStatus(paused)
        );
    }

    @NonNull
    public static String buildGpsStatusLine(
            float speedMps,
            @Nullable NavigationLocation currentLocation,
            float accuracyMeters,
            @Nullable Integer fixedSatelliteCount,
            @Nullable Integer acquiredFixCount,
            @NonNull NavigationTextResources textResources
    ) {
        return NavCompassStateFactory.buildGpsStatusLine(
                speedMps,
                currentLocation,
                accuracyMeters,
                fixedSatelliteCount,
                acquiredFixCount,
                textResources
        );
    }

    @NonNull
    public static NavGpsStatus buildGpsStatus(
            float speedMps,
            @Nullable NavigationLocation currentLocation,
            float accuracyMeters,
            @Nullable Integer fixedSatelliteCount,
            @Nullable Integer acquiredFixCount,
            long nextEvaluationDeadlineElapsedMs,
            @NonNull NavigationTextResources textResources
    ) {
        NavGpsTelemetry telemetry = NavigationGpsTelemetryFormatter.format(
                textResources,
                speedMps,
                currentLocation,
                accuracyMeters,
                fixedSatelliteCount,
                acquiredFixCount
        );
        return new NavGpsStatus(telemetry.compactLine, nextEvaluationDeadlineElapsedMs, telemetry);
    }

    @NonNull
    private static NavGpsTelemetry defaultGpsTelemetry(@NonNull NavigationTextResources textResources) {
        return NavigationGpsTelemetryFormatter.format(textResources, Float.NaN, null, Float.NaN, null, null);
    }
}
