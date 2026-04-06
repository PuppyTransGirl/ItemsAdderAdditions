import defaultMdxComponents from 'fumadocs-ui/mdx';
import { Tab, Tabs } from 'fumadocs-ui/components/tabs';
import type {MDXComponents} from 'mdx/types';
import { ModrinthIcon, SpigotIcon } from './icons';

export function getMDXComponents(components?: MDXComponents) {
    return {
        ...defaultMdxComponents,
        Tab,
        Tabs,
        ...components,
        ModrinthIcon, SpigotIcon,
    } satisfies MDXComponents;
}

export const useMDXComponents = getMDXComponents;

declare global {
    type MDXProvidedComponents = ReturnType<typeof getMDXComponents>;
}
