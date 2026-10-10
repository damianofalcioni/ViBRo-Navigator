package vibro.navigator.android.storage;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import vibro.navigator.R;
import vibro.navigator.settings.AppOutputFolderSettings.Kind;
import vibro.navigator.settings.AppOutputStorageSettings;

/** Android permission UI and request codes for the selected output folder. */
final class AndroidOutputPermissionPrompt {
    interface Active {
        boolean isActive();
    }
    static final int PHONE_REQUEST = 4100;
    static final int CARD_REQUEST = 4110;

    private AndroidOutputPermissionPrompt() { }

    static boolean show(Activity activity, Kind kind, Runnable denied, Runnable launching, Active active) {
        if (!AppOutputStorageSettings.useSdCard(activity)) {
            AndroidLegacyExternalStorageAccess.requestOutputPermission(activity, PHONE_REQUEST + kind.ordinal());
            return true;
        }
        AndroidOutputStorageVolumes.Card card = AndroidOutputStorageVolumes.find(activity,
                AppOutputStorageSettings.cardId(activity));
        if (card == null) {
            denied.run();
            return false;
        }
        new AlertDialog.Builder(activity).setTitle(R.string.label_external_storage_enabled)
                .setMessage(activity.getString(R.string.hint_sd_download_access, card.root + "/Download"))
                .setNegativeButton(android.R.string.cancel, (dialog, which) -> denied.run())
                .setOnCancelListener(dialog -> denied.run())
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    if (active.isActive()) {
                        launch(activity, kind, card.id, denied, launching);
                    }
                }).show();
        return false;
    }

    private static void launch(Activity activity, Kind kind, String id, Runnable denied, Runnable launching) {
        try {
            launching.run();
            activity.startActivityForResult(AndroidOutputStorageAccess.picker(id), CARD_REQUEST + kind.ordinal());
        } catch (ActivityNotFoundException | SecurityException e) {
            denied.run();
        }
    }

    static Kind kindFor(int code, int base) {
        int index = code - base;
        return index >= 0 && index < Kind.values().length ? Kind.values()[index] : null;
    }
}
