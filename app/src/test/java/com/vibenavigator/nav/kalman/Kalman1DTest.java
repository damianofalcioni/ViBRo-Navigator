package com.vibenavigator.nav.kalman;

import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class Kalman1DTest {

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

