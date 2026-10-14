package vibro.navigator.android.text;

import android.graphics.Typeface;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;

/** Emphasizes the complete direction symbol without resizing the adjacent metric. */
public final class NavigationInstructionSymbolStyle {
    private static final float SYMBOL_SIZE_SCALE = 1.25f;

    private NavigationInstructionSymbolStyle() {
    }

    public static CharSequence apply(String text, String symbol) {
        int symbolLength = symbol.trim().length();
        if (symbolLength == 0 || !text.startsWith(symbol.trim())) {
            return text;
        }
        SpannableString styled = new SpannableString(text);
        styled.setSpan(new RelativeSizeSpan(SYMBOL_SIZE_SCALE), 0, symbolLength, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        styled.setSpan(new StyleSpan(Typeface.BOLD), 0, symbolLength, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        return styled;
    }
}
