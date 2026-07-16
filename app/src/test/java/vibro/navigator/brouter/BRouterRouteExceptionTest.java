package vibro.navigator.brouter;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class BRouterRouteExceptionTest {
    @Test
    public void fromTextResponse_classifiesBRouterDoesNotExistsProfileMessageAsInvalidProfile() {
        BRouterRouteException error =
                BRouterRouteException.fromTextResponse("Profile custom-car does not exists");

        assertEquals(BRouterRouteException.Reason.INVALID_PROFILE, error.reason);
    }
}
