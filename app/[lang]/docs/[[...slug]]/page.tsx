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
import {gitConfig, languages} from '@/lib/shared';

export default async function Page(props: PageProps<'/[lang]/docs/[[...slug]]'>) {
    const params = await props.params;
    const page = source.getPage(params.slug, params.lang);
    if (!page) notFound();

    const MDX = page.data.body;
    const markdownUrl = getPageMarkdownUrl(page).url;

    const pageUrl = `https://itemsadderadditions.com${page.url}`;
    const breadcrumbs = [
        {name: 'ItemsAdderAdditions', url: 'https://itemsadderadditions.com'},
        {name: 'Docs', url: `https://itemsadderadditions.com/${params.lang}/docs`},
        ...(page.slugs.length > 1 ? [{
            name: page.slugs[0].charAt(0).toUpperCase() + page.slugs[0].slice(1),
            url: `https://itemsadderadditions.com/${params.lang}/docs/${page.slugs[0]}`
        }] : []),
        {name: page.data.title, url: pageUrl},
    ];

    const jsonLd = [
        {
            '@context': 'https://schema.org',
            '@type': 'TechArticle',
            headline: page.data.title,
            description: page.data.description,
            image: `https://itemsadderadditions.com${getPageImage(page).url}`,
            url: pageUrl,
            inLanguage: params.lang,
            isPartOf: {
                '@type': 'WebSite',
                '@id': 'https://itemsadderadditions.com/#website',
                name: 'ItemsAdderAdditions',
            },
        },
        {
            '@context': 'https://schema.org',
            '@type': 'BreadcrumbList',
            itemListElement: breadcrumbs.map((crumb, i) => ({
                '@type': 'ListItem',
                position: i + 1,
                name: crumb.name,
                item: crumb.url,
            })),
        },
    ];

    return (
        <DocsPage
            toc={page.data.toc}
            full={page.data.full}
            tableOfContent={{style: 'clerk'}}
        >
            <DocsTitle>{page.data.title}</DocsTitle>
            <DocsDescription className="mb-0">{page.data.description}</DocsDescription>
            <div className="flex flex-row gap-2 items-center border-b pb-6">
                {page.data.version && (
                    <span
                        className="text-xs font-medium px-2 py-0.5 rounded-full border border-fd-primary/30 bg-fd-primary/10 text-fd-primary">
                        Since v{page.data.version}
                    </span>
                )}
                <MarkdownCopyButton markdownUrl={markdownUrl}/>
                <ViewOptionsPopover
                    markdownUrl={markdownUrl}
                    githubUrl={`https://github.com/${gitConfig.user}/${gitConfig.repo}/blob/${gitConfig.branch}/content/docs/${page.path}`}
                />
            </div>
            <DocsBody>
                <MDX
                    components={getMDXComponents({
                        a: createRelativeLink(source, page),
                    }, params.lang)}
                />
            </DocsBody>
            <script
                type="application/ld+json"
                dangerouslySetInnerHTML={{__html: JSON.stringify(jsonLd)}}
            />
        </DocsPage>
    );
}

export async function generateStaticParams() {
    return source.generateParams();
}

export async function generateMetadata(props: PageProps<'/[lang]/docs/[[...slug]]'>): Promise<Metadata> {
    const params = await props.params;
    const page = source.getPage(params.slug, params.lang);
    if (!page) notFound();

    const slug = params.slug ?? [];
    const langAlternates = Object.fromEntries([
        ['x-default', 'https://itemsadderadditions.com'],
        ...languages.map((lang) => {
            const altPage = source.getPage(slug, lang);
            const altUrl = altPage
                ? `https://itemsadderadditions.com${altPage.url}`
                : `https://itemsadderadditions.com/${lang}/docs`;
            return [lang, altUrl];
        }),
    ]);
    const ogLocale = params.lang === 'fr' ? 'fr_FR' : params.lang === 'nl' ? 'nl_NL' : 'en_US';
    return {
        title: page.data.title,
        description: page.data.description,
        alternates: {
            canonical: `https://itemsadderadditions.com${page.url}`,
            languages: langAlternates,
        },
        openGraph: {
            title: `${page.data.title} | ItemsAdderAdditions`,
            description: page.data.description,
            url: `https://itemsadderadditions.com${page.url}`,
            images: getPageImage(page).url,
            locale: ogLocale,
        },
        twitter: {
            card: 'summary_large_image',
            title: `${page.data.title} | ItemsAdderAdditions`,
            description: page.data.description,
        },
    };
}
