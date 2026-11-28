package be.maarten.woolworks.resource;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/** A resource type loaded from a datapack and synchronized to connected clients. */
public record ResourceType(String translationKey, int color, int minYield, int maxYield, boolean enabled, java.util.Optional<net.minecraft.resources.Identifier> requiredItemTag, boolean automatic, ResourceCategory category) {
    private static final Codec<ResourceType> FIELDS = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("translation_key").forGetter(ResourceType::translationKey),
            Codec.intRange(0, 0xFFFFFF).fieldOf("color").forGetter(ResourceType::color),
            Codec.intRange(1, 64).fieldOf("min_yield").forGetter(ResourceType::minYield),
            Codec.intRange(1, 64).fieldOf("max_yield").forGetter(ResourceType::maxYield),
            Codec.BOOL.optionalFieldOf("enabled", true).forGetter(ResourceType::enabled),
            net.minecraft.resources.Identifier.CODEC.optionalFieldOf("required_item_tag").forGetter(ResourceType::requiredItemTag),
            Codec.BOOL.optionalFieldOf("automatic", false).forGetter(ResourceType::automatic),
            ResourceCategory.CODEC.optionalFieldOf("category", ResourceCategory.MATERIALS).forGetter(ResourceType::category)
    ).apply(instance, ResourceType::new));

    public static final Codec<ResourceType> CODEC = FIELDS.flatXmap(ResourceType::validate, ResourceType::validate);

    private static DataResult<ResourceType> validate(ResourceType type) {
        if (type.translationKey().isBlank()) {
            return DataResult.error(() -> "translation_key must not be blank");
        }
        if (type.minYield() > type.maxYield()) {
            return DataResult.error(() -> "min_yield must not exceed max_yield");
        }
        return DataResult.success(type);
    }
}
