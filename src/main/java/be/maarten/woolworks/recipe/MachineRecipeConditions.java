package be.maarten.woolworks.recipe;

import be.maarten.woolworks.resource.ResourceTypes;
import java.util.Optional;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;

/** Conditions are checked against the world's actual resource registry, not player inventory. */
public final class MachineRecipeConditions {
    private MachineRecipeConditions() {}

    public static boolean matches(Level level, Optional<Identifier> required, Optional<Identifier> absent) {
        return level.registryAccess().lookup(ResourceTypes.REGISTRY).map(registry ->
                required.map(id -> ResourceTypes.enabled(level, id)).orElse(true)
                        && absent.map(id -> !ResourceTypes.enabled(level, id)).orElse(true)).orElse(false);
    }
}
