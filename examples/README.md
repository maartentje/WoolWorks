# Wool Works compatibility test examples

These examples target **Minecraft 1.21.1, NeoForge 21.1.x, Wool Works, KubeJS 7 and Mekanism** in a disposable CurseForge instance/world. They are manual compatibility tests, not production balance recommendations. Back up worlds before testing. The examples do not edit the mod's default files.

## Validation status

The JSON formats and IDs were checked against Wool Works' current serializers and built-in recipes; `pack_format: 48` is for Minecraft 1.21/1.21.1. The custom resource and its recipes were loaded as development-server resources in a dedicated GameTest run: a focused test verified availability, Feed crafting and extraction output. This did not test installation as a standalone world datapack or client display. **None of the KubeJS examples has been runtime-tested in an external KubeJS/Mekanism instance.** In particular, adding and removing Wool Works' custom recipe serializers through KubeJS must be verified in-game. Mekanism's published 1.21.x tag data identifies `mekanism:ingot_tin` and `mekanism:block_tin`, but this is not a runtime compatibility claim.

## Setup

1. Launch the CurseForge instance once to create its `kubejs` folder. Verify the installed Wool Works version includes Tin support. Enable cheats/operator commands in a disposable world.
2. Ensure `enableAutomaticModdedResources = true`, `woolworks:tin` is not in `disabledResources`, and Mekanism provides the nonempty `c:ingots/tin` item tag. A Tin Feed creative variant and the default wheat + Tin Ingot recipe should be available. Mekanism's Tin Block should be in `c:storage_blocks/tin`.
3. Copy `kubejs/server_scripts/woolworks_test.js` from this `examples` directory into **the instance's** `kubejs/server_scripts/` directory. Open that copy and set `WOOLWORKS_TEST` to exactly one case from the table below; the initial value `none` does nothing. Do not install other recipe overrides while testing. Other mods can add competing recipes, so use a minimal instance.
4. Start the world or run `/reload` after changing the script. Inspect `logs/latest.log` and `kubejs/logs/` for script or recipe errors. If recipes or recipe-viewer displays seem stale, restart the server/world and reconnect. Use a fresh machine with no upgrades for timings; JEI is optional, and actual crafting/processing is the authoritative check.
5. To reset, set `WOOLWORKS_TEST` back to `none` or remove the script, then reload or restart. Test one case at a time; a replacement uses a different `woolworks_test:` ID after removing the original by its exact ID.

## KubeJS cases

| `WOOLWORKS_TEST` | Action and expected result |
| --- | --- |
| `generator_crafting` | Removes `woolworks:wool_generator` and adds `woolworks_test:generator_crafting`. Craft with rows `CCC / CFC / WWW`: C = copper ingot, F = furnace, W = any wool. The original iron-ingot pattern should no longer craft the generator. This changes the **block crafting recipe**, not fuel. |
| `remove_iron_extraction` | Removes machine recipe `woolworks:extraction/iron`. Iron Infused Wool should no longer produce iron in the Wool Extractor; Tin extraction and the recipe for crafting the extractor should remain. Existing iron feed and generator fuel are unaffected. |
| `remove_tin_feed` | Removes only `woolworks:tin_feed`. Wheat + Tin Ingot should no longer craft Tin Feed, while Tin extraction and other resources' feed recipes remain. Existing Tin Feed and creative variants are not deleted. |
| `tin_block_feed` | Replaces Tin Feed with wheat + any `c:storage_blocks/tin` item, such as `mekanism:block_tin`. The output must be Tin Feed, with component `woolworks:resource_type` = `woolworks:tin`; use it on an adult vanilla sheep and shear the resulting Tin Sheep to confirm its identity. Wheat + a single Tin Ingot must no longer make Tin Feed. `required_resource` keeps the recipe disabled if Tin is unavailable. |
| `tin_double_output` | Replaces `woolworks:extraction/tin`; one Tin Infused Wool should yield **2 `mekanism:ingot_tin`**, at the default 200 ticks and 40 FE/t, without upgrades. The explicit item is necessary because the existing `result_tag` field has no count option. |
| `tin_slow_expensive` | Tin extraction takes **400 processing ticks at 80 FE/t**, yielding one deterministic `c:ingots/tin` item. It consumes 32,000 FE total; the extractor holds 20,000 FE, so supply at least 80 FE/t continuously or refill it while it pauses. Without upgrades, 400 processing ticks are about 20 seconds when the server runs at 20 TPS. |
| `tin_specific_output` | Tin extraction yields exactly one `mekanism:ingot_tin` after 200 ticks at 40 FE/t, even if another ingot sorts earlier in the tag. Add another tagged Tin item to test that distinction. |

For extraction, obtain Tin Infused Wool by shearing a Tin Sheep or from the creative tab. Give the extractor energy, insert one wool, and check its output and energy consumption. Do not install upgrades: they change speed, energy use and/or output. Use a separate iron wool to verify the individual removal does not remove other resources' recipes.

The Tin default feed and extraction IDs are `woolworks:tin_feed` and `woolworks:extraction/tin`. Their serializers are `woolworks:resource_conditional_crafting` and `woolworks:wool_extraction`. The extraction ingredient uses `neoforge:components` to match the `woolworks:resource_type` component. For an explicit output, use `result` **instead of** `result_tag`: the current serializer prioritizes `result_tag` if both are present. The `required_resource` check preserves the existing disabled-resource restrictions. A `result_tag` is selected deterministically by registry ID and cached.

## Custom resource datapack — independent of KubeJS cases

1. Leave `WOOLWORKS_TEST` set to `none`, or do not install the script. Copy the entire `datapacks/woolworks_test` folder from this `examples` directory to `<instance>/saves/<world>/datapacks/woolworks_test/`. `pack.mcmeta` must be directly inside that folder, alongside `data/`.
2. Enable the pack in the world's datapack selection or with `/datapack enable "file/woolworks_test"`; check `/datapack list`. Restart the world/server and reconnect after enabling it: Wool Works synchronizes resource definitions on join, and live datapack reload/client refresh is not guaranteed.
3. With `woolworks_test:echo` absent from `disabledResources`, combine wheat and an Echo Shard to craft Feed for that resource, transform an adult sheep, and shear it. Extract its Infused Wool for one Echo Shard at 200 ticks and 40 FE/t, or use the wool as generator fuel. The resource definition uses the vanilla `item.minecraft.echo_shard` translation key, so its displayed name follows the client's vanilla language without a separate resource pack.
4. Verify a custom resource is still available if `enableAutomaticModdedResources` is false, then add `"woolworks_test:echo"` to `disabledResources` and restart: crafting, transformation, shearing and extraction should be blocked without destroying previously obtained items. Remove the entry afterward.
5. Remove or disable this datapack and restart/reconnect to return to baseline. Its resource has independent IDs `woolworks_test:echo`, `woolworks_test:echo_feed`, and `woolworks_test:extraction/echo`; it does not override Tin or vanilla recipes. Previously obtained custom items retain their component but cannot be used as available resources. Existing custom sheep are permanently converted to white vanilla sheep when loaded without an available resource definition.

If a case fails, preserve the instance's `latest.log`, `kubejs/logs/`, KubeJS and Wool Works versions, the selected case, relevant datapacks, and exact steps. Inspect script errors, missing item/tag IDs, recipe-load errors, resource availability and competing recipes before attributing the failure to an integration.
