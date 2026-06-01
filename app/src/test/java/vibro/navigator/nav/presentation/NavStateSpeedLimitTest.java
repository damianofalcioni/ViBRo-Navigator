package vibro.navigator.nav.presentation;

import android.content.Context;
import vibro.navigator.nav.location.NavigationLocation;

import androidx.annotation.NonNull;
import androidx.test.core.app.ApplicationProvider;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.Arrays;
import java.util.Collections;

import vibro.navigator.geo.LatLon;
import vibro.navigator.nav.model.NavState;
import vibro.navigator.nav.model.NavTarget;
import vibro.navigator.nav.route.GeoJsonRoute;
import vibro.navigator.nav.route.PolylineIndex;
import vibro.navigator.nav.route.RouteSpeedLimit;
import vibro.navigator.nav.route.RouteSpeedLimitSegment;

import static org.junit.Assert.assertEquals;

@RunWith(RobolectricTestRunner.class)
public class NavStateSpeedLimitTest {
    private static final String DESTINATION = "Destination";

    private final Context context = ApplicationProvider.getApplicationContext();

    @Test
    public void from_exposesCurrentRouteSpeedLimit() {
        GeoJsonRoute route = new GeoJsonRoute(
                Arrays.asList(
                        new LatLon(0.0, 0.0),
                        new LatLon(0.0, 0.001),
                        new LatLon(0.0, 0.002)
                ),
                Collections.emptyList(),
                Collections.emptyList(),
                Arrays.asList(
                        new RouteSpeedLimitSegment(
                                0.0,
                                111.0,
                                new RouteSpeedLimit(30, RouteSpeedLimit.Unit.KILOMETERS_PER_HOUR)
                        ),
                        new RouteSpeedLimitSegment(
                                111.0,
                                222.0,
                                new RouteSpeedLimit(50, RouteSpeedLimit.Unit.KILOMETERS_PER_HOUR)
                        )
                ),
                60.0,
                222.0
        );

        NavState state = NavStateComposer.from(NavStateBuildInput
                .builder(context, route, new PolylineIndex(route.track), locationAt(0.0, 0.0013))
                .routeProgress(140.0, -1, 1)
                .motion(2f, 2f, false, 5f, 5f)
                .targets(Collections.singletonList(new NavTarget(DESTINATION, 222.0)))
                .build());

        assertEquals(50, state.routeStatus.speedLimit.value);
    }

    @NonNull
    private static NavigationLocation locationAt(double lat, double lon) {
        NavigationLocation location = new NavigationLocation("test");
        location.setLatitude(lat);
        location.setLongitude(lon);
        location.setTime(1L);
        return location;
    }
}
