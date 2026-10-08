package vibro.navigator.nav.session;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

import vibro.navigator.nav.location.NavigationLocation;
import vibro.navigator.nav.orientation.NavigationHeadingSource;

public class NavigationHeadingSourceSelectionTest {
    @Test
    public void startupAndBeelineFallbackExposeTheActualSourceAndHeldCourseKeepsItsSource() {
        NavigationSessionHeadingResolver resolver = resolver();
        assertEquals(NavigationHeadingSource.COMPASS,
                resolver.selectHeading(null, true, 180.0, 5f, 1_000, null, true).headingSource);
        NavigationLocation fix = fix(1_000);
        assertEquals(NavigationHeadingSource.LOCATION,
                resolver.selectHeading(fix, false, 180.0, 5f, 1_000, null, true).headingSource);
        assertEquals(NavigationHeadingSource.COMPASS,
                resolver.selectHeading(fix, false, 180.0, 5f, 12_000, null, true).headingSource);
        NavigationSessionHeadingResolver.Selection held =
                resolver.selectHeading(fix, false, null, null, 12_100, null, true);
        assertEquals(NavigationHeadingSource.LOCATION, held.headingSource);
        assertEquals(90f, held.headingAccuracyDegrees, 0f);
        NavigationLocation inaccurate = fix(13_000);
        inaccurate.setBearingAccuracyDegrees(40f);
        assertEquals(NavigationHeadingSource.COMPASS,
                resolver.selectHeading(inaccurate, false, 180.0, 5f, 13_000, null, true).headingSource);
    }

    @Test
    public void stoppingRetainsRouteSourceUntilCompassTurnGateActivates() {
        NavigationSessionHeadingResolver resolver = resolver();
        NavigationLocation fix = fix(1_000);
        assertEquals(NavigationHeadingSource.ROUTE,
                resolver.selectHeading(fix, false, 180.0, 5f, 1_000, 90.0, false).headingSource);
        assertEquals(NavigationHeadingSource.ROUTE,
                resolver.selectHeading(fix, true, 180.0, 5f, 2_000, 90.0, false).headingSource);
        resolver.selectHeading(fix, true, 220.0, 5f, 3_000, 90.0, false);
        assertEquals(NavigationHeadingSource.COMPASS,
                resolver.selectHeading(fix, true, 220.0, 5f, 4_000, 90.0, false).headingSource);
        assertEquals(NavigationHeadingSource.ROUTE,
                resolver.selectHeading(fix, false, 220.0, 5f, 4_100, 90.0, false).headingSource);
    }

    private static NavigationSessionHeadingResolver resolver() {
        return new NavigationSessionHeadingResolver(new NavigationSessionLocationState());
    }

    private static NavigationLocation fix(long timeMs) {
        NavigationLocation fix = new NavigationLocation("gps");
        fix.setTime(timeMs, timeMs);
        fix.setAccuracy(5f);
        fix.setSpeed(3f);
        fix.setBearing(84f);
        fix.setBearingAccuracyDegrees(12f);
        return fix;
    }
}
