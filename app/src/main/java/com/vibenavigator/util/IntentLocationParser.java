package com.vibenavigator.util;

import android.content.Intent;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class IntentLocationParser {

    private static final Pattern COORDS = Pattern.compile("(-?\\d{1,2}(?:\\.\\d+)?)\\s*,\\s*(-?\\d{1,3}(?:\\.\\d+)?)");

    private IntentLocationParser() {
    }

    @Nullable
    public static String parseToQuery(@NonNull Intent intent) {
        String action = intent.getAction();
        if (Intent.ACTION_VIEW.equals(action)) {
            return parseGeoUri(intent.getData());
        }
        if (Intent.ACTION_SEND.equals(action)) {
            String text = intent.getStringExtra(Intent.EXTRA_TEXT);
            if (text == null) {
                return null;
            }
            Matcher m = COORDS.matcher(text);
            if (m.find()) {
                return m.group(1) + "," + m.group(2);
            }
            return text.trim().isEmpty() ? null : text.trim();
        }
        return null;
    }

    @Nullable
    private static String parseGeoUri(@Nullable Uri uri) {
        if (uri == null) {
            return null;
        }
        if (!"geo".equalsIgnoreCase(uri.getScheme())) {
            return null;
        }

        // Examples:
        // geo:lat,lon
        // geo:0,0?q=lat,lon(label)
        // geo:0,0?q=address
        String schemeSpecific = uri.getSchemeSpecificPart();
        if (schemeSpecific == null) {
            return null;
        }

        String q = uri.getQueryParameter("q");
        if (q != null && !q.trim().isEmpty()) {
            Matcher m = COORDS.matcher(q);
            if (m.find()) {
                return m.group(1) + "," + m.group(2);
            }
            return q.trim();
        }

        Matcher m = COORDS.matcher(schemeSpecific);
        if (m.find()) {
            return m.group(1) + "," + m.group(2);
        }
        return null;
    }
}
