package vibro.navigator.auto;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import android.graphics.Bitmap;

import androidx.car.app.navigation.model.Maneuver;
import androidx.test.core.app.ApplicationProvider;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.GraphicsMode;

import vibro.navigator.R;
import vibro.navigator.nav.route.VoiceHint;

@RunWith(RobolectricTestRunner.class)
public class ViBRoCarManeuverIconsTest {
    private final ViBRoCarManeuvers.Icons icons =
            ViBRoCarManeuvers.Icons.from(ApplicationProvider.getApplicationContext());

    @Test
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    public void roundaboutIconsDistinguishDirectionsAndExitNumbers() {
        Maneuver exitTwo = maneuver(13, 2);
        Maneuver exitThree = maneuver(13, 3);
        Maneuver clockwise = maneuver(14, -2);
        Maneuver unnumbered = maneuver(13, 0);

        assertEquals(2, exitTwo.getRoundaboutExitNumber());
        assertEquals(2, clockwise.getRoundaboutExitNumber());
        assertEquals(Maneuver.TYPE_ROUNDABOUT_ENTER_CCW, unnumbered.getType());
        assertFalse(bitmap(exitTwo).sameAs(bitmap(exitThree)));
        assertFalse(bitmap(exitTwo).sameAs(bitmap(clockwise)));
        assertFalse(bitmap(exitTwo).sameAs(bitmap(unnumbered)));
        assertSame(exitTwo.getIcon(), maneuver(13, 2).getIcon());
    }

    @Test
    public void offRouteUsesAnXWhileUnrecognizedCommandsHaveNoIcon() {
        assertEquals(Maneuver.TYPE_UNKNOWN, maneuver(12, 0).getType());
        assertEquals(R.drawable.ic_auto_off_route, drawable(12));
        assertNull(maneuver(0, 0).getIcon());
    }

    @Test
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    public void keepsAndExitsUsePhoneArrowsWithTheirOwnManeuverTypes() {
        assertDirectionIconAndManeuverType(8, 3, Maneuver.TYPE_KEEP_LEFT);
        assertDirectionIconAndManeuverType(9, 6, Maneuver.TYPE_KEEP_RIGHT);
        assertDirectionIconAndManeuverType(17, 2, Maneuver.TYPE_OFF_RAMP_NORMAL_LEFT);
        assertDirectionIconAndManeuverType(18, 5, Maneuver.TYPE_OFF_RAMP_NORMAL_RIGHT);
    }

    @Test
    public void finalDestinationUsesACheckeredFlag() {
        assertEquals(R.drawable.ic_auto_destination, drawable(100));
    }

    private void assertDirectionIconAndManeuverType(int command, int matchingTurn, int expectedType) {
        Maneuver actual = maneuver(command, 0);
        assertEquals(expectedType, actual.getType());
        assertTrue(bitmap(actual).sameAs(bitmap(maneuver(matchingTurn, 0))));
    }

    private Maneuver maneuver(int command, int exitNumber) {
        return ViBRoCarManeuvers.maneuver(new VoiceHint(0, command, exitNumber, 0, 0), icons);
    }

    private int drawable(int command) {
        return maneuver(command, 0).getIcon().getIcon().getResId();
    }

    private static Bitmap bitmap(Maneuver maneuver) {
        return maneuver.getIcon().getIcon().getBitmap();
    }
}
