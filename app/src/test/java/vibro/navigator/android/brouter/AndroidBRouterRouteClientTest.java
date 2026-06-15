package vibro.navigator.android.brouter;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import android.os.Bundle;

import androidx.annotation.NonNull;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.zip.GZIPOutputStream;

import vibro.navigator.brouter.BRouterRouteRequest;
import vibro.navigator.brouter.NogoPoint;
import vibro.navigator.geo.LatLon;

@RunWith(RobolectricTestRunner.class)
public class AndroidBRouterRouteClientTest {

    @Test
    public void buildRouteParams_encodesProfilePointsNogosAndGeoJsonMode() {
        BRouterRouteRequest request = new BRouterRouteRequest(
                new LatLon(48.0, 16.0),
                Collections.singletonList(new LatLon(48.1, 16.1)),
                new LatLon(48.2, 16.2),
                "trekking",
                Arrays.asList(
                        new NogoPoint(48.3, 16.3, 25.0),
                        new NogoPoint(48.4, 16.4, 40.0)
                )
        );

        Bundle params = AndroidBRouterRouteClient.buildRouteParams(request);

        assertArrayEquals(new double[]{48.0, 48.1, 48.2}, params.getDoubleArray("lats"), 0.0);
        assertArrayEquals(new double[]{16.0, 16.1, 16.2}, params.getDoubleArray("lons"), 0.0);
        assertArrayEquals(new double[]{48.3, 48.4}, params.getDoubleArray("nogoLats"), 0.0);
        assertArrayEquals(new double[]{16.3, 16.4}, params.getDoubleArray("nogoLons"), 0.0);
        assertArrayEquals(new double[]{25.0, 40.0}, params.getDoubleArray("nogoRadi"), 0.0);
        assertEquals("trekking", params.getString("profile"));
        assertEquals("json", params.getString("format"));
        assertEquals("json", params.getString("trackFormat"));
        assertEquals("9", params.getString("timode"));
        assertEquals("true", params.getString("acceptCompressedResult"));
        assertFalse(params.containsKey("extraParams"));
        assertFalse(params.containsKey("v"));
    }

    @Test
    public void buildRouteParams_encodesProfileParameterOverridesAsExtraParams() {
        BRouterRouteRequest request = new BRouterRouteRequest(
                new LatLon(48.0, 16.0),
                Collections.emptyList(),
                new LatLon(48.2, 16.2),
                "trekking",
                "avoid_path=1&uphillcost=90",
                Collections.emptyList()
        );

        Bundle params = AndroidBRouterRouteClient.buildRouteParams(request);

        assertEquals("avoid_path=1&uphillcost=90", params.getString("extraParams"));
    }

    @Test
    public void decodeRoutePayload_returnsRawPayloadWhenItIsNotCompressed() throws Exception {
        String payload = "{\"type\":\"FeatureCollection\",\"features\":[]}";

        assertEquals(payload, AndroidBRouterRouteClient.decodeRoutePayload(payload));
    }

    @Test
    public void decodeRoutePayload_decodesBRouterZ64Payload() throws Exception {
        String payload = "{\"type\":\"FeatureCollection\",\"features\":[{\"type\":\"Feature\"}]}";

        assertEquals(payload, AndroidBRouterRouteClient.decodeRoutePayload(z64(payload)));
    }

    @NonNull
    private static String z64(@NonNull String payload) throws IOException {
        ByteArrayOutputStream encoded = new ByteArrayOutputStream();
        encoded.write('z');
        encoded.write('6');
        encoded.write('4');
        try (GZIPOutputStream gzip = new GZIPOutputStream(encoded)) {
            gzip.write(payload.getBytes(StandardCharsets.UTF_8));
        }
        return java.util.Base64.getEncoder().encodeToString(encoded.toByteArray());
    }
}
