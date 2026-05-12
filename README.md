# ItemsAdderAdditions

<div align="center">

[![Modrinth](https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/compact/available/modrinth_vector.svg)](https://modrinth.com/plugin/itemsadderadditions)
[![Hangar](https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/compact/available/hangar_vector.svg)](https://hangar.papermc.io/PuppyTransGirl/ItemsAdderAdditions)
[![Spigot](https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/compact/available/spigot_vector.svg)](https://www.spigotmc.org/resources/itemsadderadditions.133918/)

[![Documentation](https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/cozy/documentation/ghpages_vector.svg)](https://itemsadderadditions.com/docs)
[![Discord](https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/cozy/social/discord-plural_vector.svg)](https://discord.gg/jbQmacZ58H)
</div>

ItemsAdderAdditions is an add-on plugin for [ItemsAdder](https://itemsadder.com/) that extends it with extra actions,
behaviours, recipe types, and other advanced features, all configured directly inside your existing ItemsAdder YML
files.

## Table of Contents

- [Features](#features)
- [Requirements](#requirements)
- [Installation](#installation)
- [Usage](#usage)
- [Documentation](#documentation)
- [Creative Inventory Integration](#creative-inventory-integration)
- [Custom Paintings](#custom-paintings)
- [Client Registry Refresh](#client-registry-refresh)
- [Contributing](#contributing)
- [Support](#support)
- [License](#license)
- [Credits](#credits)

## Features

Please see the [documentation](https://itemsadderadditions.com/docs) for the full feature list, configuration details,
and examples.

### Extra Behaviours

ItemsAdderAdditions adds new behaviours that can be attached to your custom items, blocks, and furniture.

Examples include:

- [Connectable](https://itemsadderadditions.com/docs/behaviours/connectable)
- [Contact Damage](https://itemsadderadditions.com/docs/behaviours/contact-damage)
- [Storage](https://itemsadderadditions.com/docs/behaviours/storage)
- And more...

### Extra Actions

ItemsAdderAdditions adds new actions that can be triggered by configured events.

Examples include:

- [Actionbar](https://itemsadderadditions.com/docs/actions/actionbar)
- [Ignite](https://itemsadderadditions.com/docs/actions/ignite)
- [Mythic Mobs Skill](https://itemsadderadditions.com/docs/actions/mythic-mobs-skill)
- And more...

### New Recipe Types

ItemsAdderAdditions adds additional recipe types that are not available by default in ItemsAdder.

Currently includes:

- [Campfire recipes](https://itemsadderadditions.com/docs/recipes/campfire-cooking)
- [Stonecutter recipes](https://itemsadderadditions.com/docs/recipes/stonecutter)
- And more...

## Creative Inventory Integration

> [!NOTE]
> This feature is only available on Minecraft 1.21.5 and newer.
> It also requires Operator permissions and the Operator Items Tab to be enabled.

Any custom item you add through ItemsAdder can be integrated into the Operator Items Tab, making it much easier to
access your custom content in creative mode.

This allows operators to:

- browse custom items directly in creative mode
- search for them in the creative search tab
- quickly access custom content without using commands

<details>
<summary>How it works</summary>

Minecraft 1.21.5 introduced data-driven paintings, allowing resource packs and data packs to define custom painting
variants.

Each painting variant appears as its own item inside the Operator Items Tab.

ItemsAdderAdditions uses this system by generating painting variants for custom items.
These generated entries use the custom item's icon or model so they visually appear like proper creative inventory
entries.

When a player takes one of these generated painting items from the creative inventory, or drops it into their inventory,
ItemsAdderAdditions detects it and replaces it with the actual associated ItemsAdder item.

This makes the integration feel native while working within vanilla Minecraft's creative inventory systems.

</details>

## Custom Paintings

> [!NOTE]
> This feature is only available on Minecraft 1.21.5 and newer.

Custom paintings can be defined directly in ItemsAdder content YML files using a top-level `paintings` section.
Each entry registers a real Minecraft painting variant and can optionally link an ItemsAdder item that places it
in-game.

```yaml
info:
  namespace: mynamespace

paintings:
  sunset:
    enabled: true
    width: 2
    height: 1
    asset: mynamespace:sunset
    title: "<yellow>Sunset"
    author: "Toutou"
    item: mynamespace:sunset_painting
    include_in_random: true
```

The `asset` value points to the painting texture asset. For example, `mynamespace:sunset` uses:

```text
contents/mynamespace/resourcepack/assets/mynamespace/textures/painting/sunset.png
```

No datapack files are required for this feature. The plugin injects the painting variants and updates the vanilla
`minecraft:placeable` painting tag at runtime.

The linked `item` must be an existing ItemsAdder item. When right-clicked on a valid wall face, it places the configured
custom painting and follows normal interaction cancellation/protection checks as closely as possible.

Set `include_in_random: true` to let vanilla painting items randomly choose this custom painting when it fits the wall.
Leave it false or omit it to make the custom painting placeable only through its linked ItemsAdder item.

## Client Registry Refresh

Some features inject or update server registries at runtime, such as custom painting variants and the creative inventory
painting variants. On supported Paper versions, ItemsAdderAdditions can briefly send online players back through the
configuration phase after an ItemsAdderAdditions reload when one of its managed registry entries changed.

This lets the client receive the updated registry data without requiring a full server restart or a manual reconnect.
No datapacks are generated or required.

```yaml
features:
  client_registry_refresh: true

client_registry_refresh:
  delay_ticks: 20
  complete_delay_ticks: 10
  notify_players: true
  message: "<gray>Refreshing client registries..."
```

The refresh only runs when ItemsAdderAdditions detects that one of its own managed registry systems changed something.
It does not run after every reload by default.

## Runtime Configuration

The plugin-level `config.yml` keeps feature gates separate from ItemsAdder content files:

```yaml
features:
  creative_inventory_integration: true
  custom_paintings: true
  client_registry_refresh: true

behaviours:
  bed: true
  connectable: true
  contact_damage: true
  stackable: true
  storage: true

actions:
  actionbar: true
  clear_item: true
  ignite: true
  message: true
  mythic_mobs_skill: true
  open_inventory: true
  play_animation: true
  play_emote: true
  replace_biome: true
  shoot_fireball: true
  swing_hand: true
  teleport: true
  title: true
  toast: true
  veinminer: true
```

Reloading ItemsAdderAdditions uses one shared reload pipeline whether it is triggered by ItemsAdder finishing its
data load or by `/itemsadderadditions reload`. The command reloads `config.yml`, reapplies enabled/disabled actions and
behaviours, rescans ItemsAdder content files, and refreshes runtime systems in a consistent order. Existing config keys
remain compatible.

For maintainers, the reload pipeline is implemented as small `ReloadableContentSystem` steps instead of one large
manager method. Actions, behaviours, paintings, creative registry data, recipes, and worldgen each reload through their
own focused system while sharing one ItemsAdder item list and one parsed content-file registry.

The Java source tree is organized by runtime responsibility and feature module. Entrypoints live under `plugin`,
long-lived runtime ownership under `runtime`, reload contracts under `runtime.reload`, user-facing settings under
`settings`, feature systems under `feature.*`, optional integrations under `integration.*`, bytecode patches under
`patch`, and shared helpers under `common.*`. See `docs/PACKAGE_LAYOUT.md` for the full maintainer map.

## Requirements

- Paper server
- A compatible Java version for your server version
- [ItemsAdder](https://itemsadder.com/) installed and working correctly
- ItemsAdderAdditions jar placed in the `plugins/` folder

Some features may depend on specific Minecraft versions.
For exact compatibility and per-feature requirements, please check the documentation.

## Installation

1. Install [ItemsAdder](https://itemsadder.com/) on your server if you have not already.
2. Download the latest version of ItemsAdderAdditions from one of the available platforms:
    - [Modrinth](https://modrinth.com/plugin/itemsadderadditions)
    - [Hangar](https://hangar.papermc.io/PuppyTransGirl/ItemsAdderAdditions)
    - [Spigot](https://www.spigotmc.org/resources/itemsadderadditions.133918/)
3. Place the ItemsAdderAdditions jar into your server's `plugins` directory.
4. Start or restart the server.
5. Configure features directly inside your existing ItemsAdder configuration files as described in the documentation.

## Usage

ItemsAdderAdditions is configured through your existing ItemsAdder YML files.

The plugin is designed to integrate with the ItemsAdder content pipeline, so you generally do not need to maintain
separate configuration files for the features it adds.
Instead, you define additional behaviours, actions, or recipe-related options where appropriate in your ItemsAdder
content setup.

Because the available options vary depending on the feature, please refer to the documentation for exact configuration
formats and examples.

## Documentation

Full documentation is available here:

- [ItemsAdderAdditions Documentation](https://itemsadderadditions.com/docs)

For maintainers, internal architecture notes are available in [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md).

The documentation contains:

- Full feature list
- Configuration examples
- Setup instructions
- Version-specific notes
- Behaviour and action references

## Contributing

Contributions are welcome !

If you would like to improve the project, feel free to:

- Submit a pull request
- Suggest documentation improvements
- Share feedback and ideas in the [Discord server](https://discord.gg/jbQmacZ58H)

## Support

If you need help, want to report a bug, or have a feature suggestion, please use
the [Discord server](https://discord.gg/jbQmacZ58H).

Useful channels include:

- `#support-chat` for help and setup questions
- `#bug-report` for reporting bugs
- `#suggestions` for feature requests and ideas
- `#showcase` if you want to share what you made with the plugin

You can also check the [documentation](https://itemsadderadditions.com/docs) for setup guides and feature references.

## License

This project is licensed under the GNU General Public License v3.0.
See the [LICENSE](LICENSE) file for details.

Some parts of this project may include code licensed under additional third-party licenses.
See [THIRD_PARTY_LICENSES](THIRD_PARTY_LICENSES.md) for details.

## Credits

Special thanks to:

- [twinkycome](https://github.com/bruhhhwarrior/) for being the co-maintainer, creating the logo and showcases, and
  making the models and textures for the default optional pack
- [LoneDev](https://github.com/LoneDev6/) for creating ItemsAdder, which is why ItemsAdderAdditions exists
- [misieur](https://github.com/misieur) for creating the original creative menu integration code that inspired and
  helped shape this implementation
- [Andre601](https://github.com/Andre601) for helping create this README
