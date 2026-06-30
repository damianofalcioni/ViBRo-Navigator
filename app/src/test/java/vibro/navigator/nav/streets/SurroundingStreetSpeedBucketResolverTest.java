package vibro.navigator.nav.streets;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class SurroundingStreetSpeedBucketResolverTest {
    private final SurroundingStreetSpeedBucketResolver resolver = new SurroundingStreetSpeedBucketResolver();

    @Test
    public void resolve_usesNominalBucketsWithoutCurrentBucket() {
        assertEquals(SurroundingStreetSpeedBucket.LOW, resolver.resolve(mps(39.9f), null));
        assertEquals(SurroundingStreetSpeedBucket.MEDIUM, resolver.resolve(mps(40f), null));
        assertEquals(SurroundingStreetSpeedBucket.HIGH, resolver.resolve(mps(80f), null));
    }

    @Test
    public void resolve_keepsCurrentBucketAroundLowMediumBoundary() {
        assertEquals(
                SurroundingStreetSpeedBucket.LOW,
                resolver.resolve(mps(42.9f), SurroundingStreetSpeedBucket.LOW)
        );
        assertEquals(
                SurroundingStreetSpeedBucket.MEDIUM,
                resolver.resolve(mps(43f), SurroundingStreetSpeedBucket.LOW)
        );
        assertEquals(
                SurroundingStreetSpeedBucket.MEDIUM,
                resolver.resolve(mps(36f), SurroundingStreetSpeedBucket.MEDIUM)
        );
        assertEquals(
                SurroundingStreetSpeedBucket.LOW,
                resolver.resolve(mps(35.9f), SurroundingStreetSpeedBucket.MEDIUM)
        );
    }

    @Test
    public void resolve_keepsCurrentBucketAroundMediumHighBoundary() {
        assertEquals(
                SurroundingStreetSpeedBucket.MEDIUM,
                resolver.resolve(mps(83.9f), SurroundingStreetSpeedBucket.MEDIUM)
        );
        assertEquals(
                SurroundingStreetSpeedBucket.HIGH,
                resolver.resolve(mps(84f), SurroundingStreetSpeedBucket.MEDIUM)
        );
        assertEquals(
                SurroundingStreetSpeedBucket.HIGH,
                resolver.resolve(mps(72f), SurroundingStreetSpeedBucket.HIGH)
        );
        assertEquals(
                SurroundingStreetSpeedBucket.MEDIUM,
                resolver.resolve(mps(71.9f), SurroundingStreetSpeedBucket.HIGH)
        );
    }

    private static float mps(float kmh) {
        return kmh / 3.6f;
    }
}
