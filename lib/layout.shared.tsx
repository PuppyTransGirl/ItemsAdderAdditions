import type {BaseLayoutProps} from 'fumadocs-ui/layouts/shared';
import type {LayoutTab} from 'fumadocs-ui/layouts/shared';
import {appName, gitConfig, localizedDocsRoute} from './shared';
import Image from 'next/image';

export const langTabs: LayoutTab[] = [
    {
        title: 'English',
        url: localizedDocsRoute('en'),
        icon: <span className="text-base leading-none">🇬🇧</span>,
    },
    {
        title: 'Français',
        url: localizedDocsRoute('fr'),
        icon: <span className="text-base leading-none">🇫🇷</span>,
    },
    {
        title: 'Nederlands',
        url: localizedDocsRoute('nl'),
        icon: <span className="text-base leading-none">🇳🇱</span>,
    },
];

export function baseOptions(): BaseLayoutProps {
    return {
        nav: {
            title: (
                <>
                    <Image
                        src="/icon_36x36.png"
                        alt={appName}
                        width={36}
                        height={36}
                        className="rounded-sm"
                    />
                    {appName}
                </>
            ),
        },
        githubUrl: `https://github.com/${gitConfig.user}/${gitConfig.repo}`,
    };
}
