package com.vibenavigator.nav.directions;

import androidx.annotation.NonNull;

import com.vibenavigator.R;
import com.vibenavigator.nav.route.VoiceHint;

public final class VoiceHintMapper {

    private VoiceHintMapper() {
    }

    @NonNull
    public static DirectionInfo toDirection(@NonNull VoiceHint hint) {
        int cmd = hint.command;
        switch (cmd) {
            case 1:
                return new DirectionInfo("⬆️", R.string.direction_continue, 0, DirectionKind.STRAIGHT);
            case 2:
                return new DirectionInfo("⬅️", R.string.direction_turn_left, 0, DirectionKind.LEFT);
            case 3:
                return new DirectionInfo("↖️", R.string.direction_slight_left, 0, DirectionKind.LEFT);
            case 4:
                return new DirectionInfo("⤺", R.string.direction_sharp_left, 0, DirectionKind.LEFT);
            case 5:
                return new DirectionInfo("➡️", R.string.direction_turn_right, 0, DirectionKind.RIGHT);
            case 6:
                return new DirectionInfo("↗️", R.string.direction_slight_right, 0, DirectionKind.RIGHT);
            case 7:
                return new DirectionInfo("⤻", R.string.direction_sharp_right, 0, DirectionKind.RIGHT);
            case 8:
                return new DirectionInfo("↖️", R.string.direction_keep_left, 0, DirectionKind.LEFT);
            case 9:
                return new DirectionInfo("↗️", R.string.direction_keep_right, 0, DirectionKind.RIGHT);
            case 10:
                return new DirectionInfo("↩️", R.string.direction_uturn_left, 0, DirectionKind.LEFT);
            case 11:
                return new DirectionInfo("↪️", R.string.direction_uturn_right, 0, DirectionKind.RIGHT);
            case 15:
                return new DirectionInfo("↩️", R.string.direction_uturn, 0, DirectionKind.OTHER);
            case 12:
                return new DirectionInfo("⚠️", R.string.direction_offroute, 0, DirectionKind.OTHER);
            case 13:
                return new DirectionInfo("🔄", R.string.direction_roundabout_exit, hint.exitNumber, DirectionKind.RIGHT);
            case 14:
                return new DirectionInfo("🔄", R.string.direction_roundabout_exit_left, Math.abs(hint.exitNumber), DirectionKind.LEFT);
            case 16:
                return new DirectionInfo("🎯", R.string.direction_beeline, 0, DirectionKind.OTHER);
            case 17:
                return new DirectionInfo("⬅️", R.string.direction_exit_left, 0, DirectionKind.LEFT);
            case 18:
                return new DirectionInfo("➡️", R.string.direction_exit_right, 0, DirectionKind.RIGHT);
            case 100:
                return new DirectionInfo("🏁", R.string.direction_arrive, 0, DirectionKind.OTHER);
            default:
                return new DirectionInfo("❓", R.string.direction_unknown, 0, DirectionKind.OTHER);
        }
    }
}
