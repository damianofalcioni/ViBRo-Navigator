package vibro.navigator.nav.compass;

import androidx.annotation.NonNull;

import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

/** Immutable user visibility mask applied in addition to the speed-based street filter. */
public final class CompassStreetVisibility {
    @NonNull
    private final EnumSet<CompassStreetCategory> disabledCategories;
    @NonNull
    private final EnumSet<CompassStreetType> disabledTypes;

    public CompassStreetVisibility(
            @NonNull Set<CompassStreetCategory> disabledCategories,
            @NonNull Set<CompassStreetType> disabledTypes
    ) {
        this.disabledCategories = copyCategories(disabledCategories);
        this.disabledTypes = copyTypes(disabledTypes);
    }

    @NonNull
    public static CompassStreetVisibility all() {
        return new CompassStreetVisibility(
                EnumSet.noneOf(CompassStreetCategory.class),
                EnumSet.noneOf(CompassStreetType.class)
        );
    }

    public boolean isCategoryEnabled(@NonNull CompassStreetCategory category) {
        return !disabledCategories.contains(category);
    }

    public boolean isTypeEnabled(@NonNull CompassStreetType type) {
        return !disabledTypes.contains(type);
    }

    public boolean isVisible(@NonNull CompassStreetType type) {
        return isCategoryEnabled(type.category()) && isTypeEnabled(type);
    }

    @NonNull
    public EnumSet<CompassStreetCategory> disabledCategories() {
        return copyCategories(disabledCategories);
    }

    @NonNull
    public EnumSet<CompassStreetType> disabledTypes() {
        return copyTypes(disabledTypes);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof CompassStreetVisibility)) {
            return false;
        }
        CompassStreetVisibility that = (CompassStreetVisibility) other;
        return disabledCategories.equals(that.disabledCategories)
                && disabledTypes.equals(that.disabledTypes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(disabledCategories, disabledTypes);
    }

    @NonNull
    private static EnumSet<CompassStreetCategory> copyCategories(
            @NonNull Set<CompassStreetCategory> values
    ) {
        return values.isEmpty()
                ? EnumSet.noneOf(CompassStreetCategory.class)
                : EnumSet.copyOf(values);
    }

    @NonNull
    private static EnumSet<CompassStreetType> copyTypes(@NonNull Set<CompassStreetType> values) {
        return values.isEmpty()
                ? EnumSet.noneOf(CompassStreetType.class)
                : EnumSet.copyOf(values);
    }
}
