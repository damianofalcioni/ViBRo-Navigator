package vibro.navigator.main;

import vibro.navigator.R;

import android.view.View;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;

final class MainActivityStopVoiceButtons {
    private MainActivityStopVoiceButtons() {
    }

    static void setVisible(@NonNull LinearLayout stopsContainer, boolean visible) {
        int visibility = visible ? View.VISIBLE : View.GONE;
        for (int index = 0; index < stopsContainer.getChildCount(); index++) {
            stopsContainer.getChildAt(index).findViewById(R.id.stopVoiceButton).setVisibility(visibility);
        }
    }
}
