package vibro.navigator.about;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.net.Uri;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import vibro.navigator.R;
import vibro.navigator.logging.AppLogger;

final class AboutProjectLinks {
    private static final String TAG = "AboutProjectLinks";
    private static final String SOURCE_CODE_URL = "https://github.com/damianofalcioni/ViBRo-Navigator";
    private static final String ISSUE_URL = "https://github.com/damianofalcioni/ViBRo-Navigator/issues/new";

    private AboutProjectLinks() {
    }

    static void configure(@NonNull Activity activity) {
        TextView sourceCodeLink = activity.findViewById(R.id.aboutSourceCodeLink);
        TextView reportIssueLink = activity.findViewById(R.id.aboutReportIssueLink);
        TextView credits = activity.findViewById(R.id.aboutCredits);
        ColorStateList linkColors = credits.getLinkTextColors();
        applyLinkColors(sourceCodeLink, linkColors);
        applyLinkColors(reportIssueLink, linkColors);
        sourceCodeLink.setOnClickListener(v -> openWebUrl(activity, SOURCE_CODE_URL));
        reportIssueLink.setOnClickListener(v -> openWebUrl(activity, ISSUE_URL));
    }

    private static void applyLinkColors(@NonNull TextView link, @NonNull ColorStateList linkColors) {
        link.setTextColor(linkColors);
    }

    private static void openWebUrl(@NonNull Activity activity, @NonNull String url) {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        try {
            activity.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            AppLogger.w(TAG, "Failed to open URL: " + url, e);
            Toast.makeText(activity, R.string.msg_open_url_failed, Toast.LENGTH_SHORT).show();
        }
    }
}
