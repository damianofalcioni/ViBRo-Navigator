package vibro.navigator.android.language;

import android.app.Activity;
import android.content.Context;

/** Applies the saved language before view inflation and refreshes screens returning from settings. */
public abstract class AndroidLocalizedActivity extends Activity {
    private String appliedLanguageTag;

    @Override
    protected void attachBaseContext(Context base) {
        appliedLanguageTag = AndroidAppLanguage.effectiveLanguageTag(base);
        super.attachBaseContext(AndroidAppLanguage.activityContext(base));
    }

    @Override
    protected void onResume() {
        super.onResume();
        recreateIfLanguageChanged();
    }

    public final void recreateIfLanguageChanged() {
        if (!isFinishing() && !appliedLanguageTag.equals(AndroidAppLanguage.effectiveLanguageTag(this))) {
            recreate();
        }
    }
}
