# ItemsAdderAdditions

**ItemsAdderAdditions** is a companion plugin for [ItemsAdder](https://itemsadder.com/) that extends its feature set
with extra actions, behaviours, and quality-of-life features that aren't available out of the box.

Everything is configured directly inside your existing ItemsAdder item/block YAML files - no separate files to manage.

## What's included

### 🎨 Features

* [Creative Inventory Integration](features/creative-inventory-integration.md) - Automatically populate the vanilla
  creative menu with all your custom items *(requires 1.21.11+)*

### ⚡ Actions

Actions let you run effects when a player interacts with a custom item, block, or furniture. Attach them to any
ItemsAdder item under the `events:` section.

* [Actionbar](actions/actionbar.md) - Send a message to the action bar
* [Clear Item](actions/clear-item.md) - Remove items from the player's inventory
* [Message](actions/message.md) - Send a chat message
* [Open Inventory](actions/open-inventory.md) - Open a vanilla workstation GUI
* [Play Animation](actions/play-animation.md) - Trigger a complex furniture animation
* [Shoot Fireball](actions/shoot-fireball.md) - Shoot a fireball
* [Play Emote](actions/play-emote.md) - Make a player perform an emote
* [Swing Hand](actions/swing-hand.md) - Swing the player's main or off hand
* [Teleport](actions/teleport.md) - Teleport the player to fixed coordinates
* [Title](actions/title.md) - Show a title on screen
* [Toast](actions/toast.md) - Show an advancement-style toast notification
* [Veinminer](actions/veinminer.md) - Break connected blocks of the same type

All actions share a set of **universal parameters** (permission gate, delay, targeting) documented on
the [Action Parameters](actions/parameters.md) page.

### 🧱 Behaviours

Behaviours change how a custom block or furniture works in the world. They attach at the item level and run passively.

* [Connectable](behaviours/connectable.md) - Automatically change shape when placed next to matching blocks (like stairs
  or tables)
* [Contact Damage](behaviours/contact-damage.md) - Deal damage to players/entities that touch the block
* [Stackable](behaviours/stackable.md) - Stack one custom block on top of another to form a multi-step structure

## Basic action example

Here's a quick example of how to attach an action to a custom item inside your ItemsAdder YAML:

```yaml
items:
  my_sword:
    # ... your item config ...
    events:
      interact:
        right:
          actionbar:
            text: "<green>You right-clicked!"
            permission: "myplugin.use" # Optional - see Action Parameters
```

Actions are loaded automatically after every `/iareload`. No restart needed.

## Requirements

* **ItemsAdder** (latest recommended)
* **Paper** 1.20.6 or higher
* Creative Inventory Integration requires **1.21.11 or higher**

## Optional example pack

A simple pack that shows some of the features of ItemsAdderAdditions. You can download it by following the link.
[Pack download link](https://github.com/bruhhhwarrior/iaadditions/releases/latest)
