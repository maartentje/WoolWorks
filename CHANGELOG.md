# Changelog

All notable changes to Wool Works will be documented in this file.

## [Unreleased]

### Modpack customization

- For the in-progress 26.1.2 port, migrate built-in recipe ingredients to target item/tag strings and the `neoforge:ingredient_type` discriminator. Custom datapack and KubeJS recipe overrides using 1.21.1 ingredient syntax must be updated; target runtime compatibility is not yet verified.

## [1.0.0]

First stable release for Minecraft 1.21.1 (NeoForge).

### Features

- Raise Resource Sheep for Infused Wool, with configurable vanilla and automatically detected modded resources.
- Automate wool production, resource extraction and FE generation with Wool Works machines and cables.
- Customize production with upgrades, tools, datapacks and optional JEI and Jade integrations.
- Replace the Netherite resource with Netherite Scrap; its feed uses scrap rather than an ingot. Sheep using the old alpha resource ID are treated as invalid.
- Add vanilla materials and mob drops as Resource Sheep types.
- Include 38 vanilla and up to 128 optional modded resource types (166 total), including additional gems, industrial materials, magic resources and dimensional metals.
- Group resources into configurable categories, with effective-state filters in the individual resource editor. Correct Vibranium, Inferium and Prudentium colors.
- Automatically detect Refined Glowstone through its common ingot tag.
- Automatically detect Allthemodium, Vibranium, Unobtainium and supported Ender IO alloys through common ingot tags.
- Automatically detect Botania's Manasteel, Terrasteel, Elementium, Mana Diamond and Mana Pearl through common tags.
- Automatically detect Mystical Agriculture's five core essences, Prosperity Shard and Soulium Ingot.
- Automatically detect HOP Graphite and Twilight Forest ingots through common tags.
- Automatically detect Powah's Uraninite, Energized Steel and four crystals.
- Automatically detect Silicon and Sky Stone.
- Automatically detect Silent Gear's Crimson Iron, Crimson Steel, Azure Silver, Azure Electrum and Blaze Gold.

### Fixed

- Fixed vertical Wool Insulated Cable arms pointing away from their connections.
- Fixed a cable-network crash when its route cache is invalidated during route discovery.

## [0.1.0-alpha.1]

Initial public alpha release for Minecraft 1.21.1 (NeoForge).

### Resource Sheep

- Transform vanilla sheep into resource-producing sheep using Resource Feed.
- Harvest Infused Wool through shearing.
- Support for 11 vanilla resources.
- Automatic detection of up to 37 additional modded materials through common item tags.
- Configurable resource availability.

### Machines & Automation

- **Wool Generator:** Generate FE from Infused Wool.
- **Wool Extractor:** Process Infused Wool into valuable resources.
- **Wool Worker:** Automatically shear sheep and regrow their wool using wheat, with a configurable working area.
- **Wool Insulated Cable:** Transfer energy between machines, with configurable extraction connections.

### Upgrades & Tools

- **Woolly Speed Upgrade:** Increase extraction speed.
- **Woolly Overdrive Upgrade:** Increase extraction speed and resource output at the cost of higher energy consumption.
- **Woolly Muffler:** Control machine and sheep sounds.
- Use shears to configure cable connections and visualize Wool Worker working areas.

### Integration & Customization

- Optional JEI integration for recipes and resource variants.
- Optional Jade integration for machine and sheep information.
- Compatibility with external FE energy systems and item automation.
- Compatibility tested with Almost Unified.
- Data-driven resource definitions and extraction recipes.
- Datapack and KubeJS customization support.
- In-game resource configuration.

### General

- Custom textures, machine interfaces and item tooltips.
- Dedicated advancement tree introducing the mod's mechanics.
- Dedicated server support.

### Alpha Notice

This is an early public release. Bugs, balancing changes and compatibility issues may occur. Back up important worlds before updating to newer versions.
