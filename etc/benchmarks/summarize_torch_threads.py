"""Summarizes torch_threads.py output: medians over repeats per model, task, precision and thread count, and the best
thread count. Training is seconds per epoch (median of epochs after the first); inference is microseconds per example.

Usage: summarize_torch_threads.py results.jsonl [...]
"""
import collections, json, statistics as st, sys



def measurement(r):
    """What a row measures, without its repeat: rows of one measurement are summarized together."""
    dtype = r.get("dtype") or ("float32" if r["model"].startswith("tinylm") else r["framework"].rsplit("-", 1)[-1])
    task = "train" if r["task"] == "train" else "infer-" + r["mode"]
    return r["machine"], r["model"], task, dtype, r["threads"]


# Later files supersede earlier ones for the same measurement, as in summarize.py, so a rerun replaces its old rows
# rather than pooling with them
by_file = [[json.loads(l) for l in open(f)] for f in sys.argv[1:]]
errors = [r for file_rows in by_file for r in file_rows if "error" in r]
latest = {}
for index, file_rows in enumerate(by_file):
    for r in file_rows:
        if "error" not in r:
            latest[measurement(r)] = index
g = collections.defaultdict(list)
for index, file_rows in enumerate(by_file):
    for r in file_rows:
        if "error" in r or latest[measurement(r)] != index:
            continue
        if r["task"] == "train":
            epochs = r.get("epoch_s") or r.get("epochs_s")
            g[measurement(r)].append(st.median(epochs[1:]))
        else:
            g[measurement(r)].append(r["us_per_example"])

series = collections.defaultdict(dict)
for (machine, model, task, dtype, threads), values in g.items():
    series[(machine, model, task, dtype)][threads] = st.median(values)
for key in sorted(series):
    by_threads = series[key]
    best = min(by_threads, key=by_threads.get)
    cells = "  ".join(f"{t}:{by_threads[t]:.4g}" for t in sorted(by_threads))
    print(f"{key[0]:10} {key[1]:12} {key[2]:13} {key[3]:8} {cells}   best {best} ({by_threads[best]:.4g})")
print(f"{len(errors)} errors")
for r in errors:
    print("  ", r.get("model"), r.get("task"), r.get("threads"), r["error"][-200:])
