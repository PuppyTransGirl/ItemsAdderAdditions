import type {BaseLayoutProps} from 'fumadocs-ui/layouts/shared';
import {appName, gitConfig} from './shared';
import Image from 'next/image';

export function baseOptions(): BaseLayoutProps {
    return {
        nav: {
            title: (
                <>
                    <Image
                        src="/icon_128x128.png"       // put your icon in the /public folder
                        alt={appName}
                        width={48}
                        height={48}
                        className="rounded-sm"
                    />
                    {appName}
                </>
            ),
        },
        githubUrl: `https://github.com/${gitConfig.user}/${gitConfig.repo}`,
    };
}