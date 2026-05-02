package vibro.navigator.nav.routing;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import vibro.navigator.brouter.BRouterRouteException;

public class BRouterTransientFailureClassifierTest {

    @Test
    public void isTransientRecognizesServiceUnavailableReason() {
        assertTrue(BRouterTransientFailureClassifier.isTransient(
                BRouterRouteException.serviceUnavailable("BRouter service not available")
        ));
    }

    @Test
    public void isTransientChecksNestedMessages() {
        assertTrue(BRouterTransientFailureClassifier.isTransient(
                new IllegalStateException(
                        "Route failed",
                        new IllegalArgumentException("BRouter binding died")
                )
        ));
    }

    @Test
    public void isTransientRejectsPermanentRouteFailure() {
        assertFalse(BRouterTransientFailureClassifier.isTransient(
                BRouterRouteException.fromTextResponse("no track found at pass=0")
        ));
    }

}
