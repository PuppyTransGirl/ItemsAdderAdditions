import defaultMdxComponents from 'fumadocs-ui/mdx';
import {Tab, Tabs} from 'fumadocs-ui/components/tabs';
import type {MDXComponents} from 'mdx/types';
import {ModrinthIcon, SpigotIcon} from './icons';
import {VideoGif} from './video-gif';
import {Builder} from './builder';

export function getMDXComponents(components?: MDXComponents, locale = 'en') {
    return {
        ...defaultMdxComponents,
        Tab,
        Tabs,
        ...components,
        ModrinthIcon, SpigotIcon,
        VideoGif,
        Builder: () => <Builder locale={locale}/>,
    } satisfies MDXComponents;
}

export const useMDXComponents = getMDXComponents;

declare global {
    type MDXProvidedComponents = ReturnType<typeof getMDXComponents>;
}