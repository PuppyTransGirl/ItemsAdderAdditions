---
description: Show an advancement-style toast notification
---

# Toast

```yaml
toast:
  icon: my_item
  text:
    - "<white>My <gradient:#AC52D4:#6C3484>ItemsAdderAdditions"
    - "<#E6FBFE><bold>Custom Toast"
  frame: "task" # Optional (Default value: goal)
```

For `icon`, refer to [this link](https://jd.papermc.io/paper/org/bukkit/inventory/ItemType.html#field-summary) for the
value or a custom item with `namespace:value` or `value`

For `frame`, the possible values are `task`, `goal`, and `challenge`

<figure><img src="../.gitbook/assets/actions_toast_example.gif" alt=""><figcaption></figcaption></figure>
