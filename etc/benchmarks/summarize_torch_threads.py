"""Summarizes torch_threads.py output: medians over repeats per model, task, precision and thread count, and the best
thread count. Training is seconds per epoch (median of epochs after the first); inference is microseconds per example.

Usage: summarize_torch_threads.py results.jsonl [...]
"""
import collections, json, statistics as st, sys

rows = [json.loads(l) for f in sys.argv[1:] for l in open(f)]
errors = [r for r in rows if "error" in r]
g = collections.defaultdict(list)
for r in rows:
    if "error" in r:
        continue
    dtype = r.get("dtype") or ("float32" if r["model"].startswith("tinylm") else r["framework"].rsplit("-", 1)[-1])
    if r["task"] == "train":
        epochs = r.get("epoch_s") or r.get("epochs_s")
        g[(r["machine"], r["model"], "train", dtype, r["threads"])].append(st.median(epochs[1:]))
    else:
        g[(r["machine"], r["model"], "infer-" + r["mode"], dtype, r["threads"])].append(r["us_per_example"])

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
