# Compatibility testing checklist

These are manual test scenarios, not a record of completed tests. Use disposable worlds and back up existing saves before testing datapack changes.

## Standalone

- Run Wool Works without other mods; check existing vanilla resources, feed recipes, sheep transformation, shearing, generator and extractor operation, and item automation.
- Confirm unavailable optional materials do not appear in the creative tab, JEI, or machine ghost slots.
- Craft Iron Feed with wheat and an iron ingot. Use it on an adult vanilla sheep; babies and existing Resource Sheep should not transform. Check its tinted fleece and normal nameplate behavior, including a name tag.
- Shear the sheep, check Infused Wool, shears durability and regrowth after grazing, then save and reopen the world. Check item tint and appearance in inventories, hands, item frames and as a drop; test invisibility and glowing on the sheep.
- Test vanilla and modded resource variants, custom datapack variants, and invalid IDs. Confirm different resources do not stack together and resource-independent advancements work.
- Check the Mods menu's client settings and resource editor in an unpublished singleplayer world. Test Done, Cancel and resource restrictions.

## Machines and upgrades

- Insert valid and invalid Infused Wool into a generator; check FE output, light, progress, full-buffer pauses, item automation and any migrated remainder.
- Extract iron wool with a hopper or pipe on each side. Check progress, energy use, idle and paused states, output blocking and persistence after reload.
- Install up to eight Speed or two Overdrive Upgrades; check speed, output and FE usage, mixing rejection, manual-only upgrade slots and progress reset after changing upgrades. JEI should show base recipe values.
- Check empty input ghost suggestions and tooltips without allowing them to change inventory contents.
- Exercise cable networks with branches, loops, chunk boundaries, placement and removal. Test configured transfer limits, extract-only and bidirectional endpoints, shears toggling, persisted connection state and cable pickup in survival, creative and full inventories.
- Test both cable recipes with shears, including shift-crafting, durability, unbreakable shears and retained components. Check speed upgrades reject ordinary wool and unavailable resource variants.
- Place Workers in all directions; check area cycling and the shears-toggle outline. Test shearing and wheat regrowth independently, output-full and energy pauses, babies, resource and vanilla sheep, all-sided automation, configured durability and save/reload.
- Test the Woolly Muffler in both hands on all machines, including modes, save/reload, manual versus automatic shearing sounds, overlapping Worker areas and unrelated sounds.

## Modded resources

- Install compatible material-providing mods and check their supported materials and tags; no particular external mod is required.
- Install multiple mods supplying the same material tag; check there is one resource type and extraction consistently chooses the same output.
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
