package be.maarten.woolworks.recipe;

import be.maarten.woolworks.WoolWorks;
import be.maarten.woolworks.registry.ModItems;
import be.maarten.woolworks.machine.ModMachines;
import be.maarten.woolworks.machine.GeneratorFuels;
import be.maarten.woolworks.machine.CableBlockEntity;
import be.maarten.woolworks.machine.GeneratorBlockEntity;
import be.maarten.woolworks.resource.ResourceTypes;
import net.minecraft.nbt.IntTag;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.Unbreakable;
import net.minecraft.network.chat.Component;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import java.util.List;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(WoolWorks.MODID)
@PrefixGameTestTemplate(false)
@SuppressWarnings("removal")
public final class MachineRecipeGameTests {
    private MachineRecipeGameTests() {}

    @GameTest(template = "empty", batch = "resource_missing_tag")
    public static void unavailableTagSheepConversion(GameTestHelper helper) {
        var level = helper.getLevel();
        var tag = net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM, ResourceLocation.parse("c:ingots/osmium"));
        if (net.minecraft.core.registries.BuiltInRegistries.ITEM.getTag(tag).map(t -> t.iterator().hasNext()).orElse(false)) { helper.succeed(); return; }
        var automatic = be.maarten.woolworks.machine.MachineConfig.ENABLE_AUTOMATIC_MODDED_RESOURCES;
        var old = automatic.get(); automatic.set(true);
        var id = ResourceLocation.parse("woolworks:osmium");
        var feed = ModItems.withResource(ModItems.RESOURCE_FEED.get(), id);
        var wool = ModItems.withResource(ModItems.INFUSED_WOOL.get(), id);
        var sheep = be.maarten.woolworks.registry.ModEntities.RESOURCE_SHEEP.get().create(level);
        sheep.setResourceId(id); sheep.setNoAi(true); sheep.moveTo(helper.absolutePos(BlockPos.ZERO).getX() + .5, helper.absolutePos(BlockPos.ZERO).getY() + 1, helper.absolutePos(BlockPos.ZERO).getZ() + .5);
        level.addFreshEntity(sheep);
        helper.runAfterDelay(3, () -> {
            try {
                if (!sheep.isRemoved() || !id.equals(feed.get(be.maarten.woolworks.registry.ModComponents.RESOURCE_TYPE))
                        || !id.equals(wool.get(be.maarten.woolworks.registry.ModComponents.RESOURCE_TYPE))) { helper.fail("Missing tag conversion or saved item identity failed"); return; }
            } finally { automatic.set(old); }
            helper.succeed();
        });
    }

    @GameTest(template = "empty", batch = "resource_conversion")
    public static void invalidSheepConversion(GameTestHelper helper) {
        var level = helper.getLevel();
        var disabled = be.maarten.woolworks.machine.MachineConfig.DISABLED_RESOURCES;
        var automatic = be.maarten.woolworks.machine.MachineConfig.ENABLE_AUTOMATIC_MODDED_RESOURCES;
        var oldDisabled = disabled.get(); var oldAutomatic = automatic.get();
        disabled.set(List.of("woolworks:iron")); automatic.set(false);
        var pos = helper.absolutePos(BlockPos.ZERO);
        var iron = be.maarten.woolworks.registry.ModEntities.RESOURCE_SHEEP.get().create(level);
        var missing = be.maarten.woolworks.registry.ModEntities.RESOURCE_SHEEP.get().create(level);
        var optional = be.maarten.woolworks.registry.ModEntities.RESOURCE_SHEEP.get().create(level);
        var valid = be.maarten.woolworks.registry.ModEntities.RESOURCE_SHEEP.get().create(level);
        var ordinary = net.minecraft.world.entity.EntityType.SHEEP.create(level);
        iron.setResourceId(ResourceLocation.parse("woolworks:iron")); iron.setAge(-12000); iron.setSheared(true);
        iron.setCustomName(Component.literal("Conversion test")); iron.setCustomNameVisible(true); iron.setNoAi(true); iron.setPersistenceRequired();
        iron.setHealth(4); iron.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.MOVEMENT_SPEED, 1000));
        missing.setResourceId(ResourceLocation.parse("test:missing"));
        optional.setResourceId(ResourceLocation.parse("woolworks:osmium"));
        valid.setResourceId(ResourceLocation.parse(Boolean.getBoolean("woolworks.test.osmium") ? "woolworks_test:tagged_custom" : "woolworks:coal"));
        for (var animal : List.of(iron, missing, optional, valid, ordinary)) {
            animal.moveTo(pos.getX() + .5, pos.getY() + 1, pos.getZ() + .5); animal.setNoAi(true); animal.setNoGravity(true); level.addFreshEntity(animal);
        }
        iron.setLeashedTo(ordinary, true);
        var bounds = new net.minecraft.world.phys.AABB(pos).inflate(5);
        helper.runAfterDelay(3, () -> {
            try {
                var sheep = level.getEntitiesOfClass(net.minecraft.world.entity.animal.Sheep.class, bounds);
                var converted = sheep.stream().filter(s -> Component.literal("Conversion test").equals(s.getCustomName())).findFirst().orElse(null);
                if (!iron.isRemoved() || !missing.isRemoved() || !optional.isRemoved() || valid.isRemoved() || ordinary.isRemoved()
                        || converted == null || converted.getType() != net.minecraft.world.entity.EntityType.SHEEP || converted.getColor() != net.minecraft.world.item.DyeColor.WHITE
                        || !converted.isBaby() || !converted.isSheared() || !converted.isCustomNameVisible() || !converted.isPersistenceRequired()
                        || !converted.isNoAi() || !converted.isNoGravity() || converted.getHealth() != 4 || !converted.hasEffect(net.minecraft.world.effect.MobEffects.MOVEMENT_SPEED)
                        || converted.getLeashHolder() != ordinary
                        || sheep.size() != 5 || !level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, bounds).isEmpty()) {
                    helper.fail("Invalid sheep conversion, preservation or duplication failed"); return;
                }
                converted.setAge(0); converted.setSheared(false);
                if (!converted.readyForShearing()) { helper.fail("Converted sheep is not normally shearable"); return; }
                converted.shear(net.minecraft.sounds.SoundSource.PLAYERS);
                if (!converted.isSheared() || level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, bounds).stream().noneMatch(e -> e.getItem().is(Items.WHITE_WOOL))) {
                    helper.fail("Converted sheep did not produce vanilla white wool"); return;
                }
                disabled.set(List.of()); automatic.set(true);
                if (converted.getType() != net.minecraft.world.entity.EntityType.SHEEP || !iron.isRemoved()) { helper.fail("Conversion was not permanent"); return; }
            } finally { disabled.set(oldDisabled); automatic.set(oldAutomatic); }
            helper.succeed();
        });
    }

    @GameTest(template = "empty")
    public static void extractionDisplayOutput(GameTestHelper helper) {
        var result = new ItemStack(Items.IRON_INGOT, 2);
        result.set(DataComponents.CUSTOM_NAME, Component.literal("Custom output"));
        var recipe = new WoolExtractionRecipe(net.minecraft.world.item.crafting.Ingredient.of(Items.STICK), result,
                400, 80, java.util.Optional.empty(), java.util.Optional.empty());
        var input = new SingleRecipeInput(new ItemStack(Items.STICK));
        if (!ItemStack.matches(recipe.getResultItem(helper.getLevel().registryAccess()), recipe.assemble(input, helper.getLevel().registryAccess()))
                || recipe.getResultItem(helper.getLevel().registryAccess()).getCount() != 2
                || !Component.literal("Custom output").equals(recipe.getResultItem(helper.getLevel().registryAccess()).get(DataComponents.CUSTOM_NAME))) {
            helper.fail("Displayed extraction result lost its count or components"); return;
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void automaticCatalogue(GameTestHelper helper) {
        var level = helper.getLevel();
        var registry = level.registryAccess().registryOrThrow(ResourceTypes.REGISTRY);
        var automatic = be.maarten.woolworks.machine.MachineConfig.ENABLE_AUTOMATIC_MODDED_RESOURCES;
        var disabled = be.maarten.woolworks.machine.MachineConfig.DISABLED_RESOURCES;
        var oldAutomatic = automatic.get(); var oldDisabled = disabled.get();
        boolean fixture = Boolean.getBoolean("woolworks.test.osmium");
        if (Boolean.getBoolean("woolworks.test.empty_tag")) {
            var key = net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM, ResourceLocation.parse("c:dusts/salt"));
            var salt = ResourceLocation.parse("woolworks:salt");
            if (net.minecraft.core.registries.BuiltInRegistries.ITEM.getTag(key).map(t -> t.iterator().hasNext()).orElse(false)
                    || ResourceTypes.enabled(level, salt) || level.getRecipeManager().byKey(ResourceLocation.parse("woolworks:salt_feed")).isPresent()
                    || level.getRecipeManager().byKey(ResourceLocation.parse("woolworks:extraction/salt")).isPresent()) {
                helper.fail("Empty salt tag enabled a resource or recipe"); return;
            }
        }
        String[] metals = "osmium tin silver lead zinc uranium nickel platinum aluminum titanium tungsten iridium cobalt bismuth iesnium steel bronze brass electrum invar constantan cupronickel signalum lumium enderium stainless_steel refined_obsidian".split(" ");
        String[] gems = "ruby sapphire peridot topaz fluorite certus_quartz fluix apatite".split(" ");
        String[] dusts = "sulfur salt".split(" ");
        try {
            automatic.set(true); disabled.set(List.of());
            var seen = new java.util.HashSet<ResourceLocation>();
            for (var group : List.of(java.util.Map.entry("ingots", metals), java.util.Map.entry("gems", gems), java.util.Map.entry("dusts", dusts))) {
                for (String name : group.getValue()) {
                    var id = ResourceLocation.parse("woolworks:" + name);
                    var tagId = ResourceLocation.parse("c:" + group.getKey() + "/" + name);
                    var type = registry.get(id);
                    if (!seen.add(id) || type == null || !type.automatic() || !type.requiredItemTag().equals(java.util.Optional.of(tagId))
                            || type.color() < 0 || type.color() > 0xffffff || !net.minecraft.locale.Language.getInstance().has(type.translationKey())) {
                        helper.fail("Invalid catalogue definition: " + id); return;
                    }
                    var tag = net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM, tagId);
                    boolean present = net.minecraft.core.registries.BuiltInRegistries.ITEM.getTag(tag).map(t -> t.iterator().hasNext()).orElse(false);
                    if (fixture && !present) { helper.fail("Test tag missing: " + tagId); return; }
                    if (ResourceTypes.enabled(level, id) != present) { helper.fail("Wrong availability: " + id); return; }
                    var feed = level.getRecipeManager().byKey(ResourceLocation.parse("woolworks:" + name + "_feed"));
                    var extraction = level.getRecipeManager().byKey(ResourceLocation.parse("woolworks:extraction/" + name));
                    if (feed.isPresent() != present || extraction.isPresent() != present) { helper.fail("Wrong conditional recipes: " + id); return; }
                    if (!present) continue;
                    var crafting = (net.minecraft.world.item.crafting.CraftingRecipe) feed.orElseThrow().value();
                    var processing = (WoolExtractionRecipe) extraction.orElseThrow().value();
                    var wool = ModItems.withResource(ModItems.INFUSED_WOOL.get(), id);
                    if (!ItemStack.matches(processing.getResultItem(level.registryAccess()), processing.assemble(new SingleRecipeInput(wool), level.registryAccess()))) {
                        helper.fail("Displayed extraction result differs from machine result: " + id); return;
                    }
                    var input = CraftingInput.of(2, 1, List.of(new ItemStack(Items.WHEAT), new ItemStack(Items.IRON_INGOT)));
                    if (fixture && (!crafting.matches(input, level) || !id.equals(crafting.assemble(input, level.registryAccess()).get(be.maarten.woolworks.registry.ModComponents.RESOURCE_TYPE))
                            || !processing.matches(new SingleRecipeInput(wool), level) || !processing.assemble(new SingleRecipeInput(wool), level.registryAccess()).is(Items.COPPER_INGOT)
                            || processing.duration() != 200 || processing.energyPerTick() != 40 || GeneratorFuels.find(level, wool).isEmpty())) {
                        helper.fail("Catalogue recipes or fuel failed: " + id); return;
                    }
                    disabled.set(List.of(id.toString()));
                    if (ResourceTypes.enabled(level, id) || processing.matches(new SingleRecipeInput(wool), level) || (fixture && crafting.matches(input, level))) { helper.fail("Catalogue disabling failed: " + id); return; }
                    disabled.set(List.of());
                    automatic.set(false);
                    if (ResourceTypes.enabled(level, id)) { helper.fail("Automatic switch failed: " + id); return; }
                    automatic.set(true);
                }
            }
            if (seen.size() != 37) { helper.fail("Unexpected optional catalogue size: " + seen.size()); return; }
            var amethyst = ResourceLocation.parse("woolworks:amethyst");
            var amethystInput = CraftingInput.of(2, 1, List.of(new ItemStack(Items.WHEAT), new ItemStack(Items.AMETHYST_SHARD)));
            var amethystWool = ModItems.withResource(ModItems.INFUSED_WOOL.get(), amethyst);
            var amethystFeed = (net.minecraft.world.item.crafting.CraftingRecipe) level.getRecipeManager().byKey(ResourceLocation.parse("woolworks:amethyst_feed")).orElseThrow().value();
            var amethystExtraction = (WoolExtractionRecipe) level.getRecipeManager().byKey(ResourceLocation.parse("woolworks:extraction/amethyst")).orElseThrow().value();
            automatic.set(false);
            if (!ResourceTypes.enabled(level, amethyst) || !ResourceTypes.enabled(level, ResourceLocation.parse("woolworks:iron")) || !amethystFeed.matches(amethystInput, level)
                    || !amethyst.equals(amethystFeed.assemble(amethystInput, level.registryAccess()).get(be.maarten.woolworks.registry.ModComponents.RESOURCE_TYPE))
                    || !amethystExtraction.matches(new SingleRecipeInput(amethystWool), level) || !amethystExtraction.assemble(new SingleRecipeInput(amethystWool), level.registryAccess()).is(Items.AMETHYST_SHARD)) {
                helper.fail("Amethyst or vanilla unavailable with automatic resources off"); return;
            }
            var custom = ResourceLocation.parse("woolworks_test:tagged_custom");
            if (fixture && !ResourceTypes.enabled(level, custom)) { helper.fail("Custom tagged resource disabled by global switch"); return; }
            disabled.set(List.of(amethyst.toString(), custom.toString()));
            if (ResourceTypes.enabled(level, amethyst) || amethystFeed.matches(amethystInput, level) || amethystExtraction.matches(new SingleRecipeInput(amethystWool), level)
                    || (fixture && ResourceTypes.enabled(level, custom))) { helper.fail("Amethyst or custom resource disabling failed"); return; }
        } finally { automatic.set(oldAutomatic); disabled.set(oldDisabled); }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void osmiumAvailability(GameTestHelper helper) {
        var level = helper.getLevel();
        var id = ResourceLocation.parse("woolworks:osmium");
        var tag = net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM, ResourceLocation.parse("c:ingots/osmium"));
        boolean tagged = net.minecraft.core.registries.BuiltInRegistries.ITEM.getTag(tag).map(t -> t.iterator().hasNext()).orElse(false);
        if (Boolean.getBoolean("woolworks.test.osmium") && !tagged) { helper.fail("Osmium test tag did not load"); return; }
        var automatic = be.maarten.woolworks.machine.MachineConfig.ENABLE_AUTOMATIC_MODDED_RESOURCES;
        var disabled = be.maarten.woolworks.machine.MachineConfig.DISABLED_RESOURCES;
        var oldAutomatic = automatic.get(); var oldDisabled = disabled.get();
        try {
            automatic.set(true); disabled.set(List.of());
            if (ResourceTypes.enabled(level, id) != tagged) { helper.fail("Osmium availability disagrees with its tag"); return; }
            if (!tagged && (level.getRecipeManager().byKey(ResourceLocation.parse("woolworks:osmium_feed")).isPresent()
                    || level.getRecipeManager().byKey(ResourceLocation.parse("woolworks:extraction/osmium")).isPresent())) { helper.fail("Osmium recipes loaded without a non-empty tag"); return; }
            if (tagged) {
                var input = CraftingInput.of(2, 1, List.of(new ItemStack(Items.WHEAT), new ItemStack(Items.IRON_INGOT)));
                var recipe = level.getRecipeManager().byKey(ResourceLocation.parse("woolworks:osmium_feed")).orElseThrow().value();
                if (!(recipe instanceof net.minecraft.world.item.crafting.CraftingRecipe crafting) || !crafting.matches(input, level)
                        || !id.equals(crafting.assemble(input, level.registryAccess()).get(be.maarten.woolworks.registry.ModComponents.RESOURCE_TYPE))) {
                    helper.fail("Osmium feed recipe failed"); return;
                }
                var otherInput = CraftingInput.of(2, 1, List.of(new ItemStack(Items.WHEAT), new ItemStack(Items.COPPER_INGOT)));
                if (!crafting.matches(otherInput, level)) { helper.fail("Feed rejected a second tagged ingot"); return; }
                var wool = ModItems.withResource(ModItems.INFUSED_WOOL.get(), id);
                var extraction = level.getRecipeManager().getRecipeFor(ModRecipes.WOOL_EXTRACTION.get(), new SingleRecipeInput(wool), level).orElseThrow().value();
                if (!extraction.assemble(new SingleRecipeInput(wool), level.registryAccess()).is(Items.COPPER_INGOT) || GeneratorFuels.find(level, wool).isEmpty()) {
                    helper.fail("Osmium extraction or fuel failed"); return;
                }
                var pos = helper.absolutePos(BlockPos.ZERO);
                var sheep = net.minecraft.world.entity.EntityType.SHEEP.create(level);
                var player = helper.makeMockServerPlayerInLevel();
                sheep.moveTo(pos.getX() + .5, pos.getY() + 1, pos.getZ() + .5);
                level.addFreshEntity(sheep);
                var feed = crafting.assemble(input, level.registryAccess());
                if (((be.maarten.woolworks.item.ResourceFeedItem) ModItems.RESOURCE_FEED.get()).interactLivingEntity(feed, player, sheep, InteractionHand.MAIN_HAND) != net.minecraft.world.InteractionResult.SUCCESS) { helper.fail("Osmium sheep transformation failed"); return; }
                var animals = level.getEntitiesOfClass(be.maarten.woolworks.entity.ResourceSheep.class, new net.minecraft.world.phys.AABB(pos).inflate(3), s -> id.equals(s.resourceId()));
                if (animals.isEmpty()) { helper.fail("Transformed osmium sheep missing"); return; }
                animals.getFirst().shear(net.minecraft.sounds.SoundSource.PLAYERS);
                var drops = level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, new net.minecraft.world.phys.AABB(pos).inflate(4), e -> id.equals(e.getItem().get(be.maarten.woolworks.registry.ModComponents.RESOURCE_TYPE)));
                if (drops.stream().noneMatch(e -> e.getItem().is(ModItems.INFUSED_WOOL.get()))) { helper.fail("Osmium sheep did not produce wool"); return; }
                level.setBlockAndUpdate(pos, ModMachines.EXTRACTOR.get().defaultBlockState());
                var machine = (be.maarten.woolworks.machine.ExtractorBlockEntity) level.getBlockEntity(pos);
                machine.inventory().setStackInSlot(0, wool.copy()); machine.energy().receiveEnergy(10000, false);
                for (int i = 0; i < 200; i++) be.maarten.woolworks.machine.ExtractorBlockEntity.tick(level, pos, level.getBlockState(pos), machine);
                if (!machine.inventory().getStackInSlot(1).is(Items.COPPER_INGOT) || machine.inventory().getStackInSlot(1).getCount() != 1) { helper.fail("Osmium extractor processing failed"); return; }
                disabled.set(List.of(id.toString()));
                var disabledSheep = net.minecraft.world.entity.EntityType.SHEEP.create(level);
                if (ResourceTypes.enabled(level, id) || crafting.matches(input, level) || extraction.matches(new SingleRecipeInput(wool), level)
                        || ((be.maarten.woolworks.item.ResourceFeedItem) ModItems.RESOURCE_FEED.get()).interactLivingEntity(ModItems.withResource(ModItems.RESOURCE_FEED.get(), id), player, disabledSheep, InteractionHand.MAIN_HAND) != net.minecraft.world.InteractionResult.PASS) { helper.fail("Disabled osmium remains usable"); return; }
                disabled.set(List.of()); automatic.set(false);
                if (ResourceTypes.enabled(level, id) || crafting.matches(input, level)) { helper.fail("Automatic setting did not disable osmium"); return; }
            }
        } finally { automatic.set(oldAutomatic); disabled.set(oldDisabled); }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void disabledResourceRestrictions(GameTestHelper helper) {
        var level = helper.getLevel();
        var config = be.maarten.woolworks.machine.MachineConfig.DISABLED_RESOURCES;
        var old = config.get();
        var id = ResourceLocation.fromNamespaceAndPath("woolworks", "iron");
        var input = CraftingInput.of(2, 1, List.of(new ItemStack(Items.WHEAT), new ItemStack(Items.IRON_INGOT)));
        var wool = ModItems.withResource(ModItems.INFUSED_WOOL.get(), id);
        var feed = ModItems.withResource(ModItems.RESOURCE_FEED.get(), id);
        try {
            config.set(List.of());
            var recipe = level.getRecipeManager().byKey(ResourceLocation.fromNamespaceAndPath("woolworks", "iron_feed")).orElseThrow().value();
            if (!(recipe instanceof net.minecraft.world.item.crafting.CraftingRecipe crafting) || !crafting.matches(input, level)) { helper.fail("Iron feed is unavailable before disabling"); return; }
            config.set(List.of("woolworks:iron"));
            if (crafting.matches(input, level) || be.maarten.woolworks.machine.GeneratorFuels.find(level, wool).isPresent()
                    || level.getRecipeManager().getRecipeFor(ModRecipes.WOOL_EXTRACTION.get(), new SingleRecipeInput(wool), level).isPresent()) {
                helper.fail("Disabled iron can still be crafted or processed"); return;
            }
            var sheep = net.minecraft.world.entity.EntityType.SHEEP.create(level);
            var player = helper.makeMockServerPlayerInLevel();
            if (sheep == null || ((be.maarten.woolworks.item.ResourceFeedItem)ModItems.RESOURCE_FEED.get()).interactLivingEntity(feed, player, sheep, net.minecraft.world.InteractionHand.MAIN_HAND) != net.minecraft.world.InteractionResult.PASS) {
                helper.fail("Disabled feed transformed a sheep"); return;
            }
            config.set(List.of());
            if (!crafting.matches(input, level)) { helper.fail("Iron feed did not become available again"); return; }
        } finally { config.set(old); }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void advancementProgression(GameTestHelper helper) {
        var player = helper.makeMockServerPlayerInLevel();
        var advancement = helper.getLevel().getServer().getAdvancements().get(ResourceLocation.fromNamespaceAndPath("woolworks", "root"));
        if (advancement == null) { helper.fail("Root advancement not loaded"); return; }
        var progress = player.getAdvancements();
        progress.revoke(advancement, "infused_wool");
        var inventory = player.getInventory();
        var trigger = net.minecraft.advancements.CriteriaTriggers.INVENTORY_CHANGED;
        trigger.trigger(player, inventory, new ItemStack(Items.STRING));
        if (progress.getOrStartProgress(advancement).isDone()) { helper.fail("Non-wool unlocked root"); return; }
        String[] ids = {"woolly_overdrive_upgrade", "woolly_speed_upgrade", "wool_insulated_cable", "wool_generator", "wool_extractor", "woolly_muffler", "wool_worker", "infused_wool", "resource_feed"};
        ItemStack[] stacks = {new ItemStack(ModItems.OVERDRIVE_UPGRADE.get()), new ItemStack(ModItems.SPEED_UPGRADE.get()), new ItemStack(ModMachines.CABLE_ITEM.get()), new ItemStack(ModMachines.GENERATOR_ITEM.get()), new ItemStack(ModMachines.EXTRACTOR_ITEM.get()), new ItemStack(ModItems.MUFFLER.get()), new ItemStack(ModMachines.WORKER_ITEM.get()), ModItems.withResource(ModItems.INFUSED_WOOL.get(), ResourceLocation.fromNamespaceAndPath("example", "custom")), ModItems.withResource(ModItems.RESOURCE_FEED.get(), ResourceLocation.fromNamespaceAndPath("woolworks", "iron"))};
        for (int i = 0; i < ids.length; i++) {
            var entry = helper.getLevel().getServer().getAdvancements().get(ResourceLocation.fromNamespaceAndPath("woolworks", ids[i]));
            if (entry == null) { helper.fail("Missing advancement: " + ids[i]); return; }
            progress.revoke(entry, "item");
            trigger.trigger(player, inventory, stacks[i]);
            if (!progress.getOrStartProgress(entry).isDone()) { helper.fail("Out-of-order advancement failed: " + ids[i]); return; }
        }
        for (var item : new net.minecraft.world.item.Item[]{ModItems.INFUSED_WOOL.get(), ModItems.RESOURCE_FEED.get()}) {
            String name = item == ModItems.INFUSED_WOOL.get() ? "infused_wool" : "resource_feed";
            var entry = helper.getLevel().getServer().getAdvancements().get(ResourceLocation.fromNamespaceAndPath("woolworks", name));
            for (String resource : new String[]{"woolworks:coal", "woolworks:gold", "example:datapack_resource"}) {
                progress.revoke(entry, "item");
                trigger.trigger(player, inventory, ModItems.withResource(item, ResourceLocation.parse(resource)));
                if (!progress.getOrStartProgress(entry).isDone()) { helper.fail("Resource variant did not unlock " + name + ": " + resource); return; }
            }
        }
        if (progress.getOrStartProgress(advancement).isDone()) { helper.fail("Mod items unlocked wool root"); return; }
        trigger.trigger(player, inventory, new ItemStack(Items.BLACK_WOOL));
        if (!progress.getOrStartProgress(advancement).isDone()) { helper.fail("Vanilla wool did not unlock root"); return; }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void sheepGrassEatingEffects(GameTestHelper helper) throws Exception {
        var level = helper.getLevel();
        BlockPos workerPos = helper.absolutePos(BlockPos.ZERO);
        Direction facing = (workerPos.getZ() & 15) == 0 ? Direction.SOUTH : Direction.NORTH;
        level.setBlockAndUpdate(workerPos, ModMachines.WORKER.get().defaultBlockState().setValue(be.maarten.woolworks.machine.WorkerBlock.FACING, facing));
        var worker = (be.maarten.woolworks.machine.WorkerBlockEntity) level.getBlockEntity(workerPos);
        var player = helper.makeMockServerPlayerInLevel();
        player.setPos(workerPos.getX() + .5, workerPos.getY() + 2, workerPos.getZ() + .5);
        var connection = player.connection.getConnection();
        var channelField = net.minecraft.network.Connection.class.getDeclaredField("channel");
        channelField.setAccessible(true);
        var channel = (io.netty.channel.Channel)channelField.get(connection);
        var events = new java.util.ArrayList<net.minecraft.network.protocol.game.ClientboundLevelEventPacket>();
        var handler = new io.netty.channel.ChannelOutboundHandlerAdapter() {
            @Override public void write(io.netty.channel.ChannelHandlerContext context, Object message, io.netty.channel.ChannelPromise promise) throws Exception {
                if (message instanceof net.minecraft.network.protocol.game.ClientboundLevelEventPacket packet && packet.getType() == 2001) events.add(packet);
                super.write(context, message, promise);
            }
        };
        channel.pipeline().addLast("woolworks_grass_test", handler);
        try {
            var sheep = net.minecraft.world.entity.EntityType.SHEEP.create(level);
            var resourceSheep = be.maarten.woolworks.registry.ModEntities.RESOURCE_SHEEP.get().create(level);
            if (worker == null || sheep == null || resourceSheep == null) { helper.fail("Missing grass-eating fixtures"); return; }
            BlockPos animalPos = workerPos.relative(facing);
            for (int mode = 0; mode < 4; mode++) {
                sheep.setPos(animalPos.getX() + .5, animalPos.getY(), animalPos.getZ() + .5);
                sheep.setSheared(true);
                level.setBlockAndUpdate(animalPos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
                level.setBlockAndUpdate(animalPos.below(), net.minecraft.world.level.block.Blocks.GRASS_BLOCK.defaultBlockState());
                events.clear();
                var goal = new net.minecraft.world.entity.ai.goal.EatBlockGoal(sheep);
                goal.start(); for (int i = 0; i < 40; i++) goal.tick();
                if (events.size() != 1 || events.getFirst().getPos().equals(animalPos.below()) == false
                        || ((events.getFirst().getData() & Integer.MIN_VALUE) != 0) != (mode >= 2)
                        || (events.getFirst().getData() & Integer.MAX_VALUE) != net.minecraft.world.level.block.Block.getId(net.minecraft.world.level.block.Blocks.GRASS_BLOCK.defaultBlockState())
                        || !level.getBlockState(animalPos.below()).is(net.minecraft.world.level.block.Blocks.DIRT) || sheep.isSheared()) {
                    helper.fail("Grass-block eating effect or regrowth failed for mode " + mode + ": " + events.size()); return;
                }
                worker.cycleMuffleMode();
            }
            worker.cycleMuffleMode(); worker.cycleMuffleMode();
            resourceSheep.setPos(animalPos.getX() + .5, animalPos.getY(), animalPos.getZ() + .5);
            resourceSheep.setSheared(true);
            level.setBlockAndUpdate(animalPos, net.minecraft.world.level.block.Blocks.SHORT_GRASS.defaultBlockState());
            events.clear();
            var goal = new net.minecraft.world.entity.ai.goal.EatBlockGoal(resourceSheep);
            goal.start(); for (int i = 0; i < 40; i++) goal.tick();
            if (events.size() != 1 || (events.getFirst().getData() & Integer.MIN_VALUE) == 0
                    || !level.getBlockState(animalPos).isAir() || resourceSheep.isSheared()) {
                helper.fail("Resource sheep short-grass effect or regrowth failed"); return;
            }
            BlockPos outside = workerPos.relative(facing, 4);
            sheep.setPos(outside.getX() + .5, outside.getY(), outside.getZ() + .5);
            sheep.setSheared(true);
            level.setBlockAndUpdate(outside, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
            level.setBlockAndUpdate(outside.below(), net.minecraft.world.level.block.Blocks.GRASS_BLOCK.defaultBlockState());
            events.clear();
            var outsideGoal = new net.minecraft.world.entity.ai.goal.EatBlockGoal(sheep);
            outsideGoal.start(); for (int i = 0; i < 40; i++) outsideGoal.tick();
            if (events.size() != 1 || (events.getFirst().getData() & Integer.MIN_VALUE) != 0 || sheep.isSheared()) {
                helper.fail("Sheep outside the zone was incorrectly silenced or did not regrow"); return;
            }
            level.setBlockAndUpdate(animalPos, net.minecraft.world.level.block.Blocks.SHORT_GRASS.defaultBlockState());
            events.clear(); level.destroyBlock(animalPos, false, player, 512);
            if (events.size() != 1 || (events.getFirst().getData() & Integer.MIN_VALUE) != 0) {
                helper.fail("Player grass breaking was incorrectly marked silent"); return;
            }
        } finally { channel.pipeline().remove(handler); }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void mufflerRecipe(GameTestHelper helper) {
        var level = helper.getLevel();
        var ids = level.registryAccess().registryOrThrow(ResourceTypes.REGISTRY).keySet().stream().filter(id -> ResourceTypes.enabled(level, id)).toList();
        if (ids.size() < 2) { helper.fail("Resource registry unexpectedly empty"); return; }
        for (var id : ids) {
            var first = ModItems.withResource(ModItems.INFUSED_WOOL.get(), id);
            var second = ModItems.withResource(ModItems.INFUSED_WOOL.get(), ids.getFirst());
            var input = CraftingInput.of(3, 3, List.of(first, ItemStack.EMPTY, second,
                    new ItemStack(Items.IRON_INGOT), new ItemStack(Items.COPPER_INGOT), new ItemStack(Items.IRON_INGOT),
                    new ItemStack(Items.STICK), ItemStack.EMPTY, new ItemStack(Items.STICK)));
            var recipe = level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, level);
            if (recipe.isEmpty() || !recipe.get().value().assemble(input, level.registryAccess()).is(ModItems.MUFFLER.get())) {
                helper.fail("Muffler recipe rejected " + id); return;
            }
        }
        for (ItemStack invalid : List.of(new ItemStack(Items.WHITE_WOOL), new ItemStack(ModItems.INFUSED_WOOL.get()),
                ModItems.withResource(ModItems.INFUSED_WOOL.get(), ResourceLocation.fromNamespaceAndPath("test", "invalid")))) {
            var input = CraftingInput.of(3, 3, List.of(invalid, ItemStack.EMPTY, ModItems.withResource(ModItems.INFUSED_WOOL.get(), ids.getFirst()),
                    new ItemStack(Items.IRON_INGOT), new ItemStack(Items.COPPER_INGOT), new ItemStack(Items.IRON_INGOT),
                    new ItemStack(Items.STICK), ItemStack.EMPTY, new ItemStack(Items.STICK)));
            if (level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, level).isPresent()) { helper.fail("Muffler accepted invalid wool"); return; }
        }
        if (new ItemStack(ModItems.MUFFLER.get()).getMaxStackSize() != 1) { helper.fail("Muffler stack limit incorrect"); return; }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void mufflerModesAndInteraction(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos pos = helper.absolutePos(BlockPos.ZERO);
        level.setBlockAndUpdate(pos, ModMachines.WORKER.get().defaultBlockState());
        var worker = (be.maarten.woolworks.machine.WorkerBlockEntity) level.getBlockEntity(pos);
        var player = helper.makeMockServerPlayerInLevel();
        player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        player.setPos(pos.getX() + .5, pos.getY() + 1, pos.getZ() + .5);
        var muffler = new ItemStack(ModItems.MUFFLER.get());
        player.setItemInHand(InteractionHand.OFF_HAND, muffler);
        player.setShiftKeyDown(true);
        var hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
        for (var expected : List.of(be.maarten.woolworks.machine.MuffleMode.MACHINE, be.maarten.woolworks.machine.MuffleMode.SHEEP,
                be.maarten.woolworks.machine.MuffleMode.BOTH, be.maarten.woolworks.machine.MuffleMode.OFF)) {
            player.gameMode.useItemOn(player, level, muffler, InteractionHand.OFF_HAND, hit);
            if (worker.muffleMode() != expected || muffler.getCount() != 1 || muffler.isDamaged()) { helper.fail("Muffler interaction did not cycle to " + expected); return; }
        }
        player.setShiftKeyDown(false);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STONE));
        var result = player.gameMode.useItemOn(player, level, player.getMainHandItem(), InteractionHand.MAIN_HAND, hit);
        if (!result.consumesAction() || worker.muffleMode() != be.maarten.woolworks.machine.MuffleMode.MACHINE
                || player.containerMenu != player.inventoryMenu || muffler.getCount() != 1) {
            helper.fail("Offhand muffler did not take priority over opening the machine"); return;
        }
        worker.cycleMuffleMode(); worker.cycleMuffleMode(); worker.cycleMuffleMode();
        worker.cycleMuffleMode(); worker.cycleMuffleMode();
        var restored = new be.maarten.woolworks.machine.WorkerBlockEntity(pos, level.getBlockState(pos));
        restored.loadCustomOnly(worker.saveCustomOnly(level.registryAccess()), level.registryAccess());
        if (restored.muffleMode() != be.maarten.woolworks.machine.MuffleMode.SHEEP) { helper.fail("Worker mode did not persist"); return; }
        BlockPos other = pos.east();
        level.setBlockAndUpdate(other, ModMachines.GENERATOR.get().defaultBlockState());
        var generator = (GeneratorBlockEntity) level.getBlockEntity(other);
        CompoundTag invalid = generator.saveCustomOnly(level.registryAccess()); invalid.putInt("MuffleMode", 3);
        generator.loadCustomOnly(invalid, level.registryAccess());
        if (generator.muffleMode() != be.maarten.woolworks.machine.MuffleMode.OFF) { helper.fail("Unsupported generator mode was accepted"); return; }
        generator.cycleMuffleMode(); generator.cycleMuffleMode();
        if (generator.muffleMode() != be.maarten.woolworks.machine.MuffleMode.OFF) { helper.fail("Generator mode cycle incorrect"); return; }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void sheepSoundZones(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos pos = helper.absolutePos(BlockPos.ZERO);
        Direction facing = (pos.getZ() & 15) == 0 ? Direction.SOUTH : Direction.NORTH;
        level.setBlockAndUpdate(pos, ModMachines.WORKER.get().defaultBlockState().setValue(be.maarten.woolworks.machine.WorkerBlock.FACING, facing));
        var worker = (be.maarten.woolworks.machine.WorkerBlockEntity) level.getBlockEntity(pos);
        var sheep = net.minecraft.world.entity.EntityType.SHEEP.create(level);
        if (worker == null || sheep == null) { helper.fail("Missing sound test entities"); return; }
        sheep.setPos(pos.getX() + .5, pos.getY(), pos.getZ() + .5 + facing.getStepZ());
        sheep.setNoAi(true); sheep.setNoGravity(true); sheep.setInvulnerable(true); sheep.setAge(-1000); level.addFreshEntity(sheep);
        worker.cycleMuffleMode(); worker.cycleMuffleMode();
        var registry = net.minecraft.core.registries.BuiltInRegistries.SOUND_EVENT;
        var ambient = new net.neoforged.neoforge.event.PlayLevelSoundEvent.AtPosition(level, sheep.position(), registry.wrapAsHolder(net.minecraft.sounds.SoundEvents.SHEEP_AMBIENT), net.minecraft.sounds.SoundSource.NEUTRAL, 1, 1);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(ambient);
        var manual = new net.neoforged.neoforge.event.PlayLevelSoundEvent.AtEntity(sheep, registry.wrapAsHolder(net.minecraft.sounds.SoundEvents.SHEEP_SHEAR), net.minecraft.sounds.SoundSource.PLAYERS, 1, 1);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(manual);
        if (!ambient.isCanceled() || manual.isCanceled() || sheep.isSilent()) { helper.fail("Sheep filtering: ambient=" + ambient.isCanceled() + ", manual=" + manual.isCanceled() + ", indexed=" + be.maarten.woolworks.machine.SheepSoundZones.isMuted(sheep) + ", mode=" + worker.muffleMode()); return; }
        worker.cycleMuffleMode(); worker.cycleMuffleMode();
        if (be.maarten.woolworks.machine.SheepSoundZones.isMuted(sheep)) { helper.fail("Disabled zone remained active"); return; }
        sheep.discard(); helper.succeed();
    }

    @GameTest(template = "empty")
    public static void sheepZonesTrackArea(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos pos = helper.absolutePos(BlockPos.ZERO);
        level.setBlockAndUpdate(pos, ModMachines.WORKER.get().defaultBlockState());
        var worker = (be.maarten.woolworks.machine.WorkerBlockEntity) level.getBlockEntity(pos);
        var sheep = net.minecraft.world.entity.EntityType.SHEEP.create(level);
        if (worker == null || sheep == null) { helper.fail("Missing sheep zone fixtures"); return; }
        worker.cycleMuffleMode(); worker.cycleMuffleMode();
        for (Direction direction : List.of(Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST)) {
            level.setBlockAndUpdate(pos, level.getBlockState(pos).setValue(be.maarten.woolworks.machine.WorkerBlock.FACING, direction));
            for (int size : List.of(3, 5, 7, 9, 1)) {
                if (worker.area() != size) { helper.fail("Unexpected area cycle"); return; }
                var inside = pos.relative(direction, size);
                sheep.setPos(inside.getX() + .5, inside.getY(), inside.getZ() + .5);
                if (!be.maarten.woolworks.machine.SheepSoundZones.isMuted(sheep)) { helper.fail("Sheep zone missed " + direction + " " + size); return; }
                var outside = pos.relative(direction, size + 1);
                sheep.setPos(outside.getX() + .5, outside.getY(), outside.getZ() + .5);
                if (be.maarten.woolworks.machine.SheepSoundZones.isMuted(sheep)) { helper.fail("Sheep outside area was muted"); return; }
                worker.cycleArea();
            }
        }
        var inside = pos.relative(Direction.WEST);
        sheep.setPos(inside.getX() + .5, inside.getY(), inside.getZ() + .5);
        level.removeBlock(pos, false);
        if (be.maarten.woolworks.machine.SheepSoundZones.isMuted(sheep)) { helper.fail("Removed worker left a sound zone"); return; }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void overlappingSheepZones(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos pos = helper.absolutePos(BlockPos.ZERO);
        BlockPos other = pos.east(2);
        level.setBlockAndUpdate(pos, ModMachines.WORKER.get().defaultBlockState().setValue(be.maarten.woolworks.machine.WorkerBlock.FACING, Direction.EAST));
        level.setBlockAndUpdate(other, ModMachines.WORKER.get().defaultBlockState().setValue(be.maarten.woolworks.machine.WorkerBlock.FACING, Direction.WEST));
        var first = (be.maarten.woolworks.machine.WorkerBlockEntity) level.getBlockEntity(pos);
        var second = (be.maarten.woolworks.machine.WorkerBlockEntity) level.getBlockEntity(other);
        var sheep = net.minecraft.world.entity.EntityType.SHEEP.create(level);
        if (first == null || second == null || sheep == null) { helper.fail("Missing overlapping fixtures"); return; }
        sheep.setPos(pos.getX() + 1.5, pos.getY(), pos.getZ() + .5);
        first.cycleMuffleMode(); first.cycleMuffleMode(); second.cycleMuffleMode(); second.cycleMuffleMode();
        first.cycleMuffleMode(); first.cycleMuffleMode();
        if (!be.maarten.woolworks.machine.SheepSoundZones.isMuted(sheep)) { helper.fail("Overlapping active zone was ignored"); return; }
        second.cycleMuffleMode(); second.cycleMuffleMode();
        if (be.maarten.woolworks.machine.SheepSoundZones.isMuted(sheep)) { helper.fail("Disabled overlapping zones still muted"); return; }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void machineSoundEvents(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos pos = helper.absolutePos(BlockPos.ZERO);
        level.setBlockAndUpdate(pos, ModMachines.GENERATOR.get().defaultBlockState());
        var generator = (GeneratorBlockEntity) level.getBlockEntity(pos);
        var fuel = ModItems.withResource(ModItems.INFUSED_WOOL.get(), ResourceLocation.fromNamespaceAndPath("woolworks", "iron"));
        fuel.setCount(2); generator.fuel().setStackInSlot(0, fuel);
        int[] heard = {0};
        java.util.function.Consumer<net.neoforged.neoforge.event.PlayLevelSoundEvent.AtPosition> listener = event -> {
            if (event.getLevel() == level && event.getPosition().distanceToSqr(Vec3.atCenterOf(pos)) < .1
                    && event.getSound() != null && event.getSound().value() == net.minecraft.sounds.SoundEvents.FIRE_EXTINGUISH) heard[0]++;
        };
        var bus = net.neoforged.neoforge.common.NeoForge.EVENT_BUS;
        bus.addListener(net.neoforged.neoforge.event.PlayLevelSoundEvent.AtPosition.class, listener);
        try {
            for (int i = 0; i < 400; i++) GeneratorBlockEntity.tick(level, pos, level.getBlockState(pos), generator);
            if (heard[0] != 1) { helper.fail("Generator did not play once per fuel item: " + heard[0]); return; }
            generator.cycleMuffleMode();
            GeneratorBlockEntity.tick(level, pos, level.getBlockState(pos), generator);
            if (heard[0] != 1 || !generator.fuel().getStackInSlot(0).isEmpty()) { helper.fail("Generator muffle did not suppress second fuel sound"); return; }
        } finally { bus.unregister(listener); }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void extractorCompletionSound(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos pos = helper.absolutePos(BlockPos.ZERO);
        level.setBlockAndUpdate(pos, ModMachines.EXTRACTOR.get().defaultBlockState());
        var extractor = (be.maarten.woolworks.machine.ExtractorBlockEntity) level.getBlockEntity(pos);
        var input = ModItems.withResource(ModItems.INFUSED_WOOL.get(), ResourceLocation.fromNamespaceAndPath("woolworks", "iron"));
        input.setCount(2); extractor.inventory().setStackInSlot(0, input);
        extractor.inventory().setStackInSlot(2, new ItemStack(ModItems.OVERDRIVE_UPGRADE.get(), 2));
        extractor.energy().receiveEnergy(20000, false);
        int[] heard = {0};
        java.util.function.Consumer<net.neoforged.neoforge.event.PlayLevelSoundEvent.AtPosition> listener = event -> {
            if (event.getLevel() == level && event.getPosition().distanceToSqr(Vec3.atCenterOf(pos)) < .1
                    && event.getSound() != null && event.getSound().value() == net.minecraft.sounds.SoundEvents.PISTON_CONTRACT) heard[0]++;
        };
        var bus = net.neoforged.neoforge.common.NeoForge.EVENT_BUS;
        bus.addListener(net.neoforged.neoforge.event.PlayLevelSoundEvent.AtPosition.class, listener);
        try {
            for (int i = 0; i < 34; i++) be.maarten.woolworks.machine.ExtractorBlockEntity.tick(level, pos, level.getBlockState(pos), extractor);
            if (heard[0] != 1 || extractor.inventory().getStackInSlot(1).getCount() != 4) { helper.fail("Extractor completion sound duplicated or missing"); return; }
            extractor.cycleMuffleMode();
            for (int i = 0; i < 34; i++) be.maarten.woolworks.machine.ExtractorBlockEntity.tick(level, pos, level.getBlockState(pos), extractor);
            if (heard[0] != 1 || extractor.inventory().getStackInSlot(1).getCount() != 8) { helper.fail("Muted extractor changed processing or played sound"); return; }
        } finally { bus.unregister(listener); }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void workerAutomatedShearingSound(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos pos = helper.absolutePos(BlockPos.ZERO);
        Direction facing = (pos.getZ() & 15) == 0 ? Direction.SOUTH : Direction.NORTH;
        level.setBlockAndUpdate(pos, ModMachines.WORKER.get().defaultBlockState().setValue(be.maarten.woolworks.machine.WorkerBlock.FACING, facing));
        var worker = (be.maarten.woolworks.machine.WorkerBlockEntity) level.getBlockEntity(pos);
        var sheep = net.minecraft.world.entity.EntityType.SHEEP.create(level);
        if (worker == null || sheep == null) { helper.fail("Missing worker or sheep"); return; }
        sheep.setPos(pos.getX() + .5, pos.getY(), pos.getZ() + .5 + facing.getStepZ());
        sheep.setNoAi(true); sheep.setNoGravity(true); sheep.setInvulnerable(true); sheep.setAge(0); level.addFreshEntity(sheep);
        worker.inventory().setStackInSlot(0, new ItemStack(Items.SHEARS));
        worker.energy().receiveEnergy(1000, false);
        int[] heard = {0};
        java.util.function.Consumer<net.neoforged.neoforge.event.PlayLevelSoundEvent.AtEntity> listener = event -> {
            if (event.getEntity() == sheep && event.getSound() != null && event.getSound().value() == net.minecraft.sounds.SoundEvents.SHEEP_SHEAR) heard[0]++;
        };
        var bus = net.neoforged.neoforge.common.NeoForge.EVENT_BUS;
        bus.addListener(net.neoforged.bus.api.EventPriority.LOWEST, net.neoforged.neoforge.event.PlayLevelSoundEvent.AtEntity.class, listener);
        try {
            int[] expected = {1, 1, 2, 2};
            for (int i = 0; i < 4; i++) {
                sheep.setSheared(false);
                CompoundTag saved = worker.saveCustomOnly(level.registryAccess());
                saved.putInt("OperationProgress", be.maarten.woolworks.machine.MachineConfig.WORKER_INTERVAL.get() - 1);
                worker.loadCustomOnly(saved, level.registryAccess());
                be.maarten.woolworks.machine.WorkerBlockEntity.tick(level, pos, level.getBlockState(pos), worker);
                if (!sheep.isSheared() || heard[0] != expected[i]) { helper.fail("Automated shearing sound incorrect in mode " + worker.muffleMode() + ": " + heard[0]); return; }
                worker.cycleMuffleMode();
            }
        } finally { bus.unregister(listener); sheep.discard(); }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void overdriveRecipeAndSlot(GameTestHelper helper) {
        var level = helper.getLevel();
        var speed = new ItemStack(ModItems.SPEED_UPGRADE.get());
        var recipeInput = CraftingInput.of(3, 3, List.of(speed, speed, speed, speed, new ItemStack(Items.NETHER_STAR), speed, speed, speed, speed));
        var match = level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, recipeInput, level);
        if (match.isEmpty() || !match.get().value().assemble(recipeInput, level.registryAccess()).is(ModItems.OVERDRIVE_UPGRADE.get())) {
            helper.fail("Overdrive recipe did not match"); return;
        }
        BlockPos pos = helper.absolutePos(BlockPos.ZERO);
        level.setBlockAndUpdate(pos, ModMachines.EXTRACTOR.get().defaultBlockState());
        var extractor = (be.maarten.woolworks.machine.ExtractorBlockEntity) level.getBlockEntity(pos);
        var remaining = extractor.inventory().insertItem(2, new ItemStack(ModItems.OVERDRIVE_UPGRADE.get(), 5), false);
        if (extractor.inventory().getStackInSlot(2).getCount() != 2 || remaining.getCount() != 3
                || extractor.inventory().insertItem(2, new ItemStack(ModItems.SPEED_UPGRADE.get()), false).isEmpty()) {
            helper.fail("Overdrive slot limit or mixed-upgrade rejection failed"); return;
        }
        var saved = extractor.saveCustomOnly(level.registryAccess());
        var restored = new be.maarten.woolworks.machine.ExtractorBlockEntity(pos, level.getBlockState(pos));
        restored.loadCustomOnly(saved, level.registryAccess());
        if (restored.inventory().getStackInSlot(2).getCount() != 2 || !restored.inventory().getStackInSlot(2).is(ModItems.OVERDRIVE_UPGRADE.get())) {
            helper.fail("Installed overdrive did not persist"); return;
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void overdriveShiftClick(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos pos = helper.absolutePos(BlockPos.ZERO);
        level.setBlockAndUpdate(pos, ModMachines.EXTRACTOR.get().defaultBlockState());
        var extractor = (be.maarten.woolworks.machine.ExtractorBlockEntity) level.getBlockEntity(pos);
        var player = helper.makeMockServerPlayerInLevel();
        player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        player.getInventory().setItem(9, new ItemStack(ModItems.OVERDRIVE_UPGRADE.get(), 10));
        player.getInventory().setItem(10, new ItemStack(ModItems.SPEED_UPGRADE.get(), 8));
        var menu = be.maarten.woolworks.machine.MachineMenu.extractor(1, player.getInventory(), extractor);
        menu.quickMoveStack(player, 3);
        menu.quickMoveStack(player, 4);
        if (extractor.inventory().getStackInSlot(2).getCount() != 2 || player.getInventory().getItem(9).getCount() != 8
                || player.getInventory().getItem(10).getCount() != 8 || !extractor.inventory().getStackInSlot(2).is(ModItems.OVERDRIVE_UPGRADE.get())) {
            helper.fail("Shift-click exceeded limits or mixed upgrades"); return;
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void overdriveOversizedSavedStack(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos pos = helper.absolutePos(BlockPos.ZERO);
        level.setBlockAndUpdate(pos, ModMachines.EXTRACTOR.get().defaultBlockState());
        var extractor = (be.maarten.woolworks.machine.ExtractorBlockEntity) level.getBlockEntity(pos);
        var oldInventory = new net.neoforged.neoforge.items.ItemStackHandler(3);
        oldInventory.setStackInSlot(2, new ItemStack(ModItems.OVERDRIVE_UPGRADE.get(), 5));
        CompoundTag data = new CompoundTag();
        data.put("Inventory", oldInventory.serializeNBT(level.registryAccess()));
        extractor.loadCustomOnly(data, level.registryAccess());
        var recovered = extractor.saveCustomOnly(level.registryAccess()).getList("RecoveredUpgrades", 10);
        if (extractor.inventory().getStackInSlot(2).getCount() != 2 || recovered.size() != 1
                || ItemStack.parseOptional(level.registryAccess(), recovered.getCompound(0)).getCount() != 3) {
            helper.fail("Invalid saved upgrade stack was not safely recovered"); return;
        }
        be.maarten.woolworks.machine.ExtractorBlockEntity.tick(level, pos, level.getBlockState(pos), extractor);
        var drops = level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
                new net.minecraft.world.phys.AABB(pos).inflate(2), e -> e.getItem().is(ModItems.OVERDRIVE_UPGRADE.get()));
        if (drops.size() != 1 || drops.getFirst().getItem().getCount() != 3) { helper.fail("Recovered upgrades were not returned"); return; }
        drops.getFirst().discard(); helper.succeed();
    }

    @GameTest(template = "empty")
    public static void extractorUpgradeBalancing(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos pos = helper.absolutePos(BlockPos.ZERO);
        var iron = ModItems.withResource(ModItems.INFUSED_WOOL.get(), ResourceLocation.fromNamespaceAndPath("woolworks", "iron"));
        for (int variant = 0; variant < 4; variant++) {
            level.setBlockAndUpdate(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
            level.setBlockAndUpdate(pos, ModMachines.EXTRACTOR.get().defaultBlockState());
            var extractor = (be.maarten.woolworks.machine.ExtractorBlockEntity) level.getBlockEntity(pos);
            extractor.inventory().setStackInSlot(0, iron.copy());
            if (variant == 1) extractor.inventory().setStackInSlot(2, new ItemStack(ModItems.SPEED_UPGRADE.get(), 8));
            if (variant >= 2) extractor.inventory().setStackInSlot(2, new ItemStack(ModItems.OVERDRIVE_UPGRADE.get(), variant - 1));
            int ticks = new int[]{200, 40, 67, 34}[variant];
            int output = new int[]{1, 1, 2, 4}[variant];
            int cost = new int[]{40, 40, 80, 160}[variant];
            extractor.energy().receiveEnergy(20000, false);
            for (int i = 0; i < ticks - 1; i++) {
                be.maarten.woolworks.machine.ExtractorBlockEntity.tick(level, pos, level.getBlockState(pos), extractor);
                if (extractor.remainingTicks() != ticks - i - 1) { helper.fail("Incorrect remaining extraction ticks for upgrade " + variant); return; }
            }
            if (!extractor.inventory().getStackInSlot(1).isEmpty()) { helper.fail("Upgrade completed too early: " + variant); return; }
            be.maarten.woolworks.machine.ExtractorBlockEntity.tick(level, pos, level.getBlockState(pos), extractor);
            if (extractor.inventory().getStackInSlot(1).getCount() != output || extractor.energy().getEnergyStored() != 20000 - ticks * cost) {
                helper.fail("Upgrade balancing incorrect for variant " + variant); return;
            }
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void extractorOverdrivePausesAndResets(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos pos = helper.absolutePos(BlockPos.ZERO);
        level.setBlockAndUpdate(pos, ModMachines.EXTRACTOR.get().defaultBlockState());
        var extractor = (be.maarten.woolworks.machine.ExtractorBlockEntity) level.getBlockEntity(pos);
        extractor.inventory().setStackInSlot(0, ModItems.withResource(ModItems.INFUSED_WOOL.get(), ResourceLocation.fromNamespaceAndPath("woolworks", "iron")));
        extractor.inventory().setStackInSlot(2, new ItemStack(ModItems.OVERDRIVE_UPGRADE.get(), 2));
        extractor.energy().receiveEnergy(159, false);
        be.maarten.woolworks.machine.ExtractorBlockEntity.tick(level, pos, level.getBlockState(pos), extractor);
        if (extractor.energy().getEnergyStored() != 159 || extractor.menuData().get(1) != 0) { helper.fail("Insufficient energy did not pause"); return; }
        extractor.energy().receiveEnergy(10000, false);
        for (int i = 0; i < 10; i++) be.maarten.woolworks.machine.ExtractorBlockEntity.tick(level, pos, level.getBlockState(pos), extractor);
        extractor.inventory().setStackInSlot(2, new ItemStack(ModItems.SPEED_UPGRADE.get(), 8));
        be.maarten.woolworks.machine.ExtractorBlockEntity.tick(level, pos, level.getBlockState(pos), extractor);
        if (extractor.menuData().get(1) != 25 || !extractor.inventory().getStackInSlot(1).isEmpty()) {
            helper.fail("Upgrade change did not reset progress"); return;
        }
        extractor.inventory().setStackInSlot(1, new ItemStack(Items.IRON_INGOT, 64));
        int energy = extractor.energy().getEnergyStored();
        long remaining = extractor.remainingTicks();
        be.maarten.woolworks.machine.ExtractorBlockEntity.tick(level, pos, level.getBlockState(pos), extractor);
        if (extractor.energy().getEnergyStored() != energy || extractor.remainingTicks() != remaining || extractor.isProcessing()) { helper.fail("Blocked output consumed energy or progressed"); return; }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void overdriveDatapackRecipes(GameTestHelper helper) {
        if (!Boolean.getBoolean("woolworks.test.overdrive_recipes")) { helper.succeed(); return; }
        var level = helper.getLevel();
        BlockPos pos = helper.absolutePos(BlockPos.ZERO);
        level.setBlockAndUpdate(pos, ModMachines.EXTRACTOR.get().defaultBlockState());
        var extractor = (be.maarten.woolworks.machine.ExtractorBlockEntity) level.getBlockEntity(pos);
        extractor.inventory().setStackInSlot(0, new ItemStack(Items.STICK));
        extractor.inventory().setStackInSlot(2, new ItemStack(ModItems.OVERDRIVE_UPGRADE.get(), 2));
        extractor.energy().receiveEnergy(1000, false);
        for (int i = 0; i < 2; i++) be.maarten.woolworks.machine.ExtractorBlockEntity.tick(level, pos, level.getBlockState(pos), extractor);
        var output = extractor.inventory().getStackInSlot(1);
        if (!output.is(Items.IRON_INGOT) || output.getCount() != 64 || !Component.literal("Datapack result").equals(output.get(DataComponents.CUSTOM_NAME))
                || extractor.energy().getEnergyStored() != 928 || !extractor.inventory().getStackInSlot(0).isEmpty()) {
            helper.fail("Custom recipe multiplication or components failed: " + output); return;
        }
        extractor.inventory().setStackInSlot(0, new ItemStack(Items.BLAZE_ROD));
        extractor.inventory().setStackInSlot(1, ItemStack.EMPTY);
        int before = extractor.energy().getEnergyStored();
        be.maarten.woolworks.machine.ExtractorBlockEntity.tick(level, pos, level.getBlockState(pos), extractor);
        if (extractor.menuData().get(6) != 1 || extractor.energy().getEnergyStored() != before
                || extractor.inventory().getStackInSlot(0).isEmpty() || !extractor.inventory().getStackInSlot(1).isEmpty()) {
            helper.fail("Oversized multiplied result was not blocked"); return;
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void machineActiveStates(GameTestHelper helper) {
        var level = helper.getLevel();
        var generatorPos = helper.absolutePos(BlockPos.ZERO);
        var extractorPos = generatorPos.east(2);
        var generatorState = ModMachines.GENERATOR.get().defaultBlockState()
                .setValue(be.maarten.woolworks.machine.GeneratorBlock.FACING, Direction.EAST);
        var extractorState = ModMachines.EXTRACTOR.get().defaultBlockState()
                .setValue(be.maarten.woolworks.machine.ExtractorBlock.FACING, Direction.WEST);
        level.setBlockAndUpdate(generatorPos, generatorState);
        level.setBlockAndUpdate(extractorPos, extractorState);
        var generator = (GeneratorBlockEntity) level.getBlockEntity(generatorPos);
        var extractor = (be.maarten.woolworks.machine.ExtractorBlockEntity) level.getBlockEntity(extractorPos);
        if (generator == null || extractor == null) { helper.fail("Machine block entities missing"); return; }
        var iron = ModItems.withResource(ModItems.INFUSED_WOOL.get(), ResourceLocation.fromNamespaceAndPath("woolworks", "iron"));
        generator.fuel().setStackInSlot(0, iron.copy());
        extractor.inventory().setStackInSlot(0, iron.copy());
        extractor.energy().receiveEnergy(1000, false);
        GeneratorBlockEntity.tick(level, generatorPos, generatorState, generator);
        be.maarten.woolworks.machine.ExtractorBlockEntity.tick(level, extractorPos, extractorState, extractor);
        if (!level.getBlockState(generatorPos).getValue(be.maarten.woolworks.machine.GeneratorBlock.ACTIVE)
                || !level.getBlockState(extractorPos).getValue(be.maarten.woolworks.machine.ExtractorBlock.ACTIVE)
                || level.getBlockState(generatorPos).getLightEmission() != 13
                || level.getBlockState(extractorPos).getLightEmission() != 0
                || ModMachines.WORKER.get().defaultBlockState().getLightEmission() != 0
                || level.getBlockEntity(generatorPos) != generator || level.getBlockEntity(extractorPos) != extractor
                || level.getBlockState(generatorPos).getValue(be.maarten.woolworks.machine.GeneratorBlock.FACING) != Direction.EAST
                || level.getBlockState(extractorPos).getValue(be.maarten.woolworks.machine.ExtractorBlock.FACING) != Direction.WEST) {
            helper.fail("Active state did not preserve machines and orientation"); return;
        }
        generator.energy().deserializeNBT(level.registryAccess(), IntTag.valueOf(20000));
        extractor.inventory().setStackInSlot(1, new ItemStack(Items.DIRT, 64));
        GeneratorBlockEntity.tick(level, generatorPos, level.getBlockState(generatorPos), generator);
        be.maarten.woolworks.machine.ExtractorBlockEntity.tick(level, extractorPos, level.getBlockState(extractorPos), extractor);
        if (!level.getBlockState(generatorPos).getValue(be.maarten.woolworks.machine.GeneratorBlock.ACTIVE)) {
            helper.fail("Generator visual state did not retain its inactivity delay"); return;
        }
        for (int i = 0; i < 38; i++) GeneratorBlockEntity.tick(level, generatorPos, level.getBlockState(generatorPos), generator);
        if (!level.getBlockState(generatorPos).getValue(be.maarten.woolworks.machine.GeneratorBlock.ACTIVE)) {
            helper.fail("Generator visual state deactivated before 40 inactive ticks"); return;
        }
        GeneratorBlockEntity.tick(level, generatorPos, level.getBlockState(generatorPos), generator);
        if (level.getBlockState(generatorPos).getValue(be.maarten.woolworks.machine.GeneratorBlock.ACTIVE)
                || level.getBlockState(extractorPos).getValue(be.maarten.woolworks.machine.ExtractorBlock.ACTIVE)
                || level.getBlockState(generatorPos).getLightEmission() != 0
                || generator.energy().getEnergyStored() != 20000 || extractor.inventory().getStackInSlot(0).isEmpty()) {
            helper.fail("Blocked machines remained active or lost their contents"); return;
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void generatorFuelProgress(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos pos = helper.absolutePos(BlockPos.ZERO);
        level.setBlockAndUpdate(pos, ModMachines.GENERATOR.get().defaultBlockState());
        var generator = (GeneratorBlockEntity)level.getBlockEntity(pos);
        var player = helper.makeMockServerPlayerInLevel();
        var menu = be.maarten.woolworks.machine.MachineMenu.generator(1, player.getInventory(), generator);
        if (menu.slots.getFirst().x != 91 || menu.slots.getFirst().y != 49) { helper.fail("Generator input slot alignment changed"); return; }
        CompoundTag saved = generator.saveCustomOnly(level.registryAccess());
        saved.putInt("Energy", 20000); saved.putInt("BurnRemaining", 200); saved.putInt("BurnTotal", 400); saved.putInt("GenerationRate", 40);
        generator.loadCustomOnly(saved, level.registryAccess());
        if (generator.menuData().get(1) != 500) { helper.fail("Fuel progress was not elapsed progress"); return; }
        GeneratorBlockEntity.tick(level, pos, level.getBlockState(pos), generator);
        if (generator.menuData().get(1) != 500 || generator.remainingFuelTicks() != 200 || generator.isProcessing()) { helper.fail("Fuel progress advanced while generator was full"); return; }
        generator.energy().deserializeNBT(level.registryAccess(), IntTag.valueOf(0));
        GeneratorBlockEntity.tick(level, pos, level.getBlockState(pos), generator);
        if (generator.menuData().get(1) <= 500) { helper.fail("Fuel progress did not resume"); return; }
        saved = generator.saveCustomOnly(level.registryAccess()); saved.putInt("BurnRemaining", 1);
        generator.loadCustomOnly(saved, level.registryAccess());
        GeneratorBlockEntity.tick(level, pos, level.getBlockState(pos), generator);
        if (generator.menuData().get(1) != 0) { helper.fail("Completed fuel progress did not reset"); return; }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void cablePlacementDefaults(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos pos = helper.absolutePos(BlockPos.ZERO);
        level.setBlockAndUpdate(pos.west(), ModMachines.GENERATOR.get().defaultBlockState());
        level.setBlockAndUpdate(pos.east(), ModMachines.EXTRACTOR.get().defaultBlockState());
        level.setBlockAndUpdate(pos.north(), ModMachines.CABLE.get().defaultBlockState());
        level.setBlockAndUpdate(pos, ModMachines.CABLE.get().defaultBlockState());
        var cable = (CableBlockEntity)level.getBlockEntity(pos);
        ModMachines.CABLE.get().setPlacedBy(level, pos, level.getBlockState(pos), null, new ItemStack(ModMachines.CABLE_ITEM.get()));
        if (cable == null || !cable.isExtracting(Direction.WEST) || cable.isExtracting(Direction.EAST) || cable.isExtracting(Direction.NORTH)) {
            helper.fail("Cable did not default only its extract-only source to extraction"); return;
        }
        var storage = new net.neoforged.neoforge.energy.EnergyStorage(100, 100, 100);
        var receiver = new net.neoforged.neoforge.energy.EnergyStorage(100, 100, 0);
        var source = new net.neoforged.neoforge.energy.EnergyStorage(100, 0, 100);
        if (be.maarten.woolworks.machine.CableBlock.defaultsToExtraction(storage)
                || be.maarten.woolworks.machine.CableBlock.defaultsToExtraction(receiver)
                || !be.maarten.woolworks.machine.CableBlock.defaultsToExtraction(source)) {
            helper.fail("Generic FE source classification was incorrect"); return;
        }
        cable.setExtracting(Direction.WEST, false);
        level.setBlockAndUpdate(pos.west(), level.getBlockState(pos.west()).setValue(be.maarten.woolworks.machine.GeneratorBlock.ACTIVE, true));
        var restored = new CableBlockEntity(pos, level.getBlockState(pos));
        restored.loadCustomOnly(cable.saveCustomOnly(level.registryAccess()), level.registryAccess());
        if (cable.isExtracting(Direction.WEST) || restored.isExtracting(Direction.WEST)) { helper.fail("Manual passive setting was overwritten"); return; }
        ItemStack tagged = new ItemStack(ModMachines.CABLE_ITEM.get());
        CompoundTag data = new CompoundTag(); data.putInt("ExtractionMask", 0);
        net.minecraft.world.item.BlockItem.setBlockEntityData(tagged, ModMachines.CABLE_ENTITY.get(), data);
        ModMachines.CABLE.get().setPlacedBy(level, pos, level.getBlockState(pos), null, tagged);
        if (cable.isExtracting(Direction.WEST)) { helper.fail("Saved explicit extraction state was overwritten"); return; }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void cableBlockItemPlacement(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos pos = helper.absolutePos(BlockPos.ZERO);
        level.setBlockAndUpdate(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
        level.setBlockAndUpdate(pos.west(), ModMachines.GENERATOR.get().defaultBlockState());
        level.setBlockAndUpdate(pos.below(), net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
        var player = helper.makeMockServerPlayerInLevel();
        player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        player.setPos(pos.getX() + .5, pos.getY() + 2, pos.getZ() + .5);
        var stack = new ItemStack(ModMachines.CABLE_ITEM.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        var hit = new BlockHitResult(Vec3.atCenterOf(pos.below()), Direction.UP, pos.below(), false);
        var context = new net.minecraft.world.item.context.BlockPlaceContext(new net.minecraft.world.item.context.UseOnContext(player, InteractionHand.MAIN_HAND, hit));
        var result = ((net.minecraft.world.item.BlockItem)ModMachines.CABLE_ITEM.get()).place(context);
        if (!result.consumesAction() || !(level.getBlockEntity(pos) instanceof CableBlockEntity cable) || !cable.isExtracting(Direction.WEST)) {
            helper.fail("Block item placement: result=" + result + ", state=" + level.getBlockState(pos) + ", entity=" + level.getBlockEntity(pos)); return;
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void workerCrafting(GameTestHelper helper) {
        var level = helper.getLevel();
        for (var id : level.registryAccess().registryOrThrow(ResourceTypes.REGISTRY).keySet()) {
            if (!ResourceTypes.enabled(level, id)) continue;
            var input = CraftingInput.of(3, 3, List.of(new ItemStack(Items.IRON_INGOT), new ItemStack(Items.IRON_INGOT), new ItemStack(Items.IRON_INGOT),
                    new ItemStack(Items.COPPER_INGOT), new ItemStack(Items.COPPER_INGOT), new ItemStack(Items.REDSTONE),
                    new ItemStack(Items.REDSTONE), new ItemStack(Items.SHEARS), ModItems.withResource(ModItems.INFUSED_WOOL.get(), id)));
            var recipe = level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, level);
            if (recipe.isEmpty() || !recipe.get().value().assemble(input, level.registryAccess()).is(ModMachines.WORKER_ITEM.get())) {
                helper.fail("Worker recipe rejected " + id); return;
            }
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void speedUpgradeLimit(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos pos = helper.absolutePos(BlockPos.ZERO);
        level.setBlockAndUpdate(pos, ModMachines.EXTRACTOR.get().defaultBlockState());
        var extractor = (be.maarten.woolworks.machine.ExtractorBlockEntity) level.getBlockEntity(pos);
        var stack = new ItemStack(ModItems.SPEED_UPGRADE.get(), 16);
        var remainder = extractor.inventory().insertItem(2, stack, false);
        if (extractor.inventory().getStackInSlot(2).getCount() != 8 || remainder.getCount() != 8
                || be.maarten.woolworks.item.ExtractorUpgradeItem.limit(stack) != 8) {
            helper.fail("Speed upgrade item limit is not enforced"); return;
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void workerAreaAndAutomation(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos pos = helper.absolutePos(BlockPos.ZERO);
        level.setBlockAndUpdate(pos, ModMachines.WORKER.get().defaultBlockState());
        var worker = (be.maarten.woolworks.machine.WorkerBlockEntity) level.getBlockEntity(pos);
        if (worker == null || worker.area() != 3) { helper.fail("Worker default area incorrect"); return; }
        var overCapacity = worker.saveCustomOnly(level.registryAccess());
        overCapacity.putInt("Energy", 90000);
        var cappedWorker = new be.maarten.woolworks.machine.WorkerBlockEntity(pos, level.getBlockState(pos));
        cappedWorker.loadCustomOnly(overCapacity, level.registryAccess());
        if (cappedWorker.energy().getMaxEnergyStored() != 20000 || cappedWorker.energy().getEnergyStored() != 20000) {
            helper.fail("Worker energy was not capped at fixed capacity"); return;
        }
        for (Direction dir : List.of(Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST)) {
            for (int size : List.of(1, 3, 5, 7, 9)) {
                var bounds = be.maarten.woolworks.machine.WorkerArea.bounds(pos, dir, size);
                if (!bounds.contains(Vec3.atCenterOf(pos.relative(dir, size))) || bounds.contains(Vec3.atCenterOf(pos))
                        || bounds.contains(Vec3.atCenterOf(pos.relative(dir, size + 1))) || bounds.contains(Vec3.atCenterOf(pos.relative(dir).above(2)))) {
                    helper.fail("Incorrect worker bounds " + dir + " " + size); return;
                }
            }
        }
        for (Direction dir : Direction.values()) {
            var items = level.getCapability(Capabilities.ItemHandler.BLOCK, pos, dir);
            var energy = level.getCapability(Capabilities.EnergyStorage.BLOCK, pos, dir);
            if (items == null || items.getSlots() != 20 || energy == null || !energy.canReceive() || energy.canExtract()
                    || !items.isItemValid(0, new ItemStack(Items.SHEARS)) || items.isItemValid(2, new ItemStack(Items.WHITE_WOOL))) {
                helper.fail("Worker automation failed on " + dir); return;
            }
        }
        for (int size : List.of(5, 7, 9, 1, 3)) {
            worker.cycleArea();
            var saved = worker.saveCustomOnly(level.registryAccess());
            var restored = new be.maarten.woolworks.machine.WorkerBlockEntity(pos, level.getBlockState(pos));
            restored.loadCustomOnly(saved, level.registryAccess());
            if (worker.area() != size || restored.area() != size) { helper.fail("Worker area cycle or persistence failed at " + size); return; }
        }
        if (be.maarten.woolworks.machine.MachineConfig.WORKER_INTERVAL.getDefault() != 200) { helper.fail("Worker interval default is not 200 ticks"); return; }
        int interval = be.maarten.woolworks.machine.MachineConfig.WORKER_INTERVAL.get();
        var data = worker.data();
        int syncedElapsed = (data.get(3) & 65535) | ((data.get(4) & 65535) << 16);
        int syncedInterval = (data.get(5) & 65535) | ((data.get(6) & 65535) << 16);
        if (syncedInterval != interval || syncedElapsed < 0 || syncedElapsed > interval) {
            helper.fail("Worker timer does not reflect the operation schedule"); return;
        }
        var legacy = worker.saveCustomOnly(level.registryAccess());
        legacy.putInt("Area", 11);
        var restoredLegacy = new be.maarten.woolworks.machine.WorkerBlockEntity(pos, level.getBlockState(pos));
        restoredLegacy.loadCustomOnly(legacy, level.registryAccess());
        if (restoredLegacy.area() != 9) { helper.fail("Legacy 11x11 area was not migrated"); return; }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void workerTimerPausesAndPersists(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos pos = helper.absolutePos(BlockPos.ZERO);
        level.setBlockAndUpdate(pos, ModMachines.WORKER.get().defaultBlockState());
        var worker = (be.maarten.woolworks.machine.WorkerBlockEntity) level.getBlockEntity(pos);
        CompoundTag saved = worker.saveCustomOnly(level.registryAccess());
        saved.putInt("OperationProgress", 7);
        worker.loadCustomOnly(saved, level.registryAccess());
        worker.inventory().setStackInSlot(0, new ItemStack(Items.SHEARS));
        for (int i = 0; i < 20; i++) be.maarten.woolworks.machine.WorkerBlockEntity.tick(level, pos, level.getBlockState(pos), worker);
        if (worker.data().get(3) != 7 || worker.operationStatus() != be.maarten.woolworks.machine.WorkerBlockEntity.INSUFFICIENT_ENERGY) {
            helper.fail("Worker timer did not pause without energy"); return;
        }
        var restored = new be.maarten.woolworks.machine.WorkerBlockEntity(pos, level.getBlockState(pos));
        restored.loadCustomOnly(worker.saveCustomOnly(level.registryAccess()), level.registryAccess());
        if (restored.data().get(3) != 7) { helper.fail("Paused worker progress did not persist"); return; }
        worker.energy().receiveEnergy(1000, false);
        for (int i = 0; i < 10; i++) be.maarten.woolworks.machine.WorkerBlockEntity.tick(level, pos, level.getBlockState(pos), worker);
        if (worker.data().get(3) != 17 || worker.energy().getEnergyStored() != 1000) {
            helper.fail("Worker timer did not resume safely"); return;
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void workerFullOutputRegrowth(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos pos = helper.absolutePos(BlockPos.ZERO);
        Direction facing = (pos.getZ() & 15) == 0 ? Direction.SOUTH : Direction.NORTH;
        level.setBlockAndUpdate(pos, ModMachines.WORKER.get().defaultBlockState().setValue(be.maarten.woolworks.machine.WorkerBlock.FACING, facing));
        var worker = (be.maarten.woolworks.machine.WorkerBlockEntity) level.getBlockEntity(pos);
        var sheep = net.minecraft.world.entity.EntityType.SHEEP.create(level);
        if (worker == null || sheep == null) { helper.fail("Missing worker or sheep"); return; }
        sheep.setPos(pos.getX() + .5, pos.getY(), pos.getZ() + .5 + facing.getStepZ());
        sheep.setNoAi(true); sheep.setNoGravity(true); sheep.setInvulnerable(true); sheep.setAge(0); sheep.setSheared(true);
        level.addFreshEntity(sheep);
        worker.inventory().setStackInSlot(0, new ItemStack(Items.SHEARS));
        worker.inventory().setStackInSlot(1, new ItemStack(Items.WHEAT));
        for (int i = 2; i < 20; i++) worker.inventory().setStackInSlot(i, new ItemStack(Items.STONE, 64));
        worker.energy().receiveEnergy(1000, false);
        CompoundTag saved = worker.saveCustomOnly(level.registryAccess());
        saved.putInt("OperationProgress", be.maarten.woolworks.machine.MachineConfig.WORKER_INTERVAL.get() - 1);
        worker.loadCustomOnly(saved, level.registryAccess());
        be.maarten.woolworks.machine.WorkerBlockEntity.tick(level, pos, level.getBlockState(pos), worker);
        if (sheep.isSheared() || worker.energy().getEnergyStored() != 600 || !worker.inventory().getStackInSlot(1).isEmpty()
                || worker.operationStatus() != be.maarten.woolworks.machine.WorkerBlockEntity.OUTPUT_FULL) {
            helper.fail("Full output blocked regrowth or consumed shearing energy"); return;
        }
        worker.inventory().setStackInSlot(2, new ItemStack(Items.WHITE_WOOL, 61));
        saved = worker.saveCustomOnly(level.registryAccess());
        saved.putInt("OperationProgress", be.maarten.woolworks.machine.MachineConfig.WORKER_INTERVAL.get() - 1);
        worker.loadCustomOnly(saved, level.registryAccess());
        be.maarten.woolworks.machine.WorkerBlockEntity.tick(level, pos, level.getBlockState(pos), worker);
        if (!sheep.isSheared() || worker.energy().getEnergyStored() != 400 || worker.inventory().getStackInSlot(2).getCount() <= 61) {
            helper.fail("Worker did not resume into partially filled compatible output"); return;
        }
        sheep.discard(); helper.succeed();
    }

    @GameTest(template = "empty")
    public static void workerReservedYield(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos pos = helper.absolutePos(BlockPos.ZERO);
        Direction facing = (pos.getZ() & 15) == 0 ? Direction.SOUTH : Direction.NORTH;
        level.setBlockAndUpdate(pos, ModMachines.WORKER.get().defaultBlockState().setValue(be.maarten.woolworks.machine.WorkerBlock.FACING, facing));
        var worker = (be.maarten.woolworks.machine.WorkerBlockEntity) level.getBlockEntity(pos);
        var sheep = net.minecraft.world.entity.EntityType.SHEEP.create(level);
        if (worker == null || sheep == null) { helper.fail("Missing worker or sheep"); return; }
        sheep.setColor(net.minecraft.world.item.DyeColor.WHITE);
        sheep.setPos(pos.getX() + .5, pos.getY(), pos.getZ() + .5 + facing.getStepZ());
        sheep.setNoAi(true); sheep.setNoGravity(true); sheep.setInvulnerable(true); sheep.setAge(0); level.addFreshEntity(sheep);
        worker.inventory().setStackInSlot(0, new ItemStack(Items.SHEARS));
        worker.inventory().setStackInSlot(2, new ItemStack(Items.WHITE_WOOL, 63));
        for (int i = 3; i < 20; i++) worker.inventory().setStackInSlot(i, new ItemStack(Items.STONE, 64));
        worker.energy().receiveEnergy(1000, false);
        CompoundTag saved = worker.saveCustomOnly(level.registryAccess());
        CompoundTag plan = new CompoundTag();
        plan.putUUID("Sheep", sheep.getUUID());
        plan.put("Wool", new ItemStack(Items.WHITE_WOOL).save(level.registryAccess()));
        var drops = new net.minecraft.nbt.ListTag();
        drops.add(new ItemStack(Items.WHITE_WOOL, 2).save(level.registryAccess()));
        plan.put("Drops", drops);
        var plans = new net.minecraft.nbt.ListTag();
        plans.add(plan);
        saved.put("PendingShearing", plans);
        saved.putInt("OperationProgress", be.maarten.woolworks.machine.MachineConfig.WORKER_INTERVAL.get() - 1);
        worker.loadCustomOnly(saved, level.registryAccess());
        be.maarten.woolworks.machine.WorkerBlockEntity.tick(level, pos, level.getBlockState(pos), worker);
        if (sheep.isSheared() || worker.energy().getEnergyStored() != 1000 || worker.inventory().getStackInSlot(2).getCount() != 63
                || worker.saveCustomOnly(level.registryAccess()).getList("PendingShearing", 10).size() != 1) {
            helper.fail("A result that did not fit was consumed, changed, or lost"); return;
        }
        worker.inventory().setStackInSlot(2, new ItemStack(Items.WHITE_WOOL, 62));
        saved = worker.saveCustomOnly(level.registryAccess());
        saved.putInt("OperationProgress", be.maarten.woolworks.machine.MachineConfig.WORKER_INTERVAL.get() - 1);
        worker.loadCustomOnly(saved, level.registryAccess());
        be.maarten.woolworks.machine.WorkerBlockEntity.tick(level, pos, level.getBlockState(pos), worker);
        if (!sheep.isSheared() || worker.energy().getEnergyStored() != 800 || worker.inventory().getStackInSlot(2).getCount() != 64
                || !worker.saveCustomOnly(level.registryAccess()).getList("PendingShearing", 10).isEmpty()) {
            helper.fail("Reserved result did not resume exactly once"); return;
        }
        sheep.discard(); helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 240)
    public static void workerRegrowsAndShears(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos pos = helper.absolutePos(BlockPos.ZERO);
        Direction facing = (pos.getZ() & 15) == 0 ? Direction.SOUTH : Direction.NORTH;
        level.setBlockAndUpdate(pos, ModMachines.WORKER.get().defaultBlockState().setValue(be.maarten.woolworks.machine.WorkerBlock.FACING, facing));
        var worker = (be.maarten.woolworks.machine.WorkerBlockEntity) level.getBlockEntity(pos);
        var sheep = net.minecraft.world.entity.EntityType.SHEEP.create(level);
        if (worker == null || sheep == null) { helper.fail("Worker or sheep missing"); return; }
        sheep.setPos(pos.getX() + .5, pos.getY(), pos.getZ() + .5 + facing.getStepZ());
        sheep.setNoAi(true); sheep.setNoGravity(true); sheep.setInvulnerable(true); sheep.setAge(0); sheep.setSheared(true); level.addFreshEntity(sheep);
        worker.inventory().setStackInSlot(0, new ItemStack(Items.SHEARS));
        worker.inventory().setStackInSlot(1, new ItemStack(Items.WHEAT));
        worker.energy().receiveEnergy(1000, false);
        helper.runAfterDelay(210, () -> {
            int wool = 0;
            for (int i = 2; i < 20; i++) wool += worker.inventory().getStackInSlot(i).getCount();
            if (!sheep.isSheared() || wool < 1 || wool > 3 || worker.energy().getEnergyStored() != 400 || !worker.inventory().getStackInSlot(1).isEmpty()) {
                helper.fail("Worker failed cycle: sheared=" + sheep.isSheared() + ", wool=" + wool + ", energy=" + worker.energy().getEnergyStored() + ", wheat=" + worker.inventory().getStackInSlot(1).getCount() + ", pos=" + sheep.position()); return;
            }
            sheep.discard(); helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 240)
    public static void workerShearsResourceSheep(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos pos = helper.absolutePos(BlockPos.ZERO);
        Direction facing = (pos.getZ() & 15) == 0 ? Direction.SOUTH : Direction.NORTH;
        level.setBlockAndUpdate(pos, ModMachines.WORKER.get().defaultBlockState().setValue(be.maarten.woolworks.machine.WorkerBlock.FACING, facing));
        var worker = (be.maarten.woolworks.machine.WorkerBlockEntity) level.getBlockEntity(pos);
        var sheep = be.maarten.woolworks.registry.ModEntities.RESOURCE_SHEEP.get().create(level);
        if (worker == null || sheep == null) { helper.fail("Worker or resource sheep missing"); return; }
        var id = ResourceLocation.fromNamespaceAndPath("woolworks", "iron");
        sheep.setResourceId(id); sheep.setPos(pos.getX() + .5, pos.getY(), pos.getZ() + .5 + facing.getStepZ());
        sheep.setNoAi(true); sheep.setNoGravity(true); sheep.setInvulnerable(true); sheep.setAge(0); level.addFreshEntity(sheep);
        worker.inventory().setStackInSlot(0, new ItemStack(Items.SHEARS));
        worker.energy().receiveEnergy(1000, false);
        long startTime = level.getGameTime();
        helper.runAfterDelay(210, () -> {
            ItemStack output = worker.inventory().getStackInSlot(2);
            if (!sheep.isSheared() || !id.equals(sheep.resourceId()) || !output.is(ModItems.INFUSED_WOOL.get())
                    || !id.equals(output.get(be.maarten.woolworks.registry.ModComponents.RESOURCE_TYPE)) || worker.energy().getEnergyStored() != 800) {
                helper.fail("Worker resource sheep: sheared=" + sheep.isSheared() + ", output=" + output + ", energy=" + worker.energy().getEnergyStored() + ", pos=" + sheep.position() + ", dt=" + (level.getGameTime() - startTime) + ", interval=" + be.maarten.woolworks.machine.MachineConfig.WORKER_INTERVAL.get() + ", ticking=" + level.shouldTickBlocksAt(sheep.blockPosition()) + ", ready=" + sheep.readyForShearing() + ", alive=" + sheep.isAlive() + ", health=" + sheep.getHealth() + ", baby=" + sheep.isBaby() + ", type=" + sheep.resourceType() + ", block=" + level.getBlockState(pos)); return;
            }
            sheep.discard(); helper.succeed();
        });
    }

    @GameTest(template = "empty")
    public static void defaultRecipes(GameTestHelper helper) {
        var level = helper.getLevel();
        var manager = level.getRecipeManager();
        ItemStack iron = ModItems.withResource(ModItems.INFUSED_WOOL.get(), ResourceLocation.fromNamespaceAndPath("woolworks", "iron"));
        ItemStack coal = ModItems.withResource(ModItems.INFUSED_WOOL.get(), ResourceLocation.fromNamespaceAndPath("woolworks", "coal"));
        var extraction = manager.getRecipeFor(ModRecipes.WOOL_EXTRACTION.get(), new SingleRecipeInput(iron), level);
        if (extraction.isEmpty() || !extraction.get().value().result().is(Items.IRON_INGOT)) {
            helper.fail("Iron wool must match the iron extraction recipe");
            return;
        }
        var fuel = manager.getRecipeFor(ModRecipes.GENERATOR_FUEL.get(), new SingleRecipeInput(coal), level);
        if (fuel.isEmpty() || fuel.get().value().energyPerTick() != 40 || fuel.get().value().burnTicks() != 400) {
            helper.fail("Coal wool must match the generator fuel recipe");
            return;
        }
        if (manager.getRecipeFor(ModRecipes.GENERATOR_FUEL.get(), new SingleRecipeInput(iron), level).isEmpty()
                || manager.getRecipeFor(ModRecipes.GENERATOR_FUEL.get(), new SingleRecipeInput(new ItemStack(Items.COAL)), level).isPresent()) {
            helper.fail("Iron wool and vanilla coal must not match while coal resource exists");
            return;
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void generatorCapabilities(GameTestHelper helper) {
        BlockPos relative = BlockPos.ZERO;
        helper.setBlock(relative, ModMachines.GENERATOR.get());
        BlockPos pos = helper.absolutePos(relative);
        var level = helper.getLevel();
        var items = level.getCapability(Capabilities.ItemHandler.BLOCK, pos, Direction.UP);
        var energy = level.getCapability(Capabilities.EnergyStorage.BLOCK, pos, Direction.NORTH);
        if (items == null || energy == null || energy.canReceive() || !energy.canExtract()) {
            helper.fail("Generator capabilities must expose insertion and extraction-only energy");
            return;
        }
        ItemStack invalid = new ItemStack(Items.DIRT);
        if (items.insertItem(0, invalid, true).isEmpty()) {
            helper.fail("Generator accepted invalid fuel");
            return;
        }
        ItemStack coal = ModItems.withResource(ModItems.INFUSED_WOOL.get(), ResourceLocation.fromNamespaceAndPath("woolworks", "coal"));
        if (!items.insertItem(0, coal, false).isEmpty()) {
            helper.fail("Generator rejected coal wool");
            return;
        }
        helper.runAfterDelay(3, () -> {
            if (energy.getEnergyStored() <= 0 || !items.getStackInSlot(0).isEmpty()) {
                helper.fail("Generator did not consume fuel and produce energy");
                return;
            }
            helper.succeed();
        });
    }

    @GameTest(template = "empty")
    public static void generatorBurnAndMigration(GameTestHelper helper) {
        var level = helper.getLevel();
        var pos = helper.absolutePos(BlockPos.ZERO);
        level.setBlockAndUpdate(pos, ModMachines.GENERATOR.get().defaultBlockState());
        var generator = (GeneratorBlockEntity) level.getBlockEntity(pos);
        var old = new net.neoforged.neoforge.items.ItemStackHandler(2);
        old.setStackInSlot(1, new ItemStack(Items.BUCKET));
        var save = new CompoundTag();
        save.put("Fuel", old.serializeNBT(level.registryAccess()));
        generator.loadCustomOnly(save, level.registryAccess());
        if (generator.fuel().getSlots() != 1 || !generator.migratedRemainder().is(Items.BUCKET)) {
            helper.fail("Legacy output was not migrated"); return;
        }
        generator.fuel().setStackInSlot(0, ModItems.withResource(ModItems.INFUSED_WOOL.get(), ResourceLocation.fromNamespaceAndPath("woolworks", "iron")));
        for (int i = 0; i < 400; i++) GeneratorBlockEntity.tick(level, pos, level.getBlockState(pos), generator);
        if (generator.energy().getEnergyStored() != 16000 || !generator.fuel().getStackInSlot(0).isEmpty()) {
            helper.fail("Generator burn did not produce exactly 16000 FE"); return;
        }
        generator.energy().deserializeNBT(level.registryAccess(), IntTag.valueOf(20000));
        generator.fuel().setStackInSlot(0, ModItems.withResource(ModItems.INFUSED_WOOL.get(), ResourceLocation.fromNamespaceAndPath("woolworks", "iron")));
        GeneratorBlockEntity.tick(level, pos, level.getBlockState(pos), generator);
        if (generator.fuel().getStackInSlot(0).isEmpty()) { helper.fail("Full generator consumed fuel"); return; }
        var nearby = level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
                new net.minecraft.world.phys.AABB(pos).inflate(2), e -> e.getItem().is(Items.BUCKET));
        if (nearby.size() != 1) { helper.fail("Legacy output was not safely returned"); return; }
        nearby.getFirst().discard();
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void furnaceFuelsAndRemainders(GameTestHelper helper) {
        var level = helper.getLevel();
        var lava = new ItemStack(Items.LAVA_BUCKET);
        var selected = GeneratorFuels.find(level, lava);
        var wool = ModItems.withResource(ModItems.INFUSED_WOOL.get(), ResourceLocation.fromNamespaceAndPath("woolworks", "coal"));
        var special = GeneratorFuels.find(level, wool);
        if (selected.isPresent() || special.isEmpty() || special.get().energyPerTick() != 40 || special.get().ticks() != 400) {
            helper.fail("Furnace fuel selection or custom-fuel priority failed"); return;
        }
        helper.setBlock(BlockPos.ZERO, ModMachines.GENERATOR.get());
        var handler = level.getCapability(Capabilities.ItemHandler.BLOCK, helper.absolutePos(BlockPos.ZERO), Direction.UP);
        if (handler == null || handler.getSlots() != 1 || handler.insertItem(0, lava, false).isEmpty()) { helper.fail("Lava fuel accepted or unexpected output slot"); return; }
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 260)
    public static void extractorProcessesIron(GameTestHelper helper) {
        helper.setBlock(BlockPos.ZERO, ModMachines.EXTRACTOR.get());
        BlockPos pos = helper.absolutePos(BlockPos.ZERO);
        var level = helper.getLevel();
        var items = level.getCapability(Capabilities.ItemHandler.BLOCK, pos, Direction.UP);
        var energy = level.getCapability(Capabilities.EnergyStorage.BLOCK, pos, Direction.DOWN);
        if (items == null || energy == null || energy.canExtract() || !energy.canReceive()) {
            helper.fail("Extractor capabilities are missing or incorrectly directed");
            return;
        }
        ItemStack iron = ModItems.withResource(ModItems.INFUSED_WOOL.get(), ResourceLocation.fromNamespaceAndPath("woolworks", "iron"));
        if (!items.insertItem(0, iron, false).isEmpty() || energy.receiveEnergy(8000, false) != 8000) {
            helper.fail("Extractor rejected valid input or energy");
            return;
        }
        helper.runAfterDelay(210, () -> {
            if (!items.extractItem(1, 1, false).is(Items.IRON_INGOT) || !items.getStackInSlot(0).isEmpty()) {
                helper.fail("Extractor did not produce iron from infused wool");
                return;
            }
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 260)
    public static void adjacentGeneratorFeedsExtractor(GameTestHelper helper) {
        BlockPos generatorPos = helper.absolutePos(BlockPos.ZERO);
        BlockPos extractorPos = generatorPos.east();
        var level = helper.getLevel();
        level.setBlockAndUpdate(generatorPos, ModMachines.GENERATOR.get().defaultBlockState());
        level.setBlockAndUpdate(extractorPos, ModMachines.EXTRACTOR.get().defaultBlockState());
        var fuel = level.getCapability(Capabilities.ItemHandler.BLOCK, generatorPos, Direction.UP);
        var inventory = level.getCapability(Capabilities.ItemHandler.BLOCK, extractorPos, Direction.UP);
        if (fuel == null || inventory == null) { helper.fail("Machine capabilities missing"); return; }
        var coal = ModItems.withResource(ModItems.INFUSED_WOOL.get(), ResourceLocation.fromNamespaceAndPath("woolworks", "coal"));
        var iron = ModItems.withResource(ModItems.INFUSED_WOOL.get(), ResourceLocation.fromNamespaceAndPath("woolworks", "iron"));
        if (!fuel.insertItem(0, coal, false).isEmpty() || !inventory.insertItem(0, iron, false).isEmpty()) {
            helper.fail("Machine inputs were rejected");
            return;
        }
        helper.runAfterDelay(220, () -> {
            if (!inventory.extractItem(1, 1, false).is(Items.IRON_INGOT)) {
                helper.fail("Adjacent generator did not power the extractor");
                return;
            }
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 260)
    public static void cableTransfersAndLimits(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos start = helper.absolutePos(BlockPos.ZERO);
        BlockPos end = start.east(2);
        level.setBlockAndUpdate(start.west(), ModMachines.GENERATOR.get().defaultBlockState());
        level.setBlockAndUpdate(start, ModMachines.CABLE.get().defaultBlockState());
        level.setBlockAndUpdate(start.east(), ModMachines.CABLE.get().defaultBlockState());
        level.setBlockAndUpdate(end, ModMachines.EXTRACTOR.get().defaultBlockState());
        var cable = level.getCapability(Capabilities.EnergyStorage.BLOCK, start, Direction.WEST);
        var receiver = level.getCapability(Capabilities.EnergyStorage.BLOCK, end, Direction.WEST);
        if (cable == null || cable.canReceive() || cable.receiveEnergy(1500, false) != 0) {
            helper.fail("Passive cable accepted pushed energy"); return;
        }
        ((CableBlockEntity) level.getBlockEntity(start)).setExtracting(Direction.WEST, true);
        if (cable == null || receiver == null || cable.receiveEnergy(1500, true) != 160 || receiver.getEnergyStored() != 0) {
            helper.fail("Cable simulation or transfer limit is incorrect"); return;
        }
        if (cable.receiveEnergy(1500, false) != 160 || cable.receiveEnergy(1, false) != 0 || receiver.getEnergyStored() != 160) {
            helper.fail("Cable did not enforce its shared per-tick limit"); return;
        }
        helper.runAfterDelay(1, () -> {
            if (cable.receiveEnergy(500, false) != 160 || receiver.getEnergyStored() != 320) {
                helper.fail("Cable did not reset its transfer limit"); return;
            }
            level.removeBlock(start.east(), false);
            helper.runAfterDelay(1, () -> {
                if (cable.receiveEnergy(100, false) != 0) { helper.fail("Cable transferred through a removed connection"); return; }
                helper.succeed();
            });
        });
    }

    @GameTest(template = "empty")
    public static void cableRecipeUsesCopperWool(GameTestHelper helper) {
        var level = helper.getLevel();
        var copper = ModItems.withResource(ModItems.INFUSED_WOOL.get(), ResourceLocation.fromNamespaceAndPath("woolworks", "copper"));
        ItemStack shears = new ItemStack(Items.SHEARS);
        shears.setDamageValue(17);
        shears.set(DataComponents.CUSTOM_NAME, Component.literal("Test shears"));
        ItemStack damaged = shears.copy();
        damaged.setDamageValue(18);
        var good = CraftingInput.of(2, 1, List.of(copper, shears));
        var result = level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, good, level);
        if (result.isEmpty() || !result.get().value().assemble(good, level.registryAccess()).is(ModMachines.CABLE_ITEM.get())) {
            helper.fail("Copper-infused wool did not craft cable"); return;
        }
        if (!ItemStack.matches(result.get().value().getRemainingItems(good).get(1), damaged)) {
            helper.fail("Shears were not returned with one point of damage"); return;
        }
        var almostBroken = shears.copy();
        almostBroken.setDamageValue(almostBroken.getMaxDamage() - 1);
        var breakingInput = CraftingInput.of(2, 1, List.of(copper, almostBroken));
        if (!result.get().value().getRemainingItems(breakingInput).get(1).isEmpty()) {
            helper.fail("Exhausted shears did not break"); return;
        }
        var unbreakable = almostBroken.copy();
        unbreakable.set(DataComponents.UNBREAKABLE, new Unbreakable(true));
        var unbreakableInput = CraftingInput.of(2, 1, List.of(copper, unbreakable));
        if (!ItemStack.matches(result.get().value().getRemainingItems(unbreakableInput).get(1), unbreakable)) {
            helper.fail("Unbreakable shears were damaged"); return;
        }
        var fallback = CraftingInput.of(3, 1, List.of(new ItemStack(Items.WHITE_WOOL), new ItemStack(Items.COPPER_INGOT), shears));
        var vanilla = level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, fallback, level);
        if (vanilla.isEmpty() || vanilla.get().value().assemble(fallback, level.registryAccess()).getCount() != 4
                || !ItemStack.matches(vanilla.get().value().getRemainingItems(fallback).get(2), damaged)) {
            helper.fail("Vanilla cable recipe or shears remainder failed"); return;
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void cableCycleAndBranch(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos root = helper.absolutePos(BlockPos.ZERO);
        level.setBlockAndUpdate(root.west(), ModMachines.GENERATOR.get().defaultBlockState());
        level.setBlockAndUpdate(root, ModMachines.CABLE.get().defaultBlockState());
        level.setBlockAndUpdate(root.east(), ModMachines.CABLE.get().defaultBlockState());
        level.setBlockAndUpdate(root.east().south(), ModMachines.CABLE.get().defaultBlockState());
        level.setBlockAndUpdate(root.south(), ModMachines.CABLE.get().defaultBlockState());
        level.setBlockAndUpdate(root.east(2), ModMachines.EXTRACTOR.get().defaultBlockState());
        level.setBlockAndUpdate(root.south(2), ModMachines.EXTRACTOR.get().defaultBlockState());
        var input = level.getCapability(Capabilities.EnergyStorage.BLOCK, root, Direction.WEST);
        ((CableBlockEntity) level.getBlockEntity(root)).setExtracting(Direction.WEST, true);
        var first = level.getCapability(Capabilities.EnergyStorage.BLOCK, root.east(2), Direction.WEST);
        var second = level.getCapability(Capabilities.EnergyStorage.BLOCK, root.south(2), Direction.NORTH);
        if (input == null || first == null || second == null || input.receiveEnergy(1000, false) != 160
                || first.getEnergyStored() + second.getEnergyStored() != 160) {
            helper.fail("Cyclic cable network failed to deliver energy without duplication"); return;
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void sharedCableSegmentLimit(GameTestHelper helper) {
        var level = helper.getLevel();
        var root = helper.absolutePos(BlockPos.ZERO);
        level.setBlockAndUpdate(root, ModMachines.CABLE.get().defaultBlockState());
        level.setBlockAndUpdate(root.west(), ModMachines.GENERATOR.get().defaultBlockState());
        level.setBlockAndUpdate(root.north(), ModMachines.GENERATOR.get().defaultBlockState());
        level.setBlockAndUpdate(root.east(), ModMachines.EXTRACTOR.get().defaultBlockState());
        var cable = (CableBlockEntity) level.getBlockEntity(root);
        cable.setExtracting(Direction.WEST, true);
        cable.setExtracting(Direction.NORTH, true);
        var west = level.getCapability(Capabilities.EnergyStorage.BLOCK, root, Direction.WEST);
        var north = level.getCapability(Capabilities.EnergyStorage.BLOCK, root, Direction.NORTH);
        int total = west.receiveEnergy(100, false) + north.receiveEnergy(100, false);
        if (total != 160 || west.receiveEnergy(1, false) != 0) { helper.fail("Sources exceeded shared cable segment throughput"); return; }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void extractorUpgradeEnergy(GameTestHelper helper) {
        var level = helper.getLevel();
        var pos = helper.absolutePos(BlockPos.ZERO);
        level.setBlockAndUpdate(pos, ModMachines.EXTRACTOR.get().defaultBlockState());
        var entity = (be.maarten.woolworks.machine.ExtractorBlockEntity) level.getBlockEntity(pos);
        entity.inventory().setStackInSlot(0, ModItems.withResource(ModItems.INFUSED_WOOL.get(), ResourceLocation.fromNamespaceAndPath("woolworks", "iron")));
        entity.inventory().setStackInSlot(2, new ItemStack(ModItems.SPEED_UPGRADE.get(), 8));
        entity.energy().receiveEnergy(1000, false);
        be.maarten.woolworks.machine.ExtractorBlockEntity.tick(level, pos, level.getBlockState(pos), entity);
        if (entity.energy().getEnergyStored() != 960) { helper.fail("Speed upgrades changed FE/t consumption"); return; }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void cableExtractionIsDirectional(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos cablePos = helper.absolutePos(BlockPos.ZERO);
        BlockPos sourcePos = cablePos.west();
        BlockPos targetPos = cablePos.east();
        level.setBlockAndUpdate(sourcePos, ModMachines.GENERATOR.get().defaultBlockState());
        level.setBlockAndUpdate(cablePos, ModMachines.CABLE.get().defaultBlockState());
        level.setBlockAndUpdate(targetPos, ModMachines.EXTRACTOR.get().defaultBlockState());
        var generator = (GeneratorBlockEntity) level.getBlockEntity(sourcePos);
        var cable = (CableBlockEntity) level.getBlockEntity(cablePos);
        var receiver = level.getCapability(Capabilities.EnergyStorage.BLOCK, targetPos, Direction.WEST);
        if (generator == null || cable == null || receiver == null) { helper.fail("Missing machine or cable"); return; }
        generator.energy().deserializeNBT(level.registryAccess(), IntTag.valueOf(200));
        GeneratorBlockEntity.tick(level, sourcePos, level.getBlockState(sourcePos), generator);
        if (receiver.getEnergyStored() != 0 || generator.energy().getEnergyStored() != 200) {
            helper.fail("Passive cable accepted generator push"); return;
        }
        cable.tickExtraction();
        if (receiver.getEnergyStored() != 0 || generator.energy().getEnergyStored() != 200) {
            helper.fail("Passive cable extracted energy"); return;
        }
        cable.setExtracting(Direction.WEST, true);
        cable.tickExtraction();
        if (receiver.getEnergyStored() != 100 || generator.energy().getEnergyStored() != 100 || !cable.isExtracting(Direction.WEST)) {
            helper.fail("Enabled extraction did not respect the source transfer rate"); return;
        }
        GeneratorBlockEntity.tick(level, sourcePos, level.getBlockState(sourcePos), generator);
        if (receiver.getEnergyStored() != 100 || generator.energy().getEnergyStored() != 100) {
            helper.fail("Combined push and pull exceeded the generator limit"); return;
        }
        if (cable.isExtracting(Direction.EAST)) { helper.fail("Extraction leaked to another side"); return; }
        var saved = cable.saveCustomOnly(level.registryAccess());
        var restored = new CableBlockEntity(cablePos, level.getBlockState(cablePos));
        restored.loadCustomOnly(saved, level.registryAccess());
        if (!restored.isExtracting(Direction.WEST) || restored.isExtracting(Direction.EAST)) {
            helper.fail("Extraction settings failed to persist"); return;
        }
        level.removeBlock(sourcePos, false);
        cable.tickExtraction();
        if (cable.isExtracting(Direction.WEST)) { helper.fail("Removed source was not cleared"); return; }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void speedUpgradeRequiresValidResource(GameTestHelper helper) {
        var level = helper.getLevel();
        var sugar = new ItemStack(Items.SUGAR);
        var redstone = new ItemStack(Items.REDSTONE);
        if (Boolean.getBoolean("woolworks.test.custom_resource")
                && !level.registryAccess().registryOrThrow(ResourceTypes.REGISTRY).containsKey(ResourceLocation.fromNamespaceAndPath("woolworks_test", "custom"))) {
            helper.fail("The custom test datapack resource did not load"); return;
        }
        for (ResourceLocation name : level.registryAccess().registryOrThrow(ResourceTypes.REGISTRY).keySet()) {
            if (!ResourceTypes.enabled(level, name)) continue;
            var wool = ModItems.withResource(ModItems.INFUSED_WOOL.get(), name);
            var fuel = GeneratorFuels.find(level, wool);
            if (fuel.isEmpty() || fuel.get().ticks() != 400 || fuel.get().energyPerTick() != 40) {
                helper.fail("Invalid default fuel for resource " + name); return;
            }
            var input = CraftingInput.of(3, 1, List.of(sugar, redstone, wool));
            var match = level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, level);
            if (match.isEmpty() || !match.get().value().assemble(input, level.registryAccess()).is(ModItems.SPEED_UPGRADE.get())) {
                helper.fail("Speed upgrade rejected resource " + name); return;
            }
        }
        var missing = ModItems.withResource(ModItems.INFUSED_WOOL.get(), ResourceLocation.fromNamespaceAndPath("test", "missing"));
        for (ItemStack invalid : List.of(new ItemStack(Items.WHITE_WOOL), new ItemStack(ModItems.INFUSED_WOOL.get()), missing)) {
            if (GeneratorFuels.find(level, invalid).isPresent()) { helper.fail("Generator accepted invalid fuel"); return; }
            var input = CraftingInput.of(3, 1, List.of(sugar, redstone, invalid));
            if (level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, level).isPresent()) {
                helper.fail("Speed upgrade accepted invalid wool"); return;
            }
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void sneakShearsPickup(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos pos = helper.absolutePos(BlockPos.ZERO);
        level.setBlockAndUpdate(pos, ModMachines.CABLE.get().defaultBlockState());
        var player = helper.makeMockServerPlayerInLevel();
        player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        player.setPos(pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5);
        player.setShiftKeyDown(true);
        var shears = new ItemStack(Items.SHEARS);
        shears.setDamageValue(7);
        player.setItemInHand(InteractionHand.MAIN_HAND, shears);
        var hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
        var outcome = player.gameMode.useItemOn(player, level, shears, InteractionHand.MAIN_HAND, hit);
        if (!outcome.consumesAction() || !level.getBlockState(pos).isAir()
                || !player.getInventory().contains(new ItemStack(ModMachines.CABLE_ITEM.get()))
                || shears.getDamageValue() != 7) {
            helper.fail("Pickup: result=" + outcome + ", air=" + level.getBlockState(pos).isAir() + ", item=" + player.getInventory().contains(new ItemStack(ModMachines.CABLE_ITEM.get())) + ", creative=" + player.getAbilities().instabuild + ", damage=" + shears.getDamageValue()); return;
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void shiftCraftDamagesShears(GameTestHelper helper) {
        var player = helper.makeMockServerPlayerInLevel();
        player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        var wool = ModItems.withResource(ModItems.INFUSED_WOOL.get(), ResourceLocation.fromNamespaceAndPath("woolworks", "copper"));
        wool.setCount(3);
        var shears = new ItemStack(Items.SHEARS);
        shears.setDamageValue(shears.getMaxDamage() - 2);
        var menu = player.inventoryMenu;
        menu.getSlot(1).set(wool);
        menu.getSlot(2).set(shears);
        menu.clicked(0, 0, net.minecraft.world.inventory.ClickType.QUICK_MOVE, player);
        if (!menu.getSlot(2).getItem().isEmpty() || menu.getSlot(1).getItem().getCount() != 1) {
            helper.fail("Shift crafting did not stop when shears broke"); return;
        }
        int cables = 0;
        for (ItemStack item : player.getInventory().items) if (item.is(ModMachines.CABLE_ITEM.get())) cables += item.getCount();
        if (cables != 16) { helper.fail("Shift crafting produced " + cables + " cables instead of 16"); return; }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void shearsToggleAndOffhandPickup(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos pos = helper.absolutePos(BlockPos.ZERO);
        level.setBlockAndUpdate(pos.west(), ModMachines.GENERATOR.get().defaultBlockState());
        level.setBlockAndUpdate(pos.east(), ModMachines.EXTRACTOR.get().defaultBlockState());
        level.setBlockAndUpdate(pos, ModMachines.CABLE.get().defaultBlockState());
        var cable = (CableBlockEntity)level.getBlockEntity(pos);
        var player = helper.makeMockServerPlayerInLevel();
        player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        player.setPos(pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5);
        var shears = new ItemStack(Items.SHEARS);
        player.setItemInHand(InteractionHand.MAIN_HAND, shears);
        var west = new BlockHitResult(new Vec3(pos.getX() + 0.1, pos.getY() + 0.5, pos.getZ() + 0.5), Direction.WEST, pos, false);
        player.gameMode.useItemOn(player, level, shears, InteractionHand.MAIN_HAND, west);
        if (cable == null || !cable.isExtracting(Direction.WEST) || cable.isExtracting(Direction.EAST)) {
            helper.fail("Shears did not enable the selected empty energy source"); return;
        }
        var east = new BlockHitResult(new Vec3(pos.getX() + 0.9, pos.getY() + 0.5, pos.getZ() + 0.5), Direction.EAST, pos, false);
        player.gameMode.useItemOn(player, level, shears, InteractionHand.MAIN_HAND, east);
        if (cable.isExtracting(Direction.EAST)) { helper.fail("Receive-only machine enabled as source"); return; }
        player.gameMode.useItemOn(player, level, shears, InteractionHand.MAIN_HAND, west);
        if (cable.isExtracting(Direction.WEST)) { helper.fail("Shears did not disable extraction"); return; }
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        player.setItemInHand(InteractionHand.OFF_HAND, shears);
        player.setShiftKeyDown(true);
        player.gameMode.useItemOn(player, level, shears, InteractionHand.OFF_HAND, west);
        if (!level.getBlockState(pos).isAir() || !player.getInventory().contains(new ItemStack(ModMachines.CABLE_ITEM.get()))) {
            helper.fail("Offhand shears failed to pick up the cable"); return;
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void pickupPreservesPendingEnergy(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos pos = helper.absolutePos(BlockPos.ZERO);
        level.setBlockAndUpdate(pos, ModMachines.CABLE.get().defaultBlockState());
        var cable = (CableBlockEntity)level.getBlockEntity(pos);
        if (cable == null) { helper.fail("Missing cable entity"); return; }
        CompoundTag saved = new CompoundTag();
        saved.putInt("PendingEnergy", 37);
        saved.putInt("PendingSide", Direction.WEST.ordinal());
        cable.loadCustomOnly(saved, level.registryAccess());
        var player = helper.makeMockServerPlayerInLevel();
        player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        player.setPos(pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5);
        player.setShiftKeyDown(true);
        var shears = new ItemStack(Items.SHEARS);
        player.setItemInHand(InteractionHand.MAIN_HAND, shears);
        player.gameMode.useItemOn(player, level, shears, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false));
        for (ItemStack stack : player.getInventory().items) {
            if (!stack.is(ModMachines.CABLE_ITEM.get())) continue;
            var data = stack.get(DataComponents.BLOCK_ENTITY_DATA);
            if (data != null && data.copyTag().getInt("PendingEnergy") == 37) { helper.succeed(); return; }
        }
        helper.fail("Pending cable energy was not preserved on pickup");
    }

    @GameTest(template = "empty", timeoutTicks = 80)
    public static void passiveThenActiveGeneratorPush(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos pos = helper.absolutePos(BlockPos.ZERO);
        level.setBlockAndUpdate(pos.west(), ModMachines.GENERATOR.get().defaultBlockState());
        level.setBlockAndUpdate(pos, ModMachines.CABLE.get().defaultBlockState());
        level.setBlockAndUpdate(pos.east(), ModMachines.EXTRACTOR.get().defaultBlockState());
        var generator = (GeneratorBlockEntity)level.getBlockEntity(pos.west());
        var cable = (CableBlockEntity)level.getBlockEntity(pos);
        var receiver = level.getCapability(Capabilities.EnergyStorage.BLOCK, pos.east(), Direction.WEST);
        if (generator == null || cable == null || receiver == null) { helper.fail("Missing machines"); return; }
        generator.energy().deserializeNBT(level.registryAccess(), IntTag.valueOf(1000));
        helper.runAfterDelay(3, () -> {
            if (receiver.getEnergyStored() != 0 || generator.energy().getEnergyStored() != 1000) {
                helper.fail("Passive cable accepted a pushed transfer"); return;
            }
            cable.setExtracting(Direction.WEST, true);
            helper.runAfterDelay(3, () -> {
                int received = receiver.getEnergyStored();
                int remaining = generator.energy().getEnergyStored();
                if (received <= 0 || received + remaining != 1000) {
                    helper.fail("Active transfer lost or failed to move energy"); return;
                }
                cable.setExtracting(Direction.WEST, false);
                helper.runAfterDelay(3, () -> {
                    if (receiver.getEnergyStored() != received || generator.energy().getEnergyStored() != remaining) {
                        helper.fail("Disabled extraction continued transferring energy"); return;
                    }
                    helper.succeed();
                });
            });
        });
    }

    @GameTest(template = "empty")
    public static void fullInventoryCablePickup(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos pos = helper.absolutePos(BlockPos.ZERO);
        level.setBlockAndUpdate(pos, ModMachines.CABLE.get().defaultBlockState());
        var player = helper.makeMockServerPlayerInLevel();
        player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        player.setPos(pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5);
        for (int i = 0; i < player.getInventory().items.size(); i++) player.getInventory().items.set(i, new ItemStack(Items.STONE, 64));
        var shears = new ItemStack(Items.SHEARS);
        player.setItemInHand(InteractionHand.MAIN_HAND, shears);
        player.setShiftKeyDown(true);
        player.gameMode.useItemOn(player, level, shears, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false));
        var dropped = level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
                new net.minecraft.world.phys.AABB(pos).inflate(5), item -> item.getItem().is(ModMachines.CABLE_ITEM.get()));
        if (!level.getBlockState(pos).isAir() || dropped.size() != 1 || dropped.getFirst().getItem().getCount() != 1) {
            helper.fail("Full-inventory pickup did not drop exactly one cable"); return;
        }
        dropped.getFirst().discard();
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void creativeCablePickup(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos pos = helper.absolutePos(BlockPos.ZERO);
        level.setBlockAndUpdate(pos, ModMachines.CABLE.get().defaultBlockState());
        var player = helper.makeMockServerPlayerInLevel();
        player.setGameMode(net.minecraft.world.level.GameType.CREATIVE);
        player.setPos(pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5);
        var shears = new ItemStack(Items.SHEARS);
        player.setItemInHand(InteractionHand.MAIN_HAND, shears);
        player.setShiftKeyDown(true);
        player.gameMode.useItemOn(player, level, shears, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false));
        if (!level.getBlockState(pos).isAir() || player.getInventory().contains(new ItemStack(ModMachines.CABLE_ITEM.get()))) {
            helper.fail("Creative pickup created a cable item or failed to remove the block"); return;
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void droppedCablePreservesPendingEnergy(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos pos = helper.absolutePos(BlockPos.ZERO);
        level.setBlockAndUpdate(pos, ModMachines.CABLE.get().defaultBlockState());
        var cable = (CableBlockEntity)level.getBlockEntity(pos);
        if (cable == null) { helper.fail("Missing cable block entity"); return; }
        CompoundTag data = new CompoundTag();
        data.putInt("PendingEnergy", 53);
        data.putInt("PendingSide", Direction.WEST.ordinal());
        cable.loadCustomOnly(data, level.registryAccess());
        level.destroyBlock(pos, true);
        var drops = level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
                new net.minecraft.world.phys.AABB(pos).inflate(2), entity -> entity.getItem().is(ModMachines.CABLE_ITEM.get()));
        for (var entity : drops) {
            var component = entity.getItem().get(DataComponents.BLOCK_ENTITY_DATA);
            if (component != null && component.copyTag().getInt("PendingEnergy") == 53) {
                entity.discard();
                helper.succeed();
                return;
            }
        }
        helper.fail("Normal cable drop lost pending energy");
    }
}
