package vibro.navigator.main;

import static org.junit.Assert.assertEquals;

import android.app.Activity;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;

import vibro.navigator.R;

@RunWith(RobolectricTestRunner.class)
public class ProfileSpinnerAdapterTest {

    private static final String PROFILE_TREKKING = "trekking";

    @Test
    public void getDropDownView_showsInfoButtonForBundledProfile() {
        Activity activity = Robolectric.buildActivity(Activity.class).setup().get();
        ProfileSpinnerAdapter adapter = new ProfileSpinnerAdapter(activity);
        adapter.add(new ProfileSpinnerOption(PROFILE_TREKKING, PROFILE_TREKKING, false));

        View row = adapter.getDropDownView(0, null, null);
        ImageButton infoButton = row.findViewById(R.id.profileInfoButton);
        ImageView attentionIcon = row.findViewById(R.id.profileAttentionIcon);

        assertEquals(View.VISIBLE, infoButton.getVisibility());
        assertEquals(View.GONE, attentionIcon.getVisibility());
    }

    @Test
    public void getDropDownView_keepsInfoButtonFromStealingRowSelection() {
        Activity activity = Robolectric.buildActivity(Activity.class).setup().get();
        ProfileSpinnerAdapter adapter = new ProfileSpinnerAdapter(activity);
        adapter.add(new ProfileSpinnerOption(PROFILE_TREKKING, PROFILE_TREKKING, false));

        View row = adapter.getDropDownView(0, null, null);
        ImageButton infoButton = row.findViewById(R.id.profileInfoButton);

        assertEquals(false, infoButton.isClickable());
        assertEquals(false, infoButton.isFocusable());
        assertEquals(false, infoButton.isFocusableInTouchMode());
    }

    @Test
    public void getDropDownView_showsAttentionIconForExperimentalProfile() {
        Activity activity = Robolectric.buildActivity(Activity.class).setup().get();
        ProfileSpinnerAdapter adapter = new ProfileSpinnerAdapter(activity);
        adapter.add(new ProfileSpinnerOption("moped", "moped", false));

        View row = adapter.getDropDownView(0, null, null);
        ImageView attentionIcon = row.findViewById(R.id.profileAttentionIcon);

        assertEquals(View.VISIBLE, attentionIcon.getVisibility());
    }

    @Test
    public void getDropDownView_hidesInfoButtonForCustomProfile() {
        Activity activity = Robolectric.buildActivity(Activity.class).setup().get();
        ProfileSpinnerAdapter adapter = new ProfileSpinnerAdapter(activity);
        adapter.add(new ProfileSpinnerOption("Custom", null, true));

        View row = adapter.getDropDownView(0, null, null);
        ImageButton infoButton = row.findViewById(R.id.profileInfoButton);

        assertEquals(View.GONE, infoButton.getVisibility());
    }
}
