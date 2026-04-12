import {getPageImage, getPageMarkdownUrl, source} from '@/lib/source';
import {
    DocsBody,
    DocsDescription,
    DocsPage,
    DocsTitle,
    MarkdownCopyButton,
    ViewOptionsPopover,
} from 'fumadocs-ui/layouts/docs/page';
import {notFound} from 'next/navigation';
import {getMDXComponents} from '@/components/mdx';
import type {Metadata} from 'next';
import {createRelativeLink} from 'fumadocs-ui/mdx';
import {gitConfig} from '@/lib/shared';

export default async function Page(props: PageProps<'/docs/[[...slug]]'>) {
    const params = await props.params;
    const page = source.getPage(params.slug);
    if (!page) notFound();

    const MDX = page.data.body;
    const markdownUrl = getPageMarkdownUrl(page).url;

    const jsonLd = {
        '@context': 'https://schema.org',
        '@type': 'TechArticle',
        headline: page.data.title,
        description: page.data.description,
        url: `https://itemsadderadditions.com/docs/${params.slug?.join('/') ?? ''}`,

    };

    return (
        <DocsPage
            toc={page.data.toc}
            full={page.data.full}
            tableOfContent={{ style: 'clerk' }}
        >
            <DocsTitle>{page.data.title}</DocsTitle>
            <DocsDescription className="mb-0">{page.data.description}</DocsDescription>
            <div className="flex flex-row gap-2 items-center border-b pb-6">
                <MarkdownCopyButton markdownUrl={markdownUrl} />
                <ViewOptionsPopover
                    markdownUrl={markdownUrl}
                    githubUrl={`https://github.com/${gitConfig.user}/${gitConfig.repo}/blob/${gitConfig.branch}/content/docs/${page.path}`}
                />
            </div>
            <DocsBody>
                <MDX
                    components={getMDXComponents({
                        a: createRelativeLink(source, page),
                    })}
                />
            </DocsBody>
            <script
                type="application/ld+json"
                dangerouslySetInnerHTML={{ __html: JSON.stringify(jsonLd) }}
            />
        </DocsPage>
    );
}

export async function generateStaticParams() {
    return source.generateParams();
}

export async function generateMetadata(props: PageProps<'/docs/[[...slug]]'>): Promise<Metadata> {
    const params = await props.params;
    const page = source.getPage(params.slug);
    if (!page) notFound();

    const slug = params.slug ?? [];
    return {
        title: page.data.title,
        description: page.data.description,
        alternates: {
            canonical: `https://itemsadderadditions.com/docs/${slug.join('/')}`,
        },
        openGraph: {
            title: `${page.data.title} | ItemsAdderAdditions`,
            description: page.data.description,
            url: `https://itemsadderadditions.com/docs/${slug.join('/')}`,
            images: getPageImage(page).url,
        },
        twitter: {
            card: 'summary_large_image',
            title: `${page.data.title} | ItemsAdderAdditions`,
            description: page.data.description,
        },
    };
}