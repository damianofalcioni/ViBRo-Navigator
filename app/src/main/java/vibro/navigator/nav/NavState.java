package vibro.navigator.nav;

import android.content.Context;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import vibro.navigator.geo.GeoMath;
import vibro.navigator.R;
import vibro.navigator.geo.LatLon;
import vibro.navigator.nav.route.GeoJsonRoute;
import vibro.navigator.nav.route.PolylineIndex;
import vibro.navigator.nav.route.VoiceHint;

import android.location.Location;

import java.util.ArrayList;
import java.util.List;

public final class NavState {
    public static final long NO_DEADLINE = -1L;
    private static final float COMPASS_MOVING_LOOKAHEAD_SECONDS = 60f;
    private static final float COMPASS_MIN_VISIBLE_RADIUS_METERS = 90f;
    private static final long COMPASS_RADIUS_SMOOTHING_TIME_CONSTANT_MS = 1_200L;
    private static final int MAX_COMPASS_ROUTE_POINTS = 240;
    private static final int MAX_COMPASS_HINT_POINTS = 48;

    @NonNull
    public final String nextLine;
    @NonNull
    public final String afterNextLine;
    @NonNull
    public final String destinationLine;
    @NonNull
    public final String stopProgressBlock;
    @NonNull
    public final String gpsStatusLine;
    public final long nextEvaluationDeadlineElapsedMs;
    @NonNull
    public final String detailBlock;
    @Nullable
    public final NavCompassState compassState;
    public final boolean paused;

    private NavState(@NonNull String nextLine,
                     @NonNull String afterNextLine,
                     @NonNull String destinationLine,
                     @NonNull String stopProgressBlock,
                     @NonNull String gpsStatusLine,
                     long nextEvaluationDeadlineElapsedMs,
                     @NonNull String detailBlock,
                     @Nullable NavCompassState compassState,
                     boolean paused) {
        this.nextLine = nextLine;
        this.afterNextLine = afterNextLine;
        this.destinationLine = destinationLine;
        this.stopProgressBlock = stopProgressBlock;
        this.gpsStatusLine = gpsStatusLine;
        this.nextEvaluationDeadlineElapsedMs = nextEvaluationDeadlineElapsedMs;
        this.detailBlock = detailBlock;
        this.compassState = compassState;
        this.paused = paused;
    }

    @NonNull
    public String displayStatusBlock() {
        if (!detailBlock.isEmpty()) {
            return detailBlock;
        }
        if (destinationLine.isEmpty()) {
            return stopProgressBlock;
        }
        if (stopProgressBlock.isEmpty()) {
            return destinationLine;
        }
        return destinationLine + "\n" + stopProgressBlock;
    }

    @NonNull
    public static NavState waiting(@NonNull Context context) {
        String noRoute = context.getString(R.string.nav_no_route);
        return new NavState(noRoute, "", "", "", defaultGpsStatusLine(context), NO_DEADLINE, noRoute, null, false);
    }

    @NonNull
    public static NavState waitingForLocation(@NonNull Context context) {
        return waitingForLocation(context, NO_DEADLINE);
    }

    @NonNull
    public static NavState waitingForLocation(@NonNull Context context, long nextEvaluationDeadlineElapsedMs) {
        return new NavState(
                context.getString(R.string.nav_waiting_for_location_title),
                "",
                "",
                "",
                defaultGpsStatusLine(context),
                nextEvaluationDeadlineElapsedMs,
                context.getString(R.string.nav_waiting_for_location_body),
                null,
                false
        );
    }

    @NonNull
    public static NavState calculatingRoute(@NonNull Context context) {
        return calculatingRoute(context, NO_DEADLINE);
    }

    @NonNull
    public static NavState calculatingRoute(@NonNull Context context, long nextEvaluationDeadlineElapsedMs) {
        return new NavState(
                context.getString(R.string.nav_calculating_route_title),
                "",
                "",
                "",
                defaultGpsStatusLine(context),
                nextEvaluationDeadlineElapsedMs,
                context.getString(R.string.nav_calculating_route_body),
                null,
                false
        );
    }

    @NonNull
    public static NavState routeUnavailable(@NonNull Context context, @NonNull String detail) {
        return routeUnavailable(context, detail, NO_DEADLINE);
    }

    @NonNull
    public static NavState routeUnavailable(@NonNull Context context,
                                            @NonNull String detail,
                                            long nextEvaluationDeadlineElapsedMs) {
        return new NavState(
                context.getString(R.string.nav_route_unavailable_title),
                "",
                "",
                "",
                defaultGpsStatusLine(context),
                nextEvaluationDeadlineElapsedMs,
                context.getString(R.string.format_nav_route_unavailable_body, detail),
                null,
                false
        );
    }

    @NonNull
    public static NavState withNotice(@NonNull NavState base, @NonNull String notice) {
        if (notice.trim().isEmpty()) {
            return base;
        }
        String detail = base.detailBlock.isEmpty()
                ? notice
                : notice + "\n" + base.detailBlock;
        return new NavState(
                base.nextLine,
                base.afterNextLine,
                base.destinationLine,
                base.stopProgressBlock,
                base.gpsStatusLine,
                base.nextEvaluationDeadlineElapsedMs,
                detail,
                base.compassState,
                base.paused
        );
    }

    @NonNull
    public static NavState withGpsStatus(@NonNull NavState base, @NonNull String gpsStatusLine) {
        return new NavState(
                base.nextLine,
                base.afterNextLine,
                base.destinationLine,
                base.stopProgressBlock,
                gpsStatusLine,
                base.nextEvaluationDeadlineElapsedMs,
                base.detailBlock,
                base.compassState,
                base.paused
        );
    }

    @NonNull
    public static NavState withPauseState(@NonNull Context context, @NonNull NavState base, boolean paused) {
        String detail = base.detailBlock;
        if (paused) {
            String pauseNotice = context.getString(R.string.nav_paused_notice);
            detail = detail.isEmpty() ? pauseNotice : pauseNotice + "\n" + detail;
        }
        return new NavState(
                base.nextLine,
                base.afterNextLine,
                base.destinationLine,
                base.stopProgressBlock,
                base.gpsStatusLine,
                base.nextEvaluationDeadlineElapsedMs,
                detail,
                base.compassState,
                paused
        );
    }

    @NonNull
    public static NavState from(
            @NonNull GeoJsonRoute route,
            @NonNull PolylineIndex index,
            double alongTrackMeters,
            int nextHintIdx,
            int currentSegmentIndex,
            float speedMps,
            boolean likelyStationary,
            float accuracyMeters,
            @NonNull Location currentLocation,
            @Nullable Integer fixedSatelliteCount,
            @Nullable Double headingDegrees,
            @Nullable Float headingAccuracyDegrees,
            @Nullable Float previousCompassVisibleRadiusMeters,
            @Nullable Float previousReliableMovingCompassVisibleRadiusMeters,
            long compassRadiusUpdateDeltaMs,
            long nextEvaluationDeadlineElapsedMs,
            long nowMs,
            @NonNull List<NavTarget> targets,
            @NonNull Context context
    ) {
        return from(
                route,
                index,
                alongTrackMeters,
                nextHintIdx,
                currentSegmentIndex,
                speedMps,
                speedMps,
                likelyStationary,
                accuracyMeters,
                accuracyMeters,
                currentLocation,
                fixedSatelliteCount,
                headingDegrees,
                headingAccuracyDegrees,
                previousCompassVisibleRadiusMeters,
                previousReliableMovingCompassVisibleRadiusMeters,
                compassRadiusUpdateDeltaMs,
                null,
                null,
                nextEvaluationDeadlineElapsedMs,
                nowMs,
                targets,
                context
        );
    }

    @NonNull
    public static NavState from(
            @NonNull GeoJsonRoute route,
            @NonNull PolylineIndex index,
            double alongTrackMeters,
            int nextHintIdx,
            int currentSegmentIndex,
            float speedMps,
            float etaSpeedMps,
            boolean likelyStationary,
            float accuracyMeters,
            @NonNull Location currentLocation,
            @Nullable Integer fixedSatelliteCount,
            @Nullable Double headingDegrees,
            @Nullable Float headingAccuracyDegrees,
            @Nullable Float previousCompassVisibleRadiusMeters,
            @Nullable Float previousReliableMovingCompassVisibleRadiusMeters,
            long compassRadiusUpdateDeltaMs,
            long nextEvaluationDeadlineElapsedMs,
            long nowMs,
            @NonNull List<NavTarget> targets,
            @NonNull Context context
    ) {
        return from(
                route,
                index,
                alongTrackMeters,
                nextHintIdx,
                currentSegmentIndex,
                speedMps,
                etaSpeedMps,
                likelyStationary,
                accuracyMeters,
                accuracyMeters,
                currentLocation,
                fixedSatelliteCount,
                headingDegrees,
                headingAccuracyDegrees,
                previousCompassVisibleRadiusMeters,
                previousReliableMovingCompassVisibleRadiusMeters,
                compassRadiusUpdateDeltaMs,
                null,
                null,
                nextEvaluationDeadlineElapsedMs,
                nowMs,
                targets,
                context
        );
    }

    @NonNull
    static NavState from(
            @NonNull GeoJsonRoute route,
            @NonNull PolylineIndex index,
            double alongTrackMeters,
            int nextHintIdx,
            int currentSegmentIndex,
            float speedMps,
            float etaSpeedMps,
            boolean likelyStationary,
            float accuracyMeters,
            float compassAccuracyMeters,
            @NonNull Location currentLocation,
            @Nullable Integer fixedSatelliteCount,
            @Nullable Double headingDegrees,
            @Nullable Float headingAccuracyDegrees,
            @Nullable Float previousCompassVisibleRadiusMeters,
            @Nullable Float previousReliableMovingCompassVisibleRadiusMeters,
            long compassRadiusUpdateDeltaMs,
            @Nullable CompassRouteGeometry compassRouteGeometry,
            @Nullable CompassRadiusTransition compassRadiusTransition,
            long nextEvaluationDeadlineElapsedMs,
            long nowMs,
            @NonNull List<NavTarget> targets,
            @NonNull Context context
    ) {
        List<String> directionLines = buildDirectionLines(
                route,
                index,
                alongTrackMeters,
                nextHintIdx,
                currentSegmentIndex,
                etaSpeedMps,
                accuracyMeters,
                context
        );
        String next = directionLines.isEmpty() ? "" : directionLines.get(0);
        String afterNext = directionLines.size() > 1 ? directionLines.get(1) : "";
        String destination = buildDestinationLine(
                route,
                index,
                alongTrackMeters,
                currentSegmentIndex,
                etaSpeedMps,
                nowMs,
                targets,
                context
        );
        String stopProgress = buildStopProgress(
                route,
                index,
                alongTrackMeters,
                currentSegmentIndex,
                etaSpeedMps,
                nowMs,
                targets,
                context
        );
        String gpsStatus = buildGpsStatusLine(speedMps, currentLocation, accuracyMeters, fixedSatelliteCount, context);
        NavCompassState compassState = buildCompassState(
                route,
                index,
                alongTrackMeters,
                currentLocation,
                speedMps,
                likelyStationary,
                compassAccuracyMeters,
                headingDegrees,
                headingAccuracyDegrees,
                previousCompassVisibleRadiusMeters,
                previousReliableMovingCompassVisibleRadiusMeters,
                compassRadiusUpdateDeltaMs,
                compassRouteGeometry,
                compassRadiusTransition,
                nowMs
        );
        return new NavState(
                next,
                afterNext,
                destination,
                stopProgress,
                gpsStatus,
                nextEvaluationDeadlineElapsedMs,
                "",
                compassState,
                false
        );
    }

    @NonNull
    static CompassRouteGeometry buildCompassRouteGeometry(
            @NonNull GeoJsonRoute route,
            @NonNull PolylineIndex index
    ) {
        List<CompassRouteGeometry.SamplePoint> routeSamplePoints = new ArrayList<>();
        if (!route.track.isEmpty()) {
            if (route.track.size() == 1) {
                routeSamplePoints.add(new CompassRouteGeometry.SamplePoint(route.track.get(0), 0.0));
            } else {
                double totalLengthMeters = index.totalLengthMeters();
                double stepMeters = Math.max(12.0, totalLengthMeters / MAX_COMPASS_ROUTE_POINTS);
                addSampledRoutePoints(index, 0.0, totalLengthMeters, stepMeters, routeSamplePoints);
            }
        }

        List<LatLon> hintSamplePoints = new ArrayList<>();
        if (!route.voiceHints.isEmpty()) {
            if (route.voiceHints.size() <= MAX_COMPASS_HINT_POINTS) {
                addHintSamplePoints(route, index, 0, route.voiceHints.size() - 1, hintSamplePoints);
            } else {
                double hintStep = (route.voiceHints.size() - 1d) / (MAX_COMPASS_HINT_POINTS - 1d);
                int lastSelectedIndex = -1;
                for (int i = 0; i < MAX_COMPASS_HINT_POINTS; i++) {
                    int selectedIndex = (int) Math.round(i * hintStep);
                    selectedIndex = Math.min(route.voiceHints.size() - 1, Math.max(0, selectedIndex));
                    if (selectedIndex == lastSelectedIndex) {
                        continue;
                    }
                    addHintSamplePoints(route, index, selectedIndex, selectedIndex, hintSamplePoints);
                    lastSelectedIndex = selectedIndex;
                }
            }
        }
        return new CompassRouteGeometry(routeSamplePoints, hintSamplePoints);
    }

    @NonNull
    static String buildGpsStatusLine(
            float speedMps,
            @Nullable Location currentLocation,
            float accuracyMeters,
            @Nullable Integer fixedSatelliteCount,
            @NonNull Context context
    ) {
        Double elevationMeters = currentLocation != null && currentLocation.hasAltitude()
                ? currentLocation.getAltitude()
                : null;
        Float bearingDegrees = currentLocation != null && currentLocation.hasBearing()
                ? currentLocation.getBearing()
                : null;
        Float bearingAccuracyDegrees = currentLocation != null
                && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                && currentLocation.hasBearingAccuracy()
                ? currentLocation.getBearingAccuracyDegrees()
                : null;
        return NavigationTextFormatter.formatGpsStatus(
                context,
                speedMps,
                elevationMeters,
                accuracyMeters,
                bearingDegrees,
                bearingAccuracyDegrees,
                fixedSatelliteCount
        );
    }

    @NonNull
    private static String defaultGpsStatusLine(@NonNull Context context) {
        return buildGpsStatusLine(Float.NaN, null, Float.NaN, null, context);
    }

    @NonNull
    private static List<String> buildDirectionLines(
            @NonNull GeoJsonRoute route,
            @NonNull PolylineIndex index,
            double alongTrackMeters,
            int hintIdx,
            int currentSegmentIndex,
            float speedMps,
            float accuracyMeters,
            @NonNull Context context
    ) {
        if (route.voiceHints.isEmpty() || hintIdx < 0 || hintIdx >= route.voiceHints.size()) {
            return new ArrayList<>();
        }
        List<UpcomingHint> upcomingHints = new ArrayList<>(2);
        double minReliableDistanceMeters = minimumReliableTurnDistanceMeters(accuracyMeters);
        for (int i = hintIdx; i < route.voiceHints.size() && upcomingHints.size() < 2; i++) {
            VoiceHint hint = route.voiceHints.get(i);
            double hintDist = index.distanceAtPointIndex(hint.indexInTrack);
            double dist = Math.max(0.0, hintDist - alongTrackMeters);
            if (dist <= minReliableDistanceMeters) {
                continue;
            }
            Double timeSeconds = RouteTimeEstimator.estimateSecondsToTrackPoint(
                    route,
                    index,
                    alongTrackMeters,
                    currentSegmentIndex,
                    hint.indexInTrack,
                    speedMps
            );
            upcomingHints.add(new UpcomingHint(
                    hint,
                    dist,
                    timeSeconds != null ? timeSeconds : Double.NaN
            ));
        }
        List<String> lines = new ArrayList<>(upcomingHints.size());
        if (upcomingHints.isEmpty()) {
            return lines;
        }
        UpcomingHint nextHint = upcomingHints.get(0);
        lines.add(NavigationTextFormatter.formatTurnNotification(
                context,
                nextHint.hint,
                nextHint.distanceMeters,
                nextHint.timeSeconds
        ));
        if (upcomingHints.size() > 1) {
            UpcomingHint afterNextHint = upcomingHints.get(1);
            double relativeDistanceMeters = Math.max(
                    0.0,
                    afterNextHint.distanceMeters - nextHint.distanceMeters
            );
            double relativeTimeSeconds = Double.isFinite(nextHint.timeSeconds)
                    && Double.isFinite(afterNextHint.timeSeconds)
                    ? Math.max(0.0, afterNextHint.timeSeconds - nextHint.timeSeconds)
                    : resolveRelativeHintTimeSeconds(route, index, nextHint, afterNextHint, speedMps);
            lines.add(NavigationTextFormatter.formatTurnNotification(
                    context,
                    afterNextHint.hint,
                    relativeDistanceMeters,
                    relativeTimeSeconds
            ));
        }
        return lines;
    }

    private static double minimumReliableTurnDistanceMeters(float accuracyMeters) {
        double safeAccuracyMeters = Float.isFinite(accuracyMeters) && accuracyMeters > 0f
                ? accuracyMeters
                : 0.0;
        return Math.max(5.0, safeAccuracyMeters);
    }

    private static double resolveRelativeHintTimeSeconds(
            @NonNull GeoJsonRoute route,
            @NonNull PolylineIndex index,
            @NonNull UpcomingHint nextHint,
            @NonNull UpcomingHint afterNextHint,
            float speedMps
    ) {
        Double estimatedSeconds = RouteTimeEstimator.estimateSecondsBetweenTrackPoints(
                route,
                index,
                nextHint.hint.indexInTrack,
                afterNextHint.hint.indexInTrack
        );
        return estimatedSeconds != null ? estimatedSeconds : Double.NaN;
    }

    @NonNull
    private static String buildDestinationLine(
            @NonNull GeoJsonRoute route,
            @NonNull PolylineIndex index,
            double alongTrackMeters,
            int currentSegmentIndex,
            float speedMps,
            long nowMs,
            @NonNull List<NavTarget> targets,
            @NonNull Context context
    ) {
        if (targets.isEmpty()) {
            return "";
        }
        NavTarget destination = targets.get(targets.size() - 1);
        double distTo = Math.max(0.0, destination.alongTrackMeters - alongTrackMeters);
        Double secTo = RouteTimeEstimator.estimateSecondsToAlongTrack(
                route,
                index,
                alongTrackMeters,
                currentSegmentIndex,
                destination.alongTrackMeters,
                speedMps
        );
        return buildProgressLine(context, destination.label, distTo, secTo, nowMs);
    }

    @NonNull
    private static String buildStopProgress(
            @NonNull GeoJsonRoute route,
            @NonNull PolylineIndex index,
            double alongTrackMeters,
            int currentSegmentIndex,
            float speedMps,
            long nowMs,
            @NonNull List<NavTarget> targets,
            @NonNull Context context
    ) {
        int lastStopIndex = Math.max(0, targets.size() - 1);
        for (int i = 0; i < lastStopIndex; i++) {
            NavTarget t = targets.get(i);
            double distTo = Math.max(0.0, t.alongTrackMeters - alongTrackMeters);
            if (distTo <= 0.0) {
                continue;
            }
            Double secTo = RouteTimeEstimator.estimateSecondsToAlongTrack(
                    route,
                    index,
                    alongTrackMeters,
                    currentSegmentIndex,
                    t.alongTrackMeters,
                    speedMps
            );
            return buildProgressLine(context, t.label, distTo, secTo, nowMs);
        }
        return "";
    }

    @NonNull
    private static String buildProgressLine(
            @NonNull Context context,
            @NonNull String label,
            double distanceMeters,
            @Nullable Double seconds,
            long nowMs
    ) {
        String timeText = NavigationTextFormatter.formatTimeSeconds(
                context,
                seconds != null ? seconds : Double.NaN
        );
        String etaText = seconds != null && Double.isFinite(seconds)
                ? NavigationTextFormatter.formatEta(nowMs + (long) (seconds * 1000))
                : context.getString(R.string.nav_status_unavailable);
        return context.getString(
                R.string.format_progress_line,
                label,
                NavigationTextFormatter.formatDistance(context, distanceMeters),
                timeText,
                context.getString(R.string.nav_eta),
                etaText
        );
    }

    @Nullable
    private static NavCompassState buildCompassState(
            @NonNull GeoJsonRoute route,
            @NonNull PolylineIndex index,
            double alongTrackMeters,
            @NonNull Location currentLocation,
            float speedMps,
            boolean likelyStationary,
            float compassAccuracyMeters,
            @Nullable Double headingDegrees,
            @Nullable Float headingAccuracyDegrees,
            @Nullable Float previousCompassVisibleRadiusMeters,
            @Nullable Float previousReliableMovingCompassVisibleRadiusMeters,
            long compassRadiusUpdateDeltaMs,
            @Nullable CompassRouteGeometry compassRouteGeometry,
            @Nullable CompassRadiusTransition compassRadiusTransition,
            long nowMs
    ) {
        if (route.track.isEmpty()) {
            return null;
        }

        double currentLat = currentLocation.getLatitude();
        double currentLon = currentLocation.getLongitude();
        CompassRouteGeometry routeGeometry = compassRouteGeometry != null
                ? compassRouteGeometry
                : buildCompassRouteGeometry(route, index);

        LatLon routeEndPoint = route.track.get(route.track.size() - 1);
        float destinationEastMeters = (float) GeoMath.eastMeters(currentLat, currentLon, routeEndPoint.lat, routeEndPoint.lon);
        float destinationNorthMeters = (float) GeoMath.northMeters(currentLat, routeEndPoint.lat);
        double destinationDistanceMeters = Math.hypot(destinationEastMeters, destinationNorthMeters);
        double furthestDistanceMeters = Math.max(
                resolveFurthestRouteSampleDistanceMeters(routeGeometry, currentLat, currentLon),
                destinationDistanceMeters
        );

        float fullRouteVisibleRadiusMeters = (float) Math.max(
                COMPASS_MIN_VISIBLE_RADIUS_METERS,
                furthestDistanceMeters * 1.15
        );
        boolean reliableMovingSpeed = hasReliableMovingSpeed(currentLocation, likelyStationary);
        boolean hasReusableMovingRadius = previousReliableMovingCompassVisibleRadiusMeters != null
                && Float.isFinite(previousReliableMovingCompassVisibleRadiusMeters)
                && previousReliableMovingCompassVisibleRadiusMeters > 0f;
        float targetVisibleRadiusMeters;
        if (likelyStationary) {
            targetVisibleRadiusMeters = fullRouteVisibleRadiusMeters;
        } else if (reliableMovingSpeed) {
            targetVisibleRadiusMeters = Math.min(
                    fullRouteVisibleRadiusMeters,
                    resolveMovingVisibleRadiusMeters(speedMps)
            );
        } else if (hasReusableMovingRadius) {
            targetVisibleRadiusMeters = Math.min(
                    fullRouteVisibleRadiusMeters,
                    previousReliableMovingCompassVisibleRadiusMeters
            );
        } else {
            targetVisibleRadiusMeters = fullRouteVisibleRadiusMeters;
        }
        boolean fullRouteOverview = likelyStationary
                || (!reliableMovingSpeed && !hasReusableMovingRadius);
        float visibleRadiusMeters;
        if (fullRouteOverview) {
            visibleRadiusMeters = compassRadiusTransition == null
                    ? fullRouteVisibleRadiusMeters
                    : compassRadiusTransition.resolve(
                            previousCompassVisibleRadiusMeters != null
                                    ? previousCompassVisibleRadiusMeters
                                    : fullRouteVisibleRadiusMeters,
                            fullRouteVisibleRadiusMeters,
                            previousCompassVisibleRadiusMeters != null
                                    && Float.isFinite(previousCompassVisibleRadiusMeters)
                                    && previousCompassVisibleRadiusMeters > 0f,
                            nowMs
                    );
        } else {
            if (compassRadiusTransition != null) {
                compassRadiusTransition.reset();
            }
            visibleRadiusMeters = hasReusableMovingRadius && !reliableMovingSpeed
                    ? targetVisibleRadiusMeters
                    : smoothVisibleRadiusMeters(
                            targetVisibleRadiusMeters,
                            previousCompassVisibleRadiusMeters,
                            compassRadiusUpdateDeltaMs
                    );
        }
        boolean usingMovingScale = !likelyStationary && (reliableMovingSpeed || hasReusableMovingRadius);
        float referenceSpeedMps = usingMovingScale
                ? resolveMovingLegendReferenceSpeedMps(visibleRadiusMeters)
                : sanitizeReferenceSpeedMps(speedMps);
        float routeThresholdMeters = usingMovingScale
                ? (float) RouteDeviationPolicy.resolveOffTrackThresholdMeters(compassAccuracyMeters)
                : 0f;
        float resolvedHeading = normalizeHeading(headingDegrees == null ? 0.0 : headingDegrees);
        return new NavCompassState(
                resolvedHeading,
                sanitizeHeadingAccuracyDegrees(headingAccuracyDegrees),
                referenceSpeedMps,
                visibleRadiusMeters,
                sanitizeAccuracyMeters(compassAccuracyMeters),
                usingMovingScale,
                routeThresholdMeters,
                routeGeometry,
                currentLat,
                currentLon,
                routeGeometry.passedRoutePointCount(alongTrackMeters),
                destinationEastMeters,
                destinationNorthMeters,
                destinationDistanceMeters <= visibleRadiusMeters
        );
    }

    private static void addSampledRoutePoints(
            @NonNull PolylineIndex index,
            double startMeters,
            double endMeters,
            double stepMeters,
            @NonNull List<CompassRouteGeometry.SamplePoint> target
    ) {
        if (endMeters < startMeters) {
            return;
        }
        for (double distance = startMeters; distance < endMeters; distance += stepMeters) {
            LatLon point = index.pointAtDistance(distance);
            if (point == null) {
                continue;
            }
            target.add(new CompassRouteGeometry.SamplePoint(point, distance));
        }
        LatLon endPoint = index.pointAtDistance(endMeters);
        if (endPoint != null) {
            target.add(new CompassRouteGeometry.SamplePoint(endPoint, endMeters));
        }
    }

    private static void addHintSamplePoints(
            @NonNull GeoJsonRoute route,
            @NonNull PolylineIndex index,
            int startHintIndex,
            int endHintIndex,
            @NonNull List<LatLon> target
    ) {
        for (int i = startHintIndex; i <= endHintIndex; i++) {
            VoiceHint hint = route.voiceHints.get(i);
            LatLon hintPoint = index.pointAtDistance(index.distanceAtPointIndex(hint.indexInTrack));
            if (hintPoint != null) {
                target.add(hintPoint);
            }
        }
    }

    private static double resolveFurthestRouteSampleDistanceMeters(
            @NonNull CompassRouteGeometry routeGeometry,
            double currentLat,
            double currentLon
    ) {
        double furthestDistanceMeters = 0.0;
        for (int i = 0; i < routeGeometry.routeSamplePointCount(); i++) {
            LatLon point = routeGeometry.routeSamplePointAt(i);
            if (point == null) {
                continue;
            }
            double eastMeters = GeoMath.eastMeters(currentLat, currentLon, point.lat, point.lon);
            double northMeters = GeoMath.northMeters(currentLat, point.lat);
            furthestDistanceMeters = Math.max(furthestDistanceMeters, Math.hypot(eastMeters, northMeters));
        }
        return furthestDistanceMeters;
    }

    private static float resolveMovingVisibleRadiusMeters(float speedMps) {
        float safeSpeedMps = Float.isFinite(speedMps) && speedMps > 0f ? speedMps : 0f;
        float targetRadiusMeters = safeSpeedMps * COMPASS_MOVING_LOOKAHEAD_SECONDS;
        return Math.max(COMPASS_MIN_VISIBLE_RADIUS_METERS, targetRadiusMeters);
    }

    static boolean hasReliableMovingSpeed(
            @NonNull Location currentLocation,
            boolean likelyStationary
    ) {
        return !likelyStationary
                && currentLocation.hasSpeed()
                && Float.isFinite(currentLocation.getSpeed())
                && currentLocation.getSpeed() > 0f;
    }

    private static float smoothVisibleRadiusMeters(
            float targetVisibleRadiusMeters,
            @Nullable Float previousVisibleRadiusMeters,
            long compassRadiusUpdateDeltaMs
    ) {
        if (previousVisibleRadiusMeters == null
                || !Float.isFinite(previousVisibleRadiusMeters)
                || previousVisibleRadiusMeters <= 0f
                || compassRadiusUpdateDeltaMs <= 0L) {
            return targetVisibleRadiusMeters;
        }
        long boundedDeltaMs = Math.min(compassRadiusUpdateDeltaMs, 5_000L);
        double alpha = 1.0 - Math.exp(-boundedDeltaMs / (double) COMPASS_RADIUS_SMOOTHING_TIME_CONSTANT_MS);
        return (float) (previousVisibleRadiusMeters
                + (targetVisibleRadiusMeters - previousVisibleRadiusMeters) * alpha);
    }

    private static float resolveMovingLegendReferenceSpeedMps(float visibleRadiusMeters) {
        float safeRadiusMeters = Float.isFinite(visibleRadiusMeters) && visibleRadiusMeters > 0f
                ? visibleRadiusMeters
                : COMPASS_MIN_VISIBLE_RADIUS_METERS;
        return Math.max(1f, safeRadiusMeters / COMPASS_MOVING_LOOKAHEAD_SECONDS);
    }

    private static float sanitizeAccuracyMeters(float accuracyMeters) {
        return Float.isFinite(accuracyMeters) && accuracyMeters > 0f ? accuracyMeters : 0f;
    }

    private static float sanitizeReferenceSpeedMps(float speedMps) {
        return Float.isFinite(speedMps) && speedMps > 0f ? Math.max(1f, speedMps) : 1f;
    }

    @Nullable
    private static Float sanitizeHeadingAccuracyDegrees(@Nullable Float headingAccuracyDegrees) {
        return headingAccuracyDegrees != null
                && Float.isFinite(headingAccuracyDegrees)
                && headingAccuracyDegrees > 0f
                ? headingAccuracyDegrees
                : null;
    }

    private static float normalizeHeading(double headingDegrees) {
        double normalized = headingDegrees % 360.0;
        if (normalized < 0.0) {
            normalized += 360.0;
        }
        return (float) normalized;
    }

    private static final class UpcomingHint {
        @NonNull
        final VoiceHint hint;
        final double distanceMeters;
        final double timeSeconds;

        private UpcomingHint(@NonNull VoiceHint hint, double distanceMeters, double timeSeconds) {
            this.hint = hint;
            this.distanceMeters = distanceMeters;
            this.timeSeconds = timeSeconds;
        }
    }
}
