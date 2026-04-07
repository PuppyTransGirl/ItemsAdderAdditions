import {RootProvider} from 'fumadocs-ui/provider/next';
import './global.css';
import {Inter} from 'next/font/google';
import type {Metadata} from 'next';

const inter = Inter({
    subsets: ['latin'],
});

export const metadata: Metadata = {
    metadataBase: new URL('https://itemsadderadditions.com'),
    title: {
        template: '%s | ItemsAdderAdditions',
        default: 'ItemsAdderAdditions',
    },
    description: 'Extends ItemsAdder with extra actions, behaviours, and features - configured inside your existing YAML files.',
    openGraph: {
        siteName: 'ItemsAdderAdditions',
        url: 'https://itemsadderadditions.com',
        type: 'website',
    },
/*    twitter: {
        card: 'summary_large_image',
    },*/
    icons: {
        icon: '/favicon.ico',
        shortcut: '/favicon.ico',
        apple: '/apple-touch-icon.png',
    },
};

export default function Layout({ children }: LayoutProps<'/'>) {
    return (
        <html lang="en" className={inter.className} suppressHydrationWarning>
        <body className="flex flex-col min-h-screen">
        <RootProvider>{children}</RootProvider>
        </body>
        </html>
    );
}