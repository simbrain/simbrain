# /// script
# requires-python = ">=3.11"
# dependencies = []
# ///
"""
One-shot processing script: re-order `src/main/resources/nettalk/nettalk.data`
by English word frequency, using the top-20K Google list from
https://github.com/first20hours/google-10000-english as the rank source.

Output: `src/main/resources/nettalk/nettalk_by_frequency.data` — same TSV
format as nettalk.data, but rows are sorted: ranked entries first (in
ascending rank), then unranked entries in their original order.

Usage:
    uv run tools/sort_nettalk_by_frequency.py
"""

from __future__ import annotations

import sys
import urllib.request
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parent.parent
NETTALK_PATH = REPO_ROOT / "src" / "main" / "resources" / "nettalk" / "nettalk.data"
OUTPUT_PATH = REPO_ROOT / "src" / "main" / "resources" / "nettalk" / "nettalk_by_frequency.data"
FREQUENCY_URL = "https://raw.githubusercontent.com/first20hours/google-10000-english/refs/heads/master/20k.txt"
CACHE_PATH = Path("/tmp") / "google_20k.txt"


def fetch_frequency_list() -> list[str]:
    if not CACHE_PATH.exists():
        print(f"Downloading {FREQUENCY_URL} ...", file=sys.stderr)
        urllib.request.urlretrieve(FREQUENCY_URL, CACHE_PATH)
    return [w.strip().lower() for w in CACHE_PATH.read_text().splitlines() if w.strip()]


def is_data_line(parts: list[str]) -> bool:
    if len(parts) < 4:
        return False
    word, phonemes, stress = parts[0], parts[1], parts[2]
    if not word or not word.isalpha():
        return False
    if len(word) != len(phonemes) or len(word) != len(stress):
        return False
    flag = parts[3].strip()
    return flag.isdigit()


def main() -> None:
    freq = fetch_frequency_list()
    rank = {w: i for i, w in enumerate(freq)}

    raw = NETTALK_PATH.read_text().splitlines()
    data_lines = [line for line in raw if is_data_line(line.split("\t"))]
    print(f"NETtalk valid data lines: {len(data_lines)}", file=sys.stderr)
    print(f"Frequency list size:       {len(freq)}", file=sys.stderr)

    indexed = list(enumerate(data_lines))
    ranked = []
    unranked = []
    for orig_idx, line in indexed:
        word = line.split("\t", 1)[0].lower()
        r = rank.get(word)
        if r is None:
            unranked.append((orig_idx, line))
        else:
            ranked.append((r, orig_idx, line))

    ranked.sort(key=lambda t: (t[0], t[1]))
    unranked.sort(key=lambda t: t[0])

    print(f"Ranked entries (in 20k):   {len(ranked)}", file=sys.stderr)
    print(f"Unranked entries:          {len(unranked)}", file=sys.stderr)
    print(f"Top-100 sample:", file=sys.stderr)
    for r, _, line in ranked[:30]:
        word = line.split("\t", 1)[0]
        print(f"  rank {r:>5}  {word}", file=sys.stderr)

    out_lines = [line for _, _, line in ranked] + [line for _, line in unranked]
    OUTPUT_PATH.write_text("\n".join(out_lines) + "\n")
    print(f"Wrote {len(out_lines)} lines to {OUTPUT_PATH.relative_to(REPO_ROOT)}", file=sys.stderr)


if __name__ == "__main__":
    main()
