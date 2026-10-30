"""Summarizes torch_threads.py output: medians over repeats per model, task, precision and thread count, and the best
thread count. Training is seconds per epoch (median of epochs after the first); inference is microseconds per example.

Usage: summarize_torch_threads.py results.jsonl [...]
"""
import collections, sys
from _common import load_latest, med, steady


def measurement(r):
    """What a row measures, without its repeat: rows of one measurement are summarized together."""
    # Rows from before the scripts recorded dtype carry it only in the framework label
    dtype = r.get("dtype") or ("float32" if r["model"].startswith("tinylm") else r["framework"].rsplit("-", 1)[-1])
    task = "train" if r["task"] == "train" else "infer-" + r["mode"]
    return r["machine"], r["model"], task, dtype, r["threads"]


# Later files supersede earlier ones for the same measurement, as in summarize.py, so a rerun replaces its old rows
# rather than pooling with them
rows, errors = load_latest(sys.argv[1:], measurement)
g = collections.defaultdict(list)
for r in rows:
    if r.get("suite") == "meta":
        continue
    g[measurement(r)].append(steady(r["epoch_s"]) if r["task"] == "train" else r["us_per_example"])

series = collections.defaultdict(dict)
for (machine, model, task, dtype, threads), values in g.items():
    series[(machine, model, task, dtype)][threads] = med(values)
for key in sorted(series):
    by_threads = series[key]
    best = min(by_threads, key=by_threads.get)
    cells = "  ".join(f"{t}:{by_threads[t]:.4g}" for t in sorted(by_threads))
    print(f"{key[0]:10} {key[1]:12} {key[2]:13} {key[3]:8} {cells}   best {best} ({by_threads[best]:.4g})")
print(f"{len(errors)} errors")
for r in errors:
    print("  ", r.get("model"), r.get("task"), r.get("threads"), r["error"][-200:])
