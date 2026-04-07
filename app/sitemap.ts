import { source } from '@/lib/source';
import type { MetadataRoute } from 'next';

export const dynamic = 'force-static'

export default function sitemap(): MetadataRoute.Sitemap {
    const pages = source.getPages();
    return pages.map((page) => ({
        url: `https://itemsadderadditions.com/docs/${page.slugs.join('/')}`,
        lastModified: new Date(),
        changeFrequency: 'weekly',
        priority: page.slugs.length === 0 ? 1 : 0.8,
    }));
}