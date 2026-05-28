package vibro.navigator.nav.session;


import vibro.navigator.nav.routing.NavigationRouteRequestSnapshot;
import vibro.navigator.nav.guidance.NavigationTurnEvent;
import vibro.navigator.nav.guidance.RouteDeviationPolicy;
import vibro.navigator.nav.model.NavigationRequest;
import vibro.navigator.nav.model.NavState;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.location.Location;

import androidx.annotation.NonNull;
import androidx.test.core.app.ApplicationProvider;

import vibro.navigator.R;
import vibro.navigator.brouter.BRouterRouteException;
import vibro.navigator.brouter.NogoPoint;
import vibro.navigator.geo.LatLon;
import vibro.navigator.nav.route.GeoJsonRoute;
import vibro.navigator.nav.route.VoiceHint;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

abstract class NavigationSessionRouteStateTestSupport {
    static final String DESTINATION = "Destination";
    static final String TREKKING_PROFILE = "trekking";


    @NonNull
    static NavigationRouteRequestSnapshot snapshot(@NonNull NavigationRequest request) {
        return new NavigationRouteRequestSnapshot(
                1,
                1,
                new LatLon(0.0, 0.0),
                request.stops,
                request.destination,
                request.profile,
                Collections.emptyList()
        );
    }

    @NonNull
    static GeoJsonRoute routeWithHint() {
        return new GeoJsonRoute(
                Arrays.asList(new LatLon(0.0, 0.0), new LatLon(0.0, 0.001)),
                Collections.singletonList(new VoiceHint(1, 2, 0, 0.0, 0)),
                60.0,
                111.0
        );
    }

    @NonNull
    static GeoJsonRoute routeWithoutHints() {
        return new GeoJsonRoute(
                Arrays.asList(
                        new LatLon(0.0, 0.0),
                        new LatLon(0.0, 0.001),
                        new LatLon(0.0, 0.002),
                        new LatLon(0.0, 0.003)
                ),
                Collections.emptyList(),
                180.0,
                333.0
        );
    }

    @NonNull
    static GeoJsonRoute routeWithSharpTurn() {
        return new GeoJsonRoute(
                Arrays.asList(
                        new LatLon(0.0, 0.0),
                        new LatLon(0.0, 0.00018),
                        new LatLon(0.00018, 0.00018)
                ),
                Collections.emptyList(),
                40.0,
                40.0
        );
    }

    @NonNull
    static Location location(double lat, double lon, long timeMs) {
        return location(lat, lon, timeMs, 5f);
    }

    @NonNull
    static Location locationWithSpeed(double lat, double lon, long timeMs, float speedMetersPerSecond) {
        Location location = location(lat, lon, timeMs, 5f);
        location.setSpeed(speedMetersPerSecond);
        return location;
    }

    @NonNull
    static Location location(double lat, double lon, long timeMs, float accuracyMeters) {
        Location location = new Location("gps");
        location.setLatitude(lat);
        location.setLongitude(lon);
        location.setTime(timeMs);
        location.setAccuracy(accuracyMeters);
        return location;
    }

    static float normalizedTransitionProgress(float start, float current, float end) {
        if (Math.abs(end - start) < 0.01f) {
            return 1f;
        }
        return (current - start) / (end - start);
    }
}
