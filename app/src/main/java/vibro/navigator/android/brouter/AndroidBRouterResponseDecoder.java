package vibro.navigator.android.brouter;

import android.util.Base64;

import androidx.annotation.NonNull;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPInputStream;

final class AndroidBRouterResponseDecoder {

    private static final String Z64_BASE64_PREFIX = "ejY0";
    private static final int Z64_MARKER_LENGTH = 3;

    private AndroidBRouterResponseDecoder() {
    }

    @NonNull
    static String decode(@NonNull String raw) throws IOException {
        String trimmed = raw.trim();
        if (!trimmed.startsWith(Z64_BASE64_PREFIX)) {
            return raw;
        }
        return decodeZ64(trimmed);
    }

    @NonNull
    private static String decodeZ64(@NonNull String encoded) throws IOException {
        byte[] decoded = Base64.decode(encoded, Base64.DEFAULT);
        ByteArrayInputStream compressedPayload = new ByteArrayInputStream(decoded);
        compressedPayload.skip(Z64_MARKER_LENGTH);
        try (GZIPInputStream gzipInput = new GZIPInputStream(compressedPayload);
             BufferedReader reader = new BufferedReader(new InputStreamReader(gzipInput, StandardCharsets.UTF_8))) {
            StringBuilder result = new StringBuilder();
            char[] buffer = new char[4096];
            int read;
            while ((read = reader.read(buffer)) >= 0) {
                result.append(buffer, 0, read);
            }
            return result.toString();
        }
    }
}
