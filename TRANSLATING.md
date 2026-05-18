# Translating the Wiki

The wiki supports multiple languages via [Fumadocs](https://fumadocs.dev/) i18n. Each language lives in its own folder
under `content/docs/` and a few lines of code register it with the site.

Currently supported: **English** (`en`), **French** (`fr`), **Dutch** (`nl`).

---

## Adding a new language

### 1. Create the content folder

Copy the entire English content tree and rename it with your language
code ([ISO 639-1](https://en.wikipedia.org/wiki/List_of_ISO_639-1_codes)):

```
content/docs/
  en/   ← source of truth
  fr/
  nl/
  de/   ← new language
```

```bash
cp -r content/docs/en content/docs/de
```

Translate every `.mdx` file in the new folder. The `meta.json` files can generally be left as-is (they control sidebar
structure), but you may translate the `"title"` value inside them if you want localized sidebar group names.

### 2. Register the language code

Open `lib/shared.ts` and add your language code to the `languages` array:

```ts
// lib/shared.ts
export const languages = ['en', 'fr', 'nl', 'de'] as const;
```

### 3. Add a language switcher tab

Open `lib/layout.shared.tsx` and add an entry to `langTabs`:

```tsx
// lib/layout.shared.tsx
{
    title: 'Deutsch',
        url
:
    localizedDocsRoute('de'),
        icon
:
    <span className="text-base leading-none">🇩🇪</span>,
}
,
```

That's all the code changes needed. The routing, sitemap, and search index are all generated automatically from the
`languages` array.

---

## Translation rules

### What to translate

- Frontmatter `title` and `description` values
- All prose text (paragraphs, list items, table cells that contain sentences)
- Sidebar group titles in `meta.json` (optional but recommended)

### What to leave unchanged

| Element              | Example                                          | Reason                                      |
|----------------------|--------------------------------------------------|---------------------------------------------|
| YAML config keys     | `behaviour:`, `type:`, `amount:`                 | These are code - not user-facing text       |
| Code block contents  | ` ```yaml … ``` `                                | Must match what users copy into their files |
| MDX component names  | `<Callout>`, `<Card>`, `<Cards>`                 | JSX syntax                                  |
| Frontmatter keys     | `title:`, `icon:`, `description:`                | Parsed by the framework                     |
| Icon names           | `icon: House`                                    | Lucide icon identifiers                     |
| Plugin/command names | `ItemsAdder`, `/iareload`, `itemsadderadditions` | Proper nouns                                |
| Internal links       | `href="/docs/actions/parameters"`                | URL paths - never add a locale prefix       |

### Link format

Links must **not** include the locale prefix. The router injects it automatically:

```mdx
✅  [action parameters](/docs/actions/parameters)
❌  [action parameters](/de/docs/actions/parameters)
```

### Frontmatter example

```mdx
---
title: Erste Schritte mit ItemsAdderAdditions
icon: House
description: Lade ItemsAdderAdditions herunter, ein kostenloses ItemsAdder-Addon für Minecraft-Server.
---
```

---

## Keeping translations up to date

The English (`en`) folder is the source of truth. When a page is added or changed in English, the same file in each
language folder needs to be updated.

A quick way to spot outdated files is to diff them against the English original:

```bash
diff content/docs/en/behaviours/storage.mdx content/docs/de/behaviours/storage.mdx
```

If a translated page is missing content that exists in English, the English fallback is **not** shown automatically -
the page will simply be incomplete. Keep translations in sync.

---

## Testing locally

```bash
npm run dev
```

Then navigate to `http://localhost:3000/de/docs` to preview your translation. Use the language switcher tabs in the top
bar to switch between locales.
