import {ImageResponse} from 'next/og';
import {generate as DefaultImage} from 'fumadocs-ui/og';

export const dynamic = 'force-static';

export async function GET() {
    return new ImageResponse(
        <DefaultImage
            title="ItemsAdderAdditions"
            description="Free ItemsAdder addons for Minecraft servers. Contact damage, storage, connectable furniture, veinminer, MythicMobs skills, and more."
            site="itemsadderadditions.com"
            primaryColor="rgba(172,82,212,0.3)"
            primaryTextColor="rgb(172,82,212)"
        />,
        {
            width: 1200,
            height: 630,
        },
    );
}
