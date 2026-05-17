'use client';

import {useEffect} from 'react';

export function ClipboardPolyfill() {
    useEffect(() => {
        if (window.isSecureContext || navigator.clipboard) return;
        const polyfill = {
            writeText(text: string) {
                try {
                    const el = document.createElement('textarea');
                    el.value = text;
                    el.style.position = 'fixed';
                    el.style.opacity = '0';
                    document.body.appendChild(el);
                    el.select();
                    document.execCommand('copy');
                    document.body.removeChild(el);
                } catch {
                }
                return Promise.resolve();
            },
            write() {
                return Promise.resolve();
            },
            readText() {
                return Promise.resolve('');
            },
            read() {
                return Promise.resolve([] as ClipboardItem[]);
            },
        } as Clipboard;
        Object.defineProperty(navigator, 'clipboard', {value: polyfill, configurable: true});
    }, []);
    return null;
}
