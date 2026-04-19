import {source} from '@/lib/source';
import {createFromSource} from 'fumadocs-core/search/server';

// Required for Next.js static export (GitHub Pages).
// This tells Next.js to call this route at BUILD TIME and save it as a static file.
//
// IMPORTANT: we export `staticGET` (not `GET`) as the handler.
// The regular `GET` handler returns [] when called with no `query` param - which is
// exactly what happens at build time - so the exported file would be empty.
// `staticGET` unconditionally serialises the full Orama index, which is what the
// client-side static search needs to fetch once and search locally.
export const dynamic = 'force-static';

const server = createFromSource(source, {
    language: 'english',
});

export const GET = server.staticGET;
