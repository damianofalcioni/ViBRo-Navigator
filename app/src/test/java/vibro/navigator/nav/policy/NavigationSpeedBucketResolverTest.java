package vibro.navigator.nav.policy;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class NavigationSpeedBucketResolverTest {
    private final NavigationSpeedBucketResolver resolver = new NavigationSpeedBucketResolver();

    @Test
    public void resolve_usesNominalBucketsWithoutCurrentBucket() {
        assertEquals(NavigationSpeedBucket.LOW, resolver.resolve(mps(39.9f), null));
        assertEquals(NavigationSpeedBucket.MEDIUM, resolver.resolve(mps(40f), null));
        assertEquals(NavigationSpeedBucket.HIGH, resolver.resolve(mps(80f), null));
    }

    @Test
    public void resolve_keepsCurrentBucketAroundLowMediumBoundary() {
        assertEquals(
                NavigationSpeedBucket.LOW,
                resolver.resolve(mps(42.9f), NavigationSpeedBucket.LOW)
        );
        assertEquals(
                NavigationSpeedBucket.MEDIUM,
                resolver.resolve(mps(43f), NavigationSpeedBucket.LOW)
        );
        assertEquals(
                NavigationSpeedBucket.MEDIUM,
                resolver.resolve(mps(36f), NavigationSpeedBucket.MEDIUM)
        );
        assertEquals(
                NavigationSpeedBucket.LOW,
                resolver.resolve(mps(35.9f), NavigationSpeedBucket.MEDIUM)
        );
    }

    @Test
    public void resolve_keepsCurrentBucketAroundMediumHighBoundary() {
        assertEquals(
                NavigationSpeedBucket.MEDIUM,
                resolver.resolve(mps(83.9f), NavigationSpeedBucket.MEDIUM)
        );
        assertEquals(
                NavigationSpeedBucket.HIGH,
                resolver.resolve(mps(84f), NavigationSpeedBucket.MEDIUM)
        );
        assertEquals(
                NavigationSpeedBucket.HIGH,
                resolver.resolve(mps(72f), NavigationSpeedBucket.HIGH)
        );
        assertEquals(
                NavigationSpeedBucket.MEDIUM,
                resolver.resolve(mps(71.9f), NavigationSpeedBucket.HIGH)
        );
    }

    private static float mps(float kmh) {
        return kmh / 3.6f;
    }
}
