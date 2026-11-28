package be.maarten.woolworks.recipe;

import be.maarten.woolworks.WoolWorks;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.crafting.RecipeHolder;

/** A single ordered, registry-aware snapshot of both machine recipe types. */
public record MachineRecipesPayload(List<RecipeHolder<?>> fuels, List<RecipeHolder<?>> extraction) implements CustomPacketPayload {
    public static final Type<MachineRecipesPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(WoolWorks.MODID, "machine_recipes"));
    private static final StreamCodec<RegistryFriendlyByteBuf, List<RecipeHolder<?>>> RECIPES = RecipeHolder.STREAM_CODEC.apply(ByteBufCodecs.list());
    public static final StreamCodec<RegistryFriendlyByteBuf, MachineRecipesPayload> STREAM = StreamCodec.composite(
            RECIPES, MachineRecipesPayload::fuels, RECIPES, MachineRecipesPayload::extraction, MachineRecipesPayload::new);
    public MachineRecipesPayload { fuels = List.copyOf(fuels); extraction = List.copyOf(extraction); }
    @Override public Type<MachineRecipesPayload> type() { return TYPE; }
}
