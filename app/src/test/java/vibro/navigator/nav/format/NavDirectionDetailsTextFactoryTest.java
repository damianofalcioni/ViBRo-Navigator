package vibro.navigator.nav.format;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import androidx.annotation.NonNull;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import vibro.navigator.R;
import vibro.navigator.geo.LatLon;
import vibro.navigator.nav.model.NavTarget;
import vibro.navigator.nav.route.GeoJsonRoute;
import vibro.navigator.nav.route.PolylineIndex;
import vibro.navigator.nav.route.VoiceHint;

public class NavDirectionDetailsTextFactoryTest {
    private static final String DESTINATION = "Destination";
    private static final String DISTANCE_111_METERS = "111 m";

    private final NavigationTextResources context = TestNavigationTextResources.metric();

    @Test
    public void buildRelativeLinesFormatsAllRowsRelativeToPreviousDirection() {
        GeoJsonRoute route = new GeoJsonRoute(
                Arrays.asList(
                        new LatLon(0.0, 0.0),
                        new LatLon(0.0, 0.001),
                        new LatLon(0.0, 0.002),
                        new LatLon(0.0, 0.003)
                ),
                Arrays.asList(
                        new VoiceHint(1, 2, 0, 0.0, 0),
                        new VoiceHint(2, 3, 0, 0.0, 0)
                ),
                Arrays.asList(0.0, 20.0, 45.0, 75.0),
                75.0,
                333.0
        );

        List<String> lines = NavDirectionDetailsTextFactory.buildRelativeLines(
                route,
                new PolylineIndex(route.track),
                0.0,
                0,
                0,
                0f,
                5f,
                false,
                -1,
                Collections.singletonList(new NavTarget(DESTINATION, 333.0)),
                context
        );

        assertEquals(3, lines.size());
        assertLineContains(lines.get(0), "20 s");
        assertLineContains(lines.get(1), "25 s");
        assertLineContains(lines.get(2), "30 s");
        assertTrue(lines.get(2).contains(context.getString(R.string.direction_arrive)));
    }

    private static void assertLineContains(@NonNull String line, @NonNull String timeText) {
        assertTrue(line.contains(DISTANCE_111_METERS));
        assertTrue(line.contains(timeText));
    }
}
