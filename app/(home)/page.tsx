import Link from 'next/link';

// Each entry: [indent, key | null, value | null, isListItem]
type YLine = { indent: string; key?: string; value?: string; list?: string; comment?: string };

const yamlLines: YLine[] = [
    { indent: '', key: 'items' },
    { indent: '  ', key: 'my_cactus' },
    { indent: '    ', key: 'display_name', value: '"<green>Cactus"' },
    { indent: '    ', key: 'behaviours' },
    { indent: '      ', key: 'contact_damage' },
    { indent: '        ', key: 'amount', value: '1.5' },
    { indent: '        ', key: 'interval', value: '10' },
    { indent: '        ', key: 'potion_effect' },
    { indent: '          ', key: 'type', value: 'POISON' },
    { indent: '          ', key: 'duration', value: '60' },
    { indent: '      ', key: 'stackable' },
    { indent: '        ', list: 'my_cactus_2' },
    { indent: '        ', list: 'my_cactus_3' },
];

function YamlBlock() {
    return (
        <>
            {yamlLines.map((line, i) => {
                const isBehaviours = line.key === 'behaviours';
                if (line.list) {
                    return (
                        <div key={i}>
                            <span className="text-fd-muted-foreground">{line.indent}</span>
                            <span className="text-fd-muted-foreground">{'- '}</span>
                            <span className="text-fd-foreground">{line.list}</span>
                        </div>
                    );
                }
                return (
                    <div key={i}>
                        <span>{line.indent}</span>
                        <span className={isBehaviours ? 'text-fd-primary font-semibold' : 'text-fd-foreground'}>
              {line.key}
            </span>
                        <span className="text-fd-muted-foreground">:</span>
                        {line.value && (
                            <>
                                <span>{' '}</span>
                                <span className="text-fd-muted-foreground">{line.value}</span>
                            </>
                        )}
                    </div>
                );
            })}
        </>
    );
}

export default function HomePage() {
    return (
        <main className="flex flex-col w-full">

            {/* ── Hero ── */}
            <section className="flex flex-col items-center text-center px-6 pt-24 pb-20 gap-6 border-b border-fd-border">
                <h1 className="max-w-2xl text-4xl md:text-6xl font-bold tracking-tight leading-[1.1]">
                    More from ItemsAdder.
                    <br />
                    No extra work.
                </h1>
                <p className="max-w-lg text-base md:text-lg text-fd-muted-foreground leading-relaxed">
                    ItemsAdderAdditions extends ItemsAdder with new actions, behaviours, and features -
                    configured directly inside your existing YAML files.
                </p>
                <div className="flex flex-wrap items-center justify-center gap-3 mt-1">
                    <a
                        href="https://www.spigotmc.org/resources/itemsadderadditions.133918/"
                        target="_blank"
                        rel="noreferrer"
                        className="rounded-md bg-fd-primary px-5 py-2.5 text-sm font-semibold text-fd-primary-foreground hover:opacity-90 transition-opacity"
                    >
                        Download on SpigotMC
                    </a>
                    <Link
                        href="/docs"
                        className="rounded-md border border-fd-border bg-fd-card px-5 py-2.5 text-sm font-semibold text-fd-foreground hover:bg-fd-accent transition-colors"
                    >
                        Documentation
                    </Link>
                </div>
            </section>

            {/* ── What it adds ── */}
            <section className="w-full max-w-5xl mx-auto px-6 py-20 grid md:grid-cols-3 gap-12">
                <div className="flex flex-col gap-3">
                    <h2 className="text-sm font-semibold uppercase tracking-widest text-fd-muted-foreground">Behaviours</h2>
                    <p className="text-xl font-semibold leading-snug">Custom logic for blocks and furniture</p>
                    <p className="text-fd-muted-foreground text-sm leading-relaxed">
                        Make blocks deal contact damage, hold inventories, stack on top of each other, or
                        connect to adjacent blocks - all from a few lines of YAML.
                    </p>
                    <Link href="/docs/behaviours/connectable" className="text-sm font-medium text-fd-primary hover:underline mt-1">
                        Browse behaviours →
                    </Link>
                </div>

                <div className="flex flex-col gap-3">
                    <h2 className="text-sm font-semibold uppercase tracking-widest text-fd-muted-foreground">Actions</h2>
                    <p className="text-xl font-semibold leading-snug">Trigger effects on any ItemsAdder event</p>
                    <p className="text-fd-muted-foreground text-sm leading-relaxed">
                        Send messages, show titles, fire projectiles, run MythicMobs skills, veinmine, and more.
                        Actions support delays, permission gates, and player targeting out of the box.
                    </p>
                    <Link href="/docs/actions/parameters" className="text-sm font-medium text-fd-primary hover:underline mt-1">
                        Browse actions →
                    </Link>
                </div>

                <div className="flex flex-col gap-3">
                    <h2 className="text-sm font-semibold uppercase tracking-widest text-fd-muted-foreground">Features</h2>
                    <p className="text-xl font-semibold leading-snug">Quality-of-life for server owners</p>
                    <p className="text-fd-muted-foreground text-sm leading-relaxed">
                        Populate the creative inventory with your custom items automatically, or add campfire
                        and stonecutter recipes - no new config files required.
                    </p>
                    <Link href="/docs/features/campfire-stonecutter-recipes" className="text-sm font-medium text-fd-primary hover:underline mt-1">
                        Browse features →
                    </Link>
                </div>
            </section>

            <div className="border-t border-fd-border" />

            {/* ── YAML example ── */}
            <section className="w-full max-w-5xl mx-auto px-6 py-20 grid md:grid-cols-2 gap-16 items-center">
                <div className="flex flex-col gap-4">
                    <h2 className="text-2xl md:text-3xl font-bold tracking-tight leading-snug">
                        Works inside your existing config.
                    </h2>
                    <p className="text-fd-muted-foreground leading-relaxed">
                        There are no separate config files to manage. Behaviours and actions live under
                        the same item or block entry you already have in ItemsAdder. A reload is all it takes.
                    </p>
                    <code className="w-fit rounded bg-fd-muted px-2 py-1 text-sm font-mono text-fd-foreground">
                        /iareload
                    </code>
                </div>

                <div className="rounded-lg border border-fd-border overflow-hidden">
                    <div className="px-4 py-2.5 border-b border-fd-border bg-fd-muted/50">
                        <span className="text-xs font-mono text-fd-muted-foreground">my_items.yml</span>
                    </div>
                    <pre className="p-5 font-mono text-xs leading-relaxed overflow-x-auto bg-fd-card text-fd-foreground">
            <YamlBlock />
          </pre>
                </div>
            </section>

            <div className="border-t border-fd-border" />

            {/* ── Requirements + links ── */}
            <section className="w-full max-w-5xl mx-auto px-6 py-16 flex flex-col md:flex-row gap-12 justify-between">
                <div className="flex flex-col gap-3">
                    <h2 className="text-sm font-semibold uppercase tracking-widest text-fd-muted-foreground">Requirements</h2>
                    <ul className="flex flex-col gap-1 text-sm">
                        <li><span className="text-fd-muted-foreground">Server - </span>Paper 1.20.6 or later</li>
                        <li><span className="text-fd-muted-foreground">Plugin - </span>ItemsAdder (latest recommended)</li>
                    </ul>
                </div>
                <div className="flex flex-col gap-3">
                    <h2 className="text-sm font-semibold uppercase tracking-widest text-fd-muted-foreground">Get started</h2>
                    <div className="flex flex-col gap-1.5">
                        <a href="https://www.spigotmc.org/resources/itemsadderadditions.133918/" target="_blank" rel="noreferrer" className="text-sm font-medium text-fd-primary hover:underline">
                            Download on SpigotMC →
                        </a>
                        <a href="https://github.com/PuppyTransGirl/ItemsAdderAdditions/releases/latest" target="_blank" rel="noreferrer" className="text-sm font-medium text-fd-primary hover:underline">
                            Download example pack →
                        </a>
                        <Link href="/docs" className="text-sm font-medium text-fd-primary hover:underline">
                            Read the docs →
                        </Link>
                    </div>
                </div>
            </section>

        </main>
    );
}