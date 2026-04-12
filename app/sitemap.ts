import {source} from '@/lib/source';
import type {MetadataRoute} from 'next';

export const dynamic = 'force-static'

export default function sitemap(): MetadataRoute.Sitemap {
    const pages = source.getPages();
    const docPages = pages.map((page) => ({
        url: `https://itemsadderadditions.com/docs/${page.slugs.join('/')}`,
        lastModified: new Date(),
        changeFrequency: 'weekly' as const,
        priority: page.slugs.length === 0 ? 0.9 : 0.7,
    }));

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
