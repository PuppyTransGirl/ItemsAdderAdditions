import {getPageImage, source} from '@/lib/source';
import {notFound} from 'next/navigation';
import {ImageResponse} from 'next/og';
import {generate as DefaultImage} from 'fumadocs-ui/og';

export const revalidate = false;

export async function GET(_req: Request, { params }: RouteContext<'/og/docs/[...slug]'>) {
  const { slug } = await params;
    // slug is [locale, ...pageSlug, 'image.png']
    const locale = slug[0];
    const pageSlug = slug.slice(1, -1);
    const page = source.getPage(pageSlug, locale);
  if (!page) notFound();

  return new ImageResponse(
      <DefaultImage
          title={page.data.title}
          description={page.data.description}
          site="ItemsAdderAdditions"
          primaryColor="rgba(172,82,212,0.3)"
          primaryTextColor="rgb(172,82,212)"
      />,
    {
      width: 1200,
      height: 630,
    },
  );
}

export function generateStaticParams() {
  return source.getPages().map((page) => ({
    slug: getPageImage(page).segments,
  }));
}
