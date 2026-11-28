package be.maarten.woolworks.item;

import be.maarten.woolworks.registry.ModComponents;
import be.maarten.woolworks.resource.ResourceTypes;
import java.util.function.Consumer;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;

public class ResourceItem extends Item {
    public ResourceItem(Properties properties) { super(properties); }

    @Override public Component getName(ItemStack stack) {
        var id = stack.get(ModComponents.RESOURCE_TYPE);
        if (id == null) return super.getName(stack);
        String translationKey = "resource." + id.getNamespace() + "." + id.getPath();
        if (FMLEnvironment.getDist() == Dist.CLIENT) {
            translationKey = be.maarten.woolworks.client.ClientResourceNames.translationKey(id).orElse(translationKey);
        }
        return Component.translatable(getDescriptionId() + ".named", Component.translatable(translationKey));
    }

    @Override public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> lines, TooltipFlag flag) {
        var id = stack.get(ModComponents.RESOURCE_TYPE);
        if (id == null) return;
        lines.accept(Component.literal(id.toString()).withStyle(ChatFormatting.GRAY));
        if (context.registries() != null) {
            context.registries().lookup(ResourceTypes.REGISTRY).flatMap(reg -> reg.get(net.minecraft.resources.ResourceKey.create(ResourceTypes.REGISTRY, id)))
                    .ifPresent(holder -> lines.accept(Component.translatable(holder.value().translationKey()).withStyle(ChatFormatting.GRAY)));
        }
    }
}
