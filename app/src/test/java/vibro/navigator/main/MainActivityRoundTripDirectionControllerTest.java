package vibro.navigator.main;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.app.Activity;
import android.widget.EditText;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;

import vibro.navigator.nav.model.NavigationRoutingMode;
import vibro.navigator.nav.orientation.DisplayRotation;
import vibro.navigator.nav.orientation.GeomagneticOrientationMonitor;
import vibro.navigator.nav.orientation.HeadingAccuracyStatus;
import vibro.navigator.nav.orientation.NavigationHeadingMonitor;
import vibro.navigator.nav.time.ElapsedRealtimeClock;

@RunWith(RobolectricTestRunner.class)
public class MainActivityRoundTripDirectionControllerTest {
    @Test
    public void roundTripModeStartsMonitorAndFillsDirectionFromDisplayHeading() {
        Fixture fixture = Fixture.create();
        fixture.displayRotation = DisplayRotation.ROTATION_90;

        fixture.controller.onRouteModeChanged(NavigationRoutingMode.ROUND_TRIP);
        fixture.controller.onResume();
        fixture.monitor.emit(sample(44.6, 7.5, 1_000L));

        assertTrue(fixture.monitor.started);
        assertEquals("135", fixture.directionEdit.getText().toString());
        assertEquals(Float.valueOf(135f), fixture.compassView.headingDegreesForTest());
        assertTrue(fixture.compassView.isHeadingAccuracyOkForTest());
    }

    @Test
    public void directionFieldIsNotOverwrittenWhileUserIsEditing() {
        Fixture fixture = Fixture.create();
        fixture.directionEdit.requestFocus();
        fixture.directionEdit.setText("77");

        fixture.controller.onRouteModeChanged(NavigationRoutingMode.ROUND_TRIP);
        fixture.controller.onResume();
        fixture.monitor.emit(sample(44.6, 7.5, 1_000L));

        assertEquals("77", fixture.directionEdit.getText().toString());
        assertEquals(Float.valueOf(45f), fixture.compassView.headingDegreesForTest());
    }

    @Test
    public void monitorRunsOnlyWhileResumedRoundTripModeIsVisible() {
        Fixture fixture = Fixture.create();

        fixture.controller.onResume();
        assertFalse(fixture.monitor.started);

        fixture.controller.onRouteModeChanged(NavigationRoutingMode.ROUND_TRIP);
        assertTrue(fixture.monitor.started);

        fixture.controller.onRouteModeChanged(NavigationRoutingMode.STRAIGHT_LINE);
        assertFalse(fixture.monitor.started);
        assertFalse(fixture.compassView.isHeadingAccuracyOkForTest());
    }

    @NonNull
    private static GeomagneticOrientationMonitor.Sample sample(
            double headingDegrees,
            double headingAccuracyDegrees,
            long elapsedRealtimeMs
    ) {
        return new GeomagneticOrientationMonitor.Sample(
                headingDegrees,
                0.0,
                0.0,
                HeadingAccuracyStatus.HIGH,
                headingAccuracyDegrees,
                elapsedRealtimeMs
        );
    }

    private static final class Fixture {
        @NonNull
        final EditText directionEdit;
        @NonNull
        final MainRoundTripDirectionCompassView compassView;
        @NonNull
        final FakeHeadingMonitor monitor = new FakeHeadingMonitor();
        @NonNull
        final MainActivityRoundTripDirectionController controller;
        int displayRotation = DisplayRotation.ROTATION_0;

        private Fixture(@NonNull Activity activity) {
            directionEdit = new EditText(activity);
            compassView = new MainRoundTripDirectionCompassView(activity);
            LinearLayout root = new LinearLayout(activity);
            root.addView(directionEdit);
            root.addView(compassView);
            activity.setContentView(root);
            ElapsedRealtimeClock clock = () -> 1_000L;
            controller = new MainActivityRoundTripDirectionController(
                    directionEdit,
                    compassView,
                    callback -> monitor.attach(callback),
                    () -> displayRotation,
                    clock
            );
        }

        @NonNull
        static Fixture create() {
            Activity activity = Robolectric.buildActivity(Activity.class).setup().get();
            return new Fixture(activity);
        }
    }

    private static final class FakeHeadingMonitor implements NavigationHeadingMonitor {
        @Nullable
        private GeomagneticOrientationMonitor.Callback callback;
        @Nullable
        private GeomagneticOrientationMonitor.Sample latestSample;
        private boolean started;

        @NonNull
        FakeHeadingMonitor attach(@NonNull GeomagneticOrientationMonitor.Callback callback) {
            this.callback = callback;
            return this;
        }

        @Override
        public boolean start() {
            started = true;
            return true;
        }

        @Override
        public void stop() {
            started = false;
            latestSample = null;
        }

        @Override
        @Nullable
        public GeomagneticOrientationMonitor.Sample getLatestSample() {
            return latestSample;
        }

        void emit(@NonNull GeomagneticOrientationMonitor.Sample sample) {
            latestSample = sample;
            if (callback != null) {
                callback.onSampleUpdated(sample);
            }
        }
    }
}
