package vibro.navigator.auto;

import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.car.app.CarContext;

import vibro.navigator.about.AboutActivity;
import vibro.navigator.main.MainActivity;

final class ViBRoAutoPhoneLauncher {
    private ViBRoAutoPhoneLauncher() {
    }

    static void openMain(@NonNull CarContext carContext) {
        Intent intent = new Intent(carContext, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        carContext.startActivity(intent);
    }

    static void openSettings(@NonNull CarContext carContext) {
        Intent intent = AboutActivity.settingsIntent(carContext)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        carContext.startActivity(intent);
    }
}
