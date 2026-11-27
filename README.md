<p align="center">
  <img src="src/main/resources/logo.png" alt="Wool Works logo" width="320">
</p>

# Wool Works

**Turn a flock of sheep into a wool-powered resource industry.**

![Minecraft 1.21.1](https://img.shields.io/badge/Minecraft-1.21.1-5C8A3C)
![NeoForge](https://img.shields.io/badge/Mod%20loader-NeoForge-orange)
![Release 1.0.0](https://img.shields.io/badge/Release-1.0.0-blueviolet)
[![CurseForge](https://img.shields.io/badge/Download-CurseForge-orange)](https://www.curseforge.com/minecraft/mc-mods/wool-works)

Craft special feed, transform ordinary sheep into resource sheep, and shear their Infused Wool. Extract the resources you need—or turn wool into energy to keep your farm running.

![Resource Sheep with differently colored mineral fleeces in a wooden barn](docs/images/resource-sheep.png)

*A flock of Resource Sheep, ready to supply your wool-powered workshop.*

## What you can build

- **Resource Sheep:** Feed an adult sheep to give its wool a resource of your choice. Shear it and let it graze to regrow its fleece.
- **Wool Extractor:** Process Infused Wool into useful materials. Add Speed or Overdrive Upgrades to improve production.
- **Wool Generator:** Turn Infused Wool into Forge Energy (FE) for your machines.
- **Wool Insulated Cable:** Connect machines and move FE around your farm. Use shears to configure extraction connections.
- **Wool Worker:** Automate shearing and use wheat to regrow wool in a configurable area.
- **Woolly Muffler:** Set machines—and, around a Wool Worker, nearby sheep—to quieter sound modes.

![Wool Works machines, insulated cable, shears, upgrades and feed on display](docs/images/blocks-and-items.png)

*Machines, upgrades and tools at a glance.*

### Wool-powered processing

![A Wool Extractor and Wool Generator connected by insulated cable, with hoppers and resource displays](docs/images/wool-processing.png)

*Extract materials and generate energy from Infused Wool.*

### Automated sheep farming

![A Wool Worker beside a fenced flock of sheared sheep, connected to supplies and energy](docs/images/automated-shearing.png)

*Let the Wool Worker handle shearing and regrowth.*

## Getting started

1. Craft Resource Feed from wheat and a resource, such as an iron ingot.
2. Use the feed on an **adult vanilla sheep** to create a Resource Sheep.
3. Shear it for Infused Wool, then let it eat grass to regrow its fleece. Resource Sheep do not spawn naturally or breed.
4. Build a Wool Extractor to recover materials, or a Wool Generator to produce FE.
5. Connect machines with Wool Insulated Cable and expand your farm with a Wool Worker and upgrades.

The Wool Works advancement tab offers an in-game guide. JEI is helpful for looking up recipes and variants, but is not required.

![The Wool Works advancement tab showing Feed, Infused Wool, machines and upgrades](docs/images/advancements.png)

*Follow the advancement tab as your wool-powered industry grows.*

## Modded materials and compatibility

Wool Works automatically enables supported materials from installed mods when their common item tags are available. No specific resource mod is required. Resource availability can also be configured; see the [Modpack Author Guide](docs/modpack-customization.md) for details.

[JEI](https://github.com/mezz/JustEnoughItems) can show recipes and resource variants. [Jade](https://github.com/Snownee/Jade) can show machine and sheep information; detailed machine data on a dedicated server requires Jade on both server and client. Both integrations are optional.

Manual compatibility testing has covered KubeJS recipe customization, Almost Unified, external FE systems and item automation, including Pipez. Individual mod versions and configurations can differ; this is not a guarantee of universal compatibility.

## Installation

1. Install **Minecraft Java Edition 1.21.1** and **NeoForge 21.1.267 or newer in the 21.1.x series**.
2. Download the Wool Works release JAR from [CurseForge](https://www.curseforge.com/minecraft/mc-mods/wool-works) and place it in the instance's `mods` folder. For multiplayer, install it on both the server and connecting clients.
3. Launch the game. JEI, Jade and material-providing mods may be installed separately, but none is required.

## More information

- [Modpack Author Guide](docs/modpack-customization.md)
- [Developer Guide](docs/development.md)
- [Changelog](CHANGELOG.md)
- [Report an issue or suggest a feature](https://github.com/maartentje/WoolWorks/issues)
- [License](LICENSE.txt)
