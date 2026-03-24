# Stackable

{% tabs %}
{% tab title="Simple" %}
```yaml
stackable:
  - my_block_2
  - my_block_3
  - my_block_4
```
{% endtab %}

{% tab title="Complex" %}
```yaml
stackable:
  blocks:
    - my_block_2
    - my_block_3
    - my_block_4
  items: # Optional (Defaults to the item's ID where the behaviour is specified)
    - my_item
    - BONE_MEAL
  sound: # Optional
    name: entity.villager.ambient # or a custom sound
    volume: 1.0 # Optional (Default value: 1.0)
    pitch: 1.0 # Optional (Default value: 1.0)
    category: MASTER # Optional (Default value: MASTER)
  decrement_amount: 2 # Optional (Default value: 1)
```

For `name`, refer to [this link](https://minecraft.wiki/w/Sounds.json/Java_Edition_values) for the value or a custom sound with `namespace:value`
{% endtab %}

{% tab title="Full" %}
```yaml
stackable:
  first_step:
    block: my_block_2
    items: # Optional (Defaults to the item's ID where the behaviour is specified)
      - my_item
      - BONE_MEAL
    sound: # Optional
      name: entity.villager.ambient # or a custom sound
      volume: 1.0 # Optional (Default value: 1.0)
      pitch: 1.0 # Optional (Default value: 1.0)
      category: MASTER # Optional (Default value: MASTER)
    decrement_amount: 2 # Optional (Default value: 1)
  another_step:
    ...
```

For `name`, refer to [this link](https://minecraft.wiki/w/Sounds.json/Java_Edition_values) for the value or a custom sound with `namespace:value`
{% endtab %}
{% endtabs %}

The IDs specified must be custom blocks. (You can use `REAL`, `REAL_NOTE`, `REAL_TRANSPARENT`, and `REAL_WIRE`)

You can put as many steps as you want

<figure><img src="../.gitbook/assets/behaviours_stackable_example.gif" alt=""><figcaption></figcaption></figure>

