package vibro.navigator.brouter;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class BRouterRouteExceptionTest {
    @Test
    public void recognizesMissingTileOrSegmentDirectoryFromBRouter() {
        assertEquals(BRouterRouteException.Reason.MISSING_ROUTING_DATA,
                BRouterRouteException.fromTextResponse("datafile E15_N45.rd5 not found").reason);
        assertEquals(BRouterRouteException.Reason.MISSING_ROUTING_DATA,
                BRouterRouteException.fromTextResponse("segment directory /brouter/segments4 does not exist").reason);
        assertEquals(BRouterRouteException.Reason.UNKNOWN,
                BRouterRouteException.fromTextResponse("to-position not mapped in existing datafile").reason);
    }

    @Test
    public void fromTextResponse_classifiesBRouterDoesNotExistsProfileMessageAsInvalidProfile() {
        BRouterRouteException error =
                BRouterRouteException.fromTextResponse("Profile custom-car does not exists");

        assertEquals(BRouterRouteException.Reason.INVALID_PROFILE, error.reason);
    }
}
