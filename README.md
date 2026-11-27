# Wool Works

Cable extraction indicators can be changed in the client configuration with `cableExtractionIndicatorStyle` (`SQUARE`, the default, or `LINE`). The choice is local and does not affect cable behavior. Machine progress tooltips use server-synchronized processing state; pauses do not consume the displayed remaining time. If Jade is installed, it can show machine and cable details and Resource Sheep information. Jade is optional and is not bundled with the mod.

## Advancement guide

The Wool Works advancement tab is an in-game progression guide, beginning with any vanilla wool. Its Feed and Infused Wool criteria are independent of resource variants, including datapack-defined variants. Modpackmakers can override the advancement JSON files with datapacks. If all resource definitions are disabled, some advancements may be unobtainable through normal gameplay; the neutral Feed and Infused Wool icons do not depend on any enabled built-in resource.

Wool Works is a Minecraft 1.21.1 NeoForge mod about renewable resources from sheep. It includes resource feed, resource sheep, non-placeable infused wool, a generator, extractor, insulated cable and speed upgrades. Natural spawning and breeding are not implemented.

The Wool Generator and Wool Extractor have basic screens, accept items through automation and can transfer FE directly or through insulated cables. The generator emits light while generating and its vertical fuel bar shows the elapsed portion of the current fuel. Empty machine input slots show visual-only item suggestions; Infused Wool variants cycle through the current machine's accepted resource types. The extractor accepts either up to eight Woolly Speed Upgrades or up to two Woolly Overdrive Upgrades in its manually operated upgrade slot; the types cannot be mixed. Eight speed upgrades give 5x speed with unchanged output and FE/t. One overdrive gives 3x speed, 2x output and 2x FE/t; two give 6x speed, 4x output and 4x FE/t. Craft an overdrive upgrade with eight speed upgrades surrounding a Nether Star. Changing the installed upgrade configuration resets unfinished progress without consuming the input. JEI shows the base extraction recipes and the upgrade crafting recipe when installed. Sneak-right-click a cable with shears to pick it up; right-click a machine-facing cable arm with shears to toggle extraction for that connection. Green bands mark active extraction. On placement, a cable enables extraction towards adjacent extract-only FE sources, including empty generators; receive-only and bidirectional storages start passive. Generic bidirectional FE capabilities cannot reliably distinguish generators from storage, so use shears to enable those manually. This default applies only when placing a cable and never overrides later settings or existing networks. Passive connections can supply consumers but reject incoming pushes and do not pull energy.

The Wool Worker automatically regrows and shears adult vanilla and resource sheep in front of it. Its fixed energy capacity is 20,000 FE. Its default cycle is 200 ticks (approximately ten seconds): regrowth consumes one wheat and 400 FE, while shearing consumes 200 FE and normally no shears durability. Naturally woolly sheep can be sheared without wheat; regrowth also works without shears, even when the output is full. Wool goes into 18 output slots; shearing waits until the complete output fits. Hoppers and item pipes can insert shears and wheat and extract output from any side; FE can enter on every side. The timer pauses when inputs, output space or energy cannot support either action and resumes from saved progress.

Craft a Wool Worker shapelessly with three iron ingots, two copper ingots, two redstone dust, shears, and any valid Infused Wool. The machine faces the direction the player faces when placed. The area button cycles 1x1, 3x3, 5x5, 7x7 and 9x9 with ordinary clicks; newly placed machines start at 3x3. Existing 11x11 selections migrate to 9x9. The area begins in front and extends two blocks high. Right-click the machine with shears to toggle a player-local outline; use it without shears to open the menu.

Generators make a quiet fire-extinguish sound when consuming fuel, and extractors make a quiet piston-contraction sound when completing a recipe. A reusable Woolly Muffler cycles a machine's sound mode when right-clicked: generators and extractors use Off and Machine; workers use Off, Machine, Sheep and Both. Machine mode mutes the worker's automated shearing sound, not a player's manual shearing. Sheep mode filters identifiable sheep ambient, hurt, death and step sounds within the worker's current working area. These checks run on the server with a chunk-indexed set of loaded active workers; sheep-specific positional sounds are filtered only when a sheep can be identified nearby. Unattributable or client-only sounds and nonstandard modded sound events can remain audible.

Craft a Woolly Muffler in a crafting table with two valid Infused Wool in the top corners, iron ingots on the left and right of the middle row, a copper ingot in the center, and sticks in the bottom corners. The wool types may differ. The item does not wear out; each machine saves its own selected mode.

Cable routes are cached until cable or chunk topology changes. Only cables with active extraction or an unfinished transfer are polled each server tick; active transfers still resolve current endpoint capabilities. A small saved buffer retains energy if a receiver accepts less than it simulated; the buffer is carried on a cable dropped or picked up in survival. Energy in that buffer cannot be recovered if the cable is destroyed without producing an item, such as by a no-drop explosion or creative-mode removal. No performance benchmark has been performed.

Craft sheep feed from wheat and a resource, then use it on an adult vanilla sheep. Shear the resulting resource sheep for infused wool; it regrows wool by eating grass. The Wool Works creative tab contains feed and wool variants for the resource types currently loaded in the world.

**Early-version compatibility:** `woolworks:resource_infusion` was renamed to `woolworks:resource_feed`. Existing old infusion stacks and recipes referencing the old ID will need to be replaced.

Resource sheep use vanilla nameplate behavior, including names applied with name tags. The Mods-menu configuration screen includes client settings and, in an unpublished singleplayer world, server settings and a resource selection editor. Dedicated server administrators can edit the server configuration file.

## Development

Use Java 21 and import the project as a Gradle project. On Windows:

```powershell
.\gradlew.bat build
.\gradlew.bat runClient
.\gradlew.bat runServer
```

On macOS/Linux use `./gradlew`. The built JAR is in `build/libs/`. See the [NeoForge 1.21.1 documentation](https://docs.neoforged.net/docs/1.21.1/) and the [Mojang mappings license](https://github.com/NeoForged/NeoForm/blob/main/Mojang.md).

JEI is added only to the development client, not bundled or required by the mod. JEI discovers variants from the creative tab, and the optional integration identifies them by their resource component. The development server does not include JEI. A normal installation can run with or without it.

After changing Gradle dependencies or run configurations, reload the Gradle project in IntelliJ and use the ModDevGradle-generated client run configuration (or the `runClient` Gradle task). If a manually created or stale run configuration still uses the old classpath, regenerate it or select the Gradle task instead.

## Datapack resource types

Resource definitions are a synchronized datapack registry. The file path determines the ID: `data/example/woolworks/resource_type/ruby.json` defines `example:ruby`. For example:

```json
{
  "translation_key": "resource.example.ruby",
  "color": 13249582,
  "min_yield": 1,
  "max_yield": 3
}
```

Color is a decimal RGB value from 0 to 16777215. Yields must be integers between 1 and 64, with minimum no greater than maximum. Invalid registry definitions are reported during datapack loading and may prevent a world from loading; fix the definition before reopening it. Resource IDs are stored on sheep and items, so avoid renaming IDs after they are in use. On server entity load, sheep whose resources are unavailable are permanently converted to white vanilla sheep; existing items retain their resource IDs.

Provide translations via a resource pack, for example `assets/example/lang/en_us.json` containing `{"resource.example.ruby":"Ruby"}`. Add a normal datapack recipe at `data/example/recipe/ruby_feed.json`:

```json
{
  "type": "woolworks:resource_conditional_crafting",
  "required_resource": "example:ruby",
  "recipe": {
    "ingredients": [{"item": "minecraft:wheat"}, {"item": "minecraft:diamond"}],
    "result": {
      "id": "woolworks:resource_feed",
      "count": 1,
      "components": {"woolworks:resource_type": "example:ruby"}
    }
  }
}
```

Recipes are ordinary datapack recipes and do not require KubeJS. Resource definitions are loaded as world registries and synchronized on connection; reopen the world or restart the server after changing them. Datapack resources may override default definitions and recipes using the same paths. A resource pack containing custom translations must also be available to clients.

## Machine recipe data

The generator and extractor use the recipe types `woolworks:generator_fuel` and `woolworks:wool_extraction`. They accept NeoForge ingredients, including tags and component-aware ingredients. Example extraction recipe:

```json
{
  "type": "woolworks:wool_extraction",
  "ingredient": {
    "type": "neoforge:components",
    "items": "woolworks:infused_wool",
    "components": {"woolworks:resource_type": "woolworks:iron"}
  },
  "result": {"id": "minecraft:iron_ingot", "count": 1},
  "duration": 200,
  "energy_per_tick": 40,
  "required_resource": "woolworks:iron"
}
```

Fuel recipes use `ingredient`, `burn_ticks`, and `energy_per_tick`. Both recipe types may use `required_resource`; fuel recipes may instead use `absent_resource`. These conditions are checked against the loaded resource registry. Generator fuel must always be Infused Wool with a registered resource ID. The default recipe accepts every valid variant for 400 ticks at 40 FE/t (16,000 FE). Custom fuel recipes take precedence over the default and may change these values. Speed upgrades leave the extractor's configured FE/t unchanged; overdrive multiplies it per active tick. An operation pauses if its multiplied output does not fit, and an output exceeding the empty slot's capacity is indicated in the machine screen.

Recipe files under `data/woolworks/recipe/generator/` and `data/woolworks/recipe/extraction/` can be overridden by datapacks; another namespace can supply new recipes and modded outputs. The server config `serverconfig/woolworks-server.toml` in the world directory provides `cableTransferRate`, a positive integer defaulting to 160 FE/t per cable block. NeoForge synchronizes server configuration to connected clients.

The same server config provides `woolWorkerOperationInterval` (200 ticks), `woolWorkerRegrowthEnergyCost` (400), `woolWorkerShearingEnergyCost` (200), `woolWorkerWheatCost` (1), and `woolWorkerShearsDurabilityCost` (0). The former `woolWorkerEnergyCapacity` option is obsolete; previously stored FE above 20,000 is capped on load. Existing config values are preserved: to change a previously saved 100-tick interval, stop the world or server and set `woolWorkerOperationInterval = 200` in the effective `woolworks-server.toml`, then reopen it. In this development environment an existing `run/config/woolworks-server.toml` can also supply the old value; inspect the server log for the loaded path. Energy costs above capacity are capped at capacity. The obsolete overflow option is no longer used.

Cable recipes use `woolworks:resource_conditional_crafting` with a nested shapeless `recipe`. Copper-infused wool and shears make eight cables when the copper resource is present; copper ingot, any wool and shears make four cables regardless of the resource registry. The `reusable_shears` field returns the shears with one point of damage, preserving their other components; unbreakable shears are unchanged and exhausted shears break. Both recipe files can be overridden or removed independently. The speed upgrade recipe uses the `woolworks:infused_wool` ingredient plus `validate_resource_wool` to accept only variants whose resource is present in the world registry.

## Manual test checklist

- [ ] Craft Iron Sheep Feed from wheat and an iron ingot; check the pouch base stays unchanged while its overlay is tinted.
- [ ] Use it on an adult vanilla sheep and check that its name and wool appearance change; a baby or existing resource sheep should not transform.
- [ ] Compare several resource sheep with a vanilla sheep: the base fleece should remain neutral and only its mineral overlay should change color. Check for flickering or UV misalignment, including on a baby summoned for visual testing.
- [ ] Shear it and check that only Iron-infused Wool drops, shears take durability damage, and the sheep is sheared.
- [ ] Check the wool's three-dimensional appearance, untinted fleece and tinted mineral overlay on all sides when dropped, in inventories, in either hand, in item frames, and in the creative tab.
- [ ] Confirm resource sheep do not display automatic floating nameplates. Apply a vanilla name tag and verify its normal visibility and formatting.
- [ ] Check the Mods menu client settings and the server resource editor in an unpublished singleplayer world.
- [ ] Launch the development client with JEI; find feed and wool variants and check feed recipes, generator fuels and extraction recipes, including a datapack-added variant.
- [ ] Run the built mod without JEI and verify the same gameplay works.
- [ ] Let the sheep eat grass, verify wool regrows, and shear it again.
- [ ] Check invisibility and glowing effects on a resource sheep, including while sheared.
- [ ] Save and reopen the world; verify the sheep and its resource type survive.
- [ ] Try other resource types and ensure differently typed wool does not stack together; check the creative tab variants.
- [ ] Add a custom resource definition, translation and recipe with a datapack/resource pack; reopen the world, then test its feed and wool.
- [ ] Test on a dedicated server with a separate client, including conversion, shearing, synchronization and reconnecting.
- [ ] Confirm dye and breeding attempts do not change resource sheep, and test a compatible automated shearer if available.
- [ ] Insert several Infused Wool variants into a Wool Generator using a hopper; check it produces FE for an adjacent consumer and pauses when full. Confirm invalid variants and furnace fuels are rejected, and legacy remainder items are returned.
- [ ] Use a hopper or compatible item pipe to insert Iron-infused Wool into an adjacent Wool Extractor and extract the resulting iron ingot. Check all sides.
- [ ] Open both machine screens, install zero to eight speed upgrades in the extractor, and check the energy and progress displays. Confirm pipes cannot insert or remove upgrades.
- [ ] Craft and install one or two Overdrive Upgrades; check 3x/6x speed, 2x/4x output and energy use, incompatible upgrade mixing, output-space blocking, upgrade changes and JEI's base recipe values.
- [ ] Place cables between machines; test branched and circular layouts, multiple producers and consumers, placement/removal and chunk unload/reload. Verify no energy moves through a broken connection.
- [ ] Place a new cable next to an empty generator, a receive-only machine and bidirectional storage; verify only the extract-only side defaults on. Turn it off with shears and verify neighbor changes and reloads do not re-enable it.
- [ ] Check generator light and the fuel bar while burning, paused and idle. Check all machine ghost slots, their tooltips and inventory interaction with built-in and datapack wool variants.
- [ ] Verify 160 FE/t per shared segment, independent paths, and a changed server configuration with a separate multiplayer client displaying the configured tooltip.
- [ ] With an external FE source, toggle one machine-facing cable arm using shears; verify its green indicator, extraction, persistence across reloads and independence from other sides. Passive connections should neither pull nor accept pushes; active connections should support both.
- [ ] Sneak-right-click a cable with shears and verify it is returned once without damaging the shears; also test full inventory, creative mode and protected areas.
- [ ] Craft eight cables from copper-infused wool and shears, and four from wool, copper and shears; verify one durability is consumed each time and other components remain, including when shift-clicking and when the shears break.
- [ ] Craft a speed upgrade with several Infused Wool variants, including a datapack resource; ordinary wool and invalid resource IDs must not work. Check the variants in JEI.
- [ ] Save and reload while the machines are operating, then check their contents and progress persist.
- [ ] Craft and place a Wool Worker in each facing direction. Check its selected area, local shears-toggle outline, GUI controls and save/reload behavior, including a chunk boundary.
- [ ] Test resource and colored vanilla sheep, babies, wheat-only regrowth, shearing without wheat, all-sided automation, full output, timer pausing, and configured shears durability on a dedicated server.
- [ ] Test the Woolly Muffler in both hands on all three machines, including save/reload, manual versus automatic shearing, overlapping worker areas, chunk boundaries, and unrelated sounds on a separate multiplayer client.
