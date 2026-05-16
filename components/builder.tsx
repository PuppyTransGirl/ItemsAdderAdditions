'use client';

import {useCallback, useState} from 'react';

type FieldType = 'text' | 'number' | 'select' | 'textarea';
type Mode = 'action' | 'behaviour' | 'recipe' | 'worldgen';

interface FieldDef {
    k: string;
    l: string;
    t: FieldType;
    r?: boolean;
    p?: string;
    h?: string;
    opts?: string[];
}

interface PotionEffect {
    type: string;
    amplifier: string;
    duration: string;
    ambient: string;
    particles: string;
    icon: string;
}

interface StackStep {
    block: string;
    items: string;
    decrement_amount: string;
    sound_name: string;
    sound_volume: string;
    sound_pitch: string;
    sound_category: string;
}

interface TextDisplayEntry {
    id: string;
    text: string;
    offset: string;
    billboard: string;
    alignment: string;
    shadow: string;
    see_through: string;
    line_width: string;
    background: string;
    opacity: string;
    scale: string;
    view_range: string;
    refresh_interval: string;
}

interface CraftingIngredient {
    item: string;
    amount: string;
    damage: string;
    replacement: string;
    ignore_durability: string;
    potion_type: string;
}

interface ShapelessIngredient {
    item: string;
    amount: string;
    replacement: string;
    ignore_durability: string;
}

function emptyPotion(): PotionEffect {
    return {type: '', amplifier: '', duration: '', ambient: '', particles: '', icon: ''};
}

function emptyStep(): StackStep {
    return {
        block: '',
        items: '',
        decrement_amount: '',
        sound_name: '',
        sound_volume: '',
        sound_pitch: '',
        sound_category: ''
    };
}

function emptyTextDisplay(id: string): TextDisplayEntry {
    return {
        id,
        text: '',
        offset: '',
        billboard: '',
        alignment: '',
        shadow: '',
        see_through: '',
        line_width: '',
        background: '',
        opacity: '',
        scale: '',
        view_range: '',
        refresh_interval: ''
    };
}

function emptyCraftingIngredient(): CraftingIngredient {
    return {item: '', amount: '', damage: '', replacement: '', ignore_durability: '', potion_type: ''};
}

function emptyShapeless(): ShapelessIngredient {
    return {item: '', amount: '', replacement: '', ignore_durability: ''};
}

function yamlQuote(val: string): string {
    if (/[:#\[\]{}&*!|>'"%@`]/.test(val) || val.startsWith(' ')) {
        return `"${val}"`;
    }
    return val;
}

const ACTIONS: Record<string, FieldDef[]> = {
    actionbar: [
        {
            k: 'text',
            l: 'text',
            t: 'text',
            r: true,
            p: '<red>Action bar text',
            h: 'Font Images & MiniMessage & PlaceholderAPI'
        },
    ],
    clear_item: [
        {k: 'item', l: 'item', t: 'text', r: true, p: 'namespace:item'},
        {k: 'amount', l: 'amount', t: 'number', r: false, p: '1', h: 'default: 1'},
    ],
    ignite: [
        {k: 'duration', l: 'duration', t: 'number', r: true, p: '200', h: 'ticks - 200 = 10 seconds'},
    ],
    message: [
        {
            k: 'text',
            l: 'text',
            t: 'text',
            r: true,
            p: '<rainbow>Hello %player_name%',
            h: 'Font Images & MiniMessage & PlaceholderAPI'
        },
    ],
    mythic_mobs_skill: [
        {k: 'skill', l: 'skill name', t: 'text', r: true, p: 'mega_attack'},
        {k: 'power', l: 'power', t: 'number', r: false, p: '1.0', h: 'default: 1.0'},
    ],
    open_inventory: [
        {
            k: 'type',
            l: 'type',
            t: 'select',
            r: true,
            opts: ['anvil', 'cartography_table', 'crafting_table', 'enchanting_table', 'ender_chest', 'grindstone', 'loom', 'smithing_table', 'stonecutter']
        },
        {k: 'title', l: 'title', t: 'text', r: false, p: '<blue>Custom title', h: 'Requires Paper 1.21.4+'},
    ],
    play_animation: [
        {k: 'name', l: 'animation name', t: 'text', r: true, p: 'spin'},
    ],
    play_emote: [
        {k: 'name', l: 'emote name', t: 'text', r: true, p: 'wave'},
    ],
    replace_biome: [
        {k: 'biome', l: 'biome', t: 'text', r: true, p: 'minecraft:cherry_grove'},
        {
            k: 'shape',
            l: 'shape',
            t: 'select',
            r: true,
            opts: ['CUBOID', 'RHOMBUS', 'SPHERE', 'CYLINDER', 'SHELL', 'TORUS', 'CONE', 'BEAM', 'PYRAMID']
        },
        {
            k: 'radius_mode',
            l: 'radius format',
            t: 'select',
            r: true,
            opts: ['blocks_from_center', 'xyz'],
            h: 'Use blocks_from_center for uniform shapes like SPHERE, or x/y/z for custom sizes'
        },
        {k: 'blocks_from_center', l: 'blocks_from_center', t: 'number', r: false, p: '5'},
        {k: 'radius_x', l: 'x', t: 'number', r: false, p: '5'},
        {k: 'radius_y', l: 'y', t: 'number', r: false, p: '4'},
        {k: 'radius_z', l: 'z', t: 'number', r: false, p: '3'},
    ],
    shoot_fireball: [
        {k: 'power', l: 'power', t: 'number', r: true, p: '1'},
        {k: 'speed', l: 'speed', t: 'number', r: false, p: '1.0', h: 'default: 1.0'},
        {k: 'fire', l: 'fire', t: 'select', r: false, opts: ['', 'true', 'false'], h: 'default: true'},
    ],
    swing_hand: [
        {k: 'hand', l: 'hand', t: 'select', r: false, opts: ['hand', 'off_hand']},
    ],
    teleport: [
        {k: 'x', l: 'x', t: 'number', r: true, p: '100.5'},
        {k: 'y', l: 'y', t: 'number', r: true, p: '64.0'},
        {k: 'z', l: 'z', t: 'number', r: true, p: '-50.5'},
        {k: 'yaw', l: 'yaw', t: 'number', r: false, p: "player's yaw"},
        {k: 'pitch', l: 'pitch', t: 'number', r: false, p: "player's pitch"},
        {k: 'world', l: 'world', t: 'text', r: false, p: "player's world"},
    ],
    title: [
        {k: 'title', l: 'title', t: 'text', r: true, p: '<bold><gold>Welcome!'},
        {k: 'subtitle', l: 'subtitle', t: 'text', r: false, p: '<gray>Enjoy your stay'},
        {k: 'fade_in', l: 'fade_in', t: 'number', r: false, p: '10', h: 'ticks (default: 10)'},
        {k: 'stay', l: 'stay', t: 'number', r: false, p: '70', h: 'ticks (default: 70)'},
        {k: 'fade_out', l: 'fade_out', t: 'number', r: false, p: '20', h: 'ticks (default: 20)'},
    ],
    toast: [
        {k: 'icon', l: 'icon', t: 'text', r: true, p: 'minecraft:diamond or namespace:item'},
        {k: 'text', l: 'text (one per line)', t: 'textarea', r: true, p: '<white>Line one\n<bold>Line two'},
        {k: 'frame', l: 'frame', t: 'select', r: false, opts: ['', 'task', 'goal', 'challenge'], h: 'default: goal'},
    ],
    veinminer: [{k: 'max_blocks', l: 'max_blocks', t: 'number', r: true, p: '16'}],
};

const STAIR_KEYS = ['default', 'straight', 'left', 'right', 'outer', 'inner'];
const TABLE_KEYS = ['default', 'straight', 'middle', 'border', 'corner', 'end'];
const SOUND_CATEGORIES = ['MASTER', 'MUSIC', 'RECORD', 'WEATHER', 'BLOCK', 'HOSTILE', 'NEUTRAL', 'PLAYER', 'AMBIENT', 'VOICE'];

function SectionLabel({children}: { children: React.ReactNode }) {
    return <p className="text-xs font-medium uppercase tracking-wide text-fd-muted-foreground">{children}</p>;
}

function Divider() {
    return <div className="h-px bg-fd-border"/>;
}

function SubSection({children}: { children: React.ReactNode }) {
    return <div className="flex flex-col gap-3 border-l-2 border-fd-border pl-3">{children}</div>;
}

function CheckToggle({label, checked, onChange}: { label: string; checked: boolean; onChange: (v: boolean) => void }) {
    return (
        <label className="flex cursor-pointer items-center gap-2 text-sm text-fd-foreground">
            <input type="checkbox" checked={checked} onChange={e => onChange(e.target.checked)} className="rounded"/>
            {label}
        </label>
    );
}

const inputCls = 'w-full rounded-md border border-fd-border bg-fd-background px-2 py-1.5 text-sm text-fd-foreground focus:outline-none focus:ring-1 focus:ring-fd-ring';

function Field({def, value, onChange}: { def: FieldDef; value: string; onChange: (v: string) => void }) {
    return (
        <div className="flex flex-col gap-1">
            <label className="flex items-center gap-1.5 text-xs text-fd-muted-foreground">
                {def.l}
                {def.r ? (
                    <span className="text-[10px] text-red-500">required</span>
                ) : (
                    <span
                        className="rounded px-1 py-0.5 text-[10px] bg-fd-muted text-fd-muted-foreground">optional</span>
                )}
            </label>
            {def.t === 'select' ? (
                <select value={value} onChange={e => onChange(e.target.value)} className={inputCls}>
                    {(def.opts ?? []).map(o => <option key={o} value={o}>{o || '(default)'}</option>)}
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
                    type={def.t === 'number' ? 'number' : 'text'}
                    value={value}
                    onChange={e => onChange(e.target.value)}
                    placeholder={def.p}
                    className={inputCls}
                />
            )}
            {def.h && <p className="text-[11px] text-fd-muted-foreground">{def.h}</p>}
        </div>
    );
}

function SoundFields({sound: s, onChange}: {
    sound: { sound_name: string; sound_volume: string; sound_pitch: string; sound_category: string };
    onChange: (k: string, v: string) => void
}) {
    return (
        <div className="flex flex-col gap-3">
            <Field def={{
                k: 'sound_name',
                l: 'name',
                t: 'text',
                r: false,
                p: 'entity.villager.ambient',
                h: 'vanilla sound or namespace:sound'
            }} value={s.sound_name} onChange={v => onChange('sound_name', v)}/>
            <Field def={{k: 'sound_volume', l: 'volume', t: 'number', r: false, p: '1.0', h: 'default: 1.0'}}
                   value={s.sound_volume} onChange={v => onChange('sound_volume', v)}/>
            <Field def={{k: 'sound_pitch', l: 'pitch', t: 'number', r: false, p: '1.0', h: 'default: 1.0'}}
                   value={s.sound_pitch} onChange={v => onChange('sound_pitch', v)}/>
            <Field def={{
                k: 'sound_category',
                l: 'category',
                t: 'select',
                r: false,
                opts: ['', ...SOUND_CATEGORIES],
                h: 'default: MASTER'
            }} value={s.sound_category} onChange={v => onChange('sound_category', v)}/>
        </div>
    );
}

function PotionEffectFields({pe, index, onChange, onRemove}: {
    pe: PotionEffect;
    index: number;
    onChange: (k: keyof PotionEffect, v: string) => void;
    onRemove: () => void
}) {
    return (
        <div className="flex flex-col gap-3 rounded-lg border border-fd-border p-3">
            <div className="flex items-center justify-between">
                <span
                    className="text-xs font-medium text-fd-muted-foreground">{index === 0 ? 'potion_effect' : `potion_effect_${index}`}</span>
                {index > 0 && <button onClick={onRemove}
                                      className="text-xs text-fd-muted-foreground hover:text-red-500 transition-colors">Remove</button>}
            </div>
            <Field def={{k: 'type', l: 'type', t: 'text', r: true, p: 'POISON', h: 'e.g. POISON, WITHER, NAUSEA'}}
                   value={pe.type} onChange={v => onChange('type', v)}/>
            <Field def={{k: 'amplifier', l: 'amplifier', t: 'number', r: false, p: '0', h: '0 = level 1 (default: 0)'}}
                   value={pe.amplifier} onChange={v => onChange('amplifier', v)}/>
            <Field def={{k: 'duration', l: 'duration', t: 'number', r: false, p: '40', h: 'ticks (default: 40)'}}
                   value={pe.duration} onChange={v => onChange('duration', v)}/>
            <Field def={{
                k: 'ambient',
                l: 'ambient',
                t: 'select',
                r: false,
                opts: ['', 'true', 'false'],
                h: 'default: false - more translucent particles'
            }} value={pe.ambient} onChange={v => onChange('ambient', v)}/>
            <Field def={{
                k: 'particles',
                l: 'particles',
                t: 'select',
                r: false,
                opts: ['', 'true', 'false'],
                h: 'default: true'
            }} value={pe.particles} onChange={v => onChange('particles', v)}/>
            <Field def={{
                k: 'icon',
                l: 'icon',
                t: 'select',
                r: false,
                opts: ['', 'true', 'false'],
                h: 'default: true - show HUD icon'
            }} value={pe.icon} onChange={v => onChange('icon', v)}/>
        </div>
    );
}

function StackStepFields({step, index, onChange, onRemove}: {
    step: StackStep;
    index: number;
    onChange: (k: keyof StackStep, v: string) => void;
    onRemove: () => void
}) {
    const [showSound, setShowSound] = useState(false);
    return (
        <div className="flex flex-col gap-3 rounded-lg border border-fd-border p-3">
            <div className="flex items-center justify-between">
                <span className="text-xs font-medium text-fd-muted-foreground">step_{index + 1}</span>
                <button onClick={onRemove}
                        className="text-xs text-fd-muted-foreground hover:text-red-500 transition-colors">Remove
                </button>
            </div>
            <Field def={{k: 'block', l: 'block', t: 'text', r: true, p: 'namespace:block_next'}} value={step.block}
                   onChange={v => onChange('block', v)}/>
            <Field def={{
                k: 'items',
                l: 'items (one per line)',
                t: 'textarea',
                r: false,
                p: 'namespace:my_item\nBONE_MEAL',
                h: 'defaults to the item this behaviour is on'
            }} value={step.items} onChange={v => onChange('items', v)}/>
            <Field def={{k: 'decrement_amount', l: 'decrement_amount', t: 'number', r: false, p: '1', h: 'default: 1'}}
                   value={step.decrement_amount} onChange={v => onChange('decrement_amount', v)}/>
            <CheckToggle label="Add sound" checked={showSound} onChange={setShowSound}/>
            {showSound && (
                <SubSection>
                    <SoundFields sound={{
                        sound_name: step.sound_name,
                        sound_volume: step.sound_volume,
                        sound_pitch: step.sound_pitch,
                        sound_category: step.sound_category
                    }} onChange={(k, v) => onChange(k as keyof StackStep, v)}/>
                </SubSection>
            )}
        </div>
    );
}

function TextDisplayEntryFields({entry, index, onChange, onRemove}: {
    entry: TextDisplayEntry;
    index: number;
    onChange: (k: keyof TextDisplayEntry, v: string) => void;
    onRemove: () => void
}) {
    const [showAdvanced, setShowAdvanced] = useState(false);
    return (
        <div className="flex flex-col gap-3 rounded-lg border border-fd-border p-3">
            <div className="flex items-center justify-between">
                <span
                    className="text-xs font-medium text-fd-muted-foreground">{entry.id || `display_${index + 1}`}</span>
                <button onClick={onRemove}
                        className="text-xs text-fd-muted-foreground hover:text-red-500 transition-colors">Remove
                </button>
            </div>
            <Field def={{k: 'id', l: 'display id', t: 'text', r: true, p: 'label'}} value={entry.id}
                   onChange={v => onChange('id', v)}/>
            <Field def={{
                k: 'text',
                l: 'text (one per line)',
                t: 'textarea',
                r: true,
                p: '<yellow>Hello!',
                h: 'MiniMessage · PlaceholderAPI · :icons:'
            }} value={entry.text} onChange={v => onChange('text', v)}/>
            <Field def={{k: 'offset', l: 'offset', t: 'text', r: false, p: '0, 0.25, 0', h: 'x, y, z'}}
                   value={entry.offset} onChange={v => onChange('offset', v)}/>
            <Field def={{
                k: 'billboard',
                l: 'billboard',
                t: 'select',
                r: false,
                opts: ['', 'FIXED', 'VERTICAL', 'HORIZONTAL', 'CENTER'],
                h: 'default: VERTICAL'
            }} value={entry.billboard} onChange={v => onChange('billboard', v)}/>
            <Field def={{
                k: 'alignment',
                l: 'alignment',
                t: 'select',
                r: false,
                opts: ['', 'LEFT', 'CENTER', 'RIGHT'],
                h: 'default: CENTER'
            }} value={entry.alignment} onChange={v => onChange('alignment', v)}/>
            <Field
                def={{k: 'shadow', l: 'shadow', t: 'select', r: false, opts: ['', 'true', 'false'], h: 'default: true'}}
                value={entry.shadow} onChange={v => onChange('shadow', v)}/>
            <Field def={{
                k: 'see_through',
                l: 'see_through',
                t: 'select',
                r: false,
                opts: ['', 'true', 'false'],
                h: 'default: false'
            }} value={entry.see_through} onChange={v => onChange('see_through', v)}/>
            <Field def={{k: 'scale', l: 'scale', t: 'number', r: false, p: '1.0'}} value={entry.scale}
                   onChange={v => onChange('scale', v)}/>
            <Field def={{k: 'view_range', l: 'view_range', t: 'number', r: false, p: '16.0', h: 'blocks'}}
                   value={entry.view_range} onChange={v => onChange('view_range', v)}/>
            <Field def={{
                k: 'refresh_interval',
                l: 'refresh_interval',
                t: 'number',
                r: false,
                p: '0',
                h: 'ticks · 0 = disabled'
            }} value={entry.refresh_interval} onChange={v => onChange('refresh_interval', v)}/>
            <CheckToggle label="Advanced options" checked={showAdvanced} onChange={setShowAdvanced}/>
            {showAdvanced && (
                <SubSection>
                    <Field def={{
                        k: 'line_width',
                        l: 'line_width',
                        t: 'number',
                        r: false,
                        p: '200',
                        h: 'pixels (default: 200)'
                    }} value={entry.line_width} onChange={v => onChange('line_width', v)}/>
                    <Field def={{
                        k: 'background',
                        l: 'background',
                        t: 'text',
                        r: false,
                        p: '#00000080',
                        h: '#RRGGBB or #RRGGBBAA · null = vanilla default'
                    }} value={entry.background} onChange={v => onChange('background', v)}/>
                    <Field def={{
                        k: 'opacity',
                        l: 'opacity',
                        t: 'number',
                        r: false,
                        p: '-1',
                        h: '-1..255 · -1 = fully opaque'
                    }} value={entry.opacity} onChange={v => onChange('opacity', v)}/>
                </SubSection>
            )}
        </div>
    );
}

function CraftingIngredientRow({char, ing, onChange}: {
    char: string;
    ing: CraftingIngredient;
    onChange: (k: keyof CraftingIngredient, v: string) => void
}) {
    const [showAdv, setShowAdv] = useState(false);
    return (
        <div className="flex flex-col gap-3 rounded-lg border border-fd-border p-3">
            <span className="font-mono text-xs font-semibold text-fd-muted-foreground">{char}</span>
            <Field def={{k: `ing_${char}_item`, l: 'item', t: 'text', r: false, p: 'STICK or namespace:item or #tag'}}
                   value={ing.item} onChange={v => onChange('item', v)}/>
            <CheckToggle label="Advanced options" checked={showAdv} onChange={setShowAdv}/>
            {showAdv && (
                <SubSection>
                    <Field def={{
                        k: `ing_${char}_amount`,
                        l: 'amount',
                        t: 'number',
                        r: false,
                        p: '1',
                        h: 'how many of this item are consumed'
                    }} value={ing.amount} onChange={v => onChange('amount', v)}/>
                    <Field def={{
                        k: `ing_${char}_damage`,
                        l: 'damage',
                        t: 'number',
                        r: false,
                        p: '10',
                        h: 'damage instead of consuming'
                    }} value={ing.damage} onChange={v => onChange('damage', v)}/>
                    <Field def={{
                        k: `ing_${char}_replacement`,
                        l: 'replacement',
                        t: 'text',
                        r: false,
                        p: 'BUCKET',
                        h: 'item returned after crafting'
                    }} value={ing.replacement} onChange={v => onChange('replacement', v)}/>
                    <Field def={{
                        k: `ing_${char}_ignore_dur`,
                        l: 'ignore_durability',
                        t: 'select',
                        r: false,
                        opts: ['', 'true', 'false'],
                        h: 'default: false'
                    }} value={ing.ignore_durability} onChange={v => onChange('ignore_durability', v)}/>
                    <Field def={{k: `ing_${char}_potion`, l: 'potion_type', t: 'text', r: false, p: 'minecraft:water'}}
                           value={ing.potion_type} onChange={v => onChange('potion_type', v)}/>
                </SubSection>
            )}
        </div>
    );
}

function ShapelessIngredientFields({ing, index, onChange, onRemove}: {
    ing: ShapelessIngredient;
    index: number;
    onChange: (k: keyof ShapelessIngredient, v: string) => void;
    onRemove: () => void
}) {
    const [showAdvanced, setShowAdvanced] = useState(false);
    return (
        <div className="flex flex-col gap-3 rounded-lg border border-fd-border p-3">
            <div className="flex items-center justify-between">
                <span className="text-xs font-medium text-fd-muted-foreground">ingredient {index + 1}</span>
                <button onClick={onRemove}
                        className="text-xs text-fd-muted-foreground hover:text-red-500 transition-colors">Remove
                </button>
            </div>
            <Field def={{k: 'item', l: 'item', t: 'text', r: true, p: 'STICK or namespace:item or #tag'}}
                   value={ing.item} onChange={v => onChange('item', v)}/>
            <Field def={{k: 'amount', l: 'amount', t: 'number', r: false, p: '1', h: 'default: 1'}} value={ing.amount}
                   onChange={v => onChange('amount', v)}/>
            <CheckToggle label="Advanced options" checked={showAdvanced} onChange={setShowAdvanced}/>
            {showAdvanced && (
                <SubSection>
                    <Field def={{
                        k: 'replacement',
                        l: 'replacement',
                        t: 'text',
                        r: false,
                        p: 'BUCKET',
                        h: 'item to put back after crafting'
                    }} value={ing.replacement} onChange={v => onChange('replacement', v)}/>
                    <Field def={{
                        k: 'ignore_durability',
                        l: 'ignore_durability',
                        t: 'select',
                        r: false,
                        opts: ['', 'true', 'false'],
                        h: 'default: false'
                    }} value={ing.ignore_durability} onChange={v => onChange('ignore_durability', v)}/>
                </SubSection>
            )}
        </div>
    );
}

function soundYamlLines(s: {
    sound_name: string;
    sound_volume: string;
    sound_pitch: string;
    sound_category: string
}, yamlKey: string, ind: string): string[] {
    if (!s.sound_name.trim()) return [];
    const lines = [`${ind}${yamlKey}:`];
    lines.push(`${ind}  name: ${s.sound_name.trim()}`);
    if (s.sound_volume.trim()) lines.push(`${ind}  volume: ${s.sound_volume.trim()}`);
    if (s.sound_pitch.trim()) lines.push(`${ind}  pitch: ${s.sound_pitch.trim()}`);
    if (s.sound_category.trim()) lines.push(`${ind}  category: ${s.sound_category.trim()}`);
    return lines;
}

function textDisplaySpecLines(td: {
    text: string;
    offset: string;
    billboard: string;
    alignment: string;
    shadow: string;
    see_through: string;
    line_width: string;
    background: string;
    opacity: string;
    scale: string;
    view_range: string;
    refresh_interval: string
}, indent: string): string[] {
    const lines: string[] = [];
    const i = indent;
    const textLines = td.text.split('\n').filter(l => l.trim());
    if (textLines.length === 1) {
        lines.push(`${i}text: ${yamlQuote(textLines[0])}`);
    } else if (textLines.length > 1) {
        lines.push(`${i}text:`);
        textLines.forEach(l => lines.push(`${i}  - ${yamlQuote(l)}`));
    } else {
        lines.push(`${i}text: "<yellow>Hello!"`);
    }
    if (td.offset.trim()) lines.push(`${i}offset: "${td.offset.trim()}"`);
    if (td.billboard.trim()) lines.push(`${i}billboard: ${td.billboard.trim()}`);
    if (td.alignment.trim()) lines.push(`${i}alignment: ${td.alignment.trim()}`);
    if (td.shadow === 'false') lines.push(`${i}shadow: false`);
    if (td.see_through === 'true') lines.push(`${i}see_through: true`);
    if (td.scale.trim()) lines.push(`${i}scale: ${td.scale.trim()}`);
    if (td.view_range.trim()) lines.push(`${i}view_range: ${td.view_range.trim()}`);
    if (td.refresh_interval.trim() && td.refresh_interval.trim() !== '0') lines.push(`${i}refresh_interval: ${td.refresh_interval.trim()}`);
    if (td.line_width.trim()) lines.push(`${i}line_width: ${td.line_width.trim()}`);
    if (td.background.trim()) lines.push(`${i}background: "${td.background.trim()}"`);
    if (td.opacity.trim() && td.opacity.trim() !== '-1') lines.push(`${i}opacity: ${td.opacity.trim()}`);
    return lines;
}

function generateAction(type: string, values: Record<string, string>): string {
    const ind = '  ';
    const lines: string[] = [`${type}:`];

    if (type === 'replace_biome') {
        const biome = (values['biome'] ?? '').trim();
        const shape = (values['shape'] ?? '').trim();
        const radiusMode = (values['radius_mode'] ?? 'blocks_from_center').trim();
        const blocksFromCenter = (values['blocks_from_center'] ?? '').trim();
        const x = (values['radius_x'] ?? '').trim();
        const y = (values['radius_y'] ?? '').trim();
        const z = (values['radius_z'] ?? '').trim();

        if (biome) lines.push(`${ind}biome: ${yamlQuote(biome)}`);
        if (shape) lines.push(`${ind}shape: ${shape}`);
        lines.push(`${ind}radius:`);
        if (radiusMode === 'blocks_from_center') {
            if (blocksFromCenter) lines.push(`${ind}  blocks_from_center: ${blocksFromCenter}`);
        } else {
            if (x) lines.push(`${ind}  x: ${x}`);
            if (y) lines.push(`${ind}  y: ${y}`);
            if (z) lines.push(`${ind}  z: ${z}`);
        }
    } else {
        (ACTIONS[type] ?? []).forEach(f => {
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
            } else if (f.t === 'select' && !val) {
                return;
            } else {
                lines.push(`${ind}${f.k}: ${yamlQuote(val)}`);
            }
        });
    }

    const perm = (values['permission'] ?? '').trim();
    const delay = (values['delay'] ?? '').trim();
    const target = (values['u_target'] ?? '').trim();
    const radius = (values['target_radius'] ?? '').trim();
    const inSightDistance = (values['target_in_sight_distance'] ?? '').trim();

    if (perm) lines.push(`${ind}permission: "${perm}"`);
    if (delay && delay !== '0') lines.push(`${ind}delay: ${delay}`);
    if (target) {
        lines.push(`${ind}target: ${target}`);
        if (target === 'radius' && radius) lines.push(`${ind}target_radius: ${radius}`);
        if (target === 'in_sight' && inSightDistance) lines.push(`${ind}target_in_sight_distance: ${inSightDistance}`);
    }

    return lines.join('\n');
}

function generateBehaviour(
    type: string,
    values: Record<string, string>,
    extras: Record<string, boolean>,
    potions: PotionEffect[],
    stackSteps: StackStep[],
    textDisplays: TextDisplayEntry[],
): string {
    const i1 = '  ';
    const i2 = '    ';
    const i3 = '      ';
    const i4 = '        ';
    const lines: string[] = ['behaviours:', `${i1}${type}:`];

    if (type === 'bed') {
        const raw = (values['bed_slots'] ?? '').split('\n').filter(l => l.trim());
        const slotsToEmit = raw.length > 0 ? raw : ['0,0,0'];
        lines.push(`${i2}slots:`);
        slotsToEmit.forEach(s => lines.push(`${i3}- "${s.trim()}"`));
    }

    if (type === 'contact_damage') {
        const amount = (values['amount'] ?? '').trim();
        const interval = (values['interval'] ?? '').trim();
        const fireDur = (values['fire_duration'] ?? '').trim();
        const sneaking = (values['damage_when_sneaking'] ?? '').trim();

        if (amount) lines.push(`${i2}amount: ${amount}`);
        if (interval) lines.push(`${i2}interval: ${interval}`);
        if (fireDur) lines.push(`${i2}fire_duration: ${fireDur}`);
        if (sneaking) lines.push(`${i2}damage_when_sneaking: ${sneaking}`);

        if (extras['faces']) {
            const faces = ['top', 'north', 'south', 'west', 'east'];
            const anyFace = faces.some(k => (values[`face_${k}`] ?? '').trim());
            if (anyFace) {
                lines.push(`${i2}block_faces:`);
                faces.forEach(k => {
                    const v = (values[`face_${k}`] ?? '').trim();
                    if (v) lines.push(`${i3}${k}: ${v}`);
                });
            }
        }

        potions.forEach((pe, idx) => {
            if (!pe.type.trim()) return;
            const key = idx === 0 ? 'potion_effect' : `potion_effect_${idx}`;
            lines.push(`${i2}${key}:`);
            lines.push(`${i3}type: ${pe.type.trim()}`);
            if (pe.amplifier.trim()) lines.push(`${i3}amplifier: ${pe.amplifier.trim()}`);
            if (pe.duration.trim()) lines.push(`${i3}duration: ${pe.duration.trim()}`);
            if (pe.ambient.trim()) lines.push(`${i3}ambient: ${pe.ambient.trim()}`);
            if (pe.particles.trim()) lines.push(`${i3}particles: ${pe.particles.trim()}`);
            if (pe.icon.trim()) lines.push(`${i3}icon: ${pe.icon.trim()}`);
        });
    }

    if (type === 'storage') {
        const t = (values['type'] ?? 'STORAGE').trim();
        const rows = (values['rows'] ?? '').trim();
        const title = (values['title'] ?? '').trim();
        const openVariant = (values['open_variant'] ?? '').trim();

        lines.push(`${i2}type: ${t}`);
        if (rows) lines.push(`${i2}rows: ${rows}`);
        if (title) lines.push(`${i2}title: ${yamlQuote(title)}`);
        if (openVariant) lines.push(`${i2}open_variant: ${openVariant}`);

        if (extras['open_sound']) {
            soundYamlLines({
                sound_name: values['open_sound_name'] ?? '',
                sound_volume: values['open_sound_volume'] ?? '',
                sound_pitch: values['open_sound_pitch'] ?? '',
                sound_category: values['open_sound_category'] ?? ''
            }, 'open_sound', i2).forEach(l => lines.push(l));
        }
        if (extras['close_sound']) {
            soundYamlLines({
                sound_name: values['close_sound_name'] ?? '',
                sound_volume: values['close_sound_volume'] ?? '',
                sound_pitch: values['close_sound_pitch'] ?? '',
                sound_category: values['close_sound_category'] ?? ''
            }, 'close_sound', i2).forEach(l => lines.push(l));
        }
    }

    if (type === 'stackable') {
        const mode = values['stackable_mode'] ?? 'simple';
        if (mode === 'simple') {
            const blocks = (values['stack_blocks'] ?? '').split('\n').filter(l => l.trim());
            if (blocks.length) {
                blocks.forEach(b => lines.push(`${i2}- ${b.trim()}`));
            } else {
                lines.push(`${i2}# add block IDs here`);
            }
        } else if (mode === 'complex') {
            const blocks = (values['stack_blocks'] ?? '').split('\n').filter(l => l.trim());
            const items = (values['stack_items'] ?? '').split('\n').filter(l => l.trim());
            const decr = (values['decrement_amount'] ?? '').trim();
            if (blocks.length) {
                lines.push(`${i2}blocks:`);
                blocks.forEach(b => lines.push(`${i3}- ${b.trim()}`));
            }
            if (items.length) {
                lines.push(`${i2}items:`);
                items.forEach(i => lines.push(`${i3}- ${i.trim()}`));
            }
            if (extras['stack_sound']) {
                soundYamlLines({
                    sound_name: values['stack_sound_name'] ?? '',
                    sound_volume: values['stack_sound_volume'] ?? '',
                    sound_pitch: values['stack_sound_pitch'] ?? '',
                    sound_category: values['stack_sound_category'] ?? ''
                }, 'sound', i2).forEach(l => lines.push(l));
            }
            if (decr) lines.push(`${i2}decrement_amount: ${decr}`);
        } else {
            stackSteps.forEach((step, idx) => {
                if (!step.block.trim()) return;
                const stepKey = `step_${idx + 1}`;
                lines.push(`${i2}${stepKey}:`);
                lines.push(`${i3}block: ${step.block.trim()}`);
                const items = step.items.split('\n').filter(l => l.trim());
                if (items.length) {
                    lines.push(`${i3}items:`);
                    items.forEach(it => lines.push(`${i3}  - ${it.trim()}`));
                }
                soundYamlLines({
                    sound_name: step.sound_name,
                    sound_volume: step.sound_volume,
                    sound_pitch: step.sound_pitch,
                    sound_category: step.sound_category
                }, 'sound', i3).forEach(l => lines.push(l));
                if (step.decrement_amount.trim()) lines.push(`${i3}decrement_amount: ${step.decrement_amount.trim()}`);
            });
        }
    }

    if (type === 'connectable') {
        const t = (values['conn_type'] ?? 'stair').trim();
        lines.push(`${i2}type: ${t || 'stair'}`);
        const keys = t === 'table' ? TABLE_KEYS : STAIR_KEYS;
        keys.forEach(k => {
            const v = (values[`conn_${k}`] ?? '').trim();
            if (v) lines.push(`${i2}${k}: ${v}`);
        });
    }

    if (type === 'text_display') {
        const tdMode = values['td_mode'] ?? 'single';
        if (tdMode === 'single') {
            textDisplaySpecLines({
                text: values['td_text'] ?? '',
                offset: values['td_offset'] ?? '',
                billboard: values['td_billboard'] ?? '',
                alignment: values['td_alignment'] ?? '',
                shadow: values['td_shadow'] ?? '',
                see_through: values['td_see_through'] ?? '',
                line_width: values['td_line_width'] ?? '',
                background: values['td_background'] ?? '',
                opacity: values['td_opacity'] ?? '',
                scale: values['td_scale'] ?? '',
                view_range: values['td_view_range'] ?? '',
                refresh_interval: values['td_refresh_interval'] ?? '',
            }, i2).forEach(l => lines.push(l));
        } else {
            const validDisplays = textDisplays.filter(td => td.text.trim());
            if (validDisplays.length === 0) {
                lines.push(`${i2}displays:`);
                lines.push(`${i3}label:`);
                lines.push(`${i4}text: "<yellow>Hello!"`);
            } else {
                lines.push(`${i2}displays:`);
                validDisplays.forEach(td => {
                    const displayId = td.id.trim() || 'display';
                    lines.push(`${i3}${displayId}:`);
                    textDisplaySpecLines(td, i4).forEach(l => lines.push(l));
                });
            }
        }
    }

    return lines.join('\n');
}

function generateRecipe(
    type: string,
    values: Record<string, string>,
    craftingGrid: string[],
    craftingIngredients: Record<string, CraftingIngredient>,
    shapelessIngredients: ShapelessIngredient[],
): string {
    const i1 = '  ';
    const i2 = '    ';
    const i3 = '      ';
    const i4 = '        ';

    const namespace = (values['rcp_namespace'] ?? 'my_pack').trim() || 'my_pack';
    const recipeId = (values['rcp_id'] ?? 'my_recipe').trim() || 'my_recipe';

    const lines: string[] = [
        'info:',
        `${i1}namespace: ${namespace}`,
        '',
        'recipes:',
        `${i1}${type}:`,
        `${i2}${recipeId}:`,
    ];

    const enabled = (values['rcp_enabled'] ?? '').trim();
    if (enabled === 'false') lines.push(`${i3}enabled: false`);

    const permission = (values['rcp_permission'] ?? '').trim();
    if (permission) lines.push(`${i3}permission: "${permission}"`);

    if (type === 'campfire_cooking' || type === 'stonecutter') {
        const ingredient = (values['rcp_ingredient'] ?? '').trim();
        const result = (values['rcp_result'] ?? '').trim();
        const resultAmount = (values['rcp_result_amount'] ?? '').trim();

        lines.push(`${i3}ingredient:`);
        lines.push(`${i4}item: ${ingredient || 'BEEF'}`);
        lines.push(`${i3}result:`);
        lines.push(`${i4}item: ${result || 'COOKED_BEEF'}`);
        if (resultAmount && resultAmount !== '1') lines.push(`${i4}amount: ${resultAmount}`);

        if (type === 'campfire_cooking') {
            const cookTime = (values['rcp_cook_time'] ?? '').trim();
            const exp = (values['rcp_exp'] ?? '').trim();
            if (cookTime) lines.push(`${i3}cook_time: ${cookTime}`);
            if (exp) lines.push(`${i3}exp: ${exp}`);
        }
    }

    if (type === 'iaa_crafting_table') {
        const shapeless = values['rcp_shapeless'] === 'true';
        const result = (values['rcp_result'] ?? '').trim();
        const resultAmount = (values['rcp_result_amount'] ?? '').trim();

        if (shapeless) {
            lines.push(`${i3}shapeless: true`);
            const validIngredients = shapelessIngredients.filter(ing => ing.item.trim());
            if (validIngredients.length > 0) {
                lines.push(`${i3}ingredients:`);
                validIngredients.forEach(ing => {
                    const hasDetails = (ing.amount.trim() && ing.amount.trim() !== '1') || ing.replacement.trim() || (ing.ignore_durability.trim() && ing.ignore_durability !== '');
                    if (hasDetails) {
                        lines.push(`${i4}- item: ${ing.item.trim()}`);
                        if (ing.amount.trim() && ing.amount.trim() !== '1') lines.push(`${i4}  amount: ${ing.amount.trim()}`);
                        if (ing.replacement.trim()) lines.push(`${i4}  replacement: ${ing.replacement.trim()}`);
                        if (ing.ignore_durability.trim()) lines.push(`${i4}  ignore_durability: ${ing.ignore_durability.trim()}`);
                    } else {
                        lines.push(`${i4}- ${ing.item.trim()}`);
                    }
                });
            } else {
                lines.push(`${i3}ingredients:`);
                lines.push(`${i4}- STICK`);
            }
        } else {
            const row1 = `${craftingGrid[0] || 'X'}${craftingGrid[1] || 'X'}${craftingGrid[2] || 'X'}`;
            const row2 = `${craftingGrid[3] || 'X'}${craftingGrid[4] || 'X'}${craftingGrid[5] || 'X'}`;
            const row3 = `${craftingGrid[6] || 'X'}${craftingGrid[7] || 'X'}${craftingGrid[8] || 'X'}`;

            lines.push(`${i3}pattern:`);
            lines.push(`${i4}- "${row1}"`);
            lines.push(`${i4}- "${row2}"`);
            lines.push(`${i4}- "${row3}"`);

            const usedChars = [...new Set(craftingGrid.filter(c => c.trim()))].sort();
            if (usedChars.length > 0) {
                lines.push(`${i3}ingredients:`);
                usedChars.forEach(char => {
                    const ing = craftingIngredients[char];
                    if (!ing || !ing.item.trim()) {
                        lines.push(`${i4}${char}: # set item`);
                        return;
                    }
                    const hasDetails = (ing.amount.trim() && ing.amount.trim() !== '1') || ing.damage.trim() || ing.replacement.trim() || ing.ignore_durability.trim() || ing.potion_type.trim();
                    if (hasDetails) {
                        lines.push(`${i4}${char}:`);
                        lines.push(`${i4}  item: ${ing.item.trim()}`);
                        if (ing.amount.trim() && ing.amount.trim() !== '1') lines.push(`${i4}  amount: ${ing.amount.trim()}`);
                        if (ing.damage.trim()) lines.push(`${i4}  damage: ${ing.damage.trim()}`);
                        if (ing.replacement.trim()) lines.push(`${i4}  replacement: ${ing.replacement.trim()}`);
                        if (ing.ignore_durability.trim()) lines.push(`${i4}  ignore_durability: ${ing.ignore_durability.trim()}`);
                        if (ing.potion_type.trim()) lines.push(`${i4}  potion_type: ${ing.potion_type.trim()}`);
                    } else {
                        lines.push(`${i4}${char}: ${ing.item.trim()}`);
                    }
                });
            } else {
                lines.push(`${i3}ingredients:`);
                lines.push(`${i4}A: STICK`);
            }
        }

        lines.push(`${i3}result:`);
        lines.push(`${i4}item: ${result || 'DIAMOND'}`);
        if (resultAmount && resultAmount !== '1') lines.push(`${i4}amount: ${resultAmount}`);
    }

    return lines.join('\n');
}

function generateWorldgen(type: string, values: Record<string, string>): string {
    const i1 = '  ';
    const i2 = '    ';
    const i3 = '      ';

    const namespace = (values['wg_namespace'] ?? 'my_pack').trim() || 'my_pack';
    const id = (values['wg_id'] ?? 'my_entry').trim() || 'my_entry';

    const lines: string[] = [
        'info:',
        `${i1}namespace: ${namespace}`,
        '',
        `${type}:`,
        `${i1}${id}:`,
    ];

    const enabled = (values['wg_enabled'] ?? '').trim();
    if (enabled === 'false') lines.push(`${i2}enabled: false`);

    const furniture = (values['wg_furniture'] ?? '').trim();
    lines.push(`${i2}furniture: "${furniture || 'namespace:my_furniture'}"`);

    const worlds = (values['wg_worlds'] ?? '').split('\n').filter(l => l.trim());
    if (worlds.length > 0) {
        lines.push(`${i2}worlds:`);
        worlds.forEach(w => lines.push(`${i3}- ${w.trim()}`));
    }

    const biomes = (values['wg_biomes'] ?? '').split('\n').filter(l => l.trim());
    if (biomes.length > 0) {
        lines.push(`${i2}biomes:`);
        biomes.forEach(b => lines.push(`${i3}- ${b.trim()}`));
    }

    const surfaceBlocks = (values['wg_surface_blocks'] ?? '').split('\n').filter(l => l.trim());
    const surfaceKey = type === 'blocks_populators' ? 'replaceable_blocks' : 'bottom_blocks';
    if (surfaceBlocks.length > 0) {
        lines.push(`${i2}${surfaceKey}:`);
        surfaceBlocks.forEach(b => lines.push(`${i3}- ${b.trim()}`));
    }

    if (type === 'blocks_populators') {
        const veinBlocks = (values['wg_vein_blocks'] ?? '').trim();
        const chunkVeins = (values['wg_chunk_veins'] ?? '').trim();
        if (veinBlocks) lines.push(`${i2}vein_blocks: ${veinBlocks}`);
        if (chunkVeins) lines.push(`${i2}chunk_veins: ${chunkVeins}`);
    } else {
        const amount = (values['wg_amount'] ?? '').trim();
        if (amount) lines.push(`${i2}amount: ${amount}`);
    }

    const chunkChance = (values['wg_chunk_chance'] ?? '').trim();
    if (chunkChance) lines.push(`${i2}chunk_chance: ${chunkChance}`);

    const minHeight = (values['wg_min_height'] ?? '').trim();
    const maxHeight = (values['wg_max_height'] ?? '').trim();
    if (minHeight) lines.push(`${i2}min_height: ${minHeight}`);
    if (maxHeight) lines.push(`${i2}max_height: ${maxHeight}`);

    if (type === 'surface_decorators') {
        const allowLiquidSurface = (values['wg_allow_liquid_surface'] ?? '').trim();
        if (allowLiquidSurface === 'true') lines.push(`${i2}allow_liquid_surface: true`);
    }

    const allowLiquidPlacement = (values['wg_allow_liquid_placement'] ?? '').trim();
    if (allowLiquidPlacement === 'true') lines.push(`${i2}allow_liquid_placement: true`);

    return lines.join('\n');
}

function resetAll(
    setValues: (v: Record<string, string>) => void,
    setExtras: (v: Record<string, boolean>) => void,
    setPotions: (v: PotionEffect[]) => void,
    setStackSteps: (v: StackStep[]) => void,
    setTextDisplays: (v: TextDisplayEntry[]) => void,
    setCraftingGrid: (v: string[]) => void,
    setCraftingIngredients: (v: Record<string, CraftingIngredient>) => void,
    setShapelessIngredients: (v: ShapelessIngredient[]) => void,
) {
    setValues({});
    setExtras({});
    setPotions([emptyPotion()]);
    setStackSteps([emptyStep()]);
    setTextDisplays([emptyTextDisplay('label')]);
    setCraftingGrid(Array(9).fill(''));
    setCraftingIngredients({});
    setShapelessIngredients([emptyShapeless()]);
}

export function Builder() {
    const [mode, setMode] = useState<Mode>('action');
    const [actionType, setActionType] = useState('actionbar');
    const [behaviourType, setBehaviourType] = useState('contact_damage');
    const [recipeType, setRecipeType] = useState('campfire_cooking');
    const [worldgenType, setWorldgenType] = useState('blocks_populators');
    const [values, setValues] = useState<Record<string, string>>({});
    const [extras, setExtras] = useState<Record<string, boolean>>({});
    const [potions, setPotions] = useState<PotionEffect[]>([emptyPotion()]);
    const [stackSteps, setStackSteps] = useState<StackStep[]>([emptyStep()]);
    const [textDisplays, setTextDisplays] = useState<TextDisplayEntry[]>([emptyTextDisplay('label')]);
    const [craftingGrid, setCraftingGrid] = useState<string[]>(Array(9).fill(''));
    const [craftingIngredients, setCraftingIngredients] = useState<Record<string, CraftingIngredient>>({});
    const [shapelessIngredients, setShapelessIngredients] = useState<ShapelessIngredient[]>([emptyShapeless()]);
    const [copied, setCopied] = useState(false);

    const currentType = mode === 'action' ? actionType : mode === 'behaviour' ? behaviourType : mode === 'recipe' ? recipeType : worldgenType;

    const yaml = mode === 'action'
        ? generateAction(currentType, values)
        : mode === 'recipe'
            ? generateRecipe(recipeType, values, craftingGrid, craftingIngredients, shapelessIngredients)
            : mode === 'worldgen'
                ? generateWorldgen(worldgenType, values)
                : generateBehaviour(currentType, values, extras, potions, stackSteps, textDisplays);

    const setVal = useCallback((k: string, v: string) => setValues(prev => ({...prev, [k]: v})), []);
    const setExtra = useCallback((k: string, v: boolean) => setExtras(prev => ({...prev, [k]: v})), []);

    const switchMode = (m: Mode) => {
        setMode(m);
        resetAll(setValues, setExtras, setPotions, setStackSteps, setTextDisplays, setCraftingGrid, setCraftingIngredients, setShapelessIngredients);
    };

    const switchType = (t: string) => {
        if (mode === 'action') setActionType(t);
        else if (mode === 'behaviour') setBehaviourType(t);
        else if (mode === 'recipe') setRecipeType(t);
        else setWorldgenType(t);
        resetAll(setValues, setExtras, setPotions, setStackSteps, setTextDisplays, setCraftingGrid, setCraftingIngredients, setShapelessIngredients);
    };

    const updatePotion = (idx: number, k: keyof PotionEffect, v: string) =>
        setPotions(prev => prev.map((p, i) => (i === idx ? {...p, [k]: v} : p)));
    const removePotion = (idx: number) => setPotions(prev => prev.filter((_, i) => i !== idx));

    const updateStep = (idx: number, k: keyof StackStep, v: string) =>
        setStackSteps(prev => prev.map((s, i) => (i === idx ? {...s, [k]: v} : s)));
    const removeStep = (idx: number) => setStackSteps(prev => prev.filter((_, i) => i !== idx));

    const updateTextDisplay = (idx: number, k: keyof TextDisplayEntry, v: string) =>
        setTextDisplays(prev => prev.map((td, i) => (i === idx ? {...td, [k]: v} : td)));
    const removeTextDisplay = (idx: number) => setTextDisplays(prev => prev.filter((_, i) => i !== idx));

    const updateShapeless = (idx: number, k: keyof ShapelessIngredient, v: string) =>
        setShapelessIngredients(prev => prev.map((s, i) => (i === idx ? {...s, [k]: v} : s)));
    const removeShapeless = (idx: number) => setShapelessIngredients(prev => prev.filter((_, i) => i !== idx));

    const updateCraftingIngredient = useCallback((char: string, k: keyof CraftingIngredient, v: string) =>
        setCraftingIngredients(prev => ({
            ...prev,
            [char]: {...(prev[char] ?? emptyCraftingIngredient()), [k]: v}
        })), []);

    const updateGridCell = (idx: number, raw: string) => {
        const char = raw.toUpperCase().slice(-1);
        if (char === 'X') return;
        setCraftingGrid(prev => {
            const next = [...prev];
            next[idx] = char;
            return next;
        });
    };

    const copy = () =>
        navigator.clipboard.writeText(yaml).then(() => {
            setCopied(true);
            setTimeout(() => setCopied(false), 2000);
        });

    const connType = values['conn_type'] ?? 'stair';
    const connKeys = connType === 'table' ? TABLE_KEYS : STAIR_KEYS;
    const stackMode = values['stackable_mode'] ?? 'simple';
    const replaceBiomeRadiusMode = values['radius_mode'] ?? 'blocks_from_center';
    const tdMode = values['td_mode'] ?? 'single';
    const rcpShapeless = values['rcp_shapeless'] === 'true';
    const usedChars = [...new Set(craftingGrid.filter(c => c.trim()))].sort();

    const modeLabel: Record<Mode, string> = {
        action: 'Actions',
        behaviour: 'Behaviours',
        recipe: 'Recipes',
        worldgen: 'World Gen'
    };
    const behaviourTypes: Record<string, number> = {
        bed: 1,
        connectable: 1,
        contact_damage: 1,
        stackable: 1,
        storage: 1,
        text_display: 1
    };
    const recipeTypes: Record<string, number> = {campfire_cooking: 1, stonecutter: 1, iaa_crafting_table: 1};
    const worldgenTypes: Record<string, number> = {blocks_populators: 1, surface_decorators: 1};

    return (
        <div className="my-6 flex flex-col gap-4 not-prose">
            <div className="flex gap-1 rounded-lg border border-fd-border bg-fd-muted p-1 w-fit">
                {(['action', 'behaviour', 'recipe', 'worldgen'] as const).map(m => (
                    <button
                        key={m}
                        onClick={() => switchMode(m)}
                        className={`rounded-md px-4 py-1.5 text-sm transition-colors ${mode === m ? 'bg-fd-background text-fd-foreground font-medium shadow-sm' : 'text-fd-muted-foreground hover:text-fd-foreground'}`}
                    >
                        {modeLabel[m]}
                    </button>
                ))}
            </div>

            <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
                <div className="flex flex-col gap-4 rounded-xl border border-fd-border bg-fd-card p-4">
                    <div className="flex flex-col gap-1">
                        <label className="text-xs font-medium text-fd-muted-foreground">Type</label>
                        <select value={currentType} onChange={e => switchType(e.target.value)} className={inputCls}>
                            {Object.keys(mode === 'action' ? ACTIONS : mode === 'behaviour' ? behaviourTypes : mode === 'recipe' ? recipeTypes : worldgenTypes).map(k => (
                                <option key={k} value={k}>{k}</option>
                            ))}
                        </select>
                    </div>

                    <Divider/>

                    {/* ── Actions ─────────────────────────────────────────── */}
                    {mode === 'action' && (
                        <>
                            <SectionLabel>Parameters</SectionLabel>
                            <div className="flex flex-col gap-3">
                                {actionType === 'replace_biome' ? (
                                    <>
                                        <Field def={ACTIONS.replace_biome[0]} value={values['biome'] ?? ''}
                                               onChange={v => setVal('biome', v)}/>
                                        <Field def={ACTIONS.replace_biome[1]} value={values['shape'] ?? ''}
                                               onChange={v => setVal('shape', v)}/>
                                        <Field def={ACTIONS.replace_biome[2]}
                                               value={values['radius_mode'] ?? 'blocks_from_center'}
                                               onChange={v => setVal('radius_mode', v)}/>
                                        {replaceBiomeRadiusMode === 'blocks_from_center' ? (
                                            <Field def={ACTIONS.replace_biome[3]}
                                                   value={values['blocks_from_center'] ?? ''}
                                                   onChange={v => setVal('blocks_from_center', v)}/>
                                        ) : (
                                            <>
                                                <Field def={ACTIONS.replace_biome[4]} value={values['radius_x'] ?? ''}
                                                       onChange={v => setVal('radius_x', v)}/>
                                                <Field def={ACTIONS.replace_biome[5]} value={values['radius_y'] ?? ''}
                                                       onChange={v => setVal('radius_y', v)}/>
                                                <Field def={ACTIONS.replace_biome[6]} value={values['radius_z'] ?? ''}
                                                       onChange={v => setVal('radius_z', v)}/>
                                            </>
                                        )}
                                        <p className="text-[11px] text-fd-muted-foreground">Note: Minecraft biome
                                            editing is not perfectly exact at small scales. Small shapes may look a bit
                                            randomized or rough.</p>
                                    </>
                                ) : (
                                    ACTIONS[actionType]?.map(f => <Field key={f.k} def={f} value={values[f.k] ?? ''}
                                                                         onChange={v => setVal(f.k, v)}/>)
                                )}
                            </div>

                            <Divider/>
                            <SectionLabel>Universal parameters</SectionLabel>
                            <div className="flex flex-col gap-3">
                                <Field def={{k: 'permission', l: 'permission', t: 'text', r: false, p: 'myplugin.use'}}
                                       value={values['permission'] ?? ''} onChange={v => setVal('permission', v)}/>
                                <Field def={{
                                    k: 'delay',
                                    l: 'delay',
                                    t: 'number',
                                    r: false,
                                    p: '0',
                                    h: '20 ticks = 1 second'
                                }} value={values['delay'] ?? ''} onChange={v => setVal('delay', v)}/>
                                <Field def={{
                                    k: 'u_target',
                                    l: 'target',
                                    t: 'select',
                                    r: false,
                                    opts: ['', 'other', 'all', 'radius', 'in_sight'],
                                    h: 'default: self'
                                }} value={values['u_target'] ?? ''} onChange={v => setVal('u_target', v)}/>
                                {values['u_target'] === 'radius' && (
                                    <Field def={{
                                        k: 'target_radius',
                                        l: 'target_radius',
                                        t: 'number',
                                        r: false,
                                        p: '10',
                                        h: 'blocks'
                                    }} value={values['target_radius'] ?? ''}
                                           onChange={v => setVal('target_radius', v)}/>
                                )}
                                {values['u_target'] === 'in_sight' && (
                                    <Field def={{
                                        k: 'target_in_sight_distance',
                                        l: 'target_in_sight_distance',
                                        t: 'number',
                                        r: false,
                                        p: '10',
                                        h: 'blocks'
                                    }} value={values['target_in_sight_distance'] ?? ''}
                                           onChange={v => setVal('target_in_sight_distance', v)}/>
                                )}
                            </div>
                        </>
                    )}

                    {/* ── Behaviours ──────────────────────────────────────── */}
                    {mode === 'behaviour' && behaviourType === 'bed' && (
                        <>
                            <SectionLabel>Parameters</SectionLabel>
                            <Field def={{
                                k: 'bed_slots',
                                l: 'slots (one per line)',
                                t: 'textarea',
                                r: false,
                                p: '0,0,0\n0,0,1',
                                h: 'dx,dy,dz offsets in furniture-local space · defaults to "0,0,0"'
                            }} value={values['bed_slots'] ?? ''} onChange={v => setVal('bed_slots', v)}/>
                        </>
                    )}

                    {mode === 'behaviour' && behaviourType === 'contact_damage' && (
                        <>
                            <SectionLabel>Parameters</SectionLabel>
                            <div className="flex flex-col gap-3">
                                <Field def={{k: 'amount', l: 'amount', t: 'number', r: true, p: '1.0'}}
                                       value={values['amount'] ?? ''} onChange={v => setVal('amount', v)}/>
                                <Field def={{
                                    k: 'interval',
                                    l: 'interval',
                                    t: 'number',
                                    r: false,
                                    p: '20',
                                    h: 'ticks (default: 20)'
                                }} value={values['interval'] ?? ''} onChange={v => setVal('interval', v)}/>
                                <Field def={{
                                    k: 'fire_duration',
                                    l: 'fire_duration',
                                    t: 'number',
                                    r: false,
                                    p: '0',
                                    h: 'ticks (default: 0)'
                                }} value={values['fire_duration'] ?? ''} onChange={v => setVal('fire_duration', v)}/>
                                <Field def={{
                                    k: 'damage_when_sneaking',
                                    l: 'damage_when_sneaking',
                                    t: 'select',
                                    r: false,
                                    opts: ['', 'true', 'false'],
                                    h: 'default: true'
                                }} value={values['damage_when_sneaking'] ?? ''}
                                       onChange={v => setVal('damage_when_sneaking', v)}/>
                            </div>
                            <Divider/>
                            <CheckToggle label="Configure block_faces" checked={!!extras['faces']}
                                         onChange={v => setExtra('faces', v)}/>
                            {extras['faces'] && (
                                <SubSection>
                                    {['top', 'north', 'south', 'west', 'east'].map(face => (
                                        <Field key={face} def={{
                                            k: `face_${face}`,
                                            l: face,
                                            t: 'select',
                                            r: false,
                                            opts: ['', 'true', 'false'],
                                            h: 'default: true'
                                        }} value={values[`face_${face}`] ?? ''}
                                               onChange={v => setVal(`face_${face}`, v)}/>
                                    ))}
                                </SubSection>
                            )}
                            <Divider/>
                            <CheckToggle label="Configure potion effects" checked={!!extras['potions']} onChange={v => {
                                setExtra('potions', v);
                                if (v && potions.length === 0) setPotions([emptyPotion()]);
                            }}/>
                            {extras['potions'] && (
                                <div className="flex flex-col gap-3">
                                    {potions.map((pe, idx) => <PotionEffectFields key={idx} pe={pe} index={idx}
                                                                                  onChange={(k, v) => updatePotion(idx, k, v)}
                                                                                  onRemove={() => removePotion(idx)}/>)}
                                    <button onClick={() => setPotions(prev => [...prev, emptyPotion()])}
                                            className="self-start rounded-md border border-fd-border bg-fd-background px-3 py-1.5 text-xs text-fd-muted-foreground hover:text-fd-foreground transition-colors">+
                                        Add potion effect
                                    </button>
                                </div>
                            )}
                        </>
                    )}

                    {mode === 'behaviour' && behaviourType === 'storage' && (
                        <>
                            <SectionLabel>Parameters</SectionLabel>
                            <div className="flex flex-col gap-3">
                                <Field def={{
                                    k: 'type',
                                    l: 'type',
                                    t: 'select',
                                    r: true,
                                    opts: ['STORAGE', 'SHULKER', 'DISPOSAL']
                                }} value={values['type'] ?? 'STORAGE'} onChange={v => setVal('type', v)}/>
                                <Field
                                    def={{k: 'rows', l: 'rows', t: 'number', r: false, p: '3', h: '1-6 (default: 3)'}}
                                    value={values['rows'] ?? ''} onChange={v => setVal('rows', v)}/>
                                <Field def={{
                                    k: 'title',
                                    l: 'title',
                                    t: 'text',
                                    r: false,
                                    p: '<gold>My Storage',
                                    h: 'Font Images & MiniMessage & PlaceholderAPI supported'
                                }} value={values['title'] ?? ''} onChange={v => setVal('title', v)}/>
                                <Field def={{
                                    k: 'open_variant',
                                    l: 'open_variant',
                                    t: 'text',
                                    r: false,
                                    p: 'namespace:my_open_storage',
                                    h: 'Change to another block/furniture/complex furniture while opened'
                                }} value={values['open_variant'] ?? ''} onChange={v => setVal('open_variant', v)}/>
                            </div>
                            <Divider/>
                            <CheckToggle label="Add open_sound" checked={!!extras['open_sound']}
                                         onChange={v => setExtra('open_sound', v)}/>
                            {extras['open_sound'] && <SubSection><SoundFields sound={{
                                sound_name: values['open_sound_name'] ?? '',
                                sound_volume: values['open_sound_volume'] ?? '',
                                sound_pitch: values['open_sound_pitch'] ?? '',
                                sound_category: values['open_sound_category'] ?? ''
                            }} onChange={(k, v) => setVal(`open_sound_${k.replace('sound_', '')}`, v)}/></SubSection>}
                            <Divider/>
                            <CheckToggle label="Add close_sound" checked={!!extras['close_sound']}
                                         onChange={v => setExtra('close_sound', v)}/>
                            {extras['close_sound'] && <SubSection><SoundFields sound={{
                                sound_name: values['close_sound_name'] ?? '',
                                sound_volume: values['close_sound_volume'] ?? '',
                                sound_pitch: values['close_sound_pitch'] ?? '',
                                sound_category: values['close_sound_category'] ?? ''
                            }} onChange={(k, v) => setVal(`close_sound_${k.replace('sound_', '')}`, v)}/></SubSection>}
                        </>
                    )}

                    {mode === 'behaviour' && behaviourType === 'stackable' && (
                        <>
                            <SectionLabel>Format</SectionLabel>
                            <Field def={{
                                k: 'stackable_mode',
                                l: 'mode',
                                t: 'select',
                                r: true,
                                opts: ['simple', 'complex', 'full'],
                                h: 'simple = IDs only · complex = shared config · full = per-step config'
                            }} value={stackMode} onChange={v => setVal('stackable_mode', v)}/>
                            <Divider/>
                            {stackMode === 'simple' && (
                                <>
                                    <SectionLabel>Parameters</SectionLabel>
                                    <Field def={{
                                        k: 'stack_blocks',
                                        l: 'block IDs (one per line)',
                                        t: 'textarea',
                                        r: true,
                                        p: 'namespace:block_2\nnamespace:block_3',
                                        h: 'Must be custom block IDs'
                                    }} value={values['stack_blocks'] ?? ''} onChange={v => setVal('stack_blocks', v)}/>
                                </>
                            )}
                            {stackMode === 'complex' && (
                                <>
                                    <SectionLabel>Parameters</SectionLabel>
                                    <div className="flex flex-col gap-3">
                                        <Field def={{
                                            k: 'stack_blocks',
                                            l: 'block IDs (one per line)',
                                            t: 'textarea',
                                            r: true,
                                            p: 'namespace:block_2\nnamespace:block_3',
                                            h: 'Must be custom block IDs'
                                        }} value={values['stack_blocks'] ?? ''}
                                               onChange={v => setVal('stack_blocks', v)}/>
                                        <Field def={{
                                            k: 'stack_items',
                                            l: 'items (one per line)',
                                            t: 'textarea',
                                            r: false,
                                            p: 'namespace:my_item\nBONE_MEAL',
                                            h: 'defaults to the item this behaviour is on'
                                        }} value={values['stack_items'] ?? ''}
                                               onChange={v => setVal('stack_items', v)}/>
                                        <Field def={{
                                            k: 'decrement_amount',
                                            l: 'decrement_amount',
                                            t: 'number',
                                            r: false,
                                            p: '1',
                                            h: 'default: 1'
                                        }} value={values['decrement_amount'] ?? ''}
                                               onChange={v => setVal('decrement_amount', v)}/>
                                    </div>
                                    <Divider/>
                                    <CheckToggle label="Add sound" checked={!!extras['stack_sound']}
                                                 onChange={v => setExtra('stack_sound', v)}/>
                                    {extras['stack_sound'] && <SubSection><SoundFields sound={{
                                        sound_name: values['stack_sound_name'] ?? '',
                                        sound_volume: values['stack_sound_volume'] ?? '',
                                        sound_pitch: values['stack_sound_pitch'] ?? '',
                                        sound_category: values['stack_sound_category'] ?? ''
                                    }}
                                                                                       onChange={(k, v) => setVal(`stack_sound_${k.replace('sound_', '')}`, v)}/></SubSection>}
                                </>
                            )}
                            {stackMode === 'full' && (
                                <>
                                    <SectionLabel>Steps</SectionLabel>
                                    <div className="flex flex-col gap-3">
                                        {stackSteps.map((step, idx) => <StackStepFields key={idx} step={step}
                                                                                        index={idx}
                                                                                        onChange={(k, v) => updateStep(idx, k, v)}
                                                                                        onRemove={() => removeStep(idx)}/>)}
                                        <button onClick={() => setStackSteps(prev => [...prev, emptyStep()])}
                                                className="self-start rounded-md border border-fd-border bg-fd-background px-3 py-1.5 text-xs text-fd-muted-foreground hover:text-fd-foreground transition-colors">+
                                            Add step
                                        </button>
                                    </div>
                                </>
                            )}
                        </>
                    )}

                    {mode === 'behaviour' && behaviourType === 'connectable' && (
                        <>
                            <SectionLabel>Parameters</SectionLabel>
                            <div className="flex flex-col gap-3">
                                <Field def={{k: 'conn_type', l: 'type', t: 'select', r: true, opts: ['stair', 'table']}}
                                       value={connType} onChange={v => setVal('conn_type', v)}/>
                                <p className="text-[11px] text-fd-muted-foreground">
                                    All variant IDs are optional. If omitted:<br/>
                                    - <code>default</code> uses the furniture this behaviour is on<br/>
                                    - every other type uses <code>{'<furniture_name>_<type>'}</code>
                                </p>
                                {connKeys.map(k => (
                                    <Field key={k} def={{
                                        k: `conn_${k}`,
                                        l: k,
                                        t: 'text',
                                        r: false,
                                        p: k === 'default' ? 'namespace:base_furniture' : `namespace:furniture_${k}`,
                                        h: k === 'default' ? 'defaults to the current furniture ID' : `defaults to <furniture_name>_${k}`
                                    }} value={values[`conn_${k}`] ?? ''} onChange={v => setVal(`conn_${k}`, v)}/>
                                ))}
                            </div>
                        </>
                    )}

                    {mode === 'behaviour' && behaviourType === 'text_display' && (
                        <>
                            <SectionLabel>Mode</SectionLabel>
                            <Field def={{
                                k: 'td_mode',
                                l: 'mode',
                                t: 'select',
                                r: true,
                                opts: ['single', 'multiple'],
                                h: 'single = one display · multiple = several independently positioned displays'
                            }} value={tdMode} onChange={v => setVal('td_mode', v)}/>
                            <Divider/>

                            {tdMode === 'single' ? (
                                <>
                                    <SectionLabel>Display</SectionLabel>
                                    <div className="flex flex-col gap-3">
                                        <Field def={{
                                            k: 'td_text',
                                            l: 'text (one per line)',
                                            t: 'textarea',
                                            r: true,
                                            p: '<yellow>Hello!',
                                            h: 'MiniMessage · PlaceholderAPI · :icons:'
                                        }} value={values['td_text'] ?? ''} onChange={v => setVal('td_text', v)}/>
                                        <Field def={{
                                            k: 'td_offset',
                                            l: 'offset',
                                            t: 'text',
                                            r: false,
                                            p: '0, 0.25, 0',
                                            h: 'x, y, z offset from center'
                                        }} value={values['td_offset'] ?? ''} onChange={v => setVal('td_offset', v)}/>
                                        <Field def={{
                                            k: 'td_billboard',
                                            l: 'billboard',
                                            t: 'select',
                                            r: false,
                                            opts: ['', 'FIXED', 'VERTICAL', 'HORIZONTAL', 'CENTER'],
                                            h: 'default: VERTICAL'
                                        }} value={values['td_billboard'] ?? ''}
                                               onChange={v => setVal('td_billboard', v)}/>
                                        <Field def={{
                                            k: 'td_alignment',
                                            l: 'alignment',
                                            t: 'select',
                                            r: false,
                                            opts: ['', 'LEFT', 'CENTER', 'RIGHT'],
                                            h: 'default: CENTER'
                                        }} value={values['td_alignment'] ?? ''}
                                               onChange={v => setVal('td_alignment', v)}/>
                                        <Field def={{
                                            k: 'td_shadow',
                                            l: 'shadow',
                                            t: 'select',
                                            r: false,
                                            opts: ['', 'true', 'false'],
                                            h: 'default: true'
                                        }} value={values['td_shadow'] ?? ''} onChange={v => setVal('td_shadow', v)}/>
                                        <Field def={{
                                            k: 'td_see_through',
                                            l: 'see_through',
                                            t: 'select',
                                            r: false,
                                            opts: ['', 'true', 'false'],
                                            h: 'default: false'
                                        }} value={values['td_see_through'] ?? ''}
                                               onChange={v => setVal('td_see_through', v)}/>
                                        <Field def={{k: 'td_scale', l: 'scale', t: 'number', r: false, p: '1.0'}}
                                               value={values['td_scale'] ?? ''} onChange={v => setVal('td_scale', v)}/>
                                        <Field def={{
                                            k: 'td_view_range',
                                            l: 'view_range',
                                            t: 'number',
                                            r: false,
                                            p: '16.0',
                                            h: 'blocks'
                                        }} value={values['td_view_range'] ?? ''}
                                               onChange={v => setVal('td_view_range', v)}/>
                                        <Field def={{
                                            k: 'td_refresh_interval',
                                            l: 'refresh_interval',
                                            t: 'number',
                                            r: false,
                                            p: '0',
                                            h: 'ticks · 0 = disabled · use > 0 with PlaceholderAPI'
                                        }} value={values['td_refresh_interval'] ?? ''}
                                               onChange={v => setVal('td_refresh_interval', v)}/>
                                    </div>
                                    <Divider/>
                                    <CheckToggle label="Advanced options" checked={!!extras['td_advanced']}
                                                 onChange={v => setExtra('td_advanced', v)}/>
                                    {extras['td_advanced'] && (
                                        <SubSection>
                                            <Field def={{
                                                k: 'td_line_width',
                                                l: 'line_width',
                                                t: 'number',
                                                r: false,
                                                p: '200',
                                                h: 'pixels (default: 200)'
                                            }} value={values['td_line_width'] ?? ''}
                                                   onChange={v => setVal('td_line_width', v)}/>
                                            <Field def={{
                                                k: 'td_background',
                                                l: 'background',
                                                t: 'text',
                                                r: false,
                                                p: '#00000080',
                                                h: '#RRGGBB or #RRGGBBAA · null = vanilla default'
                                            }} value={values['td_background'] ?? ''}
                                                   onChange={v => setVal('td_background', v)}/>
                                            <Field def={{
                                                k: 'td_opacity',
                                                l: 'opacity',
                                                t: 'number',
                                                r: false,
                                                p: '-1',
                                                h: '-1..255 · -1 = fully opaque'
                                            }} value={values['td_opacity'] ?? ''}
                                                   onChange={v => setVal('td_opacity', v)}/>
                                        </SubSection>
                                    )}
                                </>
                            ) : (
                                <>
                                    <SectionLabel>Displays</SectionLabel>
                                    <div className="flex flex-col gap-3">
                                        {textDisplays.map((td, idx) => <TextDisplayEntryFields key={idx} entry={td}
                                                                                               index={idx}
                                                                                               onChange={(k, v) => updateTextDisplay(idx, k, v)}
                                                                                               onRemove={() => removeTextDisplay(idx)}/>)}
                                        <button
                                            onClick={() => setTextDisplays(prev => [...prev, emptyTextDisplay(`display_${prev.length + 1}`)])}
                                            className="self-start rounded-md border border-fd-border bg-fd-background px-3 py-1.5 text-xs text-fd-muted-foreground hover:text-fd-foreground transition-colors">+
                                            Add display
                                        </button>
                                    </div>
                                </>
                            )}
                        </>
                    )}

                    {/* ── Recipes ─────────────────────────────────────────── */}
                    {mode === 'recipe' && (
                        <>
                            <SectionLabel>Recipe info</SectionLabel>
                            <div className="flex flex-col gap-3">
                                <Field def={{
                                    k: 'rcp_namespace',
                                    l: 'namespace',
                                    t: 'text',
                                    r: true,
                                    p: 'my_pack',
                                    h: 'from info.namespace in your YML file'
                                }} value={values['rcp_namespace'] ?? ''} onChange={v => setVal('rcp_namespace', v)}/>
                                <Field def={{
                                    k: 'rcp_id',
                                    l: 'recipe id',
                                    t: 'text',
                                    r: true,
                                    p: 'my_recipe',
                                    h: 'unique identifier for this recipe'
                                }} value={values['rcp_id'] ?? ''} onChange={v => setVal('rcp_id', v)}/>
                                <Field def={{
                                    k: 'rcp_enabled',
                                    l: 'enabled',
                                    t: 'select',
                                    r: false,
                                    opts: ['', 'true', 'false'],
                                    h: 'default: true'
                                }} value={values['rcp_enabled'] ?? ''} onChange={v => setVal('rcp_enabled', v)}/>
                                <Field def={{
                                    k: 'rcp_permission',
                                    l: 'permission',
                                    t: 'text',
                                    r: false,
                                    p: 'myplugin.recipe.use'
                                }} value={values['rcp_permission'] ?? ''} onChange={v => setVal('rcp_permission', v)}/>
                            </div>
                            <Divider/>

                            {(recipeType === 'campfire_cooking' || recipeType === 'stonecutter') && (
                                <>
                                    <SectionLabel>Parameters</SectionLabel>
                                    <div className="flex flex-col gap-3">
                                        <Field def={{
                                            k: 'rcp_ingredient',
                                            l: 'ingredient item',
                                            t: 'text',
                                            r: true,
                                            p: 'BEEF or namespace:raw_item'
                                        }} value={values['rcp_ingredient'] ?? ''}
                                               onChange={v => setVal('rcp_ingredient', v)}/>
                                        <Field def={{
                                            k: 'rcp_result',
                                            l: 'result item',
                                            t: 'text',
                                            r: true,
                                            p: 'COOKED_BEEF or namespace:cooked_item'
                                        }} value={values['rcp_result'] ?? ''} onChange={v => setVal('rcp_result', v)}/>
                                        <Field def={{
                                            k: 'rcp_result_amount',
                                            l: 'result amount',
                                            t: 'number',
                                            r: false,
                                            p: '1',
                                            h: 'default: 1'
                                        }} value={values['rcp_result_amount'] ?? ''}
                                               onChange={v => setVal('rcp_result_amount', v)}/>
                                        {recipeType === 'campfire_cooking' && (
                                            <>
                                                <Field def={{
                                                    k: 'rcp_cook_time',
                                                    l: 'cook_time',
                                                    t: 'number',
                                                    r: false,
                                                    p: '600',
                                                    h: 'ticks (default: 600) · 20 = 1s'
                                                }} value={values['rcp_cook_time'] ?? ''}
                                                       onChange={v => setVal('rcp_cook_time', v)}/>
                                                <Field def={{
                                                    k: 'rcp_exp',
                                                    l: 'exp',
                                                    t: 'number',
                                                    r: false,
                                                    p: '0.0',
                                                    h: 'experience awarded (default: 0.0)'
                                                }} value={values['rcp_exp'] ?? ''}
                                                       onChange={v => setVal('rcp_exp', v)}/>
                                            </>
                                        )}
                                    </div>
                                </>
                            )}

                            {recipeType === 'iaa_crafting_table' && (
                                <>
                                    <SectionLabel>Recipe type</SectionLabel>
                                    <Field def={{
                                        k: 'rcp_shapeless',
                                        l: 'shapeless',
                                        t: 'select',
                                        r: false,
                                        opts: ['false', 'true'],
                                        h: 'shaped = grid pattern · shapeless = any arrangement'
                                    }} value={values['rcp_shapeless'] ?? 'false'}
                                           onChange={v => setVal('rcp_shapeless', v)}/>
                                    <Divider/>

                                    {!rcpShapeless ? (
                                        <>
                                            <SectionLabel>Crafting grid</SectionLabel>
                                            <p className="text-[11px] text-fd-muted-foreground">Type a letter in each
                                                cell. Cells with the same letter use the same ingredient. Empty cells
                                                become X in the pattern. X cannot be used as an ingredient key.</p>
                                            <div className="grid grid-cols-3 gap-1.5" style={{maxWidth: '180px'}}>
                                                {craftingGrid.map((cell, idx) => (
                                                    <input
                                                        key={idx}
                                                        type="text"
                                                        maxLength={2}
                                                        value={cell}
                                                        onChange={e => updateGridCell(idx, e.target.value)}
                                                        className="rounded-md border border-fd-border bg-fd-background px-0 py-1.5 text-center font-mono text-sm uppercase text-fd-foreground focus:outline-none focus:ring-1 focus:ring-fd-ring"
                                                        placeholder="·"
                                                    />
                                                ))}
                                            </div>

                                            {usedChars.length > 0 && (
                                                <>
                                                    <SectionLabel>Ingredients</SectionLabel>
                                                    <p className="text-[11px] text-fd-muted-foreground">Characters
                                                        without an item defined are treated as empty slots.</p>
                                                    <div className="flex flex-col gap-3">
                                                        {usedChars.map(char => (
                                                            <CraftingIngredientRow
                                                                key={char}
                                                                char={char}
                                                                ing={craftingIngredients[char] ?? emptyCraftingIngredient()}
                                                                onChange={(k, v) => updateCraftingIngredient(char, k, v)}
                                                            />
                                                        ))}
                                                    </div>
                                                </>
                                            )}
                                        </>
                                    ) : (
                                        <>
                                            <SectionLabel>Ingredients</SectionLabel>
                                            <div className="flex flex-col gap-3">
                                                {shapelessIngredients.map((ing, idx) => <ShapelessIngredientFields
                                                    key={idx} ing={ing} index={idx}
                                                    onChange={(k, v) => updateShapeless(idx, k, v)}
                                                    onRemove={() => removeShapeless(idx)}/>)}
                                                <button
                                                    onClick={() => setShapelessIngredients(prev => [...prev, emptyShapeless()])}
                                                    className="self-start rounded-md border border-fd-border bg-fd-background px-3 py-1.5 text-xs text-fd-muted-foreground hover:text-fd-foreground transition-colors">+
                                                    Add ingredient
                                                </button>
                                            </div>
                                        </>
                                    )}

                                    <Divider/>
                                    <SectionLabel>Result</SectionLabel>
                                    <div className="flex flex-col gap-3">
                                        <Field def={{
                                            k: 'rcp_result',
                                            l: 'result item',
                                            t: 'text',
                                            r: true,
                                            p: 'DIAMOND or namespace:item'
                                        }} value={values['rcp_result'] ?? ''} onChange={v => setVal('rcp_result', v)}/>
                                        <Field def={{
                                            k: 'rcp_result_amount',
                                            l: 'result amount',
                                            t: 'number',
                                            r: false,
                                            p: '1',
                                            h: 'default: 1'
                                        }} value={values['rcp_result_amount'] ?? ''}
                                               onChange={v => setVal('rcp_result_amount', v)}/>
                                    </div>
                                </>
                            )}
                        </>
                    )}

                    {/* ── World Gen ────────────────────────────────────────── */}
                    {mode === 'worldgen' && (
                        <>
                            <SectionLabel>Entry info</SectionLabel>
                            <div className="flex flex-col gap-3">
                                <Field def={{
                                    k: 'wg_namespace',
                                    l: 'namespace',
                                    t: 'text',
                                    r: true,
                                    p: 'my_pack',
                                    h: 'from info.namespace in your YML file'
                                }} value={values['wg_namespace'] ?? ''} onChange={v => setVal('wg_namespace', v)}/>
                                <Field def={{
                                    k: 'wg_id',
                                    l: 'entry id',
                                    t: 'text',
                                    r: true,
                                    p: 'my_entry',
                                    h: 'unique identifier for this entry'
                                }} value={values['wg_id'] ?? ''} onChange={v => setVal('wg_id', v)}/>
                                <Field def={{
                                    k: 'wg_enabled',
                                    l: 'enabled',
                                    t: 'select',
                                    r: false,
                                    opts: ['', 'true', 'false'],
                                    h: 'default: true'
                                }} value={values['wg_enabled'] ?? ''} onChange={v => setVal('wg_enabled', v)}/>
                                <Field def={{
                                    k: 'wg_furniture',
                                    l: 'furniture',
                                    t: 'text',
                                    r: true,
                                    p: 'namespace:my_furniture'
                                }} value={values['wg_furniture'] ?? ''} onChange={v => setVal('wg_furniture', v)}/>
                            </div>
                            <Divider/>

                            <SectionLabel>Filters</SectionLabel>
                            <div className="flex flex-col gap-3">
                                <Field def={{
                                    k: 'wg_worlds',
                                    l: 'worlds (one per line)',
                                    t: 'textarea',
                                    r: false,
                                    p: 'world\nworld_nether',
                                    h: 'empty = all worlds · supports * wildcard'
                                }} value={values['wg_worlds'] ?? ''} onChange={v => setVal('wg_worlds', v)}/>
                                <Field def={{
                                    k: 'wg_biomes',
                                    l: 'biomes (one per line)',
                                    t: 'textarea',
                                    r: false,
                                    p: 'minecraft:forest\nminecraft:plains',
                                    h: 'empty = all biomes · all entries must be valid'
                                }} value={values['wg_biomes'] ?? ''} onChange={v => setVal('wg_biomes', v)}/>
                                <Field
                                    def={{
                                        k: 'wg_surface_blocks',
                                        l: `${worldgenType === 'blocks_populators' ? 'replaceable_blocks' : 'bottom_blocks'} (one per line)`,
                                        t: 'textarea',
                                        r: false,
                                        p: 'GRASS_BLOCK\nDIRT',
                                        h: 'empty = any block below spawn point'
                                    }}
                                    value={values['wg_surface_blocks'] ?? ''}
                                    onChange={v => setVal('wg_surface_blocks', v)}
                                />
                            </div>
                            <Divider/>

                            <SectionLabel>Placement</SectionLabel>
                            <div className="flex flex-col gap-3">
                                {worldgenType === 'blocks_populators' ? (
                                    <>
                                        <Field def={{
                                            k: 'wg_vein_blocks',
                                            l: 'vein_blocks',
                                            t: 'number',
                                            r: false,
                                            p: '1',
                                            h: 'furniture pieces per vein attempt (default: 1)'
                                        }} value={values['wg_vein_blocks'] ?? ''}
                                               onChange={v => setVal('wg_vein_blocks', v)}/>
                                        <Field def={{
                                            k: 'wg_chunk_veins',
                                            l: 'chunk_veins',
                                            t: 'number',
                                            r: false,
                                            p: '4',
                                            h: 'vein attempts per chunk (default: 4)'
                                        }} value={values['wg_chunk_veins'] ?? ''}
                                               onChange={v => setVal('wg_chunk_veins', v)}/>
                                    </>
                                ) : (
                                    <Field def={{
                                        k: 'wg_amount',
                                        l: 'amount',
                                        t: 'number',
                                        r: false,
                                        p: '4',
                                        h: 'placement attempts per chunk (default: 4)'
                                    }} value={values['wg_amount'] ?? ''} onChange={v => setVal('wg_amount', v)}/>
                                )}
                                <Field def={{
                                    k: 'wg_chunk_chance',
                                    l: 'chunk_chance',
                                    t: 'number',
                                    r: false,
                                    p: '-1',
                                    h: '0-100 per chunk · -1 = always (default: -1)'
                                }} value={values['wg_chunk_chance'] ?? ''}
                                       onChange={v => setVal('wg_chunk_chance', v)}/>
                                <Field def={{k: 'wg_min_height', l: 'min_height', t: 'number', r: false, p: '60'}}
                                       value={values['wg_min_height'] ?? ''}
                                       onChange={v => setVal('wg_min_height', v)}/>
                                <Field def={{k: 'wg_max_height', l: 'max_height', t: 'number', r: false, p: '80'}}
                                       value={values['wg_max_height'] ?? ''}
                                       onChange={v => setVal('wg_max_height', v)}/>
                                {worldgenType === 'surface_decorators' && (
                                    <Field def={{
                                        k: 'wg_allow_liquid_surface',
                                        l: 'allow_liquid_surface',
                                        t: 'select',
                                        r: false,
                                        opts: ['', 'true', 'false'],
                                        h: 'allow liquid below spawn point as valid surface (default: false)'
                                    }} value={values['wg_allow_liquid_surface'] ?? ''}
                                           onChange={v => setVal('wg_allow_liquid_surface', v)}/>
                                )}
                                <Field def={{
                                    k: 'wg_allow_liquid_placement',
                                    l: 'allow_liquid_placement',
                                    t: 'select',
                                    r: false,
                                    opts: ['', 'true', 'false'],
                                    h: 'allow spawning inside water or lava (default: false)'
                                }} value={values['wg_allow_liquid_placement'] ?? ''}
                                       onChange={v => setVal('wg_allow_liquid_placement', v)}/>
                            </div>
                        </>
                    )}
                </div>

                <div className="flex flex-col gap-3 rounded-xl border border-fd-border bg-fd-card p-4">
                    <SectionLabel>Generated YAML</SectionLabel>
                    <pre
                        className="flex-1 overflow-x-auto rounded-lg bg-fd-muted p-3 font-mono text-xs leading-relaxed text-fd-foreground whitespace-pre">
                        {yaml}
                    </pre>
                    <button onClick={copy}
                            className="self-start rounded-md border border-fd-border bg-fd-background px-3 py-1.5 text-sm text-fd-foreground transition-colors hover:bg-fd-muted">
                        {copied ? 'Copied!' : 'Copy'}
                    </button>
                </div>
            </div>
        </div>
    );
}
