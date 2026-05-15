package vibro.navigator.auto;

import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.car.app.Screen;
import androidx.car.app.Session;

public final class ViBRoCarSession extends Session {

    private ViBRoCarScreen screen;

    @Override
    @NonNull
    public Screen onCreateScreen(@NonNull Intent intent) {
        screen = new ViBRoCarScreen(getCarContext());
        screen.handleIntent(intent);
        return screen;
    }

    @Override
    public void onNewIntent(@NonNull Intent intent) {
        if (screen != null) {
            screen.handleIntent(intent);
        }
    }
}
