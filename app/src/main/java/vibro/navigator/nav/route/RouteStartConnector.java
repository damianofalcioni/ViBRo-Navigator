package vibro.navigator.nav.route;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import vibro.navigator.geo.GeoMath;
import vibro.navigator.geo.LatLon;
import vibro.navigator.nav.guidance.RouteDeviationPolicy;

public final class RouteStartConnector {
    private static final int BEELINE_COMMAND = 16;
    private static final double DEFAULT_CONNECTOR_SPEED_MPS = 1.4;

    private RouteStartConnector() {
    }

    @NonNull
    public static Result apply(
            @NonNull GeoJsonRoute route,
            @NonNull LatLon requestedStart,
            float accuracyMeters
    ) {
        if (route.track.isEmpty()) {
            return Result.unchanged(route);
        }
        double connectorDistanceMeters = distanceMeters(requestedStart, route.track.get(0));
        double thresholdMeters = RouteDeviationPolicy.resolveOffTrackThresholdMeters(accuracyMeters);
        if (connectorDistanceMeters <= thresholdMeters) {
            return Result.unchanged(route);
        }
        return Result.connected(
                prependConnector(route, requestedStart, connectorDistanceMeters),
                connectorDistanceMeters,
                thresholdMeters
        );
    }

    @NonNull
    private static GeoJsonRoute prependConnector(
            @NonNull GeoJsonRoute route,
            @NonNull LatLon requestedStart,
            double connectorDistanceMeters
    ) {
        double connectorTimeSeconds = connectorTimeSeconds(route, connectorDistanceMeters);
        return new GeoJsonRoute(
                connectedTrack(route, requestedStart),
                connectedVoiceHints(route, connectorDistanceMeters),
                connectedTimes(route, connectorTimeSeconds),
                route.totalTimeSeconds + connectorTimeSeconds,
                route.trackLengthMeters + connectorDistanceMeters
        );
    }

    @NonNull
    private static List<LatLon> connectedTrack(@NonNull GeoJsonRoute route, @NonNull LatLon requestedStart) {
        List<LatLon> track = new ArrayList<>(route.track.size() + 1);
        track.add(new LatLon(requestedStart.lat, requestedStart.lon));
        track.addAll(route.track);
        return track;
    }

    @NonNull
    private static List<VoiceHint> connectedVoiceHints(
            @NonNull GeoJsonRoute route,
            double connectorDistanceMeters
    ) {
        List<VoiceHint> voiceHints = new ArrayList<>(route.voiceHints.size() + 1);
        voiceHints.add(new VoiceHint(1, BEELINE_COMMAND, 0, connectorDistanceMeters, 0));
        for (VoiceHint hint : route.voiceHints) {
            voiceHints.add(new VoiceHint(
                    hint.indexInTrack + 1,
                    hint.command,
                    hint.exitNumber,
                    hint.distanceToNextMeters,
                    hint.angleDegrees
            ));
        }
        return voiceHints;
    }

    @NonNull
    private static List<Double> connectedTimes(@NonNull GeoJsonRoute route, double connectorTimeSeconds) {
        if (route.timesSeconds.size() != route.track.size()) {
            return Collections.emptyList();
        }
        List<Double> timesSeconds = new ArrayList<>(route.timesSeconds.size() + 1);
        timesSeconds.add(0.0);
        for (Double timeSeconds : route.timesSeconds) {
            timesSeconds.add(timeSeconds + connectorTimeSeconds);
        }
        return timesSeconds;
    }

    private static double connectorTimeSeconds(@NonNull GeoJsonRoute route, double connectorDistanceMeters) {
        if (route.trackLengthMeters > 0.0 && route.totalTimeSeconds > 0.0) {
            return connectorDistanceMeters * (route.totalTimeSeconds / route.trackLengthMeters);
        }
        return connectorDistanceMeters / DEFAULT_CONNECTOR_SPEED_MPS;
    }

    private static double distanceMeters(@NonNull LatLon first, @NonNull LatLon second) {
        return GeoMath.distanceMeters(first.lat, first.lon, second.lat, second.lon);
    }

    public static final class Result {
        @NonNull
        public final GeoJsonRoute route;
        public final boolean connectorAdded;
        public final double connectorDistanceMeters;
        public final double thresholdMeters;

        private Result(
                @NonNull GeoJsonRoute route,
                boolean connectorAdded,
                double connectorDistanceMeters,
                double thresholdMeters
        ) {
            this.route = route;
            this.connectorAdded = connectorAdded;
            this.connectorDistanceMeters = connectorDistanceMeters;
            this.thresholdMeters = thresholdMeters;
        }

        @NonNull
        private static Result unchanged(@NonNull GeoJsonRoute route) {
            return new Result(route, false, 0.0, 0.0);
        }

        @NonNull
        private static Result connected(
                @NonNull GeoJsonRoute route,
                double connectorDistanceMeters,
                double thresholdMeters
        ) {
            return new Result(route, true, connectorDistanceMeters, thresholdMeters);
        }
    }
}
