package vibro.navigator.about;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;

import vibro.navigator.R;

final class AboutDiagnosticSection {

    private static final long SENSOR_STATUS_REFRESH_INTERVAL_MS = 1000L;

    @NonNull
    private final Activity activity;
    @NonNull
    private final Handler sensorStatusHandler = new Handler(Looper.getMainLooper());
    @NonNull
    private final Runnable sensorStatusRefreshRunnable = new Runnable() {
        @Override
        public void run() {
            render();
            sensorStatusHandler.postDelayed(this, SENSOR_STATUS_REFRESH_INTERVAL_MS);
        }
    };
    @NonNull
    private final AboutSensorStatusFormatter sensorStatusFormatter;
    @NonNull
    private final TextView sensorStatusTitle;
    @NonNull
    private final TextView sensorStatusBody;
    @NonNull
    private final AboutPermissionStatusRows permissionStatusRows;
    @NonNull
    private final AboutSymbolTestButtons symbolTestButtons;

    AboutDiagnosticSection(@NonNull Activity activity) {
        this.activity = activity;
        sensorStatusTitle = activity.findViewById(R.id.aboutSensorStatusTitle);
        sensorStatusBody = activity.findViewById(R.id.aboutSensorStatusBody);
        permissionStatusRows = new AboutPermissionStatusRows(activity);
        symbolTestButtons = new AboutSymbolTestButtons(activity);
        sensorStatusFormatter = new AboutSensorStatusFormatter(activity);
    }

    void render() {
        sensorStatusTitle.setVisibility(View.VISIBLE);
        sensorStatusBody.setVisibility(View.VISIBLE);
        permissionStatusRows.render();
        symbolTestButtons.show();
        sensorStatusBody.setText(sensorStatusFormatter.build(activity));
    }

    void start() {
        sensorStatusFormatter.start();
        sensorStatusHandler.post(sensorStatusRefreshRunnable);
    }

    void stop() {
        sensorStatusHandler.removeCallbacks(sensorStatusRefreshRunnable);
        sensorStatusFormatter.stop();
    }
}
