import {source} from '@/lib/source';
import {DocsLayout} from 'fumadocs-ui/layouts/docs';
import {baseOptions, langTabs} from '@/lib/layout.shared';
import {LangSetter} from '@/components/lang-setter';

export default async function Layout({children, params}: LayoutProps<'/[lang]/docs'>) {
    const {lang} = await params;
    return (
        <DocsLayout tree={source.getPageTree(lang)} tabs={langTabs} {...baseOptions()}>
            <LangSetter lang={lang}/>
            {children}
        </DocsLayout>
    );
}
