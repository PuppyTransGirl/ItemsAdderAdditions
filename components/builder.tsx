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

function emptyPotion(): PotionEffect {
    return { type: '', amplifier: '', duration: '', ambient: '', particles: '', icon: '' };
}

function emptyStep(): StackStep {
    return { block: '', items: '', decrement_amount: '', sound_name: '', sound_volume: '', sound_pitch: '', sound_category: '' };
}

function yamlQuote(val: string): string {
    if (/[:#\[\]{}&*!|>'"%@`]/.test(val) || val.startsWith(' ')) return `"${val}"`;
    return val;
}

const ACTIONS: Record<string, FieldDef[]> = {
    message: [{ k: 'text', l: 'text', t: 'text', r: true, p: '<rainbow>Hello %player_name%', h: 'MiniMessage & PlaceholderAPI' }],
    actionbar: [{ k: 'text', l: 'text', t: 'text', r: true, p: '<red>Action bar text', h: 'MiniMessage & PlaceholderAPI' }],
    title: [
        { k: 'title', l: 'title', t: 'text', r: true, p: '<bold><gold>Welcome!' },
        { k: 'subtitle', l: 'subtitle', t: 'text', r: false, p: '<gray>Enjoy your stay' },
        { k: 'fade_in', l: 'fade_in', t: 'number', r: false, p: '10', h: 'ticks (default: 10)' },
        { k: 'stay', l: 'stay', t: 'number', r: false, p: '70', h: 'ticks (default: 70)' },
        { k: 'fade_out', l: 'fade_out', t: 'number', r: false, p: '20', h: 'ticks (default: 20)' },
    ],
    toast: [
        { k: 'icon', l: 'icon', t: 'text', r: true, p: 'minecraft:diamond or namespace:item' },
        { k: 'text', l: 'text (one per line)', t: 'textarea', r: true, p: '<white>Line one\n<bold>Line two' },
        { k: 'frame', l: 'frame', t: 'select', r: false, opts: ['', 'task', 'goal', 'challenge'], h: 'default: goal' },
    ],
    ignite: [{ k: 'duration', l: 'duration', t: 'number', r: true, p: '200', h: 'ticks — 200 = 10 seconds' }],
    clear_item: [
        { k: 'item', l: 'item', t: 'text', r: true, p: 'namespace:item' },
        { k: 'amount', l: 'amount', t: 'number', r: false, p: '1', h: 'default: 1' },
    ],
    shoot_fireball: [
        { k: 'power', l: 'power', t: 'number', r: true, p: '1' },
        { k: 'speed', l: 'speed', t: 'number', r: false, p: '1.0', h: 'default: 1.0' },
        { k: 'fire', l: 'fire', t: 'select', r: false, opts: ['', 'true', 'false'], h: 'default: true' },
    ],
    teleport: [
        { k: 'x', l: 'x', t: 'number', r: true, p: '100.5' },
        { k: 'y', l: 'y', t: 'number', r: true, p: '64.0' },
        { k: 'z', l: 'z', t: 'number', r: true, p: '-50.5' },
        { k: 'yaw', l: 'yaw', t: 'number', r: false, p: "player's yaw" },
        { k: 'pitch', l: 'pitch', t: 'number', r: false, p: "player's pitch" },
        { k: 'world', l: 'world', t: 'text', r: false, p: "player's world" },
    ],
    open_inventory: [
        { k: 'type', l: 'type', t: 'select', r: true, opts: ['anvil', 'cartography_table', 'crafting_table', 'enchanting_table', 'ender_chest', 'grindstone', 'loom', 'smithing_table', 'stonecutter'] },
        { k: 'title', l: 'title', t: 'text', r: false, p: '<blue>Custom title', h: 'Requires Paper 1.21.4+' },
    ],
    play_emote: [{ k: 'name', l: 'emote name', t: 'text', r: true, p: 'wave' }],
    play_animation: [{ k: 'name', l: 'animation name', t: 'text', r: true, p: 'spin' }],
    swing_hand: [{ k: 'hand', l: 'hand', t: 'select', r: false, opts: ['hand', 'off_hand'] }],
    mythic_mobs_skill: [
        { k: 'skill', l: 'skill name', t: 'text', r: true, p: 'mega_attack' },
        { k: 'power', l: 'power', t: 'number', r: false, p: '1.0', h: 'default: 1.0' },
    ],
    veinminer: [{ k: 'max_blocks', l: 'max_blocks', t: 'number', r: true, p: '16' }],
};

const STAIR_KEYS = ['default', 'straight', 'left', 'right', 'outer', 'inner'];
const TABLE_KEYS = ['default', 'straight', 'middle', 'border', 'corner', 'end'];
const SOUND_CATEGORIES = ['MASTER', 'MUSIC', 'RECORD', 'WEATHER', 'BLOCK', 'HOSTILE', 'NEUTRAL', 'PLAYER', 'AMBIENT', 'VOICE'];

// ─── Shared UI ────────────────────────────────────────────────────────────────

function SectionLabel({ children }: { children: React.ReactNode }) {
    return <p className="text-xs font-medium uppercase tracking-wide text-fd-muted-foreground">{children}</p>;
}

function Divider() {
    return <div className="h-px bg-fd-border" />;
}

function SubSection({ children }: { children: React.ReactNode }) {
    return <div className="flex flex-col gap-3 border-l-2 border-fd-border pl-3">{children}</div>;
}

function CheckToggle({ label, checked, onChange }: { label: string; checked: boolean; onChange: (v: boolean) => void }) {
    return (
        <label className="flex cursor-pointer items-center gap-2 text-sm text-fd-foreground">
            <input type="checkbox" checked={checked} onChange={e => onChange(e.target.checked)} className="rounded" />
            {label}
        </label>
    );
}

const inputCls = "w-full rounded-md border border-fd-border bg-fd-background px-2 py-1.5 text-sm text-fd-foreground focus:outline-none focus:ring-1 focus:ring-fd-ring";

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
                <select value={value} onChange={e => onChange(e.target.value)} className={inputCls}>
                    {(def.opts ?? []).map(o => <option key={o} value={o}>{o || '(default)'}</option>)}
                </select>
            ) : def.t === 'textarea' ? (
                <textarea value={value} onChange={e => onChange(e.target.value)} placeholder={def.p} rows={3}
                          className="w-full rounded-md border border-fd-border bg-fd-background px-2 py-1.5 font-mono text-xs text-fd-foreground focus:outline-none focus:ring-1 focus:ring-fd-ring resize-y" />
            ) : (
                <input type={def.t === 'number' ? 'number' : 'text'} value={value}
                       onChange={e => onChange(e.target.value)} placeholder={def.p} className={inputCls} />
            )}
            {def.h && <p className="text-[11px] text-fd-muted-foreground">{def.h}</p>}
        </div>
    );
}

function SoundFields({ sound: s, onChange }: {
    sound: { sound_name: string; sound_volume: string; sound_pitch: string; sound_category: string };
    onChange: (k: string, v: string) => void;
}) {
    return (
        <div className="flex flex-col gap-3">
            <Field def={{ k: 'sound_name', l: 'name', t: 'text', r: false, p: 'entity.villager.ambient', h: 'vanilla sound or namespace:sound' }} value={s.sound_name} onChange={v => onChange('sound_name', v)} />
            <Field def={{ k: 'sound_volume', l: 'volume', t: 'number', r: false, p: '1.0', h: 'default: 1.0' }} value={s.sound_volume} onChange={v => onChange('sound_volume', v)} />
            <Field def={{ k: 'sound_pitch', l: 'pitch', t: 'number', r: false, p: '1.0', h: 'default: 1.0' }} value={s.sound_pitch} onChange={v => onChange('sound_pitch', v)} />
            <Field def={{ k: 'sound_category', l: 'category', t: 'select', r: false, opts: ['', ...SOUND_CATEGORIES], h: 'default: MASTER' }} value={s.sound_category} onChange={v => onChange('sound_category', v)} />
        </div>
    );
}

function PotionEffectFields({ pe, index, onChange, onRemove }: {
    pe: PotionEffect;
    index: number;
    onChange: (k: keyof PotionEffect, v: string) => void;
    onRemove: () => void;
}) {
    return (
        <div className="flex flex-col gap-3 rounded-lg border border-fd-border p-3">
            <div className="flex items-center justify-between">
                <span className="text-xs font-medium text-fd-muted-foreground">
                    {index === 0 ? 'potion_effect' : `potion_effect_${index}`}
                </span>
                {index > 0 && (
                    <button onClick={onRemove} className="text-xs text-fd-muted-foreground hover:text-red-500 transition-colors">Remove</button>
                )}
            </div>
            <Field def={{ k: 'type', l: 'type', t: 'text', r: true, p: 'POISON', h: 'e.g. POISON, WITHER, NAUSEA' }} value={pe.type} onChange={v => onChange('type', v)} />
            <Field def={{ k: 'amplifier', l: 'amplifier', t: 'number', r: false, p: '0', h: '0 = level 1 (default: 0)' }} value={pe.amplifier} onChange={v => onChange('amplifier', v)} />
            <Field def={{ k: 'duration', l: 'duration', t: 'number', r: false, p: '40', h: 'ticks (default: 40)' }} value={pe.duration} onChange={v => onChange('duration', v)} />
            <Field def={{ k: 'ambient', l: 'ambient', t: 'select', r: false, opts: ['', 'true', 'false'], h: 'default: false — more translucent particles' }} value={pe.ambient} onChange={v => onChange('ambient', v)} />
            <Field def={{ k: 'particles', l: 'particles', t: 'select', r: false, opts: ['', 'true', 'false'], h: 'default: true' }} value={pe.particles} onChange={v => onChange('particles', v)} />
            <Field def={{ k: 'icon', l: 'icon', t: 'select', r: false, opts: ['', 'true', 'false'], h: 'default: true — show HUD icon' }} value={pe.icon} onChange={v => onChange('icon', v)} />
        </div>
    );
}

function StackStepFields({ step, index, onChange, onRemove }: {
    step: StackStep;
    index: number;
    onChange: (k: keyof StackStep, v: string) => void;
    onRemove: () => void;
}) {
    const [showSound, setShowSound] = useState(false);
    return (
        <div className="flex flex-col gap-3 rounded-lg border border-fd-border p-3">
            <div className="flex items-center justify-between">
                <span className="text-xs font-medium text-fd-muted-foreground">step_{index + 1}</span>
                <button onClick={onRemove} className="text-xs text-fd-muted-foreground hover:text-red-500 transition-colors">Remove</button>
            </div>
            <Field def={{ k: 'block', l: 'block', t: 'text', r: true, p: 'namespace:block_next' }} value={step.block} onChange={v => onChange('block', v)} />
            <Field def={{ k: 'items', l: 'items (one per line)', t: 'textarea', r: false, p: 'namespace:my_item\nBONE_MEAL', h: 'defaults to the item this behaviour is on' }} value={step.items} onChange={v => onChange('items', v)} />
            <Field def={{ k: 'decrement_amount', l: 'decrement_amount', t: 'number', r: false, p: '1', h: 'default: 1' }} value={step.decrement_amount} onChange={v => onChange('decrement_amount', v)} />
            <CheckToggle label="Add sound" checked={showSound} onChange={setShowSound} />
            {showSound && (
                <SubSection>
                    <SoundFields
                        sound={{ sound_name: step.sound_name, sound_volume: step.sound_volume, sound_pitch: step.sound_pitch, sound_category: step.sound_category }}
                        onChange={(k, v) => onChange(k as keyof StackStep, v)}
                    />
                </SubSection>
            )}
        </div>
    );
}

// ─── YAML generators ──────────────────────────────────────────────────────────

function soundYamlLines(s: { sound_name: string; sound_volume: string; sound_pitch: string; sound_category: string }, yamlKey: string, ind: string): string[] {
    if (!s.sound_name.trim()) return [];
    const lines = [`${ind}${yamlKey}:`];
    lines.push(`${ind}  name: ${s.sound_name.trim()}`);
    if (s.sound_volume.trim()) lines.push(`${ind}  volume: ${s.sound_volume.trim()}`);
    if (s.sound_pitch.trim()) lines.push(`${ind}  pitch: ${s.sound_pitch.trim()}`);
    if (s.sound_category.trim()) lines.push(`${ind}  category: ${s.sound_category.trim()}`);
    return lines;
}

function generateAction(type: string, values: Record<string, string>): string {
    const ind = '  ';
    const lines: string[] = [`${type}:`];

    (ACTIONS[type] ?? []).forEach(f => {
        const val = (values[f.k] ?? '').trim();
        if (!val) return;
        if (f.t === 'textarea') {
            const rows = val.split('\n').filter(l => l.trim());
            if (rows.length === 1) lines.push(`${ind}${f.k}: ${yamlQuote(rows[0])}`);
            else { lines.push(`${ind}${f.k}:`); rows.forEach(r => lines.push(`${ind}  - ${yamlQuote(r)}`)); }
        } else if (f.t === 'number') {
            lines.push(`${ind}${f.k}: ${val}`);
        } else if (f.t === 'select' && !val) {
            return;
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
    return lines.join('\n');
}

function generateBehaviour(
    type: string,
    values: Record<string, string>,
    extras: Record<string, boolean>,
    potions: PotionEffect[],
    stackSteps: StackStep[],
): string {
    const i1 = '  ';
    const i2 = '    ';
    const i3 = '      ';
    const lines: string[] = ['behaviours:', `${i1}${type}:`];

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
                faces.forEach(k => { const v = (values[`face_${k}`] ?? '').trim(); if (v) lines.push(`${i3}${k}: ${v}`); });
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
        lines.push(`${i2}type: ${t}`);
        if (rows) lines.push(`${i2}rows: ${rows}`);
        if (title) lines.push(`${i2}title: ${yamlQuote(title)}`);
        if (extras['open_sound']) {
            soundYamlLines({ sound_name: values['open_sound_name'] ?? '', sound_volume: values['open_sound_volume'] ?? '', sound_pitch: values['open_sound_pitch'] ?? '', sound_category: values['open_sound_category'] ?? '' }, 'open_sound', i2).forEach(l => lines.push(l));
        }
        if (extras['close_sound']) {
            soundYamlLines({ sound_name: values['close_sound_name'] ?? '', sound_volume: values['close_sound_volume'] ?? '', sound_pitch: values['close_sound_pitch'] ?? '', sound_category: values['close_sound_category'] ?? '' }, 'close_sound', i2).forEach(l => lines.push(l));
        }
    }

    if (type === 'stackable') {
        const mode = values['stackable_mode'] ?? 'simple';

        if (mode === 'simple') {
            const blocks = (values['stack_blocks'] ?? '').split('\n').filter(l => l.trim());
            if (blocks.length) blocks.forEach(b => lines.push(`${i2}- ${b.trim()}`));
            else lines.push(`${i2}# add block IDs here`);

        } else if (mode === 'complex') {
            const blocks = (values['stack_blocks'] ?? '').split('\n').filter(l => l.trim());
            const items = (values['stack_items'] ?? '').split('\n').filter(l => l.trim());
            const decr = (values['decrement_amount'] ?? '').trim();
            if (blocks.length) { lines.push(`${i2}blocks:`); blocks.forEach(b => lines.push(`${i3}- ${b.trim()}`)); }
            if (items.length) { lines.push(`${i2}items:`); items.forEach(i => lines.push(`${i3}- ${i.trim()}`)); }
            if (extras['stack_sound']) {
                soundYamlLines({ sound_name: values['stack_sound_name'] ?? '', sound_volume: values['stack_sound_volume'] ?? '', sound_pitch: values['stack_sound_pitch'] ?? '', sound_category: values['stack_sound_category'] ?? '' }, 'sound', i2).forEach(l => lines.push(l));
            }
            if (decr) lines.push(`${i2}decrement_amount: ${decr}`);

        } else {
            // full mode — named steps
            stackSteps.forEach((step, idx) => {
                if (!step.block.trim()) return;
                const stepKey = `step_${idx + 1}`;
                lines.push(`${i2}${stepKey}:`);
                lines.push(`${i3}block: ${step.block.trim()}`);
                const items = step.items.split('\n').filter(l => l.trim());
                if (items.length) { lines.push(`${i3}items:`); items.forEach(it => lines.push(`${i3}  - ${it.trim()}`)); }
                soundYamlLines({ sound_name: step.sound_name, sound_volume: step.sound_volume, sound_pitch: step.sound_pitch, sound_category: step.sound_category }, 'sound', i3).forEach(l => lines.push(l));
                if (step.decrement_amount.trim()) lines.push(`${i3}decrement_amount: ${step.decrement_amount.trim()}`);
            });
        }
    }

    if (type === 'connectable') {
        const t = (values['conn_type'] ?? 'stair').trim();
        if (t) lines.push(`${i2}type: ${t}`);
        const keys = t === 'table' ? TABLE_KEYS : STAIR_KEYS;
        keys.forEach(k => { const v = (values[`conn_${k}`] ?? '').trim(); if (v) lines.push(`${i2}${k}: ${v}`); });
    }

    return lines.join('\n');
}

// ─── Main component ───────────────────────────────────────────────────────────

export function Builder() {
    const [mode, setMode] = useState<'action' | 'behaviour'>('action');
    const [actionType, setActionType] = useState('message');
    const [behaviourType, setBehaviourType] = useState('contact_damage');
    const [values, setValues] = useState<Record<string, string>>({});
    const [extras, setExtras] = useState<Record<string, boolean>>({});
    const [potions, setPotions] = useState<PotionEffect[]>([emptyPotion()]);
    const [stackSteps, setStackSteps] = useState<StackStep[]>([emptyStep()]);
    const [copied, setCopied] = useState(false);

    const type = mode === 'action' ? actionType : behaviourType;
    const yaml = mode === 'action'
        ? generateAction(type, values)
        : generateBehaviour(type, values, extras, potions, stackSteps);

    const setVal = useCallback((k: string, v: string) => setValues(prev => ({ ...prev, [k]: v })), []);
    const setExtra = useCallback((k: string, v: boolean) => setExtras(prev => ({ ...prev, [k]: v })), []);

    const switchMode = (m: typeof mode) => { setMode(m); setValues({}); setExtras({}); setPotions([emptyPotion()]); setStackSteps([emptyStep()]); };
    const switchType = (t: string) => {
        if (mode === 'action') setActionType(t); else setBehaviourType(t);
        setValues({}); setExtras({}); setPotions([emptyPotion()]); setStackSteps([emptyStep()]);
    };

    const updatePotion = (idx: number, k: keyof PotionEffect, v: string) =>
        setPotions(prev => prev.map((p, i) => i === idx ? { ...p, [k]: v } : p));
    const removePotion = (idx: number) => setPotions(prev => prev.filter((_, i) => i !== idx));

    const updateStep = (idx: number, k: keyof StackStep, v: string) =>
        setStackSteps(prev => prev.map((s, i) => i === idx ? { ...s, [k]: v } : s));
    const removeStep = (idx: number) => setStackSteps(prev => prev.filter((_, i) => i !== idx));

    const copy = () => navigator.clipboard.writeText(yaml).then(() => { setCopied(true); setTimeout(() => setCopied(false), 2000); });

    const connType = values['conn_type'] ?? 'stair';
    const connKeys = connType === 'table' ? TABLE_KEYS : STAIR_KEYS;
    const stackMode = values['stackable_mode'] ?? 'simple';

    return (
        <div className="my-6 flex flex-col gap-4 not-prose">
            <div className="flex gap-1 rounded-lg border border-fd-border bg-fd-muted p-1 w-fit">
                {(['action', 'behaviour'] as const).map(m => (
                    <button key={m} onClick={() => switchMode(m)}
                            className={`rounded-md px-4 py-1.5 text-sm transition-colors ${mode === m ? 'bg-fd-background text-fd-foreground font-medium shadow-sm' : 'text-fd-muted-foreground hover:text-fd-foreground'}`}>
                        {m === 'action' ? 'Actions' : 'Behaviours'}
                    </button>
                ))}
            </div>

            <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
                <div className="flex flex-col gap-4 rounded-xl border border-fd-border bg-fd-card p-4">
                    <div className="flex flex-col gap-1">
                        <label className="text-xs font-medium text-fd-muted-foreground">Type</label>
                        <select value={type} onChange={e => switchType(e.target.value)}
                                className={inputCls}>
                            {Object.keys(mode === 'action' ? ACTIONS : { contact_damage: 1, storage: 1, stackable: 1, connectable: 1 }).map(k => (
                                <option key={k} value={k}>{k}</option>
                            ))}
                        </select>
                    </div>

                    <Divider />

                    {/* Actions */}
                    {mode === 'action' && (
                        <>
                            <SectionLabel>Parameters</SectionLabel>
                            <div className="flex flex-col gap-3">
                                {ACTIONS[actionType]?.map(f => (
                                    <Field key={f.k} def={f} value={values[f.k] ?? ''} onChange={v => setVal(f.k, v)} />
                                ))}
                            </div>
                            <Divider />
                            <SectionLabel>Universal parameters</SectionLabel>
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

                    {/* contact_damage */}
                    {mode === 'behaviour' && behaviourType === 'contact_damage' && (
                        <>
                            <SectionLabel>Parameters</SectionLabel>
                            <div className="flex flex-col gap-3">
                                <Field def={{ k: 'amount', l: 'amount', t: 'number', r: true, p: '1.0' }} value={values['amount'] ?? ''} onChange={v => setVal('amount', v)} />
                                <Field def={{ k: 'interval', l: 'interval', t: 'number', r: false, p: '20', h: 'ticks (default: 20)' }} value={values['interval'] ?? ''} onChange={v => setVal('interval', v)} />
                                <Field def={{ k: 'fire_duration', l: 'fire_duration', t: 'number', r: false, p: '0', h: 'ticks (default: 0)' }} value={values['fire_duration'] ?? ''} onChange={v => setVal('fire_duration', v)} />
                                <Field def={{ k: 'damage_when_sneaking', l: 'damage_when_sneaking', t: 'select', r: false, opts: ['', 'true', 'false'], h: 'default: true' }} value={values['damage_when_sneaking'] ?? ''} onChange={v => setVal('damage_when_sneaking', v)} />
                            </div>
                            <Divider />
                            <CheckToggle label="Configure block_faces" checked={!!extras['faces']} onChange={v => setExtra('faces', v)} />
                            {extras['faces'] && (
                                <SubSection>
                                    {['top', 'north', 'south', 'west', 'east'].map(face => (
                                        <Field key={face} def={{ k: `face_${face}`, l: face, t: 'select', r: false, opts: ['', 'true', 'false'], h: 'default: true' }} value={values[`face_${face}`] ?? ''} onChange={v => setVal(`face_${face}`, v)} />
                                    ))}
                                </SubSection>
                            )}
                            <Divider />
                            <SectionLabel>Potion effects</SectionLabel>
                            <div className="flex flex-col gap-3">
                                {potions.map((pe, idx) => (
                                    <PotionEffectFields key={idx} pe={pe} index={idx}
                                                        onChange={(k, v) => updatePotion(idx, k, v)}
                                                        onRemove={() => removePotion(idx)} />
                                ))}
                                <button onClick={() => setPotions(prev => [...prev, emptyPotion()])}
                                        className="self-start rounded-md border border-fd-border bg-fd-background px-3 py-1.5 text-xs text-fd-muted-foreground hover:text-fd-foreground transition-colors">
                                    + Add potion effect
                                </button>
                            </div>
                        </>
                    )}

                    {/* storage */}
                    {mode === 'behaviour' && behaviourType === 'storage' && (
                        <>
                            <SectionLabel>Parameters</SectionLabel>
                            <div className="flex flex-col gap-3">
                                <Field def={{ k: 'type', l: 'type', t: 'select', r: true, opts: ['STORAGE', 'SHULKER', 'DISPOSAL'] }} value={values['type'] ?? 'STORAGE'} onChange={v => setVal('type', v)} />
                                <Field def={{ k: 'rows', l: 'rows', t: 'number', r: false, p: '3', h: '1–6 (default: 3)' }} value={values['rows'] ?? ''} onChange={v => setVal('rows', v)} />
                                <Field def={{ k: 'title', l: 'title', t: 'text', r: false, p: '<gold>My Storage', h: 'MiniMessage supported' }} value={values['title'] ?? ''} onChange={v => setVal('title', v)} />
                            </div>
                            <Divider />
                            <CheckToggle label="Add open_sound" checked={!!extras['open_sound']} onChange={v => setExtra('open_sound', v)} />
                            {extras['open_sound'] && (
                                <SubSection>
                                    <SoundFields
                                        sound={{ sound_name: values['open_sound_name'] ?? '', sound_volume: values['open_sound_volume'] ?? '', sound_pitch: values['open_sound_pitch'] ?? '', sound_category: values['open_sound_category'] ?? '' }}
                                        onChange={(k, v) => setVal(`open_sound_${k.replace('sound_', '')}`, v)}
                                    />
                                </SubSection>
                            )}
                            <Divider />
                            <CheckToggle label="Add close_sound" checked={!!extras['close_sound']} onChange={v => setExtra('close_sound', v)} />
                            {extras['close_sound'] && (
                                <SubSection>
                                    <SoundFields
                                        sound={{ sound_name: values['close_sound_name'] ?? '', sound_volume: values['close_sound_volume'] ?? '', sound_pitch: values['close_sound_pitch'] ?? '', sound_category: values['close_sound_category'] ?? '' }}
                                        onChange={(k, v) => setVal(`close_sound_${k.replace('sound_', '')}`, v)}
                                    />
                                </SubSection>
                            )}
                        </>
                    )}

                    {/* stackable */}
                    {mode === 'behaviour' && behaviourType === 'stackable' && (
                        <>
                            <SectionLabel>Format</SectionLabel>
                            <Field def={{ k: 'stackable_mode', l: 'mode', t: 'select', r: true, opts: ['simple', 'complex', 'full'], h: 'simple = IDs only · complex = shared config · full = per-step config' }} value={stackMode} onChange={v => setVal('stackable_mode', v)} />
                            <Divider />

                            {stackMode === 'simple' && (
                                <>
                                    <SectionLabel>Parameters</SectionLabel>
                                    <Field def={{ k: 'stack_blocks', l: 'block IDs (one per line)', t: 'textarea', r: true, p: 'namespace:block_2\nnamespace:block_3', h: 'Must be custom block IDs' }} value={values['stack_blocks'] ?? ''} onChange={v => setVal('stack_blocks', v)} />
                                </>
                            )}

                            {stackMode === 'complex' && (
                                <>
                                    <SectionLabel>Parameters</SectionLabel>
                                    <div className="flex flex-col gap-3">
                                        <Field def={{ k: 'stack_blocks', l: 'block IDs (one per line)', t: 'textarea', r: true, p: 'namespace:block_2\nnamespace:block_3', h: 'Must be custom block IDs' }} value={values['stack_blocks'] ?? ''} onChange={v => setVal('stack_blocks', v)} />
                                        <Field def={{ k: 'stack_items', l: 'items (one per line)', t: 'textarea', r: false, p: 'namespace:my_item\nBONE_MEAL', h: 'defaults to the item this behaviour is on' }} value={values['stack_items'] ?? ''} onChange={v => setVal('stack_items', v)} />
                                        <Field def={{ k: 'decrement_amount', l: 'decrement_amount', t: 'number', r: false, p: '1', h: 'default: 1' }} value={values['decrement_amount'] ?? ''} onChange={v => setVal('decrement_amount', v)} />
                                    </div>
                                    <Divider />
                                    <CheckToggle label="Add sound" checked={!!extras['stack_sound']} onChange={v => setExtra('stack_sound', v)} />
                                    {extras['stack_sound'] && (
                                        <SubSection>
                                            <SoundFields
                                                sound={{ sound_name: values['stack_sound_name'] ?? '', sound_volume: values['stack_sound_volume'] ?? '', sound_pitch: values['stack_sound_pitch'] ?? '', sound_category: values['stack_sound_category'] ?? '' }}
                                                onChange={(k, v) => setVal(`stack_sound_${k.replace('sound_', '')}`, v)}
                                            />
                                        </SubSection>
                                    )}
                                </>
                            )}

                            {stackMode === 'full' && (
                                <>
                                    <SectionLabel>Steps</SectionLabel>
                                    <div className="flex flex-col gap-3">
                                        {stackSteps.map((step, idx) => (
                                            <StackStepFields key={idx} step={step} index={idx}
                                                             onChange={(k, v) => updateStep(idx, k, v)}
                                                             onRemove={() => removeStep(idx)} />
                                        ))}
                                        <button onClick={() => setStackSteps(prev => [...prev, emptyStep()])}
                                                className="self-start rounded-md border border-fd-border bg-fd-background px-3 py-1.5 text-xs text-fd-muted-foreground hover:text-fd-foreground transition-colors">
                                            + Add step
                                        </button>
                                    </div>
                                </>
                            )}
                        </>
                    )}

                    {/* connectable */}
                    {mode === 'behaviour' && behaviourType === 'connectable' && (
                        <>
                            <SectionLabel>Parameters</SectionLabel>
                            <div className="flex flex-col gap-3">
                                <Field def={{ k: 'conn_type', l: 'type', t: 'select', r: false, opts: ['stair', 'table'] }} value={connType} onChange={v => setVal('conn_type', v)} />
                                {connKeys.map(k => (
                                    <Field key={k} def={{ k: `conn_${k}`, l: k, t: 'text', r: true, p: `namespace:furniture_${k}` }} value={values[`conn_${k}`] ?? ''} onChange={v => setVal(`conn_${k}`, v)} />
                                ))}
                            </div>
                        </>
                    )}
                </div>

                {/* Output panel */}
                <div className="flex flex-col gap-3 rounded-xl border border-fd-border bg-fd-card p-4">
                    <SectionLabel>Generated YAML</SectionLabel>
                    <pre className="flex-1 overflow-x-auto rounded-lg bg-fd-muted p-3 font-mono text-xs leading-relaxed text-fd-foreground whitespace-pre">{yaml}</pre>
                    <button onClick={copy}
                            className="self-start rounded-md border border-fd-border bg-fd-background px-3 py-1.5 text-sm text-fd-foreground transition-colors hover:bg-fd-muted">
                        {copied ? 'Copied!' : 'Copy'}
                    </button>
                </div>
            </div>
        </div>
    );
}