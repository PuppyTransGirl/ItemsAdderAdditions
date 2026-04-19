'use client';

import {useCallback, useState} from 'react';

type FieldType = 'text' | 'number' | 'select' | 'textarea';

interface FieldDef {
    k: string;
    l: string;
    t: FieldType;
    r?: boolean;
    p?: string;
    h?: string;
    opts?: string[];
}

interface ActionDef {
    fields: FieldDef[];
}

interface BehaviourDef {
    fields?: FieldDef[];
    extra?: string;
    custom?: string;
}

const ACTIONS: Record<string, ActionDef> = {
    message: { fields: [{ k: 'text', l: 'text', t: 'text', r: true, p: '<rainbow>Hello %player_name%', h: 'MiniMessage & PlaceholderAPI' }] },
    actionbar: { fields: [{ k: 'text', l: 'text', t: 'text', r: true, p: '<red>Action bar text', h: 'MiniMessage & PlaceholderAPI' }] },
    title: {
        fields: [
            { k: 'title', l: 'title', t: 'text', r: true, p: '<bold><gold>Welcome!' },
            { k: 'subtitle', l: 'subtitle', t: 'text', r: false, p: '<gray>Enjoy your stay' },
            { k: 'fade_in', l: 'fade_in', t: 'number', r: false, p: '10', h: 'ticks (default: 10)' },
            { k: 'stay', l: 'stay', t: 'number', r: false, p: '70', h: 'ticks (default: 70)' },
            { k: 'fade_out', l: 'fade_out', t: 'number', r: false, p: '20', h: 'ticks (default: 20)' },
        ],
    },
    toast: {
        fields: [
            { k: 'icon', l: 'icon', t: 'text', r: true, p: 'minecraft:diamond or namespace:item' },
            { k: 'text', l: 'text (one per line)', t: 'textarea', r: true, p: '<white>Line one\n<bold>Line two' },
            { k: 'frame', l: 'frame', t: 'select', r: false, opts: ['', 'task', 'goal', 'challenge'], h: 'default: goal' },
        ],
    },
    ignite: { fields: [{ k: 'duration', l: 'duration', t: 'number', r: true, p: '200', h: 'ticks — 200 = 10 seconds' }] },
    clear_item: {
        fields: [
            { k: 'item', l: 'item', t: 'text', r: true, p: 'namespace:item' },
            { k: 'amount', l: 'amount', t: 'number', r: false, p: '1', h: 'default: 1' },
        ],
    },
    shoot_fireball: {
        fields: [
            { k: 'power', l: 'power', t: 'number', r: true, p: '1' },
            { k: 'speed', l: 'speed', t: 'number', r: false, p: '1.0', h: 'default: 1.0' },
            { k: 'fire', l: 'fire', t: 'select', r: false, opts: ['', 'true', 'false'], h: 'default: true' },
        ],
    },
    teleport: {
        fields: [
            { k: 'x', l: 'x', t: 'number', r: true, p: '100.5' },
            { k: 'y', l: 'y', t: 'number', r: true, p: '64.0' },
            { k: 'z', l: 'z', t: 'number', r: true, p: '-50.5' },
            { k: 'yaw', l: 'yaw', t: 'number', r: false, p: "player's yaw" },
            { k: 'pitch', l: 'pitch', t: 'number', r: false, p: "player's pitch" },
            { k: 'world', l: 'world', t: 'text', r: false, p: "player's world" },
        ],
    },
    open_inventory: {
        fields: [
            { k: 'type', l: 'type', t: 'select', r: true, opts: ['anvil', 'cartography_table', 'crafting_table', 'enchanting_table', 'ender_chest', 'grindstone', 'loom', 'smithing_table', 'stonecutter'] },
            { k: 'title', l: 'title', t: 'text', r: false, p: '<blue>Custom title', h: 'Requires Paper 1.21.4+' },
        ],
    },
    play_emote: { fields: [{ k: 'name', l: 'emote name', t: 'text', r: true, p: 'wave' }] },
    play_animation: { fields: [{ k: 'name', l: 'animation name', t: 'text', r: true, p: 'spin' }] },
    swing_hand: { fields: [{ k: 'hand', l: 'hand', t: 'select', r: false, opts: ['hand', 'off_hand'] }] },
    mythic_mobs_skill: {
        fields: [
            { k: 'skill', l: 'skill name', t: 'text', r: true, p: 'mega_attack' },
            { k: 'power', l: 'power', t: 'number', r: false, p: '1.0', h: 'default: 1.0' },
        ],
    },
    veinminer: { fields: [{ k: 'max_blocks', l: 'max_blocks', t: 'number', r: true, p: '16' }] },
};

const BEHAVIOURS: Record<string, BehaviourDef> = {
    contact_damage: {
        fields: [
            { k: 'amount', l: 'amount', t: 'number', r: true, p: '1.0' },
            { k: 'interval', l: 'interval', t: 'number', r: false, p: '20', h: 'ticks (default: 20)' },
            { k: 'fire_duration', l: 'fire_duration', t: 'number', r: false, p: '0', h: 'ticks (default: 0)' },
            { k: 'damage_when_sneaking', l: 'damage_when_sneaking', t: 'select', r: false, opts: ['', 'true', 'false'], h: 'default: true' },
        ],
        extra: 'contact_damage_extra',
    },
    storage: {
        fields: [
            { k: 'type', l: 'type', t: 'select', r: true, opts: ['STORAGE', 'SHULKER', 'DISPOSAL'] },
            { k: 'rows', l: 'rows', t: 'number', r: false, p: '3', h: '1–6 (default: 3)' },
            { k: 'title', l: 'title', t: 'text', r: false, p: '<gold>My Storage', h: 'MiniMessage supported' },
        ],
    },
    stackable: { custom: 'stackable' },
    connectable: { custom: 'connectable' },
};

const STAIR_KEYS = ['default', 'straight', 'left', 'right', 'outer', 'inner'];
const TABLE_KEYS = ['default', 'straight', 'middle', 'border', 'corner', 'end'];

function yamlQuote(val: string): string {
    if (/[:#\[\]{}&*!|>'"%@`]/.test(val) || val.startsWith(' ')) return `"${val}"`;
    return val;
}

function generateYaml(
    mode: 'action' | 'behaviour',
    type: string,
    values: Record<string, string>,
    showPotion: boolean,
): string {
    const ind = '  ';
    const lines: string[] = [];

    if (mode === 'action') {
        lines.push(`${type}:`);
        const def = ACTIONS[type];
        if (!def) return '';

        def.fields.forEach(f => {
            const val = (values[f.k] ?? '').trim();
            if (!val) return;
            if (f.t === 'textarea') {
                const rows = val.split('\n').filter(l => l.trim());
                if (rows.length === 1) {
                    lines.push(`${ind}${f.k}: ${yamlQuote(rows[0])}`);
                } else {
                    lines.push(`${ind}${f.k}:`);
                    rows.forEach(r => lines.push(`${ind}  - ${yamlQuote(r)}`));
                }
            } else if (f.t === 'number') {
                lines.push(`${ind}${f.k}: ${val}`);
            } else {
                lines.push(`${ind}${f.k}: ${yamlQuote(val)}`);
            }
        });

        const perm = (values['permission'] ?? '').trim();
        const delay = (values['delay'] ?? '').trim();
        const target = (values['u_target'] ?? '').trim();
        const radius = (values['target_radius'] ?? '').trim();

        if (perm) lines.push(`${ind}permission: "${perm}"`);
        if (delay && delay !== '0') lines.push(`${ind}delay: ${delay}`);
        if (target) {
            lines.push(`${ind}target: ${target}`);
            if (target === 'radius' && radius) lines.push(`${ind}target_radius: ${radius}`);
        }
    } else {
        lines.push('behaviours:');
        lines.push(`${ind}${type}:`);

        const def = BEHAVIOURS[type];
        if (!def) return lines.join('\n');

        if (def.custom === 'stackable') {
            const blocks = (values['stack_blocks'] ?? '').split('\n').filter(l => l.trim());
            if (blocks.length) {
                blocks.forEach(b => lines.push(`${ind}${ind}- ${b.trim()}`));
            } else {
                lines.push(`${ind}${ind}# add block IDs here`);
            }
        } else if (def.custom === 'connectable') {
            const t = (values['conn_type'] ?? 'stair').trim();
            if (t) lines.push(`${ind}${ind}type: ${t}`);
            const keys = t === 'table' ? TABLE_KEYS : STAIR_KEYS;
            keys.forEach(k => {
                const val = (values[`conn_${k}`] ?? '').trim();
                if (val) lines.push(`${ind}${ind}${k}: ${val}`);
            });
        } else if (def.fields) {
            def.fields.forEach(f => {
                const val = (values[f.k] ?? '').trim();
                if (!val) return;
                if (f.t === 'number') {
                    lines.push(`${ind}${ind}${f.k}: ${val}`);
                } else if (f.t === 'select' && !val) {
                    return;
                } else {
                    lines.push(`${ind}${ind}${f.k}: ${yamlQuote(val)}`);
                }
            });

            if (def.extra === 'contact_damage_extra' && showPotion) {
                const pt = (values['pe_type'] ?? '').trim();
                const pa = (values['pe_amplifier'] ?? '').trim();
                const pd = (values['pe_duration'] ?? '').trim();
                if (pt) {
                    lines.push(`${ind}${ind}potion_effect:`);
                    lines.push(`${ind}${ind}${ind}type: ${pt}`);
                    if (pa) lines.push(`${ind}${ind}${ind}amplifier: ${pa}`);
                    if (pd) lines.push(`${ind}${ind}${ind}duration: ${pd}`);
                }
            }
        }
    }

    return lines.join('\n');
}

function Field({ def, value, onChange }: { def: FieldDef; value: string; onChange: (v: string) => void }) {
    return (
        <div className="flex flex-col gap-1">
            <label className="flex items-center gap-1.5 text-xs text-fd-muted-foreground">
                {def.l}
                {def.r
                    ? <span className="text-[10px] text-red-500">required</span>
                    : <span className="rounded px-1 py-0.5 text-[10px] bg-fd-muted text-fd-muted-foreground">optional</span>
                }
            </label>
            {def.t === 'select' ? (
                <select
                    value={value}
                    onChange={e => onChange(e.target.value)}
                    className="w-full rounded-md border border-fd-border bg-fd-background px-2 py-1.5 text-sm text-fd-foreground focus:outline-none focus:ring-1 focus:ring-fd-ring"
                >
                    {(def.opts ?? []).map(o => (
                        <option key={o} value={o}>{o || '(default)'}</option>
                    ))}
                </select>
            ) : def.t === 'textarea' ? (
                <textarea
                    value={value}
                    onChange={e => onChange(e.target.value)}
                    placeholder={def.p}
                    rows={3}
                    className="w-full rounded-md border border-fd-border bg-fd-background px-2 py-1.5 font-mono text-xs text-fd-foreground focus:outline-none focus:ring-1 focus:ring-fd-ring resize-y"
                />
            ) : (
                <input
                    type={def.t}
                    value={value}
                    onChange={e => onChange(e.target.value)}
                    placeholder={def.p}
                    className="w-full rounded-md border border-fd-border bg-fd-background px-2 py-1.5 text-sm text-fd-foreground focus:outline-none focus:ring-1 focus:ring-fd-ring"
                />
            )}
            {def.h && <p className="text-[11px] text-fd-muted-foreground">{def.h}</p>}
        </div>
    );
}

export function Builder() {
    const [mode, setMode] = useState<'action' | 'behaviour'>('action');
    const [actionType, setActionType] = useState('message');
    const [behaviourType, setBehaviourType] = useState('contact_damage');
    const [values, setValues] = useState<Record<string, string>>({});
    const [showPotion, setShowPotion] = useState(false);
    const [copied, setCopied] = useState(false);

    const type = mode === 'action' ? actionType : behaviourType;
    const yaml = generateYaml(mode, type, values, showPotion);

    const setVal = useCallback((k: string, v: string) => {
        setValues(prev => ({ ...prev, [k]: v }));
    }, []);

    const switchMode = (m: typeof mode) => {
        setMode(m);
        setValues({});
        setShowPotion(false);
    };

    const switchType = (t: string) => {
        if (mode === 'action') setActionType(t);
        else setBehaviourType(t);
        setValues({});
        setShowPotion(false);
    };

    const copy = () => {
        navigator.clipboard.writeText(yaml).then(() => {
            setCopied(true);
            setTimeout(() => setCopied(false), 2000);
        });
    };

    const actionDef = ACTIONS[actionType];
    const behaviourDef = BEHAVIOURS[behaviourType];
    const connType = (values['conn_type'] ?? 'stair');
    const connKeys = connType === 'table' ? TABLE_KEYS : STAIR_KEYS;

    return (
        <div className="my-6 flex flex-col gap-4 not-prose">
            <div className="flex gap-1 rounded-lg border border-fd-border bg-fd-muted p-1 w-fit">
                {(['action', 'behaviour'] as const).map(m => (
                    <button
                        key={m}
                        onClick={() => switchMode(m)}
                        className={`rounded-md px-4 py-1.5 text-sm transition-colors ${
                            mode === m
                                ? 'bg-fd-background text-fd-foreground font-medium shadow-sm'
                                : 'text-fd-muted-foreground hover:text-fd-foreground'
                        }`}
                    >
                        {m === 'action' ? 'Actions' : 'Behaviours'}
                    </button>
                ))}
            </div>

            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                <div className="flex flex-col gap-4 rounded-xl border border-fd-border bg-fd-card p-4">
                    <div className="flex flex-col gap-1">
                        <label className="text-xs font-medium text-fd-muted-foreground">Type</label>
                        <select
                            value={type}
                            onChange={e => switchType(e.target.value)}
                            className="w-full rounded-md border border-fd-border bg-fd-background px-2 py-1.5 text-sm text-fd-foreground focus:outline-none focus:ring-1 focus:ring-fd-ring"
                        >
                            {Object.keys(mode === 'action' ? ACTIONS : BEHAVIOURS).map(k => (
                                <option key={k} value={k}>{k}</option>
                            ))}
                        </select>
                    </div>

                    <div className="h-px bg-fd-border" />

                    {mode === 'action' && actionDef && (
                        <>
                            <p className="text-xs font-medium text-fd-muted-foreground uppercase tracking-wide">Parameters</p>
                            <div className="flex flex-col gap-3">
                                {actionDef.fields.map(f => (
                                    <Field key={f.k} def={f} value={values[f.k] ?? ''} onChange={v => setVal(f.k, v)} />
                                ))}
                            </div>

                            <div className="h-px bg-fd-border" />
                            <p className="text-xs font-medium text-fd-muted-foreground uppercase tracking-wide">Universal parameters</p>
                            <div className="flex flex-col gap-3">
                                <Field def={{ k: 'permission', l: 'permission', t: 'text', r: false, p: 'myplugin.use' }} value={values['permission'] ?? ''} onChange={v => setVal('permission', v)} />
                                <Field def={{ k: 'delay', l: 'delay', t: 'number', r: false, p: '0', h: '20 ticks = 1 second' }} value={values['delay'] ?? ''} onChange={v => setVal('delay', v)} />
                                <Field def={{ k: 'u_target', l: 'target', t: 'select', r: false, opts: ['', 'other', 'all', 'radius'], h: 'default: self' }} value={values['u_target'] ?? ''} onChange={v => setVal('u_target', v)} />
                                {values['u_target'] === 'radius' && (
                                    <Field def={{ k: 'target_radius', l: 'target_radius', t: 'number', r: false, p: '10', h: 'blocks' }} value={values['target_radius'] ?? ''} onChange={v => setVal('target_radius', v)} />
                                )}
                            </div>
                        </>
                    )}

                    {mode === 'behaviour' && behaviourDef && (
                        <>
                            {behaviourDef.custom === 'stackable' && (
                                <>
                                    <p className="text-xs font-medium text-fd-muted-foreground uppercase tracking-wide">Parameters</p>
                                    <Field
                                        def={{ k: 'stack_blocks', l: 'block IDs (one per line)', t: 'textarea', r: true, p: 'namespace:block_2\nnamespace:block_3', h: 'Must be custom block IDs' }}
                                        value={values['stack_blocks'] ?? ''}
                                        onChange={v => setVal('stack_blocks', v)}
                                    />
                                </>
                            )}

                            {behaviourDef.custom === 'connectable' && (
                                <>
                                    <p className="text-xs font-medium text-fd-muted-foreground uppercase tracking-wide">Parameters</p>
                                    <div className="flex flex-col gap-3">
                                        <Field
                                            def={{ k: 'conn_type', l: 'type', t: 'select', r: false, opts: ['stair', 'table'] }}
                                            value={values['conn_type'] ?? 'stair'}
                                            onChange={v => setVal('conn_type', v)}
                                        />
                                        {connKeys.map(k => (
                                            <Field
                                                key={k}
                                                def={{ k: `conn_${k}`, l: k, t: 'text', r: k === 'default', p: `namespace:furniture_${k}` }}
                                                value={values[`conn_${k}`] ?? ''}
                                                onChange={v => setVal(`conn_${k}`, v)}
                                            />
                                        ))}
                                    </div>
                                </>
                            )}

                            {!behaviourDef.custom && behaviourDef.fields && (
                                <>
                                    <p className="text-xs font-medium text-fd-muted-foreground uppercase tracking-wide">Parameters</p>
                                    <div className="flex flex-col gap-3">
                                        {behaviourDef.fields.map(f => (
                                            <Field key={f.k} def={f} value={values[f.k] ?? ''} onChange={v => setVal(f.k, v)} />
                                        ))}
                                    </div>

                                    {behaviourDef.extra === 'contact_damage_extra' && (
                                        <>
                                            <div className="h-px bg-fd-border" />
                                            <label className="flex items-center gap-2 cursor-pointer text-sm text-fd-foreground">
                                                <input
                                                    type="checkbox"
                                                    checked={showPotion}
                                                    onChange={e => setShowPotion(e.target.checked)}
                                                    className="rounded"
                                                />
                                                Add potion effect
                                            </label>
                                            {showPotion && (
                                                <div className="flex flex-col gap-3 pl-3 border-l-2 border-fd-border">
                                                    <Field def={{ k: 'pe_type', l: 'type', t: 'text', r: true, p: 'POISON', h: 'e.g. POISON, WITHER, NAUSEA' }} value={values['pe_type'] ?? ''} onChange={v => setVal('pe_type', v)} />
                                                    <Field def={{ k: 'pe_amplifier', l: 'amplifier', t: 'number', r: false, p: '0', h: '0 = level 1 (default: 0)' }} value={values['pe_amplifier'] ?? ''} onChange={v => setVal('pe_amplifier', v)} />
                                                    <Field def={{ k: 'pe_duration', l: 'duration', t: 'number', r: false, p: '40', h: 'ticks (default: 40)' }} value={values['pe_duration'] ?? ''} onChange={v => setVal('pe_duration', v)} />
                                                </div>
                                            )}
                                        </>
                                    )}
                                </>
                            )}
                        </>
                    )}
                </div>

                <div className="flex flex-col gap-3 rounded-xl border border-fd-border bg-fd-card p-4">
                    <p className="text-xs font-medium text-fd-muted-foreground uppercase tracking-wide">Generated YAML</p>
                    <pre className="flex-1 overflow-x-auto rounded-lg bg-fd-muted p-3 font-mono text-xs leading-relaxed text-fd-foreground whitespace-pre">{yaml}</pre>
                    <button
                        onClick={copy}
                        className="self-start rounded-md border border-fd-border bg-fd-background px-3 py-1.5 text-sm text-fd-foreground transition-colors hover:bg-fd-muted"
                    >
                        {copied ? 'Copied!' : 'Copy'}
                    </button>
                </div>
            </div>
        </div>
    );
}