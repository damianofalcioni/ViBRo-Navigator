package vibro.navigator.android.brouter;

import static org.junit.Assert.assertEquals;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.nio.file.Files;
import java.util.List;

import btools.routingapp.IBRouterService;
import vibro.navigator.brouter.BRouterRouteRequest;
import vibro.navigator.geo.LatLon;

@RunWith(RobolectricTestRunner.class)
public class AndroidBRouterNamedRouteOverridesTest {
    private static final String PROFILE = "trekking";
    private static final String OVERRIDES = "avoidferry=1";

    @Test
    public void sendsSavedOverrideEvenWhenViBRoSeesAnOlderProfileCopy() throws Exception {
        var directory = TestBRouterStorage.createPrimaryDirectory("Android/media/btools.routingapp/brouter/profiles2");
        Files.writeString(directory.toPath().resolve(PROFILE + ".brf"), "assign validForBikes = true");
        var context = new RoutingContext(ApplicationProvider.getApplicationContext());
        var request = new BRouterRouteRequest(new LatLon(48.0, 16.0), List.of(), new LatLon(48.2, 16.2),
                PROFILE, OVERRIDES, List.of());
        var client = new AndroidBRouterRouteClient(context);
        try {
            assertEquals(OVERRIDES, client.requestRoutePayload(request));
        } finally {
            client.close();
        }
    }

    private static final class RoutingContext extends ContextWrapper {
        private final IBRouterService.Stub routing = new IBRouterService.Stub() {
            @Override
            public String getTrackFromParams(Bundle params) {
                assertEquals(PROFILE, params.getString("profile"));
                return params.getString("extraParams");
            }
        };

        RoutingContext(Context base) {
            super(base);
        }

        @Override
        public Context getApplicationContext() {
            return this;
        }

        @Override
        public boolean bindService(Intent intent, ServiceConnection connection, int flags) {
            connection.onServiceConnected(intent.getComponent(), routing);
            return true;
        }

        @Override
        public void unbindService(ServiceConnection connection) { }
    }
}
