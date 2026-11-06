package vibro.navigator.nav.service;

import android.content.Context;
import androidx.test.core.app.ApplicationProvider;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import vibro.navigator.nav.session.NavigationSession;
import vibro.navigator.settings.AppSimulationSettings;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
public class NavigationServiceSimulationTest {
    @Test
    public void hostActivationRestartsOnceWithoutPersistingManualSettingAndClearsOnStop() {
        Context context = ApplicationProvider.getApplicationContext();
        NavigationServiceSimulation simulation = new NavigationServiceSimulation(context,
                new NavigationSession(), () -> 100000, ignored -> { });
        int[] restarts = {0};
        simulation.prepare();
        assertFalse(simulation.source().isEnabled());
        Runnable restart = () -> { restarts[0]++; simulation.prepare(); };
        simulation.enable(restart);
        simulation.enable(restart);
        assertEquals(1, restarts[0]);
        assertTrue(simulation.source().isEnabled());
        assertFalse(AppSimulationSettings.isEnabled(context));
        simulation.stop();
        simulation.prepare();
        assertFalse(simulation.source().isEnabled());
    }

    @Test
    public void manualSimulationDoesNotRestartWhenHostEnablesAutoDrive() {
        Context context = ApplicationProvider.getApplicationContext();
        AppSimulationSettings.setEnabled(context, true);
        NavigationServiceSimulation simulation = new NavigationServiceSimulation(context,
                new NavigationSession(), () -> 100000, ignored -> { });
        simulation.prepare();
        simulation.enable(() -> { throw new AssertionError(); });
        simulation.stop();
        simulation.prepare();
        assertTrue(simulation.source().isEnabled());
    }
}
