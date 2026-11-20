package vibro.navigator.brouter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

    public Set<String> overridableNames(String profileText) {
        return globalAssignments(profileText).keySet();
    }

    Map<String, String> globalAssignments(String profileText) {
        String[] tokens = profileText.replaceAll("(?m)#.*", "").trim().split("\\s+");
        Map<String, String> assignments = new LinkedHashMap<>();
        boolean global = true;
        for (int i = 0; i < tokens.length; i++) {
            String token = tokens[i];
            if (token.startsWith("---context:")) {
                global = "---context:global".equals(token);
            } else if (global && ASSIGN.equals(token)) {
                recordAssignment(tokens, i + 1, assignments);
            }
        }
        return assignments;
    }

    private static void recordAssignment(String[] tokens, int name, Map<String, String> assignments) {
        if (name + 1 >= tokens.length || TURN_INSTRUCTION_MODE.equals(tokens[name])) {
            return;
        }
        int value = "=".equals(tokens[name + 1]) ? name + 2 : name + 1;
        if (value < tokens.length && !assignments.containsKey(tokens[name])) {
            assignments.put(tokens[name], tokens[value]);
        }
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
