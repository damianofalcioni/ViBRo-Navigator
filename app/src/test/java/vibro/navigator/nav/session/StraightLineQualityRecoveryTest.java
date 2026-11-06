package vibro.navigator.nav.session;

import java.util.ArrayList;
import org.junit.Test;
import vibro.navigator.geo.LatLon;
import vibro.navigator.nav.location.NavigationLocation;
import static org.junit.Assert.*;

public class StraightLineQualityRecoveryTest {
    private final StraightLineBeelineGuidance guidance = new StraightLineBeelineGuidance();

    @Test
    public void movingQualityVerificationSurvivesOneGoodFixAndResetRestoresNormalTargetPolling() {
        assertEquals(10_000, evaluate(1_000, 5));
        assertEquals(3_000, evaluate(4_000, 130));
        assertEquals(3_000, evaluate(7_000, 5));
        assertEquals(3_000, evaluate(10_000, 5));
        assertEquals(5_000, evaluate(13_000, 5));
        guidance.reset();
        assertEquals(10_000, evaluate(16_000, 5));
    }

    private long evaluate(long time, float accuracy) {
        NavigationLocation fix = new NavigationLocation("gps");
        fix.setTime(time, time);
        fix.setAccuracy(accuracy);
        return guidance.evaluate(new ArrayList<>(), new LatLon(300 / 111_320.0, 0), fix,
                1.5f, false, accuracy, null, time, 0).getSuggestedUpdateIntervalMs();
    }
}
