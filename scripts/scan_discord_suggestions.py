#!/usr/bin/env python3
"""
Scan a Discord suggestions channel and generate a triage report.

Usage:
  DISCORD_BOT_TOKEN=your_bot_token python3 scripts/scan_discord_suggestions.py

The bot needs access to the channel plus "View Channel" and "Read Message History".
For forum channels, each suggestion post is a thread. For text channels, this also
tries to scan channel messages and public archived threads.
"""

from __future__ import annotations

import argparse
import datetime as dt
import json
import os
import re
import sys
import time
import urllib.error
import urllib.parse
import urllib.request
from dataclasses import dataclass
from typing import Any


DEFAULT_CHANNEL_ID = "1489728918108770444"
API_BASE = "https://discord.com/api/v10"
USER_AGENT = "ItemsAdderAdditionsSuggestionScanner/1.0"

CHANNEL_TYPE_NAMES = {
    0: "text",
    5: "announcement",
    10: "announcement_thread",
    11: "public_thread",
    12: "private_thread",
    15: "forum",
    16: "media",
}

BUG_WORDS = {
    "bug",
    "broken",
    "crash",
    "crashes",
    "error",
    "exception",
    "fix",
    "issue",
    "problem",
    "not working",
    "doesn't work",
    "doesnt work",
    "failed",
    "failure",
}

COMPAT_WORDS = {
    "compat",
    "compatible",
    "compatibility",
    "paper",
    "spigot",
    "purpur",
    "folia",
    "minecraft",
    "itemsadder",
    "version",
    "1.20",
    "1.21",
    "1.22",
}

QUESTION_WORDS = {
    "how",
    "can i",
    "is it possible",
    "would it be possible",
    "please add",
    "idk",
    "maybe",
}

LARGE_SCOPE_WORDS = {
    "rewrite",
    "entire",
    "system",
    "editor",
    "gui",
    "api",
    "support everything",
    "everything",
}


@dataclass
class ScoredSuggestion:
    thread_id: str
    title: str
    url: str
    created_at: str
    last_message_at: str
    author_id: str | None
    tag_names: list[str]
    message_count: int
    participant_count: int
    reaction_count: int
    attachment_count: int
    category: str
    status: str
    recommendation: str
    priority_score: int
    summary_text: str
    messages: list[dict[str, Any]]


class DiscordClient:
    def __init__(self, token: str) -> None:
        self.token = token

    def request(self, path: str, params: dict[str, Any] | None = None) -> Any:
        if params:
            query = urllib.parse.urlencode({k: v for k, v in params.items() if v is not None})
            path = f"{path}?{query}"

        url = f"{API_BASE}{path}"
        req = urllib.request.Request(
            url,
            headers={
                "Authorization": f"Bot {self.token}",
                "User-Agent": USER_AGENT,
                "Content-Type": "application/json",
            },
        )

        while True:
            try:
                with urllib.request.urlopen(req, timeout=30) as response:
                    raw = response.read().decode("utf-8")
                    return json.loads(raw) if raw else None
            except urllib.error.HTTPError as exc:
                body = exc.read().decode("utf-8", errors="replace")
                if exc.code == 429:
                    retry_after = parse_retry_after(body)
                    time.sleep(retry_after)
                    continue
                raise RuntimeError(f"Discord API error {exc.code} for {path}: {body}") from exc
            except urllib.error.URLError as exc:
                raise RuntimeError(f"Could not reach Discord API for {path}: {exc}") from exc


def parse_retry_after(body: str) -> float:
    try:
        payload = json.loads(body)
        return float(payload.get("retry_after", 1.0)) + 0.25
    except (json.JSONDecodeError, TypeError, ValueError):
        return 1.25


def fetch_all_messages(client: DiscordClient, channel_id: str, limit: int) -> list[dict[str, Any]]:
    messages: list[dict[str, Any]] = []
    before: str | None = None

    while len(messages) < limit:
        batch_limit = min(100, limit - len(messages))
        batch = client.request(
            f"/channels/{channel_id}/messages",
            {"limit": batch_limit, "before": before},
        )
        if not batch:
            break
        messages.extend(batch)
        before = batch[-1]["id"]
        if len(batch) < batch_limit:
            break

    return messages


def fetch_forum_threads(
    client: DiscordClient,
    channel_id: str,
    include_private_archived: bool,
    max_archived_pages: int,
) -> list[dict[str, Any]]:
    threads_by_id: dict[str, dict[str, Any]] = {}

    # Active threads are returned at guild level, so filter to this parent channel.
    channel = client.request(f"/channels/{channel_id}")
    guild_id = channel.get("guild_id")
    if guild_id:
        active = client.request(f"/guilds/{guild_id}/threads/active")
        for thread in active.get("threads", []):
            if thread.get("parent_id") == channel_id:
                threads_by_id[thread["id"]] = thread

    archived_paths = [f"/channels/{channel_id}/threads/archived/public"]
    if include_private_archived:
        archived_paths.append(f"/channels/{channel_id}/threads/archived/private")

    for path in archived_paths:
        before: str | None = None
        for _ in range(max_archived_pages):
            params = {"limit": 100, "before": before}
            try:
                archived = client.request(path, params)
            except RuntimeError as exc:
                if "403" in str(exc) or "404" in str(exc):
                    break
                raise

            for thread in archived.get("threads", []):
                threads_by_id[thread["id"]] = thread

            if not archived.get("has_more") or not archived.get("threads"):
                break
            before = archived["threads"][-1].get("thread_metadata", {}).get("archive_timestamp")

    return list(threads_by_id.values())


def fetch_text_channel_threads(
    client: DiscordClient,
    channel_id: str,
    include_private_archived: bool,
    max_archived_pages: int,
) -> list[dict[str, Any]]:
    return fetch_forum_threads(client, channel_id, include_private_archived, max_archived_pages)


def clean_content(message: dict[str, Any]) -> str:
    parts = [message.get("content", "").strip()]

    embeds = message.get("embeds") or []
    for embed in embeds:
        for key in ("title", "description", "url"):
            value = embed.get(key)
            if value:
                parts.append(str(value).strip())

    attachments = message.get("attachments") or []
    for attachment in attachments:
        filename = attachment.get("filename")
        url = attachment.get("url")
        if filename or url:
            parts.append(f"[attachment: {filename or url}]")

    return "\n".join(part for part in parts if part)


def normalize_text(value: str) -> str:
    return re.sub(r"\s+", " ", value.lower()).strip()


def contains_any_keyword(haystack: str, keywords: set[str]) -> bool:
    for keyword in keywords:
        keyword = keyword.lower()
        if " " in keyword or "." in keyword or "'" in keyword:
            if keyword in haystack:
                return True
            continue

        if re.search(rf"\b{re.escape(keyword)}\b", haystack):
            return True
    return False


def count_reactions(messages: list[dict[str, Any]]) -> int:
    total = 0
    for message in messages:
        for reaction in message.get("reactions") or []:
            total += int(reaction.get("count", 0))
    return total


def count_attachments(messages: list[dict[str, Any]]) -> int:
    return sum(len(message.get("attachments") or []) for message in messages)


def sort_messages_chronological(messages: list[dict[str, Any]]) -> list[dict[str, Any]]:
    return sorted(messages, key=lambda message: int(message["id"]))


def status_from_tags(tag_names: list[str]) -> str:
    normalized = {tag.lower().strip() for tag in tag_names}
    if "released" in normalized or "done" in normalized:
        return "completed"
    if "not possible for now" in normalized:
        return "not_possible"
    if "stalled" in normalized:
        return "stalled"
    return "open"


def choose_category(title: str, messages: list[dict[str, Any]], tag_names: list[str]) -> str:
    haystack = normalize_text(" ".join([title, *tag_names, *[clean_content(m) for m in messages[:5]]]))

    if contains_any_keyword(haystack, BUG_WORDS):
        return "bug"
    if contains_any_keyword(haystack, COMPAT_WORDS):
        return "compatibility"
    if contains_any_keyword(haystack, LARGE_SCOPE_WORDS):
        return "large_scope"
    if contains_any_keyword(haystack, QUESTION_WORDS):
        return "needs_clarification"
    return "enhancement"


def priority_score(category: str, messages: list[dict[str, Any]], participant_count: int) -> int:
    score = 0
    score += min(count_reactions(messages), 25)
    score += min(len(messages), 10)
    score += min(participant_count * 2, 20)

    if category == "bug":
        score += 25
    elif category == "compatibility":
        score += 20
    elif category == "needs_clarification":
        score -= 8
    elif category == "large_scope":
        score -= 10

    if count_attachments(messages) > 0:
        score += 4

    return max(score, 0)


def choose_recommendation(status: str, category: str, score: int, text: str) -> str:
    if status != "open":
        return status

    normalized = normalize_text(text)
    vague = len(normalized) < 80 or normalized.count(" ") < 12

    if category == "bug":
        return "do_now" if score >= 30 else "needs_reproduction"
    if category == "compatibility":
        return "do_now" if score >= 28 else "maybe_later"
    if category == "large_scope":
        return "decline_or_split"
    if category == "needs_clarification" or vague:
        return "needs_clarification"
    if score >= 30:
        return "do_now"
    if score >= 14:
        return "maybe_later"
    return "backlog"


def make_summary(title: str, messages: list[dict[str, Any]], max_chars: int) -> str:
    chunks = [clean_content(message) for message in messages]
    text = "\n".join(chunk for chunk in chunks if chunk).strip()
    if not text:
        return title
    text = re.sub(r"\s+", " ", text)
    if len(text) <= max_chars:
        return text
    return text[: max_chars - 3].rstrip() + "..."


def tag_lookup(channel: dict[str, Any]) -> dict[str, str]:
    tags = {}
    for tag in channel.get("available_tags") or []:
        tags[tag.get("id", "")] = tag.get("name", "")
    return tags


def discord_timestamp(snowflake: str) -> str:
    timestamp_ms = (int(snowflake) >> 22) + 1420070400000
    value = dt.datetime.fromtimestamp(timestamp_ms / 1000, tz=dt.timezone.utc)
    return value.isoformat()


def score_thread(
    channel: dict[str, Any],
    thread: dict[str, Any],
    messages: list[dict[str, Any]],
    max_summary_chars: int,
) -> ScoredSuggestion:
    messages = sort_messages_chronological(messages)
    title = thread.get("name") or f"Thread {thread['id']}"
    tags = tag_lookup(channel)
    tag_names = [tags[tag_id] for tag_id in thread.get("applied_tags", []) if tags.get(tag_id)]
    participant_ids = {
        message.get("author", {}).get("id")
        for message in messages
        if message.get("author", {}).get("id")
    }
    status = status_from_tags(tag_names)
    category = choose_category(title, messages, tag_names)
    score = priority_score(category, messages, len(participant_ids))
    summary = make_summary(title, messages, max_summary_chars)
    recommendation = choose_recommendation(status, category, score, f"{title}\n{summary}")

    return ScoredSuggestion(
        thread_id=thread["id"],
        title=title,
        url=f"https://discord.com/channels/{channel.get('guild_id', '@me')}/{thread['id']}",
        created_at=discord_timestamp(thread["id"]),
        last_message_at=discord_timestamp(thread.get("last_message_id") or thread["id"]),
        author_id=messages[-1].get("author", {}).get("id") if messages else None,
        tag_names=tag_names,
        message_count=len(messages),
        participant_count=len(participant_ids),
        reaction_count=count_reactions(messages),
        attachment_count=count_attachments(messages),
        category=category,
        status=status,
        recommendation=recommendation,
        priority_score=score,
        summary_text=summary,
        messages=messages,
    )


def score_text_message(
    channel: dict[str, Any],
    message: dict[str, Any],
    max_summary_chars: int,
) -> ScoredSuggestion:
    content = clean_content(message)
    title = first_line(content) or f"Message {message['id']}"
    messages = [message]
    category = choose_category(title, messages, [])
    status = "open"
    score = priority_score(category, messages, 1)
    summary = make_summary(title, messages, max_summary_chars)
    recommendation = choose_recommendation(status, category, score, f"{title}\n{summary}")

    return ScoredSuggestion(
        thread_id=message["id"],
        title=title,
        url=f"https://discord.com/channels/{channel.get('guild_id', '@me')}/{channel['id']}/{message['id']}",
        created_at=message.get("timestamp") or discord_timestamp(message["id"]),
        last_message_at=message.get("edited_timestamp") or message.get("timestamp") or discord_timestamp(message["id"]),
        author_id=message.get("author", {}).get("id"),
        tag_names=[],
        message_count=1,
        participant_count=1,
        reaction_count=count_reactions(messages),
        attachment_count=count_attachments(messages),
        category=category,
        status=status,
        recommendation=recommendation,
        priority_score=score,
        summary_text=summary,
        messages=messages,
    )


def message_from_dump(message: dict[str, Any]) -> dict[str, Any]:
    return {
        "id": message.get("id") or "0",
        "content": message.get("content") or "",
        "author": {
            "id": message.get("author_id"),
            "username": message.get("author"),
        },
        "timestamp": message.get("timestamp"),
        "reactions": [{"count": int(message.get("reaction_count") or 0)}],
        "attachments": message.get("attachments") or [],
        "embeds": [],
    }


def load_suggestions_dump(path: str, max_summary_chars: int) -> list[ScoredSuggestion]:
    with open(path, "r", encoding="utf-8") as handle:
        payload = json.load(handle)

    suggestions = []
    for item in payload:
        messages = sort_messages_chronological([message_from_dump(message) for message in item.get("messages", [])])
        tag_names = item.get("tag_names") or []
        participant_ids = {
            message.get("author", {}).get("id")
            for message in messages
            if message.get("author", {}).get("id")
        }
        status = status_from_tags(tag_names)
        category = choose_category(item["title"], messages, tag_names)
        score = priority_score(category, messages, len(participant_ids))
        summary = make_summary(item["title"], messages, max_summary_chars)
        recommendation = choose_recommendation(status, category, score, f"{item['title']}\n{summary}")

        suggestions.append(
            ScoredSuggestion(
                thread_id=item["thread_id"],
                title=item["title"],
                url=item["url"],
                created_at=item["created_at"],
                last_message_at=item["last_message_at"],
                author_id=item.get("author_id"),
                tag_names=tag_names,
                message_count=len(messages),
                participant_count=len(participant_ids),
                reaction_count=count_reactions(messages),
                attachment_count=count_attachments(messages),
                category=category,
                status=status,
                recommendation=recommendation,
                priority_score=score,
                summary_text=summary,
                messages=messages,
            )
        )

    return suggestions


def first_line(text: str) -> str:
    for line in text.splitlines():
        line = line.strip()
        if line:
            return line[:120]
    return ""


def write_json(path: str, suggestions: list[ScoredSuggestion]) -> None:
    payload = []
    for suggestion in suggestions:
        item = suggestion.__dict__.copy()
        item["messages"] = [
            {
                "id": message.get("id"),
                "author": message.get("author", {}).get("username"),
                "author_id": message.get("author", {}).get("id"),
                "timestamp": message.get("timestamp"),
                "content": clean_content(message),
                "reaction_count": count_reactions([message]),
                "attachments": [
                    {
                        "filename": attachment.get("filename"),
                        "url": attachment.get("url"),
                    }
                    for attachment in message.get("attachments") or []
                ],
            }
            for message in suggestion.messages
        ]
        payload.append(item)

    with open(path, "w", encoding="utf-8") as handle:
        json.dump(payload, handle, indent=2, ensure_ascii=False)


def write_markdown(path: str, suggestions: list[ScoredSuggestion]) -> None:
    grouped: dict[str, list[ScoredSuggestion]] = {}
    for suggestion in suggestions:
        grouped.setdefault(suggestion.recommendation, []).append(suggestion)

    order = [
        "do_now",
        "needs_reproduction",
        "needs_clarification",
        "maybe_later",
        "decline_or_split",
        "backlog",
        "stalled",
        "not_possible",
        "completed",
    ]

    with open(path, "w", encoding="utf-8") as handle:
        handle.write("# Discord Suggestions Triage\n\n")
        handle.write(f"Generated at: {dt.datetime.now(dt.timezone.utc).isoformat()}\n\n")
        handle.write("## Summary\n\n")
        handle.write(f"- Suggestions scanned: {len(suggestions)}\n")
        for name in order:
            handle.write(f"- {name}: {len(grouped.get(name, []))}\n")

        for name in order:
            items = grouped.get(name, [])
            if not items:
                continue
            handle.write(f"\n## {name}\n\n")
            for item in items:
                tags = f" | tags: {', '.join(item.tag_names)}" if item.tag_names else ""
                handle.write(f"### [{escape_md(item.title)}]({item.url})\n\n")
                handle.write(
                    f"- score: {item.priority_score}"
                    f" | category: {item.category}"
                    f" | status: {item.status}"
                    f" | messages: {item.message_count}"
                    f" | participants: {item.participant_count}"
                    f" | reactions: {item.reaction_count}"
                    f"{tags}\n"
                )
                handle.write(f"- created: {item.created_at}\n")
                handle.write(f"- latest activity: {item.last_message_at}\n")
                handle.write(f"- summary: {escape_md(item.summary_text)}\n\n")


def escape_md(value: str) -> str:
    return value.replace("\n", " ").strip()


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Scan Discord suggestion posts into triage reports.")
    parser.add_argument("--channel-id", default=DEFAULT_CHANNEL_ID, help="Discord suggestions channel ID.")
    parser.add_argument("--message-limit", type=int, default=40, help="Max messages to read from each post/thread.")
    parser.add_argument("--text-message-limit", type=int, default=200, help="Max top-level text messages to read.")
    parser.add_argument("--max-archived-pages", type=int, default=20, help="Max archived-thread pages to scan.")
    parser.add_argument("--summary-chars", type=int, default=500, help="Max summary characters per suggestion.")
    parser.add_argument("--include-private-archived", action="store_true", help="Also scan private archived threads.")
    parser.add_argument("--from-json", help="Rebuild the Markdown report from an existing suggestions dump.")
    parser.add_argument("--json-out", default="suggestions_dump.json", help="JSON output path.")
    parser.add_argument("--md-out", default="suggestions_report.md", help="Markdown output path.")
    return parser.parse_args()


def main() -> int:
    args = parse_args()
    if args.from_json:
        suggestions = load_suggestions_dump(args.from_json, args.summary_chars)
        suggestions.sort(key=lambda item: (item.recommendation != "do_now", -item.priority_score, item.title.lower()))
        write_markdown(args.md_out, suggestions)
        print(f"Wrote {args.md_out}")
        return 0

    token = os.environ.get("DISCORD_BOT_TOKEN")
    if not token:
        print("Missing DISCORD_BOT_TOKEN environment variable.", file=sys.stderr)
        return 2

    client = DiscordClient(token)
    channel = client.request(f"/channels/{args.channel_id}")
    channel_type = channel.get("type")
    channel_type_name = CHANNEL_TYPE_NAMES.get(channel_type, f"type_{channel_type}")
    print(f"Scanning channel {args.channel_id} ({channel_type_name})...")

    suggestions: list[ScoredSuggestion] = []

    if channel_type in {15, 16}:
        threads = fetch_forum_threads(
            client,
            args.channel_id,
            args.include_private_archived,
            args.max_archived_pages,
        )
        print(f"Found {len(threads)} forum/media posts.")
        for index, thread in enumerate(threads, start=1):
            messages = fetch_all_messages(client, thread["id"], args.message_limit)
            suggestions.append(score_thread(channel, thread, messages, args.summary_chars))
            print(f"[{index}/{len(threads)}] {thread.get('name', thread['id'])}")
    else:
        top_messages = fetch_all_messages(client, args.channel_id, args.text_message_limit)
        suggestions.extend(score_text_message(channel, message, args.summary_chars) for message in top_messages)

        threads = fetch_text_channel_threads(
            client,
            args.channel_id,
            args.include_private_archived,
            args.max_archived_pages,
        )
        print(f"Found {len(top_messages)} messages and {len(threads)} threads.")
        for index, thread in enumerate(threads, start=1):
            messages = fetch_all_messages(client, thread["id"], args.message_limit)
            suggestions.append(score_thread(channel, thread, messages, args.summary_chars))
            print(f"[thread {index}/{len(threads)}] {thread.get('name', thread['id'])}")

    suggestions.sort(key=lambda item: (item.recommendation != "do_now", -item.priority_score, item.title.lower()))

    write_json(args.json_out, suggestions)
    write_markdown(args.md_out, suggestions)

    print(f"Wrote {args.md_out}")
    print(f"Wrote {args.json_out}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
