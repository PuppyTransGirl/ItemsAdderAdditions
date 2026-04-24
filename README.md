# ItemsAdderAdditions

Extend ItemsAdder with extra actions, behaviours, and features - configured inside your existing YML files.

## Downloads

[![modrinth]](https://modrinth.com/plugin/itemsadderadditions) [![hangar]](https://hangar.papermc.io/PuppyTransGirl/ItemsAdderAdditions) [![spigot]](https://www.spigotmc.org/resources/itemsadderadditions.133918/)

## About

ItemsAdderAdditions is an add-on plugin for [ItemsAdder](https://itemsadder.com/) designed to add additional actions, behaviours, recipes and more that do not (yet) exist in ItemsAdder due to their complexity, or for being rather niche.

The plugin is designed to work with the existing ItemsAdder configuration, building off from it and allowing you to easily add new features without the need to maintain separate files.

## Features

Please see the [Documentation](https://itemsadderadditions.com/docs) for all available features.  
The below are just a small list of what actually exists in the plugin.

### Extra Behaviours

New Behaviours have been added such as [connectable furniture](https://itemsadderadditions.com/docs/behaviours/connectable) (like stairs connecting behaviour) or [adding storage](https://itemsadderadditions.com/docs/behaviours/storage) to any custom Block or furniture.

### Extra Actions

New Actions are added that can be executed on any configured Event to do things such as [send actionbar messages](https://itemsadderadditions.com/docs/actions/actionbar), [set entities on fire](https://itemsadderadditions.com/docs/actions/ignite) (even yourself!) or [veinmine blocks](https://itemsadderadditions.com/docs/actions/veinminer).

### New Recipe Types

The plugin allows you to add recipes for [Stonecutter and Campfire](https://itemsadderadditions.com/docs/features/campfire-stonecutter-recipes), allowing more variations in creating custom recipes!

### Creative Inventory Integration

> [!NOTE]
> This feature only exists for 1.21.5 and newer and requires you to have Operator permissions and the Operator Items Tab to be enabled.

Any custom item you add to ItemsAdder will be [added to the Operator Items Tab](https://itemsadderadditions.com/docs/features/creative-inventory-integration), allowing you to quickly and easily get the items you need. These items can even be searched for in the search tab!

<details><summary>How it works</summary><p>

1.21.5 added data-driven Paintings to Minecraft, allowing custom painting variants to be added by users through data and resource packs.  
Each painting variant displays as a separate item in the Operator Items Tab.

ItemsAdderAdditions uses this fact, by creating a new painting variant for each custom item, using the Item's displayed icon or model for the painting item to show in the GUI, making it appear as if it was actually integrated into the GUI.  
If a player now takes such a painting and puts it in their inventory, or drops it out of the Creative inventory, will ItemsAdderAddition replace it with the actual item associated with it.

</p></details>

## Contribute

TBD (PuppyTransGirl pls work on this. Thanks~ -Andre601)

[modrinth]: https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/cozy/available/modrinth_64h.png
[hangar]: https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/cozy/available/hangar_64h.png
[spigot]: https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/cozy/available/spigot_64h.png
