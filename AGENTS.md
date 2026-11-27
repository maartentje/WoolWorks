# Development instructions

## Project and compatibility

- Target Minecraft Java 1.21.1, NeoForge 21.1.x, Java 21, ModDevGradle and Mojang mappings. Do not upgrade these without approval or use Fabric/legacy Forge APIs.
- Verify version-specific APIs against the available dependencies. Keep code modular and avoid unnecessary dependencies and abstractions.
- Gameplay, machine state and configuration are server-authoritative. Separate client-only rendering and screens from common/server code, and keep dedicated servers working.
- JEI and Jade are optional integrations; the mod must load without either. Server-provided Jade data requires Jade on the server. Do not add mandatory integration dependencies.

## Resources, recipes and persistence

- Resource definitions are synchronized datapack registry entries; generic Feed, Infused Wool and Resource Sheep use the `woolworks:resource_type` data component or resource ID. Add materials through data files where possible, not resource-specific items, entities or textures.
- Respect the existing automatic-resource and individual disabling rules, custom datapack resources, deterministic tag-based extraction outputs, and resource availability on both sides.
- Recipes are normal datapack resources with stable IDs and may be replaced independently by datapacks or KubeJS. Do not overwrite external recipe changes through late generation.
- Preserve saved entities, components, inventories, machine progress and existing IDs unless a migration is explicitly required. Invalid Resource Sheep are permanently converted to vanilla sheep on server load; do not convert before authoritative resources are ready.
- Avoid per-tick world scans, forced chunk loads and unnecessary inventory synchronization. Preserve existing caching and invalidation behavior when making related changes.
- See `docs/modpack-customization.md`, `docs/testing.md` and `examples/README.md` for formats, configuration and compatibility scenarios.

## Workflow and validation

- Read and address every requirement in a task; preserve unrelated work. Do not substitute placeholders for requested functionality or report incomplete work as complete.
- Keep GameTest code and shared test structures in `src/gameTest`, and optional fixture data in `src/osmiumTest` or `src/emptyTagTest`; none belongs in the production JAR.
- Build with `./gradlew build` (Windows: `gradlew.bat build`) and run `./gradlew runGameTestServer` for relevant changes. Use `-PosmiumGameTest` and `-PemptyTagGameTest` in separate runs for the optional-resource fixtures.
- Review changes for regressions, fix introduced build/test failures, and update relevant documentation. Explain genuine blockers precisely.
- Provide an in-game/manual checklist when applicable. Distinguish automated, manual and untested behavior; never claim runtime verification that was not performed.
- NeoForge documentation: https://docs.neoforged.net/docs/1.21.1/
