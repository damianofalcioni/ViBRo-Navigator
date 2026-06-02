package vibro.navigator.intent;

import androidx.annotation.NonNull;

import java.io.ByteArrayOutputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

final class IntentUriDecoder {

    private IntentUriDecoder() {
    }

    @NonNull
    static String decodeComponent(@NonNull String value) {
        try {
            return URLDecoder.decode(value, "UTF-8");
        } catch (Exception ignored) {
            return decodePercentEscapesLeniently(value);
        }
    }

    @NonNull
    private static String decodePercentEscapesLeniently(@NonNull String value) {
        StringBuilder out = new StringBuilder(value.length());
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        int index = 0;
        while (index < value.length()) {
            char current = value.charAt(index);
            if (current == '%' && index + 2 < value.length()) {
                int high = hexValue(value.charAt(index + 1));
                int low = hexValue(value.charAt(index + 2));
                if (high >= 0 && low >= 0) {
                    bytes.write((high << 4) + low);
                    index += 3;
                    continue;
                }
            }
            appendPendingBytes(out, bytes);
            out.append(current);
            index++;
        }
        appendPendingBytes(out, bytes);
        return out.toString();
    }

    private static void appendPendingBytes(
            @NonNull StringBuilder out,
            @NonNull ByteArrayOutputStream bytes
    ) {
        if (bytes.size() == 0) {
            return;
        }
        out.append(new String(bytes.toByteArray(), StandardCharsets.UTF_8));
        bytes.reset();
    }

    private static int hexValue(char value) {
        if (value >= '0' && value <= '9') {
            return value - '0';
        }
        if (value >= 'A' && value <= 'F') {
            return value - 'A' + 10;
        }
        if (value >= 'a' && value <= 'f') {
            return value - 'a' + 10;
        }
        return -1;
    }
}
