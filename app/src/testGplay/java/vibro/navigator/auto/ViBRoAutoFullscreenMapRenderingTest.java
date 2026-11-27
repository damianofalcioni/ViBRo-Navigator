package vibro.navigator.auto;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.RectF;

import androidx.car.app.CarContext;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LifecycleRegistry;
import androidx.test.core.app.ApplicationProvider;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.GraphicsMode;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import vibro.navigator.R;
import vibro.navigator.geo.LatLon;
import vibro.navigator.nav.compass.CompassRouteGeometry;
import vibro.navigator.nav.compass.CompassStreetOverlay;
import vibro.navigator.nav.compass.CompassStreetSegment;
import vibro.navigator.nav.compass.NavCompassState;
import vibro.navigator.nav.model.NavGpsStatus;
import vibro.navigator.nav.model.NavGuidanceStatus;
import vibro.navigator.nav.model.NavPauseStatus;
import vibro.navigator.nav.model.NavProgressStatus;
import vibro.navigator.nav.model.NavRouteStatus;
import vibro.navigator.nav.model.NavState;

@RunWith(RobolectricTestRunner.class)
public class ViBRoAutoFullscreenMapRenderingTest {
    @Test
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    public void fullscreenDrawsRouteAndStreetsBeyondTheSafeArea() throws Exception {
        CarContext carContext = testCarContext();
        carContext.setTheme(R.style.Theme_ViBRoNavigator);
        ViBRoAutoCompassPainter painter = new ViBRoAutoCompassPainter(
                carContext, viewport -> {}, () -> 1_000L);
        CompassRouteGeometry geometry = new CompassRouteGeometry(List.of(
                new CompassRouteGeometry.SamplePoint(new LatLon(0d, 0d), 0d),
                new CompassRouteGeometry.SamplePoint(new LatLon(0.0004d, -0.001d), 120d)
        ), List.of());
        NavCompassState route = NavCompassState.fromRouteGeometry(
                0f, null, 1f, 1f, 1f, 100f, 1000f, 100f, 5f, true, 0f,
                geometry, 0d, 0d, 0, -111f, 44f, 5f, true);
        CompassStreetSegment street = new CompassStreetSegment(List.of(
                new LatLon(0.0006d, -0.0007d), new LatLon(0.0001d, -0.0007d)));
        NavCompassState routeWithStreet = route.withStreetOverlay(new CompassStreetOverlay(List.of(street)));
        RectF safeArea = new RectF(100, 0, 220, 240);
        Bitmap bitmap = Bitmap.createBitmap(320, 240, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        int[] underHostPanel = new int[100 * 240];
        painter.draw(canvas, activeNavigationState(route), 100, 0, 120, 240, true, safeArea, 1f);
        bitmap.getPixels(underHostPanel, 0, 100, 0, 0, 100, 240);
        int routeColor = carContext.getColor(R.color.compass_route);
        assertTrue(Arrays.stream(underHostPanel).anyMatch(pixel -> pixel == routeColor));

        bitmap.eraseColor(0);
        painter.draw(canvas, activeNavigationState(routeWithStreet), 100, 0, 120, 240, true, safeArea, 1f);
        int[] withStreetUnderHostPanel = new int[underHostPanel.length];
        bitmap.getPixels(withStreetUnderHostPanel, 0, 100, 0, 0, 100, 240);
        assertFalse(Arrays.equals(underHostPanel, withStreetUnderHostPanel));
    }

    private static CarContext testCarContext() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        LifecycleOwner owner = new LifecycleOwner() {
            private final LifecycleRegistry lifecycle = new LifecycleRegistry(this);

            @Override
            public LifecycleRegistry getLifecycle() {
                return lifecycle;
            }
        };
        CarContext carContext = CarContext.create(owner.getLifecycle());
        Method attachBaseContext = CarContext.class.getDeclaredMethod(
                "attachBaseContext", Context.class, Configuration.class);
        attachBaseContext.setAccessible(true);
        attachBaseContext.invoke(carContext, context, context.getResources().getConfiguration());
        return carContext;
    }

    private static NavState activeNavigationState(NavCompassState compassState) {
        return new NavState(
                new NavRouteStatus(new NavGuidanceStatus("Turn right", "Continue"),
                        new NavProgressStatus("13:51", "466 m", ""), compassState),
                new NavGpsStatus("0 km/h", NavState.NO_DEADLINE),
                new NavPauseStatus(false));
    }
}
