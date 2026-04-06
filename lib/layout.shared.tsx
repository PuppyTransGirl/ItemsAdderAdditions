import type {BaseLayoutProps} from 'fumadocs-ui/layouts/shared';
import {appName, gitConfig} from './shared';
import Image from 'next/image';

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