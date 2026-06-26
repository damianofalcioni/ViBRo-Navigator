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
    private static final String PAGES_BASE_URL = "https://damianofalcioni.github.io/ViBRo-Navigator";
    private static final String PRIVACY_POLICY_URL = PAGES_BASE_URL + "/privacy-policy/";
    private static final String TERMS_OF_SERVICE_URL = PAGES_BASE_URL + "/terms-of-service/";

    private AboutProjectLinks() {
    }

    static void configure(@NonNull Activity activity) {
        TextView credits = activity.findViewById(R.id.aboutCredits);
        ColorStateList linkColors = credits.getLinkTextColors();
        configureLink(activity, R.id.aboutSourceCodeLink, linkColors, SOURCE_CODE_URL);
        configureLink(activity, R.id.aboutReportIssueLink, linkColors, ISSUE_URL);
        configureLink(activity, R.id.aboutPrivacyPolicyLink, linkColors, PRIVACY_POLICY_URL);
        configureLink(activity, R.id.aboutTermsOfServiceLink, linkColors, TERMS_OF_SERVICE_URL);
    }

    private static void configureLink(
            @NonNull Activity activity,
            int viewId,
            @NonNull ColorStateList linkColors,
            @NonNull String url
    ) {
        TextView link = activity.findViewById(viewId);
        applyLinkColors(link, linkColors);
        link.setOnClickListener(v -> openWebUrl(activity, url));
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
