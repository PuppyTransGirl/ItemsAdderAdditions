import {RootProvider} from 'fumadocs-ui/provider/next';
import './global.css';
import {Inter} from 'next/font/google';
import type {Metadata} from 'next';

const inter = Inter({
    subsets: ['latin'],
});

export const metadata: Metadata = {
    icons: {
        icon: '/favicon.ico',          // 32×32 .ico
        shortcut: '/favicon.ico',
        apple: '/apple-touch-icon.png', // 180×180 .png
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