package vibro.navigator.about;

import android.app.Activity;
import android.content.Intent;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import vibro.navigator.R;
import vibro.navigator.android.dispatch.AndroidTaskScheduler;
import vibro.navigator.dispatch.TaskScheduler;

final class AboutDiagnosticSection {

    private static final long SENSOR_STATUS_REFRESH_INTERVAL_MS = 1000L;

    @NonNull
    private final Activity activity;
    @NonNull
    private final TaskScheduler sensorStatusScheduler = AndroidTaskScheduler.main();
    @NonNull
    private final Runnable sensorStatusRefreshRunnable = new Runnable() {
        @Override
        public void run() {
            if (!started) {
                return;
            }
            render();
            sensorStatusScheduler.postDelayed(this, SENSOR_STATUS_REFRESH_INTERVAL_MS);
        }
    };
    @Nullable
    private AboutSensorStatusFormatter sensorStatusFormatter;
    @NonNull
    private final TextView sensorStatusTitle;
    @NonNull
    private final TextView sensorStatusBody;
    @NonNull
    private final AboutPermissionStatusRows permissionStatusRows;
    @NonNull
    private final AboutSymbolTestButtons symbolTestButtons;
    private boolean started;

    AboutDiagnosticSection(@NonNull Activity activity) {
        this.activity = activity;
        sensorStatusTitle = activity.findViewById(R.id.aboutSensorStatusTitle);
        sensorStatusBody = activity.findViewById(R.id.aboutSensorStatusBody);
        permissionStatusRows = new AboutPermissionStatusRows(activity);
        symbolTestButtons = new AboutSymbolTestButtons(activity);
    }

    void render() {
        sensorStatusTitle.setVisibility(View.VISIBLE);
        sensorStatusBody.setVisibility(View.VISIBLE);
        permissionStatusRows.render();
        symbolTestButtons.show();
        sensorStatusBody.setText(sensorStatusFormatter().build(activity));
    }

    boolean onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        return permissionStatusRows.onActivityResult(requestCode, resultCode, data);
    }

    boolean onRequestPermissionsResult(int requestCode, @NonNull int[] grantResults) {
        return permissionStatusRows.onRequestPermissionsResult(requestCode, grantResults);
    }

    void start() {
        if (started) {
            return;
        }
        started = true;
        sensorStatusFormatter().start();
        sensorStatusScheduler.postDelayed(sensorStatusRefreshRunnable, SENSOR_STATUS_REFRESH_INTERVAL_MS);
    }

    void stop() {
        sensorStatusScheduler.removeCallbacks(sensorStatusRefreshRunnable);
        if (sensorStatusFormatter != null) {
            sensorStatusFormatter.stop();
        }
        started = false;
    }

    @NonNull
    private AboutSensorStatusFormatter sensorStatusFormatter() {
        if (sensorStatusFormatter == null) {
            sensorStatusFormatter = new AboutSensorStatusFormatter(activity);
        }
        return sensorStatusFormatter;
    }
}
