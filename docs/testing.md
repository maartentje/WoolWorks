# Compatibility testing checklist

These are manual test scenarios, not a record of completed tests. Use disposable worlds and back up existing saves before testing datapack changes.

## Standalone

- Run Wool Works without other mods; check existing vanilla resources, feed recipes, sheep transformation, shearing, generator and extractor operation, and item automation.
- Confirm unavailable optional materials do not appear in the creative tab, JEI, or machine ghost slots.

## Modded resources

- Install compatible material-providing mods and check their supported materials and tags; no particular external mod is required.
- Install multiple mods supplying the same material tag; check there is one resource type and extraction consistently chooses the same output.
- Automated standalone checks: `gradlew runGameTestServer`; repeat with `gradlew -PosmiumGameTest runGameTestServer` to load test-only tags containing copper and iron ingots for the optional catalogue. Do not use the test property for release builds.
- Run `gradlew -PemptyTagGameTest runGameTestServer` separately to check an explicitly empty salt tag. Do not combine test properties or use them for release builds.
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
