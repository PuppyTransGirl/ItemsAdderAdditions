# Creative Inventory Integration

**Credits** - This feature was originally coded by **Misieur** ([His Github](https://github.com/Misieur)).

{% hint style="warn" %}
**Requires Paper 1.21.11 or higher.** The feature is silently disabled on older versions.
{% endhint %}

This feature automatically adds every custom item registered by ItemsAdder into the vanilla creative menu. No manual
configuration per item is needed - it picks up everything ItemsAdder loads.

## Setup

The plugin handles everything automatically on first run. You just need to do two things after installing:

1. Run `/iazip` to regenerate and push the resource pack.
2. **Reconnect** to the server (or have players reconnect). The creative menu updates when the client joins, not during
   a reload.

That's it. After this, the creative inventory will contain all your custom items.

{% hint style="info" %}
Every time you add or remove custom items via ItemsAdder, restart the server and run `/iazip` again to see them in your
creative inventory.
{% endhint %}

## Enabling / disabling

The feature is controlled in `config.yml`:

```yaml
features:
  creative_inventory_integration: true
```

Set to `false` to disable it entirely. The plugin will not generate any resource pack files or inject any registry
entries when disabled.

## ItemsAdder config

On first setup the plugin automatically adds itself to ItemsAdder's resource pack merge list:

```yaml
# In ItemsAdder/config.yml (managed automatically)
resource-pack:
  zip:
    merge_other_plugins_resourcepacks_folders:
      - ItemsAdderAdditions/resourcepack
```

If for any reason this entry is missing (e.g. the file wasn't writable), you can add it manually. The plugin logs a
warning if it couldn't apply the change.

## Notes

* **Template items** are excluded - items marked `template: true` in their ItemsAdder config won't appear in the
  creative menu.
* Items using `graphics.icon` / `resource.icon` display their flat GUI icon. All other items use the 3D model they'd
  normally show when held in-hand.
* Vanilla paintings are unaffected - regular painting items continue to work
  normally.
