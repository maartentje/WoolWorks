# Developer Guide

## Environment

Wool Works targets **Minecraft Java 1.21.1**, **NeoForge 21.1.x**, **Java 21**, ModDevGradle and Mojang mappings. Use a Java 21 JDK and import the repository as a Gradle project. The Gradle wrapper supplies the configured Gradle version. See [NeoForge's 1.21.1 documentation](https://docs.neoforged.net/docs/1.21.1/) and the [Mojang mappings license](https://github.com/NeoForged/NeoForm/blob/main/Mojang.md).

From the repository root, on Windows:

```powershell
.\gradlew.bat build
.\gradlew.bat runClient
.\gradlew.bat runServer
.\gradlew.bat runGameTestServer
.\gradlew.bat -PosmiumGameTest runGameTestServer
.\gradlew.bat -PemptyTagGameTest runGameTestServer
```

On macOS or Linux, use `./gradlew` instead of `gradlew.bat`. The server run uses `--nogui`. Run the fixture configurations separately: both define a salt tag. They add test-only data to the GameTest source set, not the release JAR. The production artifact is written to `build/libs/woolworks-<minecraft_version>-<mod_version>.jar`, currently `woolworks-1.21.1-0.1.0-alpha.1.jar`.

ModDevGradle supplies run configurations for the IDE. After changing dependencies or run configurations, reload the Gradle project and regenerate or use the Gradle run tasks if an IDE launch has a stale classpath. Development runs use the local `run/` directory; keep its worlds and configuration out of version control.

## Project structure

| Path | Purpose |
| --- | --- |
| `src/main/java` | Common gameplay, registries, machines, client code and optional JEI/Jade integrations. |
| `src/main/resources` | Production assets, datapack recipes, resource definitions and other game data. |
| `src/main/templates` | Mod metadata expanded from `gradle.properties`. |
| `src/gameTest` | Development-only GameTest classes and test structure. |
| `src/osmiumTest`, `src/emptyTagTest` | Optional, mutually exclusive GameTest data fixtures. |
| `examples` | Datapack and KubeJS customization examples. |
| `docs` | Modpack documentation and manual testing checklist. |

The production JAR is built from `main`, not the GameTest source set. It includes `LICENSE.txt` and `TEMPLATE_LICENSE.txt`. Test classes, structures and fixture tags must remain outside the production artifact, including when fixture properties are supplied.

## Architecture notes

- Resource definitions are a synchronized datapack registry. Resource Feed and Infused Wool are generic items whose `woolworks:resource_type` data component identifies the material; Resource Sheep store the corresponding ID. Definitions and availability drive variants, colors and recipes. See the [Modpack Author Guide](modpack-customization.md) for formats and configuration.
- Resource and machine behavior is server-authoritative. Keep client rendering, screens and integration display code separate from common/server logic. Invalid Resource Sheep are converted to vanilla sheep only after server resources are ready; item identities are retained.
- Machine recipes are datapack entries with stable IDs. Extraction supports explicit results and deterministic tag-based results; generator fuel recipes can override the default. Do not regenerate recipes after datapack or KubeJS modifications.
- Machines use NeoForge FE and item capabilities. The Worker has 18 output slots and configurable operation costs; the Extractor's Speed and Overdrive Upgrades affect its processing, and unfinished progress resets when upgrades change. Cable paths are cached and invalidated on topology changes. A saved cable buffer can retain an unaccepted transfer; destroying a cable without a drop also destroys its buffer.
- The client-only cable indicator style is configured with `cableExtractionIndicatorStyle`. Server settings such as cable transfer and Worker costs are documented in the [Modpack Author Guide](modpack-customization.md).

JEI and Jade are compile-only optional integrations and are not bundled. The development client includes both; the development server includes Jade but not JEI. Jade's detailed multiplayer machine data needs Jade on both sides. Verify dedicated-server class loading and startup without either integration when changing compatibility code.

## Contributing and validation

Before proposing changes, read [AGENTS.md](../AGENTS.md), the [license](../LICENSE.txt) and the [testing checklist](testing.md). The project is **All Rights Reserved**; the license permits the listed uses but restricts redistribution of modified versions and reuse of proprietary code or assets without permission. Obtain any necessary permission before distributing changes.

Keep changes focused, preserve datapack compatibility and existing worlds, update the appropriate documentation, and run the build plus relevant GameTests. The [manual checklist](testing.md) covers visual, integration and multiplayer checks that automated tests cannot replace. Do not claim manual compatibility that was not actually tested.
