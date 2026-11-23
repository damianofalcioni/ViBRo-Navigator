package vibro.navigator.nav.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;

import android.app.AlertDialog;
import android.app.Application;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Looper;
import android.widget.TextView;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadows.ShadowAlertDialog;

import java.util.Collections;

import vibro.navigator.R;
import vibro.navigator.android.intent.AndroidNavigationRequestIntentContract;
import vibro.navigator.geo.LatLon;
import vibro.navigator.nav.model.NavigationRequest;
import vibro.navigator.nav.model.NavigationRoutingMode;
import vibro.navigator.nav.service.NavigationService;
import vibro.navigator.nav.service.NavigationServiceBinder;
import vibro.navigator.settings.AppSimulationSettings;

@RunWith(RobolectricTestRunner.class)
public class NavigationSimulationNoticeRobolectricTest {
    private final Application app = ApplicationProvider.getApplicationContext();

    @Test
    public void simulatedStartShowsOneNoticeWithRegularInstructionAndGpsText() {
        AppSimulationSettings.setEnabled(app, true);
        AppSimulationSettings.save(app, 5, "48.20,16.36");
        var service = Robolectric.buildService(NavigationService.class).create();
        try {
            var binder = connectService(service.get());
            service.get().onStartCommand(startIntent(), 0, 1);
            try (var screen = Robolectric.buildActivity(NavigationActivity.class, resumeIntent()).create().start()) {
                shadowOf(Looper.getMainLooper()).idle();
                assertNull(ShadowAlertDialog.getLatestAlertDialog());

                screen.resume();
                shadowOf(Looper.getMainLooper()).idle();
                AlertDialog notice = ShadowAlertDialog.getLatestAlertDialog();
                assertNotNull(notice);
                assertTrue(notice.isShowing());
                assertEquals(screen.get().getString(R.string.nav_simulation_mode_title), shadowOf(notice).getTitle());
                assertEquals(screen.get().getString(R.string.nav_simulation_started), shadowOf(notice).getMessage());
                assertRegularText(screen.get(), R.id.nextDirectionText);
                assertRegularText(screen.get(), R.id.gpsStatusText);

                notice.getButton(AlertDialog.BUTTON_POSITIVE).performClick();
                binder.pause();
                binder.resume();
                screen.pause().resume();
                shadowOf(Looper.getMainLooper()).idle();
                assertSame(notice, ShadowAlertDialog.getLatestAlertDialog());
                assertFalse(notice.isShowing());

                service.get().onStartCommand(startIntent(), 0, 2);
                shadowOf(Looper.getMainLooper()).idle();
                assertNotSame(notice, ShadowAlertDialog.getLatestAlertDialog());
                assertTrue(ShadowAlertDialog.getLatestAlertDialog().isShowing());
            }
        } finally {
            service.destroy();
        }
    }

    @Test
    public void realStartShowsNoSimulationNotice() {
        var service = Robolectric.buildService(NavigationService.class).create();
        try {
            connectService(service.get());
            service.get().onStartCommand(startIntent(), 0, 1);
            try (var screen = Robolectric.buildActivity(NavigationActivity.class, resumeIntent()).setup()) {
                shadowOf(Looper.getMainLooper()).idle();
                assertFalse(screen.get().isFinishing());
                assertNull(ShadowAlertDialog.getLatestAlertDialog());
            }
        } finally {
            service.destroy();
        }
    }

    private NavigationServiceBinder connectService(NavigationService service) {
        Intent bind = new Intent(app, NavigationService.class);
        var binder = (NavigationServiceBinder) service.onBind(bind);
        shadowOf(app).setComponentNameAndServiceForBindService(new ComponentName(app, NavigationService.class), binder);
        return binder;
    }

    private Intent resumeIntent() {
        return new Intent(app, NavigationActivity.class).putExtra(NavigationActivity.EXTRA_RESUME_EXISTING, true);
    }

    private static Intent startIntent() {
        Intent intent = new Intent(NavigationService.ACTION_START);
        AndroidNavigationRequestIntentContract.putInto(intent, new NavigationRequest(
                NavigationRoutingMode.STRAIGHT_LINE, null, "Vienna", new LatLon(48.2082, 16.3738),
                Collections.emptyList()));
        return intent;
    }

    private static void assertRegularText(NavigationActivity screen, int viewId) {
        TextView line = screen.findViewById(viewId);
        String text = line.getText().toString();
        assertFalse(text.isEmpty());
        assertFalse(text.contains(screen.getString(R.string.label_simulation)));
    }
}
