package vibro.navigator.nav.directions;

import androidx.annotation.NonNull;

import vibro.navigator.R;
import vibro.navigator.nav.route.VoiceHint;

public final class VoiceHintMapper {
    private static final DirectionSpec UNKNOWN_DIRECTION =
            new DirectionSpec("?", R.string.direction_unknown, DirectionKind.OTHER);
    private static final DirectionSpec[] COMMAND_DIRECTIONS = buildCommandDirections();

    private VoiceHintMapper() {
    }

    @NonNull
    public static DirectionInfo toDirection(@NonNull VoiceHint hint) {
        DirectionSpec spec = hint.command >= 0 && hint.command < COMMAND_DIRECTIONS.length
                ? COMMAND_DIRECTIONS[hint.command]
                : null;
        if (spec == null) {
            spec = UNKNOWN_DIRECTION;
        }
        int exitNumber = spec.useAbsoluteExitNumber ? Math.abs(hint.exitNumber) : hint.exitNumber;
        return new DirectionInfo(spec.emoji, spec.labelRes, spec.usesExitNumber ? exitNumber : 0, spec.kind);
    }

    @NonNull
    private static DirectionSpec[] buildCommandDirections() {
        DirectionSpec[] directions = new DirectionSpec[101];
        directions[1] = new DirectionSpec("⬆", R.string.direction_continue, DirectionKind.STRAIGHT);
        directions[2] = new DirectionSpec("←", R.string.direction_turn_left, DirectionKind.LEFT);
        directions[3] = new DirectionSpec("↖", R.string.direction_slight_left, DirectionKind.LEFT);
        directions[4] = new DirectionSpec("<←", R.string.direction_sharp_left, DirectionKind.LEFT);
        directions[5] = new DirectionSpec("→", R.string.direction_turn_right, DirectionKind.RIGHT);
        directions[6] = new DirectionSpec("↗", R.string.direction_slight_right, DirectionKind.RIGHT);
        directions[7] = new DirectionSpec("→>", R.string.direction_sharp_right, DirectionKind.RIGHT);
        directions[8] = new DirectionSpec("↖", R.string.direction_keep_left, DirectionKind.LEFT);
        directions[9] = new DirectionSpec("↗", R.string.direction_keep_right, DirectionKind.RIGHT);
        directions[10] = new DirectionSpec("U↙", R.string.direction_uturn_left, DirectionKind.LEFT);
        directions[11] = new DirectionSpec("U↘", R.string.direction_uturn_right, DirectionKind.RIGHT);
        directions[12] = new DirectionSpec("!!×", R.string.direction_offroute, DirectionKind.OTHER);
        directions[13] = new DirectionSpec("○>", R.string.direction_roundabout_exit, DirectionKind.RIGHT, true, false);
        directions[14] = new DirectionSpec("<○", R.string.direction_roundabout_exit_left, DirectionKind.LEFT, true, true);
        directions[15] = new DirectionSpec("U↓", R.string.direction_uturn, DirectionKind.OTHER);
        directions[16] = new DirectionSpec("◉", R.string.direction_beeline, DirectionKind.OTHER);
        directions[17] = new DirectionSpec("←", R.string.direction_exit_left, DirectionKind.LEFT);
        directions[18] = new DirectionSpec("→", R.string.direction_exit_right, DirectionKind.RIGHT);
        directions[100] = new DirectionSpec("■", R.string.direction_arrive, DirectionKind.OTHER);
        return directions;
    }

    private static final class DirectionSpec {
        @NonNull
        final String emoji;
        final int labelRes;
        @NonNull
        final DirectionKind kind;
        final boolean usesExitNumber;
        final boolean useAbsoluteExitNumber;

        DirectionSpec(@NonNull String emoji, int labelRes, @NonNull DirectionKind kind) {
            this(emoji, labelRes, kind, false, false);
        }

        DirectionSpec(
                @NonNull String emoji,
                int labelRes,
                @NonNull DirectionKind kind,
                boolean usesExitNumber,
                boolean useAbsoluteExitNumber
        ) {
            this.emoji = emoji;
            this.labelRes = labelRes;
            this.kind = kind;
            this.usesExitNumber = usesExitNumber;
            this.useAbsoluteExitNumber = useAbsoluteExitNumber;
        }
    }
}
