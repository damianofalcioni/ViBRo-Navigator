package vibro.navigator.brouter;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** Records each accepted parent and its independently verified children. */
public final class BRouterFolderGrant {
    private static final String LATEST = "brouter_tree_uri";
    private static final String PARENTS = "brouter_granted_parents";
    private static final String REVISION = "brouter_folder_revision";
    private static final String DIRECT = ".direct";
    private static final String STREET_ONLY = "brouter_street_only.";

    private BRouterFolderGrant() { }

    @Nullable
    public static Uri get(Context context) {
        return uri(prefs(context), LATEST);
    }

    @Nullable
    public static Uri parentFor(Context context, boolean profiles) {
        SharedPreferences settings = prefs(context);
        if (uri(settings, directoryKey(profiles)) == null) {
            return null;
        }
        Uri parent = uri(settings, parentKey(profiles));
        return parent == null ? get(context) : parent;
    }

    @Nullable
    public static Uri directory(Context context, boolean profiles) {
        return uri(prefs(context), directoryKey(profiles));
    }

    @Nullable
    public static Uri directory(Context context, Uri parent, boolean profiles) {
        Uri child = uri(prefs(context), childKey(parent, profiles));
        return child == null && parent.equals(parentFor(context, profiles)) ? directory(context, profiles) : child;
    }

    public static boolean isDirect(Context context, Uri parent, boolean profiles) {
        return prefs(context).getBoolean(childKey(parent, profiles) + DIRECT, false);
    }

    public static List<Uri> parents(Context context) {
        var parents = new LinkedHashSet<Uri>();
        addParent(parents, get(context));
        var stored = new android.text.TextUtils.SimpleStringSplitter('\n');
        stored.setString(prefs(context).getString(PARENTS, ""));
        for (String parent : stored) {
            parents.add(Uri.parse(parent));
        }
        addParent(parents, parentFor(context, true));
        addParent(parents, parentFor(context, false));
        return new ArrayList<>(parents);
    }

    private static void addParent(LinkedHashSet<Uri> parents, @Nullable Uri parent) {
        if (parent != null) {
            parents.add(parent);
        }
    }

    public static void save(Context context, Uri parent, @Nullable Uri profiles, @Nullable Uri segments) {
        save(context, parent, new Child(profiles, false, true), new Child(segments, false, true),
                true, SelectionKind.SHARED);
    }

    public record Child(@Nullable Uri uri, boolean direct, boolean prefer) { }

    public enum SelectionKind {
        SHARED, STREETS, RECHECK;

        void updateScope(SharedPreferences.Editor edit, Uri parent, boolean newParent) {
            if (this == SHARED) {
                edit.remove(STREET_ONLY + parent);
            } else if (this == STREETS && newParent) {
                edit.putBoolean(STREET_ONLY + parent, true);
            }
        }

        boolean confirmsStreetAccess(Child segments) {
            return this != RECHECK && segments.uri() != null;
        }
    }

    public static boolean isStreetOnly(Context context, Uri parent) {
        return prefs(context).getBoolean(STREET_ONLY + parent, false);
    }

    public static void save(Context context, Uri parent, Child profiles, Child segments, boolean latest,
                            SelectionKind selectionKind) {
        List<Uri> parents = parents(context);
        boolean newParent = !parents.contains(parent);
        if (newParent) {
            parents.add(0, parent);
        }
        SharedPreferences.Editor edit = prefs(context).edit();
        selectionKind.updateScope(edit, parent, newParent);
        edit.putString(PARENTS, android.text.TextUtils.join("\n", parents));
        if (latest) {
            edit.putString(LATEST, parent.toString());
        }
        putChild(context, edit, parent, profiles, true);
        boolean segmentChanged = putChild(context, edit, parent, segments, false);
        edit.putString(REVISION, UUID.randomUUID().toString());
        if (segmentChanged || selectionKind.confirmsStreetAccess(segments)) {
            BRouterSegmentSourceSettings.applySourceChange(edit);
        } else {
            edit.apply();
        }
    }

    private static boolean putChild(Context context, SharedPreferences.Editor edit, Uri parent,
                                    Child child, boolean profiles) {
        if (child.uri() == null) {
            Uri previous = parentFor(context, profiles);
            if (previous != null) {
                edit.putString(parentKey(profiles), previous.toString());
            }
            return false;
        }
        String key = childKey(parent, profiles);
        boolean changed = !Objects.equals(child.uri(), directory(context, parent, profiles))
                || child.direct() != isDirect(context, parent, profiles);
        edit.putString(key, child.uri().toString()).putBoolean(key + DIRECT, child.direct());
        if (child.prefer()) {
            changed |= !parent.equals(parentFor(context, profiles));
            edit.putString(parentKey(profiles), parent.toString());
            edit.putString(directoryKey(profiles), child.uri().toString());
        }
        return changed;
    }

    public static Runnable observe(Context context, Runnable onChanged) {
        return BRouterSegmentSourceSettings.observeKey(context, REVISION, onChanged);
    }

    @Nullable
    static Uri resolve(Context context, @Nullable Uri legacy, boolean profiles, GrantCheck check) {
        return new Candidates(context, check).first(sourceCandidates(context, legacy, profiles));
    }

    static List<Uri> discoveryTrees(Context context, @Nullable Uri legacy, boolean profiles, GrantCheck check) {
        var granted = new ArrayList<Uri>();
        for (Uri tree : sourceCandidates(context, legacy, profiles)) {
            if (check.isGranted(context, tree)) {
                granted.add(tree);
            }
        }
        return granted;
    }

    static LinkedHashSet<Uri> sourceCandidates(Context context, @Nullable Uri legacy, boolean profiles) {
        var candidates = new LinkedHashSet<Uri>();
        addParent(candidates, parentFor(context, profiles));
        candidates.addAll(parents(context));
        addParent(candidates, legacy);
        return candidates;
    }

    private static String parentKey(boolean profiles) {
        return profiles ? "brouter_profiles_tree_uri" : "brouter_segments_tree_uri";
    }

    private static String directoryKey(boolean profiles) {
        return profiles ? "brouter_profiles_uri" : "brouter_segments_uri";
    }

    private static String childKey(Uri parent, boolean profiles) {
        return directoryKey(profiles) + "." + parent;
    }

    @Nullable
    private static Uri uri(SharedPreferences prefs, String key) {
        String value = prefs.getString(key, null);
        return value == null ? null : Uri.parse(value);
    }

    private static SharedPreferences prefs(Context context) {
        return BRouterSegmentSourceSettings.prefs(context);
    }

    private record Candidates(Context context, GrantCheck check) {
        @Nullable
        Uri first(Iterable<Uri> candidates) {
            for (Uri candidate : candidates) {
                if (check.isGranted(context, candidate)) {
                    return candidate;
                }
            }
            return null;
        }
    }

    interface GrantCheck {
        boolean isGranted(Context context, @Nullable Uri uri);
    }
}
