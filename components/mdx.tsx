import defaultMdxComponents from 'fumadocs-ui/mdx';
import {Tab, Tabs} from 'fumadocs-ui/components/tabs';
import type {MDXComponents} from 'mdx/types';
import {HangarIcon, ModrinthIcon, SpigotIcon} from './icons';
import {VideoGif} from './video-gif';
import {Builder} from './builder';

export function getMDXComponents(components?: MDXComponents) {
    return {
        ...defaultMdxComponents,
        Tab,
        Tabs,
        ...components,
        ModrinthIcon, HangarIcon, SpigotIcon,
        VideoGif,
        Builder,
    } satisfies MDXComponents;
}

export const useMDXComponents = getMDXComponents;

declare global {
    type MDXProvidedComponents = ReturnType<typeof getMDXComponents>;
}