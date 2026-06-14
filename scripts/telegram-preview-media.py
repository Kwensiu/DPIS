#!/usr/bin/env python3
import argparse
import html
import json
import re
import sys

CAPTION_LIMIT = 1024
TITLE_PRESETS = {
    "preview": "DPIS 预览版 | Preview",
    "release": "DPIS 正式版 | Release",
    "local": "DPIS 本地预览 | Local Preview",
}

if hasattr(sys.stdout, "reconfigure"):
    sys.stdout.reconfigure(encoding="utf-8")


def trimmed_subject(subject):
    subject = subject.strip()
    if len(subject) <= 120:
        return subject
    return subject[:117] + "..."


def parse_commits(path):
    commits = []
    if not path:
        return commits

    with open(path, encoding="utf-8") as source:
        for line in source:
            line = line.rstrip("\n")
            if not line:
                continue
            parts = line.split("\t", 2)
            if len(parts) != 3:
                continue
            commits.append({
                "short_sha": parts[0],
                "full_sha": parts[1],
                "subject": trimmed_subject(parts[2]),
            })
    return commits


def visible_caption_length(caption):
    without_tags = re.sub(r"<[^>]*>", "", caption)
    return len(html.unescape(without_tags))


def commit_html(commit, repository_url):
    commit_url = f"{repository_url}/commit/{commit['full_sha']}"
    link = (
        f'<a href="{html.escape(commit_url, quote=True)}">'
        f"{html.escape(commit['short_sha'])}</a>"
    )
    subject = commit["subject"]
    if not subject:
        return link
    return f"{link} - {html.escape(subject)}"


def fallback_commit(args):
    if not args.commit_url or not args.short_sha:
        return ""

    commit_link = (
        f'<a href="{html.escape(args.commit_url, quote=True)}">'
        f"{html.escape(args.short_sha)}</a>"
    )
    commit_subject = trimmed_subject(args.commit_subject)
    if not commit_subject:
        return commit_link
    return f"{commit_link} - {html.escape(commit_subject)}"


def build_changes_block(args, caption_title):
    if args.release_url and not args.commits_file:
        release_text = html.escape(args.release_tag or args.version_name)
        release_url = html.escape(args.release_url, quote=True)
        return (
            "Release:\n"
            f'<blockquote><a href="{release_url}">{release_text} on GitHub</a></blockquote>'
        )

    commits = parse_commits(args.commits_file)
    release_text = html.escape(args.release_tag or "latest release")
    changes_title = f"Commits since <code>{release_text}</code>:"

    if commits:
        lines = [commit_html(commit, args.repository_url) for commit in commits]
    elif args.commits_file:
        lines = [f"No commits since <code>{release_text}</code>."]
    else:
        fallback = fallback_commit(args)
        if fallback:
            return "Commit:\n" f"<blockquote>{fallback}</blockquote>"
        lines = [f"No commits since <code>{release_text}</code>."]

    shown_lines = []
    remaining = 0
    for index, line in enumerate(lines):
        candidate_lines = shown_lines + [line]
        remaining = len(lines) - len(candidate_lines)
        block = format_changes_block(changes_title, candidate_lines, remaining, args.compare_url)
        caption = assemble_caption(
            caption_title,
            block,
            args.version_name,
            args.version_code,
            args.branch_name,
        )
        if visible_caption_length(caption) <= CAPTION_LIMIT:
            shown_lines = candidate_lines
            continue
        remaining = len(lines) - len(shown_lines)
        break

    if not shown_lines:
        shown_lines = [lines[0]]
        remaining = max(0, len(lines) - 1)

    return format_changes_block(changes_title, shown_lines, remaining, args.compare_url)


def format_changes_block(title, lines, remaining, compare_url):
    block_lines = list(lines)
    if remaining > 0:
        block_lines.append(f"... and {remaining} more commits")
    if remaining > 0 and compare_url:
        block_lines.append(
            f'<a href="{html.escape(compare_url, quote=True)}">Full changelog</a>'
        )
    return f"{title}\n<blockquote>{chr(10).join(block_lines)}</blockquote>"


def branch_notice(branch_name):
    if not branch_name or branch_name == "main":
        return ""
    return (
        "<b>Branch:</b> "
        f"<code>{html.escape(branch_name)}</code> "
        "(non-main preview)\n"
    )


def assemble_caption(title, changes_block, version_name, version_code, branch_name=""):
    return (
        f"<b>{html.escape(title)}</b>\n\n"
        f"{branch_notice(branch_name)}"
        f"{changes_block}\n\n"
        f"<b>Version:</b> <code>{html.escape(version_name)}</code>\n"
        f"<b>VersionCode:</b> <code>{html.escape(version_code)}</code>"
    )


def build_caption(args):
    title = args.title or TITLE_PRESETS[args.title_preset]
    changes_block = build_changes_block(args, title)
    return assemble_caption(
        title,
        changes_block,
        args.version_name,
        args.version_code,
        args.branch_name,
    )


def main():
    parser = argparse.ArgumentParser(
        description="Build Telegram media JSON for grouped DPIS APKs."
    )
    parser.add_argument("--title")
    parser.add_argument("--title-preset", choices=sorted(TITLE_PRESETS), default="preview")
    parser.add_argument("--short-sha")
    parser.add_argument("--commit-url")
    parser.add_argument("--commit-subject", default="")
    parser.add_argument("--commits-file")
    parser.add_argument("--repository-url", default="https://github.com/Kwensiu/DPIS")
    parser.add_argument("--release-tag")
    parser.add_argument("--release-url")
    parser.add_argument("--compare-url")
    parser.add_argument("--version-name", required=True)
    parser.add_argument("--version-code", required=True)
    parser.add_argument("--branch-name", default="")
    parser.add_argument("--modern-attach", default="modern_apk")
    parser.add_argument("--legacy-attach", default="legacy_apk")
    args = parser.parse_args()

    media = [
        {
            "type": "document",
            "media": f"attach://{args.modern_attach}",
        },
        {
            "type": "document",
            "media": f"attach://{args.legacy_attach}",
            "caption": build_caption(args),
            "parse_mode": "HTML",
        },
    ]
    print(json.dumps(media, ensure_ascii=True))


if __name__ == "__main__":
    main()
