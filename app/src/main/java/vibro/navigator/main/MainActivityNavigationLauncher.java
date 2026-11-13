package vibro.navigator.main;

import android.app.Activity;
import android.content.Intent;

import java.util.ArrayList;

import androidx.annotation.NonNull;

import vibro.navigator.android.intent.AndroidNavigationRequestIntentContract;
import vibro.navigator.logging.AppLogger;
import vibro.navigator.nav.model.NavigationRequest;
import vibro.navigator.nav.ui.NavigationActivity;

final class MainActivityNavigationLauncher {
    private static final String TAG = "MainActivity";

    private MainActivityNavigationLauncher() {
    }

    static void launch(@NonNull Activity activity, @NonNull NavigationRequest request, ArrayList<String> skippedSegmentsFolders) {
        AppLogger.i(TAG, "Starting NavigationActivity " + request.describe());
        Intent intent = new Intent(activity, NavigationActivity.class);
        AndroidNavigationRequestIntentContract.putInto(intent, request);
        intent.putStringArrayListExtra(NavigationActivity.EXTRA_SKIPPED_SEGMENTS_FOLDERS, skippedSegmentsFolders);
        activity.startActivity(intent);
    }
}
