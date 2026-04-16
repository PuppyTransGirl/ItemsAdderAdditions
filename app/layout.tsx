import {RootProvider} from 'fumadocs-ui/provider/next';
import './global.css';
import {Inter} from 'next/font/google';
import type {Metadata} from 'next';
import type {ReactNode} from 'react';
import {GoogleAnalytics} from '@next/third-parties/google';

const inter = Inter({
    subsets: ['latin'],
});

export const metadata: Metadata = {
    metadataBase: new URL('https://itemsadderadditions.com'),
    title: {
        template: '%s | ItemsAdderAdditions',
        default: 'ItemsAdderAdditions - ItemsAdder Plugin Addon for Minecraft Servers',
    },
    description: 'ItemsAdderAdditions is a free Minecraft plugin addon that extends ItemsAdder with custom behaviours, actions, and features - configured directly inside your existing YML files. No restart needed.',
    keywords: [
        'ItemsAdder addon',
        'ItemsAdder plugin',
        'ItemsAdderAdditions',
        'Minecraft custom items plugin',
        'ItemsAdder behaviours',
        'ItemsAdder actions',
        'Minecraft server plugin',
        'Paper plugin',
        'SpigotMC plugin',
        'ItemsAdder extension',
        'custom blocks Minecraft',
        'custom furniture Minecraft',
    ],
    authors: [{ name: 'ItemsAdderAdditions', url: 'https://itemsadderadditions.com' }],
    creator: 'ItemsAdderAdditions',
    openGraph: {
        siteName: 'ItemsAdderAdditions',
        url: 'https://itemsadderadditions.com',
        type: 'website',
        title: 'ItemsAdderAdditions - ItemsAdder Plugin Addon for Minecraft Servers',
        description: 'Extend ItemsAdder with custom behaviours, actions, and features - configured directly in your existing YML files. No restart needed.',
        locale: 'en_US',
    },
    twitter: {
        card: 'summary_large_image',
        title: 'ItemsAdderAdditions - ItemsAdder Plugin Addon',
        description: 'Extend ItemsAdder with custom behaviours, actions, and features - configured in your existing YML files.',
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
        <RootProvider>{children}</RootProvider>
        <GoogleAnalytics gaId="G-XJSY3N88FX" />
        </body>
        </html>
    );
}