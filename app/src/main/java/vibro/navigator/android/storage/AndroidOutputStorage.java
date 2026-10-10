package vibro.navigator.android.storage;

import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Locale;

import vibro.navigator.settings.AppOutputFolderSettings.Kind;
import vibro.navigator.settings.AppOutputStorageSettings;

/** Uses only the selected Downloads destination; missing access never redirects output. */
public final class AndroidOutputStorage {
    private static final Map<String, Boolean> FAILED = new ConcurrentHashMap<>();

    private AndroidOutputStorage() { }

    public static AndroidOutputDestination requested(Context context, Kind kind) throws IOException {
        if (!AppOutputStorageSettings.useSdCard(context)) {
            return phone(context, kind);
        }
        AndroidOutputStorageVolumes.Card card = AndroidOutputStorageVolumes.find(context,
                AppOutputStorageSettings.cardId(context));
        if (card == null) {
            throw new IOException("SD card is unavailable");
        }
        return card(context, card, kind, AndroidOutputStorageAccess.existingGrant(context, card));
    }

    public static AndroidOutputDestination card(Context context, AndroidOutputStorageVolumes.Card card,
                                                Kind kind, String grant) throws IOException {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return AndroidOutputDestination.downloads(card.mediaStoreName, card.root.getAbsolutePath(), kind);
        }
        if (grant == null) {
            throw new IOException("SD card folder access is required");
        }
        Uri target = AndroidDocumentAccess.buildExternalStorageTreeUri(
                card.id + ":Download/ViBRo/" + AndroidOutputDestination.subfolder(kind));
        if (!AndroidOutputFolderRecovery.hasGrant(context, target)) {
            throw new IOException("SD card folder permission is missing");
        }
        return AndroidOutputDestination.documents(target, card.root.getAbsolutePath(), kind);
    }

    private static AndroidOutputDestination phone(Context context, Kind kind) throws IOException {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return AndroidOutputDestination.downloads(MediaStore.VOLUME_EXTERNAL_PRIMARY,
                    Environment.getExternalStorageDirectory().getAbsolutePath(), kind);
        }
        if (AndroidLegacyExternalStorageAccess.needsOutputPermission(context)) {
            throw new IOException("Downloads permission is required");
        }
        return AndroidOutputDestination.file(new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOWNLOADS), "ViBRo/" + AndroidOutputDestination.subfolder(kind)));
    }

    public static AndroidOutputDestination current(Context context, Kind kind) {
        String id = AppOutputStorageSettings.cardId(context);
        String root = AppOutputStorageSettings.useSdCard(context) ? "/storage/" + id
                : Environment.getExternalStorageDirectory().getAbsolutePath();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return AndroidOutputDestination.downloads(AppOutputStorageSettings.useSdCard(context)
                    ? id.toLowerCase(Locale.ROOT) : MediaStore.VOLUME_EXTERNAL_PRIMARY, root, kind);
        }
        return AppOutputStorageSettings.useSdCard(context)
                ? AndroidOutputDestination.documents(AndroidDocumentAccess.buildExternalStorageTreeUri(
                        id + ":Download/ViBRo/" + AndroidOutputDestination.subfolder(kind)), root, kind)
                : AndroidOutputDestination.file(new File(Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_DOWNLOADS), "ViBRo/" + AndroidOutputDestination.subfolder(kind)));
    }

    public static boolean hasAccess(Context context, Kind kind) {
        try {
            requested(context, kind);
            return true;
        } catch (IOException | RuntimeException e) {
            return false;
        }
    }

    public static AndroidOutputFile create(Context context, Kind kind, String mime, String name) throws IOException {
        AndroidOutputDestination target = requested(context, kind);
        try {
            return AndroidOutputFile.createWritable(context, target, mime, name);
        } catch (IOException | RuntimeException e) {
            failed(kind, target);
            throw new IOException("Selected output storage is unavailable", e);
        }
    }

    public static void failed(Kind kind, AndroidOutputDestination destination) {
        FAILED.put(kind.name() + "|" + destination.token(), true);
    }

    private static boolean isFailed(Kind kind, AndroidOutputDestination destination) {
        return FAILED.containsKey(kind.name() + "|" + destination.token());
    }

    public static boolean isUsable(Context context, Kind kind) {
        AndroidOutputDestination target = null;
        try {
            target = requested(context, kind);
            if (target.tree != null) {
                target.ensureTree(context);
            }
            if (isFailed(kind, target)) {
                verify(context, target);
                FAILED.remove(kind.name() + "|" + target.token());
            }
            return true;
        } catch (IOException | RuntimeException e) {
            if (target != null) {
                failed(kind, target);
            }
            return false;
        }
    }

    public static void verify(Context context, AndroidOutputDestination target) throws IOException {
        AndroidOutputFile probe = AndroidOutputFile.create(context, target, "text/plain", ".vibro-write-check.txt");
        try (OutputStream stream = probe.open()) {
            stream.write(0);
        } finally {
            probe.removeQuietly();
        }
    }

    public static void resetFailures() {
        FAILED.clear();
    }

    public static String diagnosticLabel(Context context, Kind kind) {
        return AndroidOutputDestination.diagnosticLabel(context, kind);
    }
}
