---
description: Add persistent storage containers to custom Blocks, Furniture, and Complex Furniture
---

# Storage

Turns any custom block or furniture into a storage container. Three independent modes
are available, each with different persistence semantics.

| Type       | Behaviour                                                                |
|------------|--------------------------------------------------------------------------|
| `STORAGE`  | Shared container - all players see the same inventory                    |
| `SHULKER`  | Contents travel inside the item when broken and are restored on re-place |
| `DISPOSAL` | Trash can - contents are silently deleted when the GUI is closed         |

## Configuration

{% tabs %}
{% tab title="STORAGE" %}
```yaml
items:
  my_chest:
    # ...
    behaviours:
      storage:
        type: STORAGE
        rows: 3 # Optional (Default: 3)
        title: "<gold>Community Chest</gold>" # Optional
```
{% endtab %}

{% tab title="SHULKER" %}
```yaml
items:
  my_shulker:
    # ...
    behaviours:
      storage:
        type: SHULKER
        rows: 3 # Optional (Default: 3)
        title: "<dark_purple>Portable Storage</dark_purple>" # Optional
```
> {% endtab %}

{% tab title="DISPOSAL" %}
```yaml
items:
  my_trash_can:
    # ...
    behaviours:
      storage:
        type: DISPOSAL
        rows: 1 # Optional (Default: 3)
        title: "<red>Trash Can</red>" # Optional
```
{% endtab %}
{% endtabs %}

Supports [MiniMessage](https://docs.papermc.io/adventure/minimessage/format/) formatting, [PlaceholderAPI](https://modrinth.com/plugin/placeholderapi) placeholders, and [ItemsAdder](https://itemsadder.com/) font images.
