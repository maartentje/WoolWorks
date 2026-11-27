# Modpack Author Guide

Wool Works uses predefined optional resources rather than discovering arbitrary materials. An optional resource becomes available when automatic resources are enabled, its ID is not individually disabled, its definition is enabled, and its required item tag contains at least one item. No external material mod is mandatory. See the [complete examples](../examples/README.md) for an independently selectable KubeJS test script and a custom-resource datapack.

## Resource definitions and configuration

Resource types are datapack registry entries at `data/<namespace>/woolworks/resource_type/<name>.json`. Their ID is `<namespace>:<name>`. Existing definitions require `translation_key`, `color` (decimal RGB), `min_yield`, and `max_yield`. A resource pack can provide the corresponding translation. The registry is synchronized to connected clients when they join.

For example, `data/example/woolworks/resource_type/ruby.json`:

```json
{"translation_key":"resource.example.ruby","color":13249582,"min_yield":1,"max_yield":3}
```

Definitions are replaced as complete files by higher-priority datapacks; they are not merged. A full definition may include `"enabled": false` (default: true). The server config also provides `disabledResources`, a list of resource IDs. Disabled resources are omitted from the creative tab and machine ghost items and cannot be used to transform or shear sheep; the default feed and extraction recipes check availability. Existing items retain their resource IDs, but unavailable Resource Sheep are permanently converted to white vanilla sheep when loaded. Back up worlds before changing definitions.

Colors are decimal RGB values from 0 to 16777215. Yields are integers from 1 to 64, with `min_yield` no greater than `max_yield`. Invalid definitions can prevent a world from loading. Resource IDs are stored on items and sheep, so avoid renaming them. Add translations with a resource pack available to clients, for example `assets/example/lang/en_us.json` containing `{"resource.example.ruby":"Ruby"}`.

For example, in `woolworks-server.toml`:

```toml
disabledResources = ["woolworks:osmium", "woolworks:amethyst", "example:ruby"]
enableAutomaticModdedResources = true
```

Set `enableAutomaticModdedResources` to `false` to disable all predefined optional resources without disabling vanilla or custom resources. Optional definitions specify `"automatic": true` and `"required_item_tag"`; both the setting and a nonempty tag are required. Custom definitions default to `"automatic": false`, and may use `required_item_tag` without being affected by the global setting. Individual disabling still applies. Restart the server and reconnect clients after changes; live reload and client display refresh are not guaranteed.

In an unpublished singleplayer world, the server configuration screen's **Enabled Resources** editor provides a searchable list of loaded resources. Done saves individual choices; Cancel discards them. Globally disabled automatic resources retain their individual preferences. Unavailable automatic resources are hidden, but their saved IDs are retained. The registry is not available at the main menu; join a singleplayer world first. NeoForge prevents this server configuration from being edited from remote clients or a world open to LAN. Dedicated server administrators should edit the server config file instead.

## Supported catalogue

All IDs use the `woolworks:` namespace. The standard vanilla resources are coal, copper, iron, gold, redstone, lapis, diamond, emerald, quartz, netherite, and **amethyst** (feed and extraction use `minecraft:amethyst_shard`). These remain enabled when automatic resources are switched off unless individually disabled.

The following 37 optional resources require the indicated nonempty item tag:

| Resource ID | Required item tag |
| --- | --- |
| osmium | `c:ingots/osmium` |
| tin | `c:ingots/tin` |
| silver | `c:ingots/silver` |
| lead | `c:ingots/lead` |
| zinc | `c:ingots/zinc` |
| uranium | `c:ingots/uranium` |
| nickel | `c:ingots/nickel` |
| platinum | `c:ingots/platinum` |
| aluminum | `c:ingots/aluminum` |
| titanium | `c:ingots/titanium` |
| tungsten | `c:ingots/tungsten` |
| iridium | `c:ingots/iridium` |
| cobalt | `c:ingots/cobalt` |
| bismuth | `c:ingots/bismuth` |
| iesnium | `c:ingots/iesnium` |
| steel | `c:ingots/steel` |
| bronze | `c:ingots/bronze` |
| brass | `c:ingots/brass` |
| electrum | `c:ingots/electrum` |
| invar | `c:ingots/invar` |
| constantan | `c:ingots/constantan` |
| cupronickel | `c:ingots/cupronickel` |
| signalum | `c:ingots/signalum` |
| lumium | `c:ingots/lumium` |
| enderium | `c:ingots/enderium` |
| stainless_steel | `c:ingots/stainless_steel` |
| refined_obsidian | `c:ingots/refined_obsidian` |
| ruby | `c:gems/ruby` |
| sapphire | `c:gems/sapphire` |
| peridot | `c:gems/peridot` |
| topaz | `c:gems/topaz` |
| fluorite | `c:gems/fluorite` |
| certus_quartz | `c:gems/certus_quartz` |
| fluix (Fluix Crystal) | `c:gems/fluix` |
| apatite | `c:gems/apatite` |
| sulfur | `c:dusts/sulfur` |
| salt | `c:dusts/salt` |

The published 1.21.1 AE2 tag files use `c:gems/certus_quartz` and `c:gems/fluix`, and Mekanism's 1.21.x data uses `c:gems/fluorite`; these names were checked against source data, not verified with every mod version. Individual mods or packs may use other tags: add their items to the listed common tag with a datapack if needed. When a tag has multiple items, extraction picks the lowest registry ID in sorted order.

## Custom Feed and extraction recipes

Recipes are standard datapack resources under `data/<namespace>/recipe/`, and can be replaced by another datapack at the same ID. For example, the existing Feed and extraction IDs for iron are `woolworks:iron_feed` and `woolworks:extraction/iron`. Replacing one does not require replacing the other.

For the `example:ruby` definition above, `data/example/recipe/ruby_feed.json` can use the existing conditional shapeless serializer. This example uses a diamond as its ingredient, so it needs no other mod:

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

Add `data/example/recipe/extraction/ruby.json` to extract one diamond from that resource's wool:

```json
{
  "type": "woolworks:wool_extraction",
  "ingredient": {
    "type": "neoforge:components",
    "items": "woolworks:infused_wool",
    "components": {"woolworks:resource_type": "example:ruby"}
  },
  "result": {"id": "minecraft:diamond", "count": 1},
  "duration": 200,
  "energy_per_tick": 40,
  "required_resource": "example:ruby"
}
```

`required_resource` checks availability. For a tag-selected output, use `"result_tag": "c:ingots/tin"` instead of `result`; it produces one deterministically selected item. Use an explicit `result` for a particular item or a different quantity. If both fields are present, `result_tag` takes precedence. Recipes and definitions are separate files and must be supplied or overridden independently.

## KubeJS and compatibility

KubeJS is optional. For example, a server script can remove just the Tin Feed recipe:

```js
ServerEvents.recipes(event => {
  event.remove({ id: 'woolworks:tin_feed' })
})
```

The [example script](../examples/kubejs/server_scripts/woolworks_test.js) also shows raw-JSON replacements for Tin Feed and extraction. Enable one case at a time; see its [instructions](../examples/README.md). Basic KubeJS recipe customization and datapack customization have been manually compatibility-tested, as have Almost Unified and selected FE/item automation setups. Exact scripts, mod versions, load orders and every possible override combination have not all been verified; test your own pack and inspect logs after changes.

## Other machine configuration and recipes

![A Wool Extractor and Wool Generator linked by insulated cable with hoppers for wool processing](images/wool-processing.png)

*A wool-processing setup using an Extractor and Generator.*

The world server config `serverconfig/woolworks-server.toml` also provides `cableTransferRate` (default 160 FE/t per cable segment), `woolWorkerOperationInterval` (200 ticks), `woolWorkerRegrowthEnergyCost` (400 FE), `woolWorkerShearingEnergyCost` (200 FE), `woolWorkerWheatCost` (1) and `woolWorkerShearsDurabilityCost` (0). Existing config files retain their saved values. Worker energy costs are capped at its 20,000 FE capacity. Check the server log for the effective config path when a development or host setup provides an override. Server configuration is synchronized to connected clients.

Generator fuel recipes use `woolworks:generator_fuel` with `ingredient`, `burn_ticks` and `energy_per_tick`; `required_resource` and `absent_resource` are optional availability checks. Fuel must be valid Infused Wool. The default `woolworks:generator/infused_wool` recipe supplies 400 ticks at 40 FE/t; matching custom fuel recipes take precedence over that default. Extraction recipes support `required_resource`; their duration and FE/t are datapack fields. Speed upgrades keep configured FE/t, whereas Overdrive multiplies FE/t and output; extraction pauses if the output cannot fit.

The two cable recipes are independently replaceable: `woolworks:wool_insulated_cable` uses Copper Infused Wool and shears for eight cables, while `woolworks:wool_insulated_cable_fallback` uses copper, wool and shears for four. Their `reusable_shears` field returns shears with one durability consumed, preserving other components. The speed upgrade recipe uses the `woolworks:infused_wool` ingredient and `validate_resource_wool` to reject unavailable variants.

See the [testing checklist](testing.md) for datapack, integration and multiplayer scenarios. Resource definition and tag changes require a world/server restart and client reconnect; full live reload synchronization has not been established.
