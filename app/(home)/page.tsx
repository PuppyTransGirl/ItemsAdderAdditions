import Link from 'next/link';
import type {Metadata} from 'next';

export const metadata: Metadata = {
    title: 'ItemsAdderAdditions - Free ItemsAdder Addon for Minecraft Paper Servers',
    description: 'ItemsAdderAdditions is a free Minecraft plugin addon that extends ItemsAdder with custom behaviours (contact damage, storage, connectable blocks), actions (messages, titles, MythicMobs skills, veinminer), and features - all configured inside your existing YML files.',
    alternates: {
        canonical: 'https://itemsadderadditions.com',
    },
    openGraph: {
        title: 'ItemsAdderAdditions - Free ItemsAdder Addon for Minecraft Servers',
        description: 'Extend ItemsAdder with custom behaviours, actions, and features. No extra config files, no restart - just /iareload.',
        url: 'https://itemsadderadditions.com',
    },
};

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

const jsonLd = {
    '@context': 'https://schema.org',
    '@graph': [
        {
            '@type': 'SoftwareApplication',
            '@id': 'https://itemsadderadditions.com/#software',
            name: 'ItemsAdderAdditions',
            applicationCategory: 'GameApplication',
            applicationSubCategory: 'Minecraft Plugin',
            operatingSystem: 'Java',
            url: 'https://itemsadderadditions.com',
            description: 'A free Minecraft server plugin addon that extends ItemsAdder with custom behaviours, actions, and features, configured directly inside existing YML files.',
            offers: {
                '@type': 'Offer',
                price: '0',
                priceCurrency: 'USD',
            },
            softwareRequirements: 'ItemsAdder, Paper 1.20.6+',
            downloadUrl: 'https://www.spigotmc.org/resources/itemsadderadditions.133918/',
            isPartOf: {
                '@type': 'SoftwareApplication',
                name: 'ItemsAdder',
                url: 'https://itemsadder.com/',
            },
        },
        {
            '@type': 'WebSite',
            '@id': 'https://itemsadderadditions.com/#website',
            url: 'https://itemsadderadditions.com',
            name: 'ItemsAdderAdditions',
            description: 'Documentation and download for the ItemsAdderAdditions Minecraft plugin addon.',
        },
        {
            '@type': 'FAQPage',
            mainEntity: [
                {
                    '@type': 'Question',
                    name: 'What is ItemsAdderAdditions?',
                    acceptedAnswer: {
                        '@type': 'Answer',
                        text: 'ItemsAdderAdditions is a free Minecraft plugin addon that extends ItemsAdder with extra behaviours (such as contact damage, storage containers, connectable blocks, and stackable blocks), actions (messages, titles, veinminer, MythicMobs skills, and more), and quality-of-life features - all configured directly inside your existing YML files without restarting the server.',
                    },
                },
                {
                    '@type': 'Question',
                    name: 'Does ItemsAdderAdditions require a server restart?',
                    acceptedAnswer: {
                        '@type': 'Answer',
                        text: 'No. All changes take effect with a simple /iareload command - no server restart is needed.',
                    },
                },
                {
                    '@type': 'Question',
                    name: 'What are the requirements for ItemsAdderAdditions?',
                    acceptedAnswer: {
                        '@type': 'Answer',
                        text: 'ItemsAdderAdditions requires ItemsAdder (latest recommended) and a Paper server running version 1.20.6 or later.',
                    },
                },
                {
                    '@type': 'Question',
                    name: 'Where can I download ItemsAdderAdditions?',
                    acceptedAnswer: {
                        '@type': 'Answer',
                        text: 'ItemsAdderAdditions is available for free on SpigotMC at https://www.spigotmc.org/resources/itemsadderadditions.133918/',
                    },
                },
            ],
        },
    ],
};

export default function HomePage() {
    return (
        <main className="flex flex-col w-full">
            <script
                type="application/ld+json"
                dangerouslySetInnerHTML={{ __html: JSON.stringify(jsonLd) }}
            />

            {/* ── Hero ── */}
            <section className="flex flex-col items-center text-center px-6 pt-24 pb-20 gap-6 border-b border-fd-border">
                <h1 className="max-w-2xl text-4xl md:text-6xl font-bold tracking-tight leading-[1.1]">
                    More from ItemsAdder.
                    <br />
                    No extra work.
                </h1>
                <p className="max-w-lg text-base md:text-lg text-fd-muted-foreground leading-relaxed">
                    ItemsAdderAdditions is a free Minecraft plugin addon that extends ItemsAdder with new actions, behaviours, and features -
                    configured directly inside your existing YML files. No server restart needed, just <code>/iareload</code>.
                </p>
                <div className="flex flex-wrap items-center justify-center gap-3 mt-1">
                    <a
                        href="https://modrinth.com/plugin/itemsadderadditions"
                        target="_blank"
                        rel="noreferrer"
                        className="rounded-md bg-fd-primary px-5 py-2.5 text-sm font-semibold text-fd-primary-foreground hover:opacity-90 transition-opacity"
                    >
                        Download on Modrinth
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
                        Make ItemsAdder blocks deal contact damage, hold inventories, stack on top of each other, or
                        connect to adjacent blocks - all from a few lines of YML inside your existing item config.
                    </p>
                    <Link href="/docs/behaviours/connectable" className="text-sm font-medium text-fd-primary hover:underline mt-1">
                        Browse behaviours →
                    </Link>
                </div>

                <div className="flex flex-col gap-3">
                    <h2 className="text-sm font-semibold uppercase tracking-widest text-fd-muted-foreground">Actions</h2>
                    <p className="text-xl font-semibold leading-snug">Trigger effects on any ItemsAdder event</p>
                    <p className="text-fd-muted-foreground text-sm leading-relaxed">
                        Send messages, show titles, fire projectiles, run MythicMobs skills, veinmine blocks, and more.
                        Every action supports delays, permission gates, and flexible player targeting.
                    </p>
                    <Link href="/docs/actions/parameters" className="text-sm font-medium text-fd-primary hover:underline mt-1">
                        Browse actions →
                    </Link>
                </div>

                <div className="flex flex-col gap-3">
                    <h2 className="text-sm font-semibold uppercase tracking-widest text-fd-muted-foreground">Features</h2>
                    <p className="text-xl font-semibold leading-snug">Quality-of-life for server owners</p>
                    <p className="text-fd-muted-foreground text-sm leading-relaxed">
                        Populate the creative inventory with your custom ItemsAdder items automatically, or add campfire
                        and stonecutter recipes - no new config files required.
                    </p>
                    <Link href="/docs/features/campfire-stonecutter-recipes" className="text-sm font-medium text-fd-primary hover:underline mt-1">
                        Browse features →
                    </Link>
                </div>
            </section>

            <div className="border-t border-fd-border" />

            {/* ── YML example ── */}
            <section className="w-full max-w-5xl mx-auto px-6 py-20 grid md:grid-cols-2 gap-16 items-center">
                <div className="flex flex-col gap-4">
                    <h2 className="text-2xl md:text-3xl font-bold tracking-tight leading-snug">
                        Works inside your existing ItemsAdder config.
                    </h2>
                    <p className="text-fd-muted-foreground leading-relaxed">
                        There are no separate config files to manage. Behaviours and actions live under
                        the same item or block entry you already have in ItemsAdder. A single reload is all it takes.
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

            {/* ── FAQ ── */}
            <section className="w-full max-w-5xl mx-auto px-6 py-16 flex flex-col gap-8">
                <h2 className="text-sm font-semibold uppercase tracking-widest text-fd-muted-foreground">FAQ</h2>
                <div className="grid md:grid-cols-2 gap-8">
                    <div className="flex flex-col gap-2">
                        <h3 className="font-semibold text-fd-foreground">Do I need to restart my server?</h3>
                        <p className="text-sm text-fd-muted-foreground leading-relaxed">
                            No. All ItemsAdderAdditions features apply instantly after a <code>/iareload</code>. No server restart required.
                        </p>
                    </div>
                    <div className="flex flex-col gap-2">
                        <h3 className="font-semibold text-fd-foreground">Is it compatible with my ItemsAdder version?</h3>
                        <p className="text-sm text-fd-muted-foreground leading-relaxed">
                            ItemsAdderAdditions works with the latest recommended version of ItemsAdder on Paper 1.20.6+.
                        </p>
                    </div>
                    <div className="flex flex-col gap-2">
                        <h3 className="font-semibold text-fd-foreground">Does it add new config files?</h3>
                        <p className="text-sm text-fd-muted-foreground leading-relaxed">
                            No. Behaviours and actions are configured inside your existing ItemsAdder YML item definitions.
                        </p>
                    </div>
                    <div className="flex flex-col gap-2">
                        <h3 className="font-semibold text-fd-foreground">Is ItemsAdderAdditions free?</h3>
                        <p className="text-sm text-fd-muted-foreground leading-relaxed">
                            Yes, ItemsAdderAdditions is completely free to download on SpigotMC.
                        </p>
                    </div>
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
                        <a href="https://modrinth.com/plugin/itemsadderadditions" target="_blank" rel="noreferrer" className="text-sm font-medium text-fd-primary hover:underline">
                            Download on Modrinth →
                        </a>
                        <a href="https://github.com/bruhhhwarrior/iaadditions/releases/latest" target="_blank" rel="noreferrer" className="text-sm font-medium text-fd-primary hover:underline">
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
