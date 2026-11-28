package be.maarten.woolworks;

import be.maarten.woolworks.recipe.MachineRecipeGameTests;
import be.maarten.woolworks.machine.CableNetworkGameTests;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.RegisterEvent;
import java.util.List;
import java.util.HashMap;

@EventBusSubscriber(modid = WoolWorks.MODID)
public final class WoolWorksGameTests {
    @FunctionalInterface
    private interface TestBody { void run(GameTestHelper helper) throws Exception; }
    private record Test(String name, String batch, int ticks, TestBody body) {}
    private static final List<Test> TESTS = List.of(
        new Test("cablearmmodeldirections", "default", 100, MachineRecipeGameTests::cableArmModelDirections),
        new Test("cableconnectionsalldirections", "default", 100, MachineRecipeGameTests::cableConnectionsAllDirections),
        new Test("extractoridletoprocessing", "default", 100, MachineRecipeGameTests::extractorIdleToProcessing),
        new Test("unavailabletagsheepconversion", "resource_missing_tag", 100, MachineRecipeGameTests::unavailableTagSheepConversion),
        new Test("invalidsheepconversion", "resource_conversion", 100, MachineRecipeGameTests::invalidSheepConversion),
        new Test("extractiondisplayoutput", "default", 100, MachineRecipeGameTests::extractionDisplayOutput),
        new Test("automaticcatalogue", "default", 100, MachineRecipeGameTests::automaticCatalogue),
        new Test("categoryrestrictions", "category_restrictions", 100, MachineRecipeGameTests::categoryRestrictions),
        new Test("categorysheepconversion", "category_conversion", 100, MachineRecipeGameTests::categorySheepConversion),
        new Test("osmiumavailability", "default", 100, MachineRecipeGameTests::osmiumAvailability),
        new Test("disabledresourcerestrictions", "default", 100, MachineRecipeGameTests::disabledResourceRestrictions),
        new Test("advancementprogression", "default", 100, MachineRecipeGameTests::advancementProgression),
        new Test("sheepgrasseatingeffects", "default", 100, MachineRecipeGameTests::sheepGrassEatingEffects),
        new Test("mufflerrecipe", "default", 100, MachineRecipeGameTests::mufflerRecipe),
        new Test("mufflermodesandinteraction", "default", 100, MachineRecipeGameTests::mufflerModesAndInteraction),
        new Test("sheepsoundzones", "default", 100, MachineRecipeGameTests::sheepSoundZones),
        new Test("sheepzonestrackarea", "default", 100, MachineRecipeGameTests::sheepZonesTrackArea),
        new Test("overlappingsheepzones", "default", 100, MachineRecipeGameTests::overlappingSheepZones),
        new Test("machinesoundevents", "default", 100, MachineRecipeGameTests::machineSoundEvents),
        new Test("extractorcompletionsound", "default", 100, MachineRecipeGameTests::extractorCompletionSound),
        new Test("workerautomatedshearingsound", "default", 100, MachineRecipeGameTests::workerAutomatedShearingSound),
        new Test("overdriverecipeandslot", "default", 100, MachineRecipeGameTests::overdriveRecipeAndSlot),
        new Test("overdriveshiftclick", "default", 100, MachineRecipeGameTests::overdriveShiftClick),
        new Test("overdriveoversizedsavedstack", "default", 100, MachineRecipeGameTests::overdriveOversizedSavedStack),
        new Test("extractorupgradebalancing", "default", 100, MachineRecipeGameTests::extractorUpgradeBalancing),
        new Test("extractoroverdrivepausesandresets", "default", 100, MachineRecipeGameTests::extractorOverdrivePausesAndResets),
        new Test("overdrivedatapackrecipes", "default", 100, MachineRecipeGameTests::overdriveDatapackRecipes),
        new Test("machineactivestates", "default", 100, MachineRecipeGameTests::machineActiveStates),
        new Test("generatorfuelprogress", "default", 100, MachineRecipeGameTests::generatorFuelProgress),
        new Test("cableplacementdefaults", "default", 100, MachineRecipeGameTests::cablePlacementDefaults),
        new Test("cableblockitemplacement", "default", 100, MachineRecipeGameTests::cableBlockItemPlacement),
        new Test("workercrafting", "default", 100, MachineRecipeGameTests::workerCrafting),
        new Test("speedupgradelimit", "default", 100, MachineRecipeGameTests::speedUpgradeLimit),
        new Test("workerareaandautomation", "default", 100, MachineRecipeGameTests::workerAreaAndAutomation),
        new Test("workertimerpausesandpersists", "default", 100, MachineRecipeGameTests::workerTimerPausesAndPersists),
        new Test("workerfulloutputregrowth", "default", 100, MachineRecipeGameTests::workerFullOutputRegrowth),
        new Test("workerreservedyield", "default", 100, MachineRecipeGameTests::workerReservedYield),
        new Test("workerregrowsandshears", "default", 240, MachineRecipeGameTests::workerRegrowsAndShears),
        new Test("workershearsresourcesheep", "default", 240, MachineRecipeGameTests::workerShearsResourceSheep),
        new Test("defaultrecipes", "default", 100, MachineRecipeGameTests::defaultRecipes),
        new Test("generatorcapabilities", "default", 100, MachineRecipeGameTests::generatorCapabilities),
        new Test("generatorburnandmigration", "default", 100, MachineRecipeGameTests::generatorBurnAndMigration),
        new Test("furnacefuelsandremainders", "default", 100, MachineRecipeGameTests::furnaceFuelsAndRemainders),
        new Test("extractorprocessesiron", "default", 260, MachineRecipeGameTests::extractorProcessesIron),
        new Test("adjacentgeneratorfeedsextractor", "default", 260, MachineRecipeGameTests::adjacentGeneratorFeedsExtractor),
        new Test("cabletransfersandlimits", "default", 260, MachineRecipeGameTests::cableTransfersAndLimits),
        new Test("cablerecipeusescopperwool", "default", 100, MachineRecipeGameTests::cableRecipeUsesCopperWool),
        new Test("cablecycleandbranch", "default", 100, MachineRecipeGameTests::cableCycleAndBranch),
        new Test("sharedcablesegmentlimit", "default", 100, MachineRecipeGameTests::sharedCableSegmentLimit),
        new Test("extractorupgradeenergy", "default", 100, MachineRecipeGameTests::extractorUpgradeEnergy),
        new Test("cableextractionisdirectional", "default", 100, MachineRecipeGameTests::cableExtractionIsDirectional),
        new Test("speedupgraderequiresvalidresource", "default", 100, MachineRecipeGameTests::speedUpgradeRequiresValidResource),
        new Test("sneakshearspickup", "default", 100, MachineRecipeGameTests::sneakShearsPickup),
        new Test("shiftcraftdamagesshears", "default", 100, MachineRecipeGameTests::shiftCraftDamagesShears),
        new Test("shearstoggleandoffhandpickup", "default", 100, MachineRecipeGameTests::shearsToggleAndOffhandPickup),
        new Test("pickuppreservespendingenergy", "default", 100, MachineRecipeGameTests::pickupPreservesPendingEnergy),
        new Test("passivethenactivegeneratorpush", "default", 80, MachineRecipeGameTests::passiveThenActiveGeneratorPush),
        new Test("fullinventorycablepickup", "default", 100, MachineRecipeGameTests::fullInventoryCablePickup),
        new Test("creativecablepickup", "default", 100, MachineRecipeGameTests::creativeCablePickup),
        new Test("droppedcablepreservespendingenergy", "default", 100, MachineRecipeGameTests::droppedCablePreservesPendingEnergy),
        new Test("invalidationduringroutediscovery", "default", 100, CableNetworkGameTests::invalidationDuringRouteDiscovery)
    );
    private static Identifier id(String path) { return Identifier.fromNamespaceAndPath(WoolWorks.MODID, path); }
    @SubscribeEvent
    public static void functions(RegisterEvent event) {
        event.register(Registries.TEST_FUNCTION, registry -> {
            for (Test test : TESTS) registry.register(id(test.name()), helper -> {
                try { test.body().run(helper); }
                catch (RuntimeException e) { throw e; }
                catch (Exception e) { throw new IllegalStateException("Test failed: " + test.name(), e); }
            });
        });
    }
    @SubscribeEvent
    public static void tests(RegisterGameTestsEvent event) {
        var environments = new HashMap<String, net.minecraft.core.Holder<TestEnvironmentDefinition<?>>>();
        for (Test test : TESTS) {
            var environment = environments.computeIfAbsent(test.batch(), batch -> event.registerEnvironment(id(batch), new TestEnvironmentDefinition.AllOf()));
            event.registerTest(id(test.name()), new FunctionGameTestInstance(ResourceKey.create(Registries.TEST_FUNCTION, id(test.name())), new TestData<>(environment, id("empty"), test.ticks(), 0, true)));
        }
    }
}
