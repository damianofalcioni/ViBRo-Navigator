package vibro.navigator.nav.session;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import vibro.navigator.geo.GeoMath;
import vibro.navigator.geo.LatLon;
import vibro.navigator.nav.route.GeoJsonRoute;
import vibro.navigator.nav.route.RouteStartApproach;
import vibro.navigator.nav.route.VoiceHint;

final class NavigationRouteBeelineLegs {
    @NonNull
    private static final NavigationRouteBeelineLegs EMPTY =
            new NavigationRouteBeelineLegs(Collections.emptyList());

    @NonNull
    private final List<Leg> legs;

    private NavigationRouteBeelineLegs(@NonNull List<Leg> legs) {
        this.legs = legs.isEmpty()
                ? Collections.emptyList()
                : Collections.unmodifiableList(new ArrayList<>(legs));
    }

    @NonNull
    static NavigationRouteBeelineLegs empty() {
        return EMPTY;
    }

    @NonNull
    static NavigationRouteBeelineLegs from(@NonNull GeoJsonRoute route) {
        List<Leg> legs = new ArrayList<>();
        for (VoiceHint hint : route.voiceHints) {
            if (isLegStart(route, hint)) {
                legs.add(new Leg(
                        hint.indexInTrack,
                        hint.indexInTrack + 1,
                        route.track.get(hint.indexInTrack),
                        route.track.get(hint.indexInTrack + 1)
                ));
            }
        }
        return legs.isEmpty() ? EMPTY : new NavigationRouteBeelineLegs(legs);
    }

    int size() {
        return legs.size();
    }

    @NonNull
    Leg get(int index) {
        return legs.get(index);
    }

    private static boolean isLegStart(@NonNull GeoJsonRoute route, @NonNull VoiceHint hint) {
        return hint.command == RouteStartApproach.BEELINE_COMMAND
                && hint.indexInTrack >= 0
                && hint.indexInTrack + 1 < route.track.size();
    }

    static final class Leg {
        final int startTrackIndex;
        final int targetTrackIndex;
        @NonNull
        final LatLon start;
        @NonNull
        final LatLon target;
        final double bearingDegrees;

        Leg(
                int startTrackIndex,
                int targetTrackIndex,
                @NonNull LatLon start,
                @NonNull LatLon target
        ) {
            this.startTrackIndex = startTrackIndex;
            this.targetTrackIndex = targetTrackIndex;
            this.start = new LatLon(start.lat, start.lon);
            this.target = new LatLon(target.lat, target.lon);
            bearingDegrees = GeoMath.bearingDegrees(start.lat, start.lon, target.lat, target.lon);
        }
    }
}
