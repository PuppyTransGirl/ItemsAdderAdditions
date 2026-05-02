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

### Extra Actions

ItemsAdderAdditions adds new actions that can be triggered by configured events.

Examples include:

- [Actionbar](https://itemsadderadditions.com/docs/actions/actionbar)
- [Ignite](https://itemsadderadditions.com/docs/actions/ignite)
- [Mythic Mobs Skill](https://itemsadderadditions.com/docs/actions/mythic-mobs-skill)

### New Recipe Types

ItemsAdderAdditions adds additional recipe types that are not available by default in ItemsAdder.

Currently includes:

- [Campfire recipes](https://itemsadderadditions.com/docs/features/campfire-stonecutter-recipes)
- [Stonecutter recipes](https://itemsadderadditions.com/docs/features/campfire-stonecutter-recipes)

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
