// Wool Works / KubeJS 7 / NeoForge 1.21.1 manual tests.
// Choose exactly one case; "none" changes nothing. See examples/README.md.
const WOOLWORKS_TEST = 'none'

ServerEvents.recipes(event => {
  if (WOOLWORKS_TEST === 'generator_crafting') {
    event.remove({ id: 'woolworks:wool_generator' })
    event.custom({
      type: 'minecraft:crafting_shaped',
      pattern: ['CCC', 'CFC', 'WWW'],
      key: {
        C: { item: 'minecraft:copper_ingot' },
        F: { item: 'minecraft:furnace' },
        W: { tag: 'minecraft:wool' }
      },
      result: { id: 'woolworks:wool_generator', count: 1 }
    }).id('woolworks_test:generator_crafting')
  }

  // Remove the iron extraction machine recipe, not the extractor block recipe.
  if (WOOLWORKS_TEST === 'remove_iron_extraction') {
    event.remove({ id: 'woolworks:extraction/iron' })
  }

  if (WOOLWORKS_TEST === 'remove_tin_feed') {
    event.remove({ id: 'woolworks:tin_feed' })
  }

  if (WOOLWORKS_TEST === 'tin_block_feed') {
    event.remove({ id: 'woolworks:tin_feed' })
    event.custom({
      type: 'woolworks:resource_conditional_crafting',
      required_resource: 'woolworks:tin',
      recipe: {
        ingredients: [{ item: 'minecraft:wheat' }, { tag: 'c:storage_blocks/tin' }],
        result: {
          id: 'woolworks:resource_feed', count: 1,
          components: { 'woolworks:resource_type': 'woolworks:tin' }
        }
      }
    }).id('woolworks_test:tin_block_feed')
  }

  // Each extraction case removes the same default ID and adds exactly one recipe.
  if (WOOLWORKS_TEST === 'tin_double_output' ||
      WOOLWORKS_TEST === 'tin_slow_expensive' ||
      WOOLWORKS_TEST === 'tin_specific_output') {
    event.remove({ id: 'woolworks:extraction/tin' })
    const recipe = {
      type: 'woolworks:wool_extraction',
      ingredient: {
        type: 'neoforge:components', items: 'woolworks:infused_wool',
        components: { 'woolworks:resource_type': 'woolworks:tin' }
      },
      duration: 200, energy_per_tick: 40, required_resource: 'woolworks:tin'
    }
    if (WOOLWORKS_TEST === 'tin_slow_expensive') {
      recipe.result_tag = 'c:ingots/tin'
      recipe.duration = 400
      recipe.energy_per_tick = 80
    } else {
      // result_tag has no count option: counted output must name a specific item.
      recipe.result = { id: 'mekanism:ingot_tin', count: WOOLWORKS_TEST === 'tin_double_output' ? 2 : 1 }
    }
    event.custom(recipe).id('woolworks_test:tin_extraction')
  }
})
