package vibro.navigator.nav.session;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import vibro.navigator.nav.location.NavigationLocation;
import vibro.navigator.nav.location.NavigationLocationProviders;

public class NavigationGpsBearingTrustPolicyTest {

    private final NavigationGpsBearingTrustPolicy policy = new NavigationGpsBearingTrustPolicy();

    @Test
    public void trustedBearingDegrees_rejectsNonFiniteSpeed() {
        NavigationLocation location = locationWithAccurateBearing();

        assertNull(policy.trustedBearingDegrees(location, Float.NaN));
        assertNull(policy.trustedBearingDegrees(location, Float.POSITIVE_INFINITY));
    }

    @Test
    public void trustedBearingDegrees_acceptsAccurateBearingAtTrustedSpeed() {
        assertNotNull(policy.trustedBearingDegrees(locationWithAccurateBearing(), 1.2f));
    }

    @Test
    public void slowAccurateBearingIsUsableForDisplayButNotReroute() {
        NavigationLocation location = locationWithAccurateBearing();
        assertNotNull(policy.trustedDisplayBearingDegrees(location, 0.4f));
        assertNull(policy.trustedBearingDegrees(location, 0.4f));
        assertNull(policy.trustedDisplayBearingDegrees(location, 0.2f));
    }

    @Test
    public void slowDisplayBearingStillRequiresGoodNumericAccuracy() {
        NavigationLocation location = locationWithAccurateBearing();
        location.setBearingAccuracyDegrees(40f);
        assertNull(policy.trustedDisplayBearingDegrees(location, 0.4f));
        NavigationLocation withoutAccuracy = new NavigationLocation(NavigationLocationProviders.GPS_PROVIDER);
        withoutAccuracy.setBearing(84f);
        assertNull(policy.trustedDisplayBearingDegrees(withoutAccuracy, 1.2f));
        assertNotNull(policy.trustedDisplayBearingDegrees(withoutAccuracy, 2.5f));
    }

    private static NavigationLocation locationWithAccurateBearing() {
        NavigationLocation location = new NavigationLocation(NavigationLocationProviders.GPS_PROVIDER);
        location.setBearing(84f);
        location.setBearingAccuracyDegrees(12f);
        return location;
    }
}
