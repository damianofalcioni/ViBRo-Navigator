package vibro.navigator.main;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.Collections;
import java.util.Locale;

import vibro.navigator.R;
import vibro.navigator.brouter.BRouterProfileParameter;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = {23, 35})
public class ProfileParameterDialogLayoutTest {

    @Test
    public void translatedButtonsRemainVisibleOnNarrowScreensWithLargeText() {
        for (String language : new String[]{"en", "it", "de"}) {
            assertDialogLayout(language, 2f, 320, 480);
        }
    }

    @Test
    public void translatedButtonsRemainVisibleInLandscape() {
        for (String language : new String[]{"en", "it", "de"}) {
            assertDialogLayout(language, 1.3f, 560, 300);
        }
    }

    private static void assertDialogLayout(String language, float fontScale, int widthDp, int heightDp) {
        Activity activity = Robolectric.buildActivity(Activity.class).setup().get();
        Context context = localizedContext(activity, language, fontScale);
        ProfileParameterForm form = ProfileParameterForm.create(context,
                Collections.nCopies(20, new BRouterProfileParameter("avoid_paths", "", "0",
                        BRouterProfileParameter.ValueType.BOOLEAN, Collections.emptyList())),
                Collections.emptyMap());
        AlertDialog dialog = new AlertDialog.Builder(context)
                .setTitle(context.getString(R.string.title_profile_parameters, "Trekking"))
                .setView(form.view())
                .show();
        ViewGroup decor = (ViewGroup) dialog.getWindow().getDecorView();
        int width = Math.round(widthDp * context.getResources().getDisplayMetrics().density);
        int height = Math.round(heightDp * context.getResources().getDisplayMetrics().density);
        decor.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY));
        decor.layout(0, 0, width, height);

        ScrollView scroll = form.view().findViewById(R.id.profileParameterScroll);
        assertTrue(scroll.getHeight() > 0);
        assertTrue(scroll.getChildAt(0).getHeight() > scroll.getHeight());
        TextView explanation = form.view().findViewById(R.id.profileParameterExplanation);
        assertEquals(context.getString(R.string.msg_profile_parameters_not_localized), explanation.getText());
        assertEquals(0, ((ViewGroup) explanation.getParent()).indexOfChild(explanation));
        assertActionsVisible(decor, scroll);
        scroll.fullScroll(View.FOCUS_DOWN);
        assertActionsVisible(decor, scroll);
        dialog.dismiss();
        activity.finish();
    }

    private static void assertActionsVisible(ViewGroup decor, ScrollView scroll) {
        Rect scrollBounds = bounds(decor, scroll);
        Rect resetBounds = bounds(decor, decor.findViewById(R.id.profileParameterResetButton));
        Rect cancelBounds = bounds(decor, decor.findViewById(R.id.profileParameterCancelButton));
        Rect saveBounds = bounds(decor, decor.findViewById(R.id.profileParameterSaveButton));
        assertEquals(resetBounds.right, saveBounds.right);
        assertEquals(cancelBounds.top, saveBounds.top);
        assertTrue(resetBounds.bottom <= cancelBounds.top);
        assertTrue(cancelBounds.right <= saveBounds.left);
        for (int id : new int[]{R.id.profileParameterResetButton,
                R.id.profileParameterCancelButton, R.id.profileParameterSaveButton}) {
            Button button = decor.findViewById(id);
            Rect buttonBounds = bounds(decor, button);
            assertTrue(buttonBounds.top >= scrollBounds.bottom);
            assertTrue(buttonBounds.bottom <= decor.getHeight());
            assertTrue(buttonBounds.left >= 0);
            assertTrue(buttonBounds.right <= decor.getWidth());
            assertTrue(button.getHeight() > 0);
            assertTrue(button.getLayout().getHeight() <= button.getHeight()
                    - button.getCompoundPaddingTop() - button.getCompoundPaddingBottom());
        }
    }

    private static Rect bounds(ViewGroup decor, View view) {
        Rect bounds = new Rect(0, 0, view.getWidth(), view.getHeight());
        decor.offsetDescendantRectToMyCoords(view, bounds);
        return bounds;
    }

    private static Context localizedContext(Activity activity, String language, float fontScale) {
        Configuration configuration = new Configuration(activity.getResources().getConfiguration());
        configuration.setLocale(Locale.forLanguageTag(language));
        configuration.fontScale = fontScale;
        return new ContextThemeWrapper(activity.createConfigurationContext(configuration),
                R.style.Theme_ViBRoNavigator);
    }
}
