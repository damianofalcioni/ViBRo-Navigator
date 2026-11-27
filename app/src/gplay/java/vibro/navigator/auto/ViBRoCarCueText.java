package vibro.navigator.auto;

import androidx.car.app.model.CarText;

final class ViBRoCarCueText {
    private ViBRoCarCueText() { }

    static CarText withVariants(CharSequence fullCue, CharSequence... shorterCues) {
        var builder = new CarText.Builder(fullCue);
        int previousLength = fullCue.length();
        for (CharSequence cue : shorterCues) {
            if (cue.length() < previousLength) {
                builder.addVariant(cue);
                previousLength = cue.length();
            }
        }
        return builder.build();
    }
}
