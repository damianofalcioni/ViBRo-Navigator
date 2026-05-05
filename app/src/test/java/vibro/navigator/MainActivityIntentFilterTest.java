package vibro.navigator;

import vibro.navigator.main.MainActivity;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class MainActivityIntentFilterTest {

    @Test
    public void mainActivityDoesNotResolveGenericWebUrls() {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://example.com/articles/hello"));
        intent.addCategory(Intent.CATEGORY_BROWSABLE);
        intent.addCategory(Intent.CATEGORY_DEFAULT);

        assertFalse(resolvesToMainActivity(intent));
    }

    @Test
    public void mainActivityDoesNotResolveGenericGoogleUrls() {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=coffee"));
        intent.addCategory(Intent.CATEGORY_BROWSABLE);
        intent.addCategory(Intent.CATEGORY_DEFAULT);

        assertFalse(resolvesToMainActivity(intent));
    }

    @Test
    public void mainActivityStillResolvesSupportedMapUrls() {
        Intent intent = new Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://www.google.com/maps?q=48.2082,16.3738")
        );
        intent.addCategory(Intent.CATEGORY_BROWSABLE);
        intent.addCategory(Intent.CATEGORY_DEFAULT);

        assertTrue(resolvesToMainActivity(intent));
    }

    @Test
    public void mainActivityUsesOwnTaskForExternalLaunches() throws PackageManager.NameNotFoundException {
        String packageName = ApplicationProvider.getApplicationContext().getPackageName();
        ComponentName componentName = new ComponentName(packageName, MainActivity.class.getName());

        ActivityInfo activityInfo = ApplicationProvider.getApplicationContext()
                .getPackageManager()
                .getActivityInfo(componentName, 0);

        assertEquals(ActivityInfo.LAUNCH_SINGLE_TASK, activityInfo.launchMode);
    }

    private static boolean resolvesToMainActivity(Intent intent) {
        String packageName = ApplicationProvider.getApplicationContext().getPackageName();
        List<ResolveInfo> activities = ApplicationProvider.getApplicationContext()
                .getPackageManager()
                .queryIntentActivities(intent, 0);
        for (ResolveInfo activity : activities) {
            if (activity.activityInfo == null) {
                continue;
            }
            if (!packageName.equals(activity.activityInfo.packageName)) {
                continue;
            }
            if (MainActivity.class.getName().equals(activity.activityInfo.name)) {
                return true;
            }
        }
        return false;
    }
}

