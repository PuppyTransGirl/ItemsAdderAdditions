import {source} from '@/lib/source';
import {languages} from '@/lib/shared';
import type {MetadataRoute} from 'next';

export const dynamic = 'force-static'

export default function sitemap(): MetadataRoute.Sitemap {
    const docPages = languages.flatMap((lang) =>
        source.getPages(lang).map((page) => ({
            url: `https://itemsadderadditions.com${page.url}`,
            lastModified: new Date(),
            changeFrequency: 'weekly' as const,
            priority: page.slugs.length === 0 ? 0.9 : 0.7,
        }))
    );

    return [
        {
            url: 'https://itemsadderadditions.com',
            lastModified: new Date(),
            changeFrequency: 'weekly',
            priority: 1.0,
        },
        ...docPages,
    ];
}
