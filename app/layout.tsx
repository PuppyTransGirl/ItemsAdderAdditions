import {RootProvider} from 'fumadocs-ui/provider/next';
import StaticSearchDialog from '@/components/search';
import {ClipboardPolyfill} from '@/components/clipboard-polyfill';
import './global.css';
import {Inter} from 'next/font/google';
import type {Metadata} from 'next';
import type {ReactNode} from 'react';
import {GoogleAnalytics} from '@next/third-parties/google';
import Script from 'next/script';

const inter = Inter({
    subsets: ['latin'],
});

export const metadata: Metadata = {
    metadataBase: new URL('https://itemsadderadditions.com'),
    title: {
        template: '%s | ItemsAdderAdditions',
        default: 'ItemsAdderAdditions - Free ItemsAdder Addons for Minecraft Servers',
    },
    description: 'Free ItemsAdder addons for Minecraft servers. Add contact damage, storage containers, connectable furniture, veinminer, MythicMobs skills, and more, directly in your existing YML files. No restart needed.',
    keywords: [
        'IA',
        'ItemsAdder',
        'IAAdditions',
        'IAA',
        'IAAdditions',
        'ItemsAdder addons',
        'ItemsAdder addon',
        'ItemsAdder addon free',
        'free ItemsAdder addon',
        'ItemsAdder plugin addon',
        'ItemsAdderAdditions',
        'ItemsAdder extension',
        'ItemsAdder behaviours',
        'ItemsAdder actions',
        'items adder addon',
        'Minecraft custom items plugin',
        'Minecraft server plugin',
        'Paper plugin',
        'SpigotMC plugin',
        'custom blocks Minecraft',
        'custom furniture Minecraft',
    ],
    authors: [{ name: 'ItemsAdderAdditions', url: 'https://itemsadderadditions.com' }],
    creator: 'ItemsAdderAdditions',
    openGraph: {
        siteName: 'ItemsAdderAdditions',
        url: 'https://itemsadderadditions.com',
        type: 'website',
        title: 'ItemsAdderAdditions - Free ItemsAdder Addons for Minecraft Servers',
        description: 'Free ItemsAdder addons for Minecraft servers. Add contact damage, storage, connectable furniture, veinminer, MythicMobs skills, and more, in your existing YML files.',
        locale: 'en_US',
    },
    twitter: {
        card: 'summary_large_image',
        title: 'ItemsAdderAdditions - Free ItemsAdder Addons',
        description: 'Free ItemsAdder addons: contact damage, storage, connectable furniture, veinminer, MythicMobs skills, and more.',
    },
    icons: {
        icon: '/favicon.ico',
        shortcut: '/favicon.ico',
        apple: '/apple-touch-icon.png',
    },
    alternates: {
        canonical: 'https://itemsadderadditions.com',
    },
    robots: {
        index: true,
        follow: true,
        googleBot: {
            index: true,
            follow: true,
            'max-snippet': -1,
            'max-image-preview': 'large',
            'max-video-preview': -1,
        },
    },
};

export default function Layout({ children }: { children: ReactNode }) {
    return (
        <html lang="en" className={inter.className} suppressHydrationWarning>
        <body className="flex flex-col min-h-screen">
        <ClipboardPolyfill/>
        <RootProvider search={{ SearchDialog: StaticSearchDialog }}>{children}</RootProvider>
        <GoogleAnalytics gaId="G-XJSY3N88FX" />
        <Script id="clarity-script" strategy="afterInteractive">{`
            (function(c,l,a,r,i,t,y){
                c[a]=c[a]||function(){(c[a].q=c[a].q||[]).push(arguments)};
                t=l.createElement(r);t.async=1;t.src="https://www.clarity.ms/tag/"+i;
                y=l.getElementsByTagName(r)[0];y.parentNode.insertBefore(t,y);
            })(window, document, "clarity", "script", "wye7546a9a");
        `}</Script>

        </body>
        </html>
    );
}
