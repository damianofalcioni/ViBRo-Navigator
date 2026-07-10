package vibro.navigator.main;

import vibro.navigator.R;
import vibro.navigator.android.service.AndroidServiceRunningState;
import vibro.navigator.android.intent.AndroidNavigationRequestIntentContract;
import vibro.navigator.intent.IntentLocationParser;
import vibro.navigator.nav.service.NavigationService;
import vibro.navigator.nav.ui.NavigationActivity;


import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import vibro.navigator.poi.CoordinateParser;
import vibro.navigator.poi.Poi;
import vibro.navigator.poi.ui.PoiInputController;
import vibro.navigator.poi.ui.PoiReverseGeocodeController;
import vibro.navigator.logging.AppLogger;

final class MainActivityIntentHandler {

    private static final String TAG = "MainIntentHandler";

    private MainActivityIntentHandler() {
    }

    static boolean redirectGpxImportDuringActiveNavigation(
            @NonNull Activity activity,
            @Nullable Intent intent
    ) {
        return redirectGpxImportDuringActiveNavigation(
                activity,
                intent,
                AndroidServiceRunningState.isRunning(activity, NavigationService.class)
        );
    }

    static boolean redirectGpxImportDuringActiveNavigation(
            @NonNull Activity activity,
            @Nullable Intent intent,
            boolean navigationServiceRunning
    ) {
        if (!navigationServiceRunning || !MainActivityGpxImportController.isGpxImportIntent(intent)) {
            return false;
        }
        AppLogger.i(TAG, "Ignoring GPX import while navigation service is running");
        Toast.makeText(activity, R.string.msg_gpx_import_blocked_navigation, Toast.LENGTH_SHORT).show();
        Intent navigationIntent = new Intent(activity, NavigationActivity.class);
        navigationIntent.putExtra(NavigationActivity.EXTRA_RESUME_EXISTING, true);
        navigationIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        activity.startActivity(navigationIntent);
        return true;
    }

    static boolean handleOpenNavigationIntent(@NonNull Activity activity, @Nullable Intent intent) {
        if (intent == null || !intent.getBooleanExtra(MainActivity.EXTRA_OPEN_NAVIGATION, false)) {
            return false;
        }
        AppLogger.i(TAG, "Forwarding notification tap to NavigationActivity");
        Intent navigationIntent = new Intent(activity, NavigationActivity.class);
        navigationIntent.putExtra(NavigationActivity.EXTRA_RESUME_EXISTING, true);
        AndroidNavigationRequestIntentContract.putInto(
                navigationIntent,
                AndroidNavigationRequestIntentContract.fromIntent(intent)
        );

        intent.removeExtra(MainActivity.EXTRA_OPEN_NAVIGATION);
        intent.removeExtra(NavigationActivity.EXTRA_RESUME_EXISTING);
        activity.setIntent(intent);
        activity.startActivity(navigationIntent);
        return true;
    }

    static void handleIncomingIntent(
            @NonNull Context context,
            @Nullable Intent intent,
            @NonNull PoiInputController destinationController
    ) {
        handleIncomingIntent(
                context,
                intent,
                destinationController,
                PoiReverseGeocodeController.disabled()
        );
    }

    static void handleIncomingIntent(
            @NonNull Activity activity,
            @Nullable Intent intent,
            @NonNull PoiInputController destinationController,
            @NonNull MainActivityStopController stopController,
            @NonNull MainActivityRouteModeController routeModeController,
            @NonNull PoiReverseGeocodeController reverseGeocodeController
    ) {
        if (intent != null && MainActivityGpxImportController.importIfSupported(
                activity,
                intent,
                destinationController,
                stopController,
                routeModeController,
                reverseGeocodeController
        )) {
            return;
        }
        handleIncomingLocation(
                activity,
                intent,
                destinationController,
                reverseGeocodeController
        );
    }

    static void handleIncomingIntent(
            @NonNull Context context,
            @Nullable Intent intent,
            @NonNull PoiInputController destinationController,
            @NonNull PoiReverseGeocodeController reverseGeocodeController
    ) {
        handleIncomingLocation(
                context,
                intent,
                destinationController,
                reverseGeocodeController
        );
    }

    private static void handleIncomingLocation(
            @NonNull Context context,
            @Nullable Intent intent,
            @NonNull PoiInputController destinationController,
            @NonNull PoiReverseGeocodeController reverseGeocodeController
    ) {
        if (intent == null) {
            AppLogger.d(TAG, "handleIncomingIntent ignored null intent");
            return;
        }
        String query = IntentLocationParser.parseToQuery(
                intent.getAction(),
                intent.getDataString(),
                intent.getStringExtra(Intent.EXTRA_TEXT)
        );
        if (query == null || query.trim().isEmpty()) {
            if (Intent.ACTION_VIEW.equals(intent.getAction()) || Intent.ACTION_SEND.equals(intent.getAction())) {
                Toast.makeText(context, R.string.msg_intent_unrecognized, Toast.LENGTH_SHORT).show();
            }
            AppLogger.d(TAG, "No destination extracted from intent");
            return;
        }
        String trimmedQuery = query.trim();
        applyIncomingLocation(
                destinationController,
                reverseGeocodeController,
                trimmedQuery
        );
    }

    private static void applyIncomingLocation(
            @NonNull PoiInputController destinationController,
            @NonNull PoiReverseGeocodeController reverseGeocodeController,
            @NonNull String trimmedQuery
    ) {
        Poi parsedPoi = CoordinateParser.tryParse(trimmedQuery, trimmedQuery);
        if (parsedPoi != null) {
            reverseGeocodeController.setPoiAndResolveAddress(destinationController, parsedPoi);
            AppLogger.i(TAG, "Applied incoming destination POI=" + parsedPoi.displayLabel()
                    + " (" + parsedPoi.lat + "," + parsedPoi.lon + ")");
        } else {
            destinationController.setText(trimmedQuery);
            AppLogger.i(TAG, "Applied incoming destination query=" + trimmedQuery);
        }
        destinationController.getEditText().requestFocus();
    }

    @NonNull
    static String describeIntent(@Nullable Intent intent) {
        if (intent == null) {
            return "null";
        }
        return "action=" + safe(intent.getAction())
                + ", data=" + safe(intent.getDataString())
                + ", type=" + safe(intent.getType());
    }

    @NonNull
    private static String safe(@Nullable String value) {
        return value == null ? "null" : value;
    }
}

