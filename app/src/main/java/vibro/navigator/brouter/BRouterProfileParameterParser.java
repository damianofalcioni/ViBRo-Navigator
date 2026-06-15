package vibro.navigator.brouter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class BRouterProfileParameterParser {

    private static final String ASSIGN = "assign";
    private static final String TURN_INSTRUCTION_MODE = "turnInstructionMode";

    @NonNull
    public List<BRouterProfileParameter> parse(@Nullable String profileText) {
        if (profileText == null || profileText.trim().isEmpty()) {
            return Collections.emptyList();
        }
        List<BRouterProfileParameter> out = new ArrayList<>();
        String[] lines = profileText.split("\\r?\\n");
        for (String line : lines) {
            BRouterProfileParameter parameter = parseLine(line);
            if (parameter != null) {
                out.add(parameter);
            }
        }
        return out;
    }

    @Nullable
    private BRouterProfileParameter parseLine(@Nullable String line) {
        BRouterProfileParameterAnnotation annotation = BRouterProfileParameterAnnotation.fromLine(line);
        if (annotation == null || TURN_INSTRUCTION_MODE.equals(annotation.name)) {
            return null;
        }

        String defaultValue = parseDefaultValue(annotation.assignment, annotation.name);
        if (defaultValue == null) {
            return null;
        }

        return new BRouterProfileParameter(
                annotation.name,
                annotation.description,
                defaultValue,
                BRouterProfileParameterTypeParser.valueType(annotation.rawType),
                BRouterProfileParameterTypeParser.options(annotation.rawType)
        );
    }

    @Nullable
    private static String parseDefaultValue(@NonNull String assignment, @NonNull String parameterName) {
        String[] tokens = assignment.trim().split(" +");
        if (tokens.length < 3 || !ASSIGN.equals(tokens[0]) || !parameterName.equals(tokens[1])) {
            return null;
        }
        if ("=".equals(tokens[2])) {
            return tokens.length > 3 ? tokens[3] : null;
        }
        return tokens[2];
    }
}
