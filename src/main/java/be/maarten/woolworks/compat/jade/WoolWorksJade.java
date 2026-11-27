package be.maarten.woolworks.compat.jade;

import be.maarten.woolworks.WoolWorks;
import be.maarten.woolworks.entity.ResourceSheep;
import be.maarten.woolworks.machine.*;
import java.util.Locale;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import snownee.jade.api.*;
import snownee.jade.api.config.IPluginConfig;

@WailaPlugin
public final class WoolWorksJade implements IWailaPlugin {
    @Override public void register(IWailaCommonRegistration registration) {
        registration.registerBlockDataProvider(Blocks.INSTANCE, GeneratorBlockEntity.class);
        registration.registerBlockDataProvider(Blocks.INSTANCE, ExtractorBlockEntity.class);
        registration.registerBlockDataProvider(Blocks.INSTANCE, WorkerBlockEntity.class);
        registration.registerBlockDataProvider(Blocks.INSTANCE, CableBlockEntity.class);
    }
    @Override public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(Blocks.INSTANCE, GeneratorBlock.class);
        registration.registerBlockComponent(Blocks.INSTANCE, ExtractorBlock.class);
        registration.registerBlockComponent(Blocks.INSTANCE, WorkerBlock.class);
        registration.registerBlockComponent(Blocks.INSTANCE, CableBlock.class);
        registration.registerEntityComponent(Sheep.INSTANCE, ResourceSheep.class);
    }
    private static ResourceLocation id(String name) { return ResourceLocation.fromNamespaceAndPath(WoolWorks.MODID, name); }
    private static String seconds(long ticks) { return String.format(Locale.ROOT, "%.1f", ticks / 20.0); }

    private enum Blocks implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
        INSTANCE;
        @Override public ResourceLocation getUid() { return id("machine_information"); }
        @Override public void appendServerData(CompoundTag tag, BlockAccessor accessor) {
            var entity = accessor.getBlockEntity();
            if (entity instanceof MufflableBlockEntity machine) tag.putString("ww_mode", machine.muffleMode().name());
            if (entity instanceof GeneratorBlockEntity generator) {
                tag.putString("ww_kind", "generator");
                tag.putInt("ww_rate", generator.isProcessing() ? generator.fuelGenerationRate() : 0);
                tag.putLong("ww_ticks", generator.remainingFuelTicks());
                tag.putBoolean("ww_processing", generator.isProcessing());
            } else if (entity instanceof ExtractorBlockEntity extractor) {
                tag.putString("ww_kind", "extractor");
                tag.putLong("ww_ticks", extractor.remainingTicks());
                tag.putInt("ww_progress", extractor.progressPermille());
                tag.putBoolean("ww_processing", extractor.isProcessing());
            } else if (entity instanceof WorkerBlockEntity worker) {
                tag.putString("ww_kind", "worker");
                tag.putInt("ww_area", worker.area());
                tag.putLong("ww_ticks", worker.remainingOperationTicks());
                tag.putBoolean("ww_processing", worker.operationStatus() == WorkerBlockEntity.RUNNING);
            } else if (entity instanceof CableBlockEntity) {
                tag.putString("ww_kind", "cable");
                tag.putInt("ww_rate", MachineConfig.CABLE_TRANSFER_RATE.get());
            }
        }
        @Override public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            CompoundTag tag = accessor.getServerData();
            String kind = tag.getString("ww_kind");
            long ticks = tag.getLong("ww_ticks");
            switch (kind) {
                case "generator" -> tooltip.add(Component.translatable("jade.woolworks.generation", tag.getInt("ww_rate")));
                case "extractor" -> tooltip.add(Component.translatable("jade.woolworks.extraction", tag.getInt("ww_progress") / 10.0));
                case "worker" -> tooltip.add(Component.translatable("jade.woolworks.area", tag.getInt("ww_area"), tag.getInt("ww_area")));
                case "cable" -> tooltip.add(Component.translatable("jade.woolworks.transfer", tag.getInt("ww_rate")));
            }
            if (ticks > 0) {
                tooltip.add(Component.translatable("gui.woolworks.time_remaining", seconds(ticks)));
                if (!tag.getBoolean("ww_processing")) tooltip.add(Component.translatable("gui.woolworks.paused"));
            }
            String mode = tag.getString("ww_mode");
            if (!mode.isEmpty() && !mode.equals("OFF")) tooltip.add(Component.translatable("jade.woolworks.muffle", Component.translatable("woolworks.muffle." + mode.toLowerCase(Locale.ROOT))));
        }
    }
    private enum Sheep implements IEntityComponentProvider {
        INSTANCE;
        @Override public ResourceLocation getUid() { return id("resource_sheep"); }
        @Override public void appendTooltip(ITooltip tooltip, EntityAccessor accessor, IPluginConfig config) {
            if (!(accessor.getEntity() instanceof ResourceSheep sheep)) return;
            sheep.resourceType().ifPresent(type -> tooltip.add(Component.translatable("jade.woolworks.resource", Component.translatable(type.translationKey()))));
            tooltip.add(Component.translatable(sheep.isSheared() ? "jade.woolworks.sheared" : "jade.woolworks.unsheared"));
        }
    }
}
