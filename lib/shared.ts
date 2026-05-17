export const appName = 'ItemsAdderAdditions';
export const docsRoute = '/docs';
export const docsImageRoute = '/og/docs';
export const docsContentRoute = '/llms.mdx/docs';

export const languages = ['en', 'fr', 'nl'] as const;
export type Language = typeof languages[number];
export const defaultLanguage: Language = 'en';

export function localizedDocsRoute(lang: string = defaultLanguage) {
    return `/${lang}${docsRoute}`;
}

export const gitConfig = {
  user: 'PuppyTransGirl',
  repo: 'ItemsAdderAdditions',
  branch: 'master',
};
