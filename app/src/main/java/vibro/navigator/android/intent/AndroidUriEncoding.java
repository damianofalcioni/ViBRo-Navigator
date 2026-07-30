package vibro.navigator.android.intent;

import androidx.annotation.NonNull;

import java.nio.charset.StandardCharsets;

final class AndroidUriEncoding {
    private static final char[] HEX = "0123456789ABCDEF".toCharArray();

    private AndroidUriEncoding() {
    }

    @NonNull
    static String encode(@NonNull String value) {
        StringBuilder encoded = new StringBuilder(value.length());
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        for (byte valueByte : bytes) {
            int unsignedByte = valueByte & 0xff;
            if (isUriEncodeAllowed(unsignedByte)) {
                encoded.append((char) unsignedByte);
            } else {
                encoded.append('%');
                encoded.append(HEX[(unsignedByte >> 4) & 0xf]);
                encoded.append(HEX[unsignedByte & 0xf]);
            }
        }
        return encoded.toString();
    }

    private static boolean isUriEncodeAllowed(int value) {
        return isAlphaNumeric(value)
                || value == '_'
                || value == '-'
                || value == '!'
                || value == '.'
                || value == '~'
                || value == '\''
                || value == '('
                || value == ')'
                || value == '*';
    }

    private static boolean isAlphaNumeric(int value) {
        return value >= 'a' && value <= 'z'
                || value >= 'A' && value <= 'Z'
                || value >= '0' && value <= '9';
    }
}
