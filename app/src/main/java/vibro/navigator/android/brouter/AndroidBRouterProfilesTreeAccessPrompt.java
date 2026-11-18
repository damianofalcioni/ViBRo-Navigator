package vibro.navigator.android.brouter;

import android.app.Activity;
import android.net.Uri;
import android.os.CancellationSignal;

import java.util.Collection;
import java.util.Collections;
import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Consumer;

import vibro.navigator.android.storage.AndroidDocumentAccess;
import vibro.navigator.brouter.BRouterFolderGrant;
import vibro.navigator.brouter.BRouterProfilesRepository;

public final class AndroidBRouterProfilesTreeAccessPrompt {
    private AndroidBRouterProfilesTreeAccessPrompt() {
    }

    public static void findCustomProfileLocation(Activity activity, BRouterProfilesRepository repository,
                                                 Consumer<Uri> onResult) {
        var app = activity.getApplicationContext();
        AndroidBRouterFolderDiscovery.find(signal -> repository.getCustomProfilePickerInitialUri(app), null, onResult);
    }

    public static void show(
            @NonNull Activity activity,
            @NonNull BRouterProfilesRepository profilesRepository,
            int requestCode,
            @NonNull String logTag,
            @NonNull Runnable beforeLaunch,
            @NonNull Runnable onCancel
    ) {
        AndroidBRouterDeferredPrompt.request(activity, requestCode, false,
                (signal, progress) -> findLocation(activity, profilesRepository, Collections.emptySet(), signal, progress),
                location -> AndroidBRouterFolderPrompt.show(activity, location.initialUri, requestCode, logTag,
                        beforeLaunch, onCancel, onCancel));
    }

    @NonNull
    static AndroidBRouterPickerLocation findLocation(
            @NonNull Activity activity,
            @NonNull BRouterProfilesRepository profilesRepository,
            Collection<String> excluded,
            CancellationSignal signal,
            Consumer<AndroidBRouterPickerLocation> progress
    ) {
        return findLocation(activity, profilesRepository, excluded, signal, progress, null);
    }

    static AndroidBRouterPickerLocation findLocation(
            Activity activity, BRouterProfilesRepository profilesRepository, Collection<String> excluded,
            CancellationSignal signal, Consumer<AndroidBRouterPickerLocation> progress,
            @Nullable AndroidBRouterFolderAccess.Kind needed
    ) {
        Uri savedParent = needed == null ? savedParent(activity, excluded) : null;
        if (savedParent != null) {
            return AndroidBRouterPickerLocation.confirmed(savedParent);
        }
        Uri savedProfileHint = profilesRepository.getCustomProfileUri(activity);
        Uri hint = profilesRepository.getProfilesTreePickerHint(activity, savedProfileHint);
        String id = hint != null && AndroidDocumentAccess.isExternalStorageDocument(hint)
                ? AndroidDocumentAccess.documentId(hint) : null;
        Uri parent = parentHint(id);
        return AndroidBRouterPickerLocation.resolve(
                activity, parent, AndroidBRouterFolderAccess.FOLDER, needed == null ? null : needed.folder,
                excluded, signal, progress, needed != null);
    }

    @Nullable
    static Uri savedParent(Activity activity, Collection<String> excluded) {
        Uri tree = BRouterFolderGrant.get(activity);
        if (tree == null || !AndroidBRouterFolderAccess.hasUsableParent(activity, tree)) {
            return null;
        }
        Uri document = parentDocument(tree);
        return document != null && !excluded.contains(document.toString()) ? document : null;
    }

    @Nullable
    private static Uri parentDocument(Uri tree) {
        if (!AndroidDocumentAccess.isExternalStorageDocument(tree)) {
            return AndroidDocumentAccess.buildTreeDocumentUri(tree);
        }
        String id = AndroidDocumentAccess.treeDocumentId(tree);
        return id != null && AndroidBRouterDirectoryCheck.isSelectableDocumentId(id)
                ? AndroidDocumentAccess.buildExternalStorageDocumentUri(id) : null;
    }

    @Nullable
    private static Uri parentHint(@Nullable String id) {
        if (id == null) {
            return null;
        }
        String normalized = id.toLowerCase(Locale.ROOT);
        if (normalized.endsWith("/brouter/profiles2") || normalized.endsWith(":brouter/profiles2")) {
            return AndroidDocumentAccess.buildExternalStorageDocumentUri(id.substring(0, id.lastIndexOf('/')));
        }
        return null;
    }
}
