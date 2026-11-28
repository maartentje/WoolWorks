package be.maarten.woolworks.recipe;

import java.util.Optional;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.Identifier;

final class RecipeNetwork {
    private RecipeNetwork() {}
    static void writeOptionalId(RegistryFriendlyByteBuf buffer, Optional<Identifier> id) {
        buffer.writeBoolean(id.isPresent());
        id.ifPresent(value -> Identifier.STREAM_CODEC.encode(buffer, value));
    }
    static Optional<Identifier> readOptionalId(RegistryFriendlyByteBuf buffer) {
        return buffer.readBoolean() ? Optional.of(Identifier.STREAM_CODEC.decode(buffer)) : Optional.empty();
    }
}
