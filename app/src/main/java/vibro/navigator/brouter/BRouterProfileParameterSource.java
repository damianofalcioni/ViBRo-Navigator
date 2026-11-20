package vibro.navigator.brouter;

import android.content.Context;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import androidx.core.util.Supplier;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

import vibro.navigator.logging.AppLogger;

final class BRouterProfileParameterSource {

    private static final String BROUTER_PROFILES_ZIP = "assets/profiles2.zip";
    private static final String TAG = "BRouterProfileParams";

    @NonNull
    private final BRouterProfileDependencies.DocumentAccess documentAccess;
    @NonNull
    private final BRouterProfileDependencies.PackageAccess packageAccess;
    @NonNull
    private final BRouterProfileParameterParser parser;

    BRouterProfileParameterSource(
            @NonNull BRouterProfileDependencies.DocumentAccess documentAccess,
            @NonNull BRouterProfileDependencies.PackageAccess packageAccess,
            @NonNull BRouterProfileParameterParser parser
    ) {
        this.documentAccess = documentAccess;
        this.packageAccess = packageAccess;
        this.parser = parser;
    }

    @Nullable
    List<BRouterProfileParameter> readParameters(
            @NonNull Context context,
            @Nullable String profileName,
            @Nullable Uri customProfileUri,
            boolean custom,
            Supplier<List<Uri>> namedTrees
    ) {
        String name = BRouterProfileNames.clean(profileName);
        if (name == null) {
            return null;
        }
        if (custom) {
            return customProfileUri == null ? null : readCustomParameters(context, name, customProfileUri);
        }
        List<BRouterProfileParameter> external = readExternalProfileParameters(context, name + ".brf", namedTrees.get());
        return external == null ? readBundledProfileParameters(context, name) : external;
    }

    @Nullable
    private List<BRouterProfileParameter> readCustomParameters(Context context, String name, Uri uri) {
        String profileText = readProfileText(context, uri);
        if (profileText == null) {
            return null;
        }
        var parameters = new ArrayList<>(parser.parse(profileText));
        List<BRouterProfileParameter> bundled = readBundledProfileParameters(context, name);
        if (bundled != null && !bundled.isEmpty()) {
            applyBundledMetadata(parameters, bundled, profileText);
        }
        return parameters;
    }

    private void applyBundledMetadata(List<BRouterProfileParameter> parameters, List<BRouterProfileParameter> bundled,
                                      String profileText) {
        var assignments = parser.globalAssignments(profileText);
        var annotated = new HashSet<String>();
        for (BRouterProfileParameter parameter : parameters) {
            annotated.add(parameter.name);
        }
        for (BRouterProfileParameter metadata : bundled) {
            String value = assignments.get(metadata.name);
            if (value != null && !annotated.contains(metadata.name) && isLiteral(value)) {
                parameters.add(new BRouterProfileParameter(
                        metadata.name, metadata.description, value, metadata.valueType, metadata.options));
            }
        }
    }

    private static boolean isLiteral(String value) {
        if ("true".equals(value) || "false".equals(value)) {
            return true;
        }
        try {
            Float.parseFloat(value);
            return true;
        } catch (NumberFormatException ignored) {
            return false;
        }
    }

    @Nullable
    private List<BRouterProfileParameter> readExternalProfileParameters(
            @NonNull Context context,
            @NonNull String profileFileName,
            @NonNull List<Uri> discoveryTreeUris
    ) {
        for (Uri treeUri : discoveryTreeUris) {
            Uri childUri = documentAccess.childDocumentUri(context, treeUri, profileFileName);
            if (childUri == null) {
                continue;
            }
            List<BRouterProfileParameter> parameters = parseDocumentProfile(context, childUri);
            if (parameters != null) {
                return parameters;
            }
        }
        return null;
    }

    @Nullable
    private List<BRouterProfileParameter> parseDocumentProfile(@NonNull Context context, @NonNull Uri uri) {
        String profileText = readProfileText(context, uri);
        return profileText == null ? null : parser.parse(profileText);
    }

    @Nullable
    private String readProfileText(Context context, Uri uri) {
        try {
            return documentAccess.readText(context, uri);
        } catch (Exception e) {
            AppLogger.w(TAG, "Failed to read profile parameters from document uri=" + uri, e);
            return null;
        }
    }

    @Nullable
    private List<BRouterProfileParameter> readBundledProfileParameters(
            @NonNull Context context,
            @NonNull String profileName
    ) {
        try {
            String sourceDir = packageAccess.sourceDir(context, BRouterProfilesRepository.BROUTER_PACKAGE_NAME);
            if (sourceDir == null) {
                return null;
            }
            try (ZipFile apk = new ZipFile(sourceDir)) {
                ZipEntry profilesZipEntry = apk.getEntry(BROUTER_PROFILES_ZIP);
                if (profilesZipEntry == null) {
                    return null;
                }
                return readBundledProfileParameters(apk, profilesZipEntry, profileName);
            }
        } catch (Exception e) {
            AppLogger.w(TAG, "Failed to read bundled profile parameters profile=" + profileName, e);
            return null;
        }
    }

    @Nullable
    private List<BRouterProfileParameter> readBundledProfileParameters(
            @NonNull ZipFile apk,
            @NonNull ZipEntry profilesZipEntry,
            @NonNull String profileName
    ) throws IOException {
        try (InputStream raw = apk.getInputStream(profilesZipEntry);
             ZipInputStream zip = new ZipInputStream(raw)) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (!entry.isDirectory() && profileName.equals(BRouterProfileLister.normalizeProfileName(entry.getName()))) {
                    return parser.parse(readCurrentZipEntry(zip));
                }
                zip.closeEntry();
            }
        }
        return null;
    }

    @NonNull
    private static String readCurrentZipEntry(@NonNull InputStream input) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int read;
        while ((read = input.read(buffer)) >= 0) {
            out.write(buffer, 0, read);
        }
        return new String(out.toByteArray(), StandardCharsets.UTF_8);
    }
}
