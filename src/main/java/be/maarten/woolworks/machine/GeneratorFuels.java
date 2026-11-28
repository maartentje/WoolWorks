package be.maarten.woolworks.machine;

import be.maarten.woolworks.recipe.MachineRecipeLookup;
import java.util.Optional;
import java.util.function.BiFunction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public final class GeneratorFuels {
    public record Fuel(int ticks, int energyPerTick) {}
    private static BiFunction<Level, ItemStack, Optional<Fuel>> clientLookup;
    private GeneratorFuels() {}

    public static void setClientLookup(BiFunction<Level, ItemStack, Optional<Fuel>> lookup) { clientLookup = lookup; }

    public static Optional<Fuel> find(Level level, ItemStack stack) {
        if (stack.isEmpty()) return Optional.empty();
        if (level instanceof ServerLevel server) return MachineRecipeLookup.serverFuel(server, stack)
                .map(holder -> new Fuel(holder.value().burnTicks(), holder.value().energyPerTick()));
        if (level.isClientSide() && clientLookup != null) return clientLookup.apply(level, stack);
        return Optional.empty();
    }
}
