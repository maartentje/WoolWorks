package be.maarten.woolworks.recipe;

import java.util.Optional;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

final class RecipeNetwork {
    private RecipeNetwork() {}
    static void writeOptionalId(RegistryFriendlyByteBuf buffer, Optional<ResourceLocation> id) {
        buffer.writeBoolean(id.isPresent());
        id.ifPresent(value -> ResourceLocation.STREAM_CODEC.encode(buffer, value));
    }
    static Optional<ResourceLocation> readOptionalId(RegistryFriendlyByteBuf buffer) {
        return buffer.readBoolean() ? Optional.of(ResourceLocation.STREAM_CODEC.decode(buffer)) : Optional.empty();
    }
}
