import {getLLMText, getPageMarkdownUrl, source} from '@/lib/source';
import {defaultLanguage} from '@/lib/shared';
import {notFound} from 'next/navigation';

export const revalidate = false;

export async function GET(_req: Request, { params }: RouteContext<'/llms.mdx/docs/[[...slug]]'>) {
  const { slug } = await params;
    // slug is [locale, ...pageSlug, 'content.md']
    const locale = slug?.[0] ?? defaultLanguage;
    const pageSlug = slug ? slug.slice(1, -1) : undefined;
    const page = source.getPage(pageSlug, locale);
  if (!page) notFound();

  return new Response(await getLLMText(page), {
    headers: {
      'Content-Type': 'text/markdown',
    },
  });
}

export function generateStaticParams() {
  return source.getPages().map((page) => ({
    slug: getPageMarkdownUrl(page).segments,
  }));
}
