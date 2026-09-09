package vibro.navigator.nav.guidance;

import org.junit.Test;
import vibro.navigator.nav.location.NavigationLocation;
import static org.junit.Assert.*;

public class OffTrackEvidenceTest {
    @Test
    public void fixes23To25DoNotConfirmAmbiguousDepartureBeforeCornerRecovery() {
        OffTrackEvidence evidence = new OffTrackEvidence();
        assertFalse(evidence.supportsDeparture(fix(40.308503, 17.668176), 6, 1_000));
        assertFalse(evidence.supportsDeparture(fix(40.308500, 17.668177), 6, 2_000));
        assertFalse(evidence.supportsDeparture(fix(40.308501, 17.668305), 6, 6_000));
        evidence.clear();
        assertFalse(evidence.supportsDeparture(fix(40.308558, 17.668604), 6, 10_000));
    }

    @Test
    public void clearMovingDepartureRetainsFastConfirmation() {
        OffTrackEvidence evidence = new OffTrackEvidence();
        assertFalse(evidence.supportsDeparture(fix(0, 0), 50, 1_000));
        assertTrue(evidence.supportsDeparture(fix(0, 0.001), 100, 2_000));
    }

    @Test
    public void sustainedOffsetCanConfirmButCannotReusePreGapEvidence() {
        OffTrackEvidence evidence = new OffTrackEvidence();
        assertFalse(evidence.supportsDeparture(fix(0, 0), 5, 1_000));
        assertFalse(evidence.supportsDeparture(fix(0, 0), 5, 4_000));
        assertTrue(evidence.supportsDeparture(fix(0, 0), 5, 7_000));
        assertFalse(evidence.supportsDeparture(fix(0, 0), 5, 30_000));
    }

    private static NavigationLocation fix(double lat, double lon) {
        NavigationLocation location = new NavigationLocation("gps");
        location.setLatitude(lat);
        location.setLongitude(lon);
        location.setAccuracy(15);
        return location;
    }
}
