package vibro.navigator.android.display;

import android.content.Context;
import android.hardware.display.DisplayManager;
import android.view.Display;
import android.view.Surface;

import androidx.annotation.NonNull;

import vibro.navigator.nav.orientation.DisplayRotation;
import vibro.navigator.nav.orientation.DisplayRotationProvider;

public final class AndroidDisplayRotationProvider implements DisplayRotationProvider {
    @NonNull
    private final Context context;

    public AndroidDisplayRotationProvider(@NonNull Context context) {
        this.context = context.getApplicationContext();
    }

    @Override
    public int currentDisplayRotation() {
        DisplayManager displayManager = (DisplayManager) context.getSystemService(Context.DISPLAY_SERVICE);
        if (displayManager == null) {
            return DisplayRotation.ROTATION_0;
        }
        Display defaultDisplay = displayManager.getDisplay(Display.DEFAULT_DISPLAY);
        return defaultDisplay == null ? DisplayRotation.ROTATION_0 : toDisplayRotation(defaultDisplay.getRotation());
    }

    private static int toDisplayRotation(int surfaceRotation) {
        switch (surfaceRotation) {
            case Surface.ROTATION_90:
                return DisplayRotation.ROTATION_90;
            case Surface.ROTATION_180:
                return DisplayRotation.ROTATION_180;
            case Surface.ROTATION_270:
                return DisplayRotation.ROTATION_270;
            case Surface.ROTATION_0:
            default:
                return DisplayRotation.ROTATION_0;
        }
    }
}
