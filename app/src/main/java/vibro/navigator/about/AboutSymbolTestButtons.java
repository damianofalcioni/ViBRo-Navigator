package vibro.navigator.about;

import android.app.Activity;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;

import vibro.navigator.R;
import vibro.navigator.nav.foreground.NavigationNotificationDebugHelper;

final class AboutSymbolTestButtons {

    private final Activity activity;
    private final Button leftButton;
    private final Button otherButton;
    private final Button rightButton;

    AboutSymbolTestButtons(@NonNull Activity activity) {
        this.activity = activity;
        leftButton = activity.findViewById(R.id.aboutSymbolTestLeftButton);
        otherButton = activity.findViewById(R.id.aboutSymbolTestOtherButton);
        rightButton = activity.findViewById(R.id.aboutSymbolTestRightButton);

        leftButton.setOnClickListener(v -> sendLeftNotification());
        otherButton.setOnClickListener(v -> sendOtherNotification());
        rightButton.setOnClickListener(v -> sendRightNotification());
    }

    void show() {
        leftButton.setVisibility(View.VISIBLE);
        otherButton.setVisibility(View.VISIBLE);
        rightButton.setVisibility(View.VISIBLE);
    }

    private void sendLeftNotification() {
        NavigationNotificationDebugHelper.postLeftSymbolTestNotification(activity);
        showSentToast();
    }

    private void sendOtherNotification() {
        NavigationNotificationDebugHelper.postOtherSymbolTestNotification(activity);
        showSentToast();
    }

    private void sendRightNotification() {
        NavigationNotificationDebugHelper.postRightSymbolTestNotification(activity);
        showSentToast();
    }

    private void showSentToast() {
        Toast.makeText(activity, R.string.msg_symbol_test_notification_sent, Toast.LENGTH_SHORT).show();
    }
}
