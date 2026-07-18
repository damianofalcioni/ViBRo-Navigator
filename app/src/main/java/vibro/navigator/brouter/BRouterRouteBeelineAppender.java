package vibro.navigator.brouter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import vibro.navigator.geo.GeoMath;
import vibro.navigator.geo.LatLon;
import vibro.navigator.nav.route.GeoJsonRoute;
import vibro.navigator.nav.route.PolylineIndex;
import vibro.navigator.nav.route.RouteSpeedLimitSegment;
import vibro.navigator.nav.route.RouteStartApproach;
import vibro.navigator.nav.route.VoiceHint;

final class BRouterRouteBeelineAppender {
    private static final int FINAL_ARRIVAL_COMMAND = 100;
    private static final double DUPLICATE_POINT_TOLERANCE_DEGREES = 0.0000001;
    private static final double SEGMENT_ENDPOINT_TOLERANCE = 0.000001;
    private static final double BEELINE_SPEED_METERS_PER_SECOND = 1.4;

    private BRouterRouteBeelineAppender() {
    }

    @NonNull
    static GeoJsonRoute appendDestinationBeelines(
            @NonNull GeoJsonRoute route,
            @NonNull List<LatLon> intermediates,
            @NonNull LatLon destination
    ) {
        RouteEdits edits = new RouteEdits(route);
        int firstCandidateSegmentIndex = 0;
        for (LatLon intermediate : intermediates) {
            firstCandidateSegmentIndex = edits.appendIntermediateBeeline(
                    intermediate,
                    firstCandidateSegmentIndex
            );
        }
        edits.appendFinalDestinationBeeline(destination);
        return edits.toRoute(route);
    }

    private static boolean samePoint(@NonNull LatLon first, @NonNull LatLon second) {
        return Math.abs(first.lat - second.lat) <= DUPLICATE_POINT_TOLERANCE_DEGREES
                && Math.abs(first.lon - second.lon) <= DUPLICATE_POINT_TOLERANCE_DEGREES;
    }

    @NonNull
    private static LatLon copy(@NonNull LatLon point) {
        return new LatLon(point.lat, point.lon);
    }

    private static double distanceMeters(@NonNull LatLon first, @NonNull LatLon second) {
        return GeoMath.distanceMeters(first.lat, first.lon, second.lat, second.lon);
    }

    private static final class RouteEdits {
        @NonNull
        private final List<LatLon> track;
        @NonNull
        private final List<VoiceHint> voiceHints;
        private boolean changed;
        private boolean insertedIntermediateBeeline;
        private double addedDistanceMeters;

        RouteEdits(@NonNull GeoJsonRoute route) {
            track = new ArrayList<>(route.track);
            voiceHints = copyVoiceHints(route);
        }

        int appendIntermediateBeeline(@NonNull LatLon destination, int firstCandidateSegmentIndex) {
            SegmentMatch match = bestMatch(destination, firstCandidateSegmentIndex);
            if (match == null) {
                return firstCandidateSegmentIndex;
            }
            if (samePoint(match.routePoint, destination)) {
                return nextCandidateSegmentIndex(match);
            }
            return insertIntermediateBeeline(match, destination);
        }

        void appendFinalDestinationBeeline(@NonNull LatLon destination) {
            if (track.isEmpty()) {
                return;
            }
            int routeEndIndex = track.size() - 1;
            LatLon routeEnd = track.get(routeEndIndex);
            if (samePoint(routeEnd, destination)) {
                return;
            }
            double distanceMeters = distanceMeters(routeEnd, destination);
            addBeelineHint(routeEndIndex, distanceMeters);
            track.add(copy(destination));
            moveFinalArrivalHints(routeEndIndex, routeEndIndex + 1);
            addedDistanceMeters += distanceMeters;
            changed = true;
        }

        @NonNull
        GeoJsonRoute toRoute(@NonNull GeoJsonRoute source) {
            if (!changed) {
                return source;
            }
            sortVoiceHints();
            return new GeoJsonRoute(
                    track,
                    voiceHints,
                    Collections.emptyList(),
                    RouteMetadata.adjustedSpeedLimitSegments(source, insertedIntermediateBeeline),
                    RouteMetadata.adjustedTotalTimeSeconds(source.totalTimeSeconds, addedDistanceMeters),
                    RouteMetadata.adjustedTrackLengthMeters(source, track, addedDistanceMeters)
            );
        }

        private int insertIntermediateBeeline(
                @NonNull SegmentMatch match,
                @NonNull LatLon destination
        ) {
            int routePointIndex = ensureRoutePoint(match);
            LatLon routePoint = track.get(routePointIndex);
            double distanceMeters = distanceMeters(routePoint, destination);
            insertPoint(routePointIndex + 1, destination);
            insertPoint(routePointIndex + 2, routePoint);
            addBeelineHint(routePointIndex, distanceMeters);
            addBeelineHint(routePointIndex + 1, distanceMeters);
            addedDistanceMeters += 2.0 * distanceMeters;
            insertedIntermediateBeeline = true;
            changed = true;
            return clampCandidateSegmentIndex(routePointIndex + 2);
        }

        private int ensureRoutePoint(@NonNull SegmentMatch match) {
            if (match.isAtSegmentStart()) {
                return match.segmentIndex;
            }
            if (match.isAtSegmentEnd()) {
                return match.segmentIndex + 1;
            }
            int projectionIndex = match.segmentIndex + 1;
            insertPoint(projectionIndex, match.routePoint);
            changed = true;
            return projectionIndex;
        }

        private void insertPoint(int index, @NonNull LatLon point) {
            track.add(index, copy(point));
            shiftHintsFrom(index);
        }

        private void shiftHintsFrom(int index) {
            for (int i = 0; i < voiceHints.size(); i++) {
                VoiceHint hint = voiceHints.get(i);
                if (hint.indexInTrack >= index) {
                    voiceHints.set(i, new VoiceHint(
                            hint.indexInTrack + 1,
                            hint.command,
                            hint.exitNumber,
                            hint.distanceToNextMeters,
                            hint.angleDegrees
                    ));
                }
            }
        }

        private void addBeelineHint(int indexInTrack, double distanceMeters) {
            voiceHints.add(new VoiceHint(
                    indexInTrack,
                    RouteStartApproach.BEELINE_COMMAND,
                    0,
                    distanceMeters,
                    0
            ));
        }

        private void moveFinalArrivalHints(int oldRouteEndIndex, int newRouteEndIndex) {
            for (int i = 0; i < voiceHints.size(); i++) {
                VoiceHint hint = voiceHints.get(i);
                if (hint.command == FINAL_ARRIVAL_COMMAND && hint.indexInTrack == oldRouteEndIndex) {
                    voiceHints.set(i, new VoiceHint(
                            newRouteEndIndex,
                            hint.command,
                            hint.exitNumber,
                            hint.distanceToNextMeters,
                            hint.angleDegrees
                    ));
                }
            }
        }

        @Nullable
        private SegmentMatch bestMatch(@NonNull LatLon point, int firstCandidateSegmentIndex) {
            if (track.size() < 2) {
                return null;
            }
            SegmentMatch best = null;
            int first = Math.max(0, firstCandidateSegmentIndex);
            int last = track.size() - 2;
            for (int i = first; i <= last; i++) {
                SegmentMatch candidate = projectToSegment(point, i);
                if (candidate != null && candidate.isBetterThan(best)) {
                    best = candidate;
                }
            }
            return best;
        }

        @Nullable
        private SegmentMatch projectToSegment(@NonNull LatLon point, int segmentIndex) {
            return SegmentProjector.projectToSegment(
                    point,
                    track.get(segmentIndex),
                    track.get(segmentIndex + 1),
                    segmentIndex
            );
        }

        private int nextCandidateSegmentIndex(@NonNull SegmentMatch match) {
            return clampCandidateSegmentIndex(match.segmentIndex + 1);
        }

        private int clampCandidateSegmentIndex(int index) {
            return Math.max(0, Math.min(index, Math.max(0, track.size() - 2)));
        }

        private void sortVoiceHints() {
            Collections.sort(voiceHints, new Comparator<VoiceHint>() {
                @Override
                public int compare(VoiceHint first, VoiceHint second) {
                    int indexComparison = Integer.compare(first.indexInTrack, second.indexInTrack);
                    return indexComparison != 0
                            ? indexComparison
                            : Integer.compare(hintSortPriority(first), hintSortPriority(second));
                }
            });
        }

        private static int hintSortPriority(@NonNull VoiceHint hint) {
            return hint.command == RouteStartApproach.BEELINE_COMMAND ? 0 : 1;
        }

    }

    @NonNull
    private static List<VoiceHint> copyVoiceHints(@NonNull GeoJsonRoute route) {
        if (route.voiceHints.isEmpty()) {
            return new ArrayList<>();
        }
        List<VoiceHint> copy = new ArrayList<>(route.voiceHints.size());
        for (VoiceHint hint : route.voiceHints) {
            copy.add(new VoiceHint(
                    hint.indexInTrack,
                    hint.command,
                    hint.exitNumber,
                    hint.distanceToNextMeters,
                    hint.angleDegrees
            ));
        }
        return copy;
    }

    private static final class RouteMetadata {
        private RouteMetadata() {
        }

        static double adjustedTrackLengthMeters(
                @NonNull GeoJsonRoute source,
                @NonNull List<LatLon> track,
                double addedDistanceMeters
        ) {
            if (Double.isFinite(source.trackLengthMeters) && source.trackLengthMeters > 0.0) {
                return source.trackLengthMeters + addedDistanceMeters;
            }
            return new PolylineIndex(track).totalLengthMeters();
        }

        @NonNull
        static List<RouteSpeedLimitSegment> adjustedSpeedLimitSegments(
                @NonNull GeoJsonRoute source,
                boolean insertedIntermediateBeeline
        ) {
            return insertedIntermediateBeeline
                    ? Collections.<RouteSpeedLimitSegment>emptyList()
                    : source.speedLimitSegments;
        }

        static double adjustedTotalTimeSeconds(double sourceTotalTimeSeconds, double addedDistanceMeters) {
            if (!Double.isFinite(sourceTotalTimeSeconds) || sourceTotalTimeSeconds <= 0.0) {
                return 0.0;
            }
            return sourceTotalTimeSeconds + addedDistanceMeters / BEELINE_SPEED_METERS_PER_SECOND;
        }
    }

    private static final class SegmentProjector {
        private SegmentProjector() {
        }

        @Nullable
        static SegmentMatch projectToSegment(
                @NonNull LatLon point,
                @NonNull LatLon start,
                @NonNull LatLon end,
                int segmentIndex
        ) {
            SegmentProjection projection = SegmentProjection.from(point, start, end);
            if (projection.segmentLengthSquared <= 0.0) {
                return null;
            }
            double t = clampProjection(projection.t());
            return new SegmentMatch(
                    segmentIndex,
                    t,
                    projection.distanceToPointMeters(t),
                    projectedPoint(start, end, t)
            );
        }

        @NonNull
        private static LatLon projectedPoint(@NonNull LatLon start, @NonNull LatLon end, double t) {
            return new LatLon(
                    start.lat + (end.lat - start.lat) * t,
                    start.lon + (end.lon - start.lon) * t
            );
        }

        private static double clampProjection(double t) {
            return Math.max(0.0, Math.min(1.0, t));
        }
    }

    private static final class SegmentProjection {
        final double startX;
        final double startY;
        final double segmentX;
        final double segmentY;
        final double segmentLengthSquared;

        SegmentProjection(double startX, double startY, double segmentX, double segmentY) {
            this.startX = startX;
            this.startY = startY;
            this.segmentX = segmentX;
            this.segmentY = segmentY;
            segmentLengthSquared = segmentX * segmentX + segmentY * segmentY;
        }

        @NonNull
        static SegmentProjection from(
                @NonNull LatLon point,
                @NonNull LatLon start,
                @NonNull LatLon end
        ) {
            double refLatRad = Math.toRadians(point.lat);
            double kx = 111320.0 * Math.cos(refLatRad);
            double ky = 111320.0;
            double startX = (start.lon - point.lon) * kx;
            double startY = (start.lat - point.lat) * ky;
            double endX = (end.lon - point.lon) * kx;
            double endY = (end.lat - point.lat) * ky;
            return new SegmentProjection(startX, startY, endX - startX, endY - startY);
        }

        double t() {
            return (-startX * segmentX - startY * segmentY) / segmentLengthSquared;
        }

        double distanceToPointMeters(double t) {
            double projectionX = startX + t * segmentX;
            double projectionY = startY + t * segmentY;
            return Math.sqrt(projectionX * projectionX + projectionY * projectionY);
        }
    }

    private static final class SegmentMatch {
        final int segmentIndex;
        final double t;
        final double distanceToTrackMeters;
        @NonNull
        final LatLon routePoint;

        SegmentMatch(
                int segmentIndex,
                double t,
                double distanceToTrackMeters,
                @NonNull LatLon routePoint
        ) {
            this.segmentIndex = segmentIndex;
            this.t = t;
            this.distanceToTrackMeters = distanceToTrackMeters;
            this.routePoint = routePoint;
        }

        boolean isBetterThan(@Nullable SegmentMatch other) {
            return other == null || distanceToTrackMeters < other.distanceToTrackMeters;
        }

        boolean isAtSegmentStart() {
            return t <= SEGMENT_ENDPOINT_TOLERANCE;
        }

        boolean isAtSegmentEnd() {
            return t >= 1.0 - SEGMENT_ENDPOINT_TOLERANCE;
        }
    }
}
