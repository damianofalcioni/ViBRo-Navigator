package vibro.navigator.nav.kalman;

import org.junit.Test;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

public class Kalman1DTest {

    @Test
    public void positionOnlyCorrectionDoesNotCreateVelocity() {
        Kalman1D filter = new Kalman1D(1);
        filter.reset(20, 0, 625, 0);
        filter.predictPosition(0, 0.01);
        filter.update(0, 25);
        assertEquals(0, filter.velocity(), 0);
        assertTrue(filter.position() > 0);
        assertTrue(filter.position() < 1);
        assertTrue(filter.positionVariance() >= 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void invalidMeasurementIsRejectedBeforeStateMutation() {
        new Kalman1D(1).update(0, Double.NaN);
    }

    @Test
    public void filterTracksConstantVelocityRoughly() {
        Kalman1D k = new Kalman1D(1.0);
        k.reset(0, 1.0);

        for (int i = 0; i < 10; i++) {
            k.predict(1.0);
            k.update(i + 1, 4.0);
        }

        assertTrue(k.position() > 8.0);
        assertTrue(k.velocity() > 0.5);
    }
}

