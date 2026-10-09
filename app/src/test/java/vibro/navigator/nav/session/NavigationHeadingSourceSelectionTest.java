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
    public void stoppingRetainsTravelSourceUntilQuarterSecondCompassTurnConfirmation() {
        for (boolean beeline : new boolean[] {false, true}) {
            assertStationaryActivation(beeline);
        }
    }

    private static void assertStationaryActivation(boolean beeline) {
        NavigationSessionHeadingResolver resolver = resolver();
        NavigationLocation fix = fix(1_000);
        NavigationHeadingSource travelSource = beeline ? NavigationHeadingSource.LOCATION : NavigationHeadingSource.ROUTE;
        assertEquals(travelSource,
                resolver.selectHeading(fix, false, 180.0, 5f, 1_000, 90.0, beeline).headingSource);
        assertEquals(travelSource,
                resolver.selectHeading(fix, true, 180.0, 5f, 2_000, 90.0, beeline).headingSource);
        resolver.selectHeading(fix, true, 220.0, 5f, 3_000, 90.0, beeline);
        assertEquals(travelSource,
                resolver.selectHeading(fix, true, 220.0, 5f, 3_249, 90.0, beeline).headingSource);
        assertEquals(NavigationHeadingSource.COMPASS,
                resolver.selectHeading(fix, true, 220.0, 5f, 3_250, 90.0, beeline).headingSource);
        assertEquals(NavigationHeadingSource.COMPASS,
                resolver.selectHeading(fix, true, 225.0, 5f, 3_300, 90.0, beeline).headingSource);
        assertEquals(travelSource,
                resolver.selectHeading(fix, false, 225.0, 5f, 3_400, 90.0, beeline).headingSource);
        assertEquals(travelSource,
                resolver.selectHeading(fix, true, 260.0, 5f, 3_500, 90.0, beeline).headingSource);
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
