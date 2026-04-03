package com.vibenavigator;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.vibenavigator.nav.NavigationRequest;
import com.vibenavigator.poi.CoordinateParser;
import com.vibenavigator.poi.Poi;
import com.vibenavigator.poi.ui.PoiInputController;
import com.vibenavigator.util.AppLogger;
import com.vibenavigator.util.IntentLocationParser;

final class MainActivityIntentHandler {

    private static final String TAG = "MainIntentHandler";

    private MainActivityIntentHandler() {
    }

    static boolean handleOpenNavigationIntent(@NonNull Activity activity, @Nullable Intent intent) {
        if (intent == null || !intent.getBooleanExtra(MainActivity.EXTRA_OPEN_NAVIGATION, false)) {
            return false;
        }
        AppLogger.i(TAG, "Forwarding notification tap to NavigationActivity");
        Intent navigationIntent = new Intent(activity, NavigationActivity.class);
        navigationIntent.putExtra(NavigationActivity.EXTRA_RESUME_EXISTING, true);
        NavigationRequest.fromIntent(intent).putInto(navigationIntent);

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
        if (intent == null) {
            AppLogger.d(TAG, "handleIncomingIntent ignored null intent");
            return;
        }
        String query = IntentLocationParser.parseToQuery(intent);
        if (query == null || query.trim().isEmpty()) {
            if (Intent.ACTION_VIEW.equals(intent.getAction()) || Intent.ACTION_SEND.equals(intent.getAction())) {
                Toast.makeText(context, R.string.msg_intent_unrecognized, Toast.LENGTH_SHORT).show();
            }
            AppLogger.d(TAG, "No destination extracted from intent");
            return;
        }
        String trimmedQuery = query.trim();
        Poi parsedPoi = CoordinateParser.tryParse(trimmedQuery, trimmedQuery);
        if (parsedPoi != null) {
            destinationController.setPoi(parsedPoi);
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
