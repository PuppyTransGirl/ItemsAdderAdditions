import {defineConfig, defineDocs} from 'fumadocs-mdx/config';
import {metaSchema, pageSchema} from 'fumadocs-core/source/schema';
import {remarkStructure} from 'fumadocs-core/mdx-plugins';
import {z} from 'zod';

export const docs = defineDocs({
    dir: 'content/docs',
    docs: {
        schema: pageSchema.extend({
            version: z.string().optional(),
        }),
        postprocess: {
            includeProcessedMarkdown: true,
        },
    },
    meta: {
        schema: metaSchema,
    },
});

export default defineConfig({
    mdxOptions: {
        remarkPlugins: [remarkStructure],
    },
});