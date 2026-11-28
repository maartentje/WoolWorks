# Compatibility testing checklist

These are manual test scenarios, not a record of completed tests. Use disposable worlds and back up existing saves before testing datapack changes.

## Automated validation

GameTests are registered in `WoolWorksGameTests` through `Registries.TEST_FUNCTION` and `RegisterGameTestsEvent`, using function-based test instances. The default 26.1.2 suite completed 62/62 tests successfully during port development. The two optional fixture runs must still be verified on the target version.

## Standalone

- Run Wool Works without other mods; check existing vanilla resources, feed recipes, sheep transformation, shearing, generator and extractor operation, and item automation.
- Confirm unavailable optional materials do not appear in the creative tab, JEI, or machine ghost slots.
- Craft Iron Feed with wheat and an iron ingot. Use it on an adult vanilla sheep; babies and existing Resource Sheep should not transform. Check its tinted fleece and normal nameplate behavior, including a name tag.
- Craft Netherite Scrap Feed with wheat and netherite scrap, and check its wool extracts scrap. Confirm the former `woolworks:netherite` ID is treated as invalid.
- Shear the sheep, check Infused Wool, shears durability and regrowth after grazing, then save and reopen the world. Check item tint and appearance in inventories, hands, item frames and as a drop; test invisibility and glowing on the sheep.
- Test vanilla and modded resource variants, custom datapack variants, and invalid IDs. Confirm different resources do not stack together and resource-independent advancements work.
- Check the Mods menu's client settings and resource editor in an unpublished singleplayer world. Test Done, Cancel and resource restrictions.

## Machines and upgrades

- Break each machine with a pickaxe in survival and verify the machine and stored inventory drop exactly once.
- Insert valid and invalid Infused Wool into a generator; check FE output, light, progress, full-buffer pauses, item automation and any migrated remainder.
- Extract iron wool with a hopper or pipe on each side. Check progress, energy use, idle and paused states, output blocking and persistence after reload.
- Install up to eight Speed or two Overdrive Upgrades; check speed, output and FE usage, mixing rejection, manual-only upgrade slots and progress reset after changing upgrades. JEI should show base recipe values.
- Check empty input ghost suggestions and tooltips without allowing them to change inventory contents.
- Exercise cable networks with branches, loops, chunk boundaries, placement and removal. Test configured transfer limits, extract-only and bidirectional endpoints, shears toggling, persisted connection state and cable pickup in survival, creative and full inventories.
- With a generator feeding a cable network across a chunk boundary, load and unload nearby chunks and change the network topology while transferring energy. Confirm there is no crash and transfer resumes after the network settles.
- Inspect cable arms and extraction indicators on all six sides, including a cable above a generator and multi-sided junctions. Repeat with the `SQUARE` and `LINE` client indicator styles, connecting cables to each other, Wool Works machines and an external FE block.
- Test both cable recipes with shears, including shift-crafting, durability, unbreakable shears and retained components. Check speed upgrades reject ordinary wool and unavailable resource variants.
- Place Workers in all directions; check area cycling and the shears-toggle outline. Test shearing and wheat regrowth independently, output-full and energy pauses, babies, resource and vanilla sheep, all-sided automation, configured durability and save/reload.
- Test the Woolly Muffler in both hands on all machines, including modes, save/reload, manual versus automatic shearing sounds, overlapping Worker areas and unrelated sounds.

## Modded resources

- With AE2 installed, verify Certus Quartz and Fluix feed crafting and extraction; repeat without AE2 to verify unavailable resources cannot be used.
- Install compatible material-providing mods and check their supported materials and tags; no particular external mod is required.
- Toggle each resource category and verify individually disabled resources stay disabled after re-enabling it. Check automatic-resource settings independently, and check sheep conversion after a category is disabled and the world restarted.
- Exercise search together with All Resources, Enabled Only and Disabled Only; check reasons, alphabetical order, toggles, Done and Cancel, and that missing optional materials remain hidden. Check the three corrected colors on feed, wool and sheep.
- Install multiple mods supplying the same material tag; check there is one resource type and extraction consistently chooses the same output.
- With Modern Industrialization or EvilCraft installed without Occultism, check Coke or Dark Gem respectively is available through its exact-item fallback; also check common-tag contributions when present. Check Utherium produces Utheric Clusters and Monazite and Dragonyst produce dust.
- Automated standalone checks: `gradlew runGameTestServer`; repeat with `gradlew -PosmiumGameTest runGameTestServer` to load test-only tags containing copper and iron ingots for the optional catalogue.
- Run `gradlew -PemptyTagGameTest runGameTestServer` separately to check an explicitly empty salt tag. Do not combine test properties: both fixtures define the salt tag.
- GameTest code and the test structure are in `src/gameTest`; optional fixtures are in `src/osmiumTest` and `src/emptyTagTest`. ModDevGradle attaches these to development runs, not the production JAR. Fixture properties affect GameTest resources only and do not add them to the production JAR.
- Restart the server and reconnect clients after changing resource definitions, tags, or resource configuration. Live datapack reload and client display refresh have not been verified.

## Datapacks

- Disable Osmium while leaving Tin enabled, then re-enable Osmium.
- Override only Tin Feed and confirm Tin extraction is unchanged; independently override Tin extraction.
- Add a custom resource, then disable all definitions. Reload datapacks and reconnect; check items, sheep, recipes, advancements and client displays.

## Optional integrations

- Test JEI and Jade both installed and absent, including resource variants and existing sheep with unavailable resources.
- Jade machine details and its inventory/energy providers require Jade on both the client and dedicated server. The development `runServer` includes Jade; release installations must provide it separately. Without server-side Jade, client-only information such as Resource Sheep names can still be displayed, but machine server data cannot be requested.
- With KubeJS, remove and replace feed and extraction recipes, including component-bearing outputs and custom processing values; test ordinary crafting replacements.

## Automation and multiplayer

- Test external FE sources and consumers, item automation and cable transfer limits.
- Connect a separate client to a dedicated server; verify synchronized resources and behavior after reload/reconnect.
- In a disposable world, save resource sheep in multiple chunks, disable their resources, restart, and visit each chunk. Confirm disabled sheep convert once to white vanilla sheep, while enabled sheep and stored items are unchanged; re-enabling a resource does not reverse a conversion.
- Exercise larger sheep farms and observe basic performance and correctness.
