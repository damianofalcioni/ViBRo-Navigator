package vibro.navigator.auto;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.RectF;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.GraphicsMode;

import vibro.navigator.nav.model.NavGpsStatus;
import vibro.navigator.nav.model.NavGpsTelemetry;
import vibro.navigator.nav.model.NavRouteStatus;
import vibro.navigator.nav.model.NavState;
import vibro.navigator.nav.route.RouteSpeedLimit;

@RunWith(RobolectricTestRunner.class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class ViBRoAutoSpeedLimitTest {
    @Test
    public void badgeChangesAboveTheLimitAndRecoversBelowIt() throws Exception {
        ViBRoCarAppServiceSettingsGplayTest contexts = new ViBRoCarAppServiceSettingsGplayTest();
        contexts.setUp();
        ViBRoAutoCompassOverlayPainter painter = new ViBRoAutoCompassOverlayPainter(contexts.testCarContext());
        NavState below = atSpeed(45);
        Bitmap normal = draw(painter, below);
        assertFalse(normal.sameAs(draw(painter, atSpeed(55))));
        assertTrue(normal.sameAs(draw(painter, below)));
    }

    private static NavState atSpeed(int kilometersPerHour) {
        NavState base = CarNavigationTestStates.active(false, false);
        RouteSpeedLimit limit = new RouteSpeedLimit(50, RouteSpeedLimit.Unit.KILOMETERS_PER_HOUR);
        NavRouteStatus route = new NavRouteStatus(base.routeStatus.guidance, base.routeStatus.progress,
                base.routeStatus.compassState, limit);
        String speed = kilometersPerHour + " km/h";
        NavGpsTelemetry telemetry = new NavGpsTelemetry(kilometersPerHour / 3.6f, speed, speed,
                "", "", "", "", "", "", "");
        return new NavState(route, new NavGpsStatus(speed, NavState.NO_DEADLINE, telemetry), base.pauseStatus);
    }

    private static Bitmap draw(ViBRoAutoCompassOverlayPainter painter, NavState state) {
        Bitmap bitmap = Bitmap.createBitmap(160, 160, Bitmap.Config.ARGB_8888);
        painter.draw(new Canvas(bitmap), state, new RectF(5, 5, 160, 160), 1);
        return bitmap;
    }
}
