# Modpack customization

## Existing resource definitions

Resource types are datapack registry entries at `data/<namespace>/woolworks/resource_type/<name>.json`. Their ID is `<namespace>:<name>`. Existing definitions require `translation_key`, `color` (decimal RGB), `min_yield`, and `max_yield`. A resource pack can provide the corresponding translation. The registry is synchronized to connected clients when they join.

For example, `data/example/woolworks/resource_type/ruby.json`:

```json
{"translation_key":"resource.example.ruby","color":13249582,"min_yield":1,"max_yield":3}
```

Definitions are replaced as complete files by higher-priority datapacks; they are not merged. A full definition may include `"enabled": false` (default: true). The server config also provides `disabledResources`, a list of resource IDs. Disabled resources are omitted from the creative tab and machine ghost items and cannot be used to transform or shear sheep; the default feed and extraction recipes check availability. Existing items retain their resource IDs, but unavailable Resource Sheep are permanently converted to white vanilla sheep when loaded. Back up worlds before changing definitions.

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

No proposed tags were changed. The published 1.21.1 AE2 tag files use `c:gems/certus_quartz` and `c:gems/fluix`, and Mekanism's 1.21.x data uses `c:gems/fluorite`; this is a source inspection, not a mod compatibility test. Individual mods or packs may use other tags: add their items to the listed common tag with a datapack if needed. When a tag has multiple items, extraction picks the lowest registry ID in sorted order.

## Existing recipes

Recipes are standard datapack resources under `data/<namespace>/recipe/`, and can be replaced by another datapack at the same ID. For example, the existing Feed and extraction IDs for iron are `woolworks:iron_feed` and `woolworks:extraction/iron`. The former is a shapeless crafting recipe with a result component `woolworks:resource_type`; the latter uses `woolworks:wool_extraction` with `ingredient`, `result`, `duration`, `energy_per_tick`, and optional `required_resource`. See the actual JSON files and the examples in the project README for supported syntax. Replacing one does not require replacing the other.

KubeJS is optional. Runtime compatibility for removing or replacing these recipes, especially component-bearing results and custom recipe JSON, has not been verified and requires the checks in `docs/testing.md`.
