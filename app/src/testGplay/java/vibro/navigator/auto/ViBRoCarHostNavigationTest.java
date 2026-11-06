package vibro.navigator.auto;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import androidx.car.app.navigation.model.Maneuver;
import androidx.car.app.navigation.model.Trip;

import org.junit.Test;

import vibro.navigator.nav.model.NavState;
import vibro.navigator.nav.route.VoiceHint;
import vibro.navigator.nav.simulation.NavigationSimulationDisplay;

public class ViBRoCarHostNavigationTest {
    @Test
    public void synchronousAutoDriveRefreshCannotBeOverwrittenByTheOriginalRealState() {
        RecordingHost host = new RecordingHost();
        ViBRoCarHostNavigation navigation = new ViBRoCarHostNavigation(host, () -> { });
        NavState initial = CarNavigationTestStates.active(false, false);
        NavState simulated = NavigationSimulationDisplay.label(initial, "Simulation");
        navigation.setAutoDriveAction(() -> navigation.update(simulated));
        host.onStart = navigation::onAutoDriveEnabled;
        navigation.update(initial);
        assertEquals(1, host.updates);
        assertEquals(simulated, host.state);
    }

    @Test
    public void autoDriveCallbackActivatesSharedSimulationAndIgnoresDisposedSession() {
        int[] enables = {0};
        ViBRoCarHostNavigation navigation = new ViBRoCarHostNavigation(new RecordingHost(), () -> { });
        navigation.setAutoDriveAction(() -> enables[0]++);
        navigation.onAutoDriveEnabled();
        assertEquals(1, enables[0]);
        navigation.dispose();
        navigation.onAutoDriveEnabled();
        assertEquals(1, enables[0]);
    }

    @Test
    public void ownsNavigationOnceAndDoesNotRepublishHeadingOnlyUpdates() {
        RecordingHost host = new RecordingHost();
        ViBRoCarHostNavigation navigation = new ViBRoCarHostNavigation(host, () -> { });
        navigation.update(CarNavigationTestStates.active(false, false));
        navigation.update(CarNavigationTestStates.active(false, false));
        assertEquals(1, host.starts);
        assertEquals(1, host.updates);
        assertEquals(Maneuver.TYPE_TURN_NORMAL_RIGHT, host.trip.getSteps().get(0).getManeuver().getType());
        assertEquals(120, host.trip.getStepTravelEstimates().get(0).getRemainingDistance().getDisplayDistance(), 0);
    }

    @Test
    public void pauseArrivalAndDisconnectEndOwnershipAndClearCarNotification() {
        RecordingHost host = new RecordingHost();
        ViBRoCarHostNavigation navigation = new ViBRoCarHostNavigation(host, () -> { });
        navigation.update(CarNavigationTestStates.active(false, false));
        navigation.update(CarNavigationTestStates.active(true, false));
        assertEquals(1, host.ends);
        navigation.update(CarNavigationTestStates.active(false, false));
        navigation.update(CarNavigationTestStates.active(false, true));
        assertEquals(2, host.ends);
        navigation.update(CarNavigationTestStates.active(false, false));
        navigation.close();
        navigation.close();
        assertEquals(3, host.ends);
        assertTrue(host.clears >= 3);
    }

    @Test
    public void hostStopStopsDomainNavigationWithoutReannouncingHostEnd() {
        RecordingHost host = new RecordingHost();
        int[] stops = {0};
        ViBRoCarHostNavigation navigation = new ViBRoCarHostNavigation(host, () -> stops[0]++);
        navigation.update(CarNavigationTestStates.active(false, false));
        navigation.onStopNavigation();
        navigation.update(null);
        assertEquals(1, stops[0]);
        assertEquals(0, host.ends);
    }

    @Test
    public void synchronousRevocationDuringStartCannotPublishStaleGuidance() {
        RecordingHost host = new RecordingHost();
        int[] stops = {0};
        ViBRoCarHostNavigation navigation = new ViBRoCarHostNavigation(host, () -> stops[0]++);
        host.onStart = navigation::onStopNavigation;
        navigation.update(CarNavigationTestStates.active(false, false));
        assertEquals(1, stops[0]);
        assertEquals(0, host.updates);
    }

    @Test
    public void recalculationRetainsOwnershipAndClearsObsoleteClusterSteps() {
        RecordingHost host = new RecordingHost();
        ViBRoCarHostNavigation navigation = new ViBRoCarHostNavigation(host, () -> { });
        navigation.update(CarNavigationTestStates.active(false, false));
        navigation.update(CarNavigationTestStates.loading());
        assertEquals(1, host.starts);
        assertEquals(0, host.ends);
        assertTrue(host.trip.isLoading());
        assertTrue(host.trip.getSteps().isEmpty());
    }

    @Test
    public void destroyedScreenCannotReclaimOwnershipOrStopPhoneNavigation() {
        RecordingHost host = new RecordingHost();
        int[] stops = {0};
        ViBRoCarHostNavigation navigation = new ViBRoCarHostNavigation(host, () -> stops[0]++);
        navigation.update(CarNavigationTestStates.active(false, false));
        navigation.dispose();
        navigation.update(CarNavigationTestStates.active(false, false));
        navigation.onStopNavigation();
        assertEquals(1, host.starts);
        assertEquals(1, host.updates);
        assertEquals(1, host.ends);
        assertEquals(0, stops[0]);
    }

    @Test
    public void roundaboutDirectionAndUnknownCommandsDoNotInventRoadTurns() {
        assertEquals(Maneuver.TYPE_ROUNDABOUT_ENTER_AND_EXIT_CCW,
                ViBRoCarManeuvers.maneuver(new VoiceHint(0, 13, 3, 0, 0)).getType());
        assertEquals(3, ViBRoCarManeuvers.maneuver(new VoiceHint(0, 14, -3, 0, 0)).getRoundaboutExitNumber());
        assertEquals(Maneuver.TYPE_ROUNDABOUT_ENTER_CCW,
                ViBRoCarManeuvers.maneuver(new VoiceHint(0, 13, 0, 0, 0)).getType());
        assertEquals(Maneuver.TYPE_UNKNOWN, ViBRoCarManeuvers.type(16));
        assertEquals(Maneuver.TYPE_UNKNOWN, ViBRoCarManeuvers.type(-1));
        assertEquals(Maneuver.TYPE_DESTINATION, ViBRoCarManeuvers.type(101));
    }

    private static final class RecordingHost implements ViBRoCarHostNavigation.Host {
        private int starts;
        private int ends;
        private int updates;
        private int clears;
        private Trip trip;
        private NavState state;
        private Runnable onStart;

        @Override public void started() {
            starts++;
            if (onStart != null) { onStart.run(); }
        }
        @Override public void ended() { ends++; }
        @Override public void update(Trip trip, NavState state) { updates++; this.trip = trip; this.state = state; }
        @Override public void clear() { clears++; }
    }
}
