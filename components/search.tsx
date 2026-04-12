'use client';

import {useDocsSearch} from 'fumadocs-core/search/client';
import {
    SearchDialog,
    SearchDialogClose,
    SearchDialogContent,
    SearchDialogHeader,
    SearchDialogIcon,
    SearchDialogInput,
    SearchDialogList,
    SearchDialogOverlay,
    type SharedProps,
} from 'fumadocs-ui/components/dialog/search';
import {create} from '@orama/orama';

function initOrama() {
    return create({
        schema: { _: 'string' },
        components: {
            tokenizer: {
                language: 'english',
                normalizationCache: new Map(),
            },
        },
    });
}

export default function StaticSearchDialog(props: SharedProps) {
    const { search, setSearch, query } = useDocsSearch({
        type: 'static',
        initOrama,
    });

    const items = query.data === 'empty' ? [] : query.data ?? [];

    return (
        <SearchDialog
            search={search}
            onSearchChange={setSearch}
            isLoading={query.isLoading}
            {...props}
        >
            <SearchDialogOverlay />
            <SearchDialogContent>
                <SearchDialogHeader>
                    <SearchDialogIcon />
                    <SearchDialogInput />
                    <SearchDialogClose />
                </SearchDialogHeader>
                <SearchDialogList items={items} />
            </SearchDialogContent>
        </SearchDialog>
    );
}