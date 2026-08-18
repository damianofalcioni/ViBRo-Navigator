package vibro.navigator.main;

import android.text.TextUtils;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import vibro.navigator.nav.model.NavigationRoutingMode;
import vibro.navigator.nav.orientation.DisplayRotationProvider;
import vibro.navigator.nav.orientation.GeomagneticOrientationMonitor;
import vibro.navigator.nav.orientation.NavigationDisplayHeading;
import vibro.navigator.nav.orientation.NavigationHeadingMonitor;
import vibro.navigator.nav.time.ElapsedRealtimeClock;

final class MainActivityRoundTripDirectionController {
    interface HeadingMonitorFactory {
        @NonNull
        NavigationHeadingMonitor create(@NonNull GeomagneticOrientationMonitor.Callback callback);
    }

    @NonNull
    private final EditText directionEdit;
    @NonNull
    private final MainRoundTripDirectionCompassView compassView;
    @NonNull
    private final NavigationHeadingMonitor headingMonitor;
    @NonNull
    private final DisplayRotationProvider displayRotationProvider;
    @NonNull
    private final ElapsedRealtimeClock elapsedRealtimeClock;

    private boolean resumed;
    private boolean roundTripMode;
    private boolean monitoringActive;

    MainActivityRoundTripDirectionController(
            @NonNull EditText directionEdit,
            @NonNull MainRoundTripDirectionCompassView compassView,
            @NonNull HeadingMonitorFactory headingMonitorFactory,
            @NonNull DisplayRotationProvider displayRotationProvider,
            @NonNull ElapsedRealtimeClock elapsedRealtimeClock
    ) {
        this.directionEdit = directionEdit;
        this.compassView = compassView;
        this.displayRotationProvider = displayRotationProvider;
        this.elapsedRealtimeClock = elapsedRealtimeClock;
        headingMonitor = headingMonitorFactory.create(this::onSampleUpdated);
    }

    void onResume() {
        resumed = true;
        updateMonitoring();
    }

    void onPause() {
        resumed = false;
        updateMonitoring();
    }

    void onRouteModeChanged(@NonNull NavigationRoutingMode mode) {
        roundTripMode = mode == NavigationRoutingMode.ROUND_TRIP;
        updateMonitoring();
    }

    void dispose() {
        stopMonitoring();
    }

    private void updateMonitoring() {
        if (resumed && roundTripMode) {
            startMonitoring();
            return;
        }
        stopMonitoring();
    }

    private void startMonitoring() {
        if (monitoringActive) {
            refreshHeading(headingMonitor.getLatestSample());
            return;
        }
        monitoringActive = headingMonitor.start();
        if (!monitoringActive) {
            compassView.clearHeading();
            return;
        }
        refreshHeading(headingMonitor.getLatestSample());
    }

    private void stopMonitoring() {
        if (monitoringActive) {
            headingMonitor.stop();
        }
        monitoringActive = false;
        compassView.clearHeading();
    }

    private void onSampleUpdated(@NonNull GeomagneticOrientationMonitor.Sample sample) {
        if (!monitoringActive || !roundTripMode) {
            return;
        }
        refreshHeading(sample);
    }

    private void refreshHeading(@Nullable GeomagneticOrientationMonitor.Sample sample) {
        long nowElapsedRealtimeMs = elapsedRealtimeClock.elapsedRealtimeMs();
        Double headingDegrees = NavigationDisplayHeading.headingDegrees(
                sample,
                monitoringActive,
                nowElapsedRealtimeMs,
                displayRotationProvider.currentDisplayRotation()
        );
        Float headingAccuracyDegrees = NavigationDisplayHeading.headingAccuracyDegrees(
                sample,
                monitoringActive,
                nowElapsedRealtimeMs
        );
        compassView.setHeading(headingDegrees, headingAccuracyDegrees);
        if (headingDegrees != null && !directionEdit.hasFocus()) {
            String formattedHeading = RoundTripDirectionInput.formatHeadingDegrees(headingDegrees);
            if (!TextUtils.equals(directionEdit.getText(), formattedHeading)) {
                directionEdit.setText(formattedHeading);
            }
        }
    }
}
