"""Summarizes run_headed.py output: medians over repeats of the Izhikevich network (per machine, network size,
simulator, representation, variant and mode) and of the machine learning demos (per machine, demo and mode): training,
inference and text generation."""
import collections, sys
from _common import load_latest, med


def measurement(r):
    """What a row measures, without its repeat: rows of one measurement are summarized together."""
    if "neurons" in r:
        return r["machine"], r["neurons"], r["simulator"], r.get("representation", "objects"), r["variant"], r["mode"]
    return r["machine"], r.get("benchmark"), r.get("size", ""), r.get("mode")


# Later files supersede earlier ones for the same measurement, as in summarize.py, so a rerun replaces its old rows
# rather than pooling with them
rows, errors = load_latest(sys.argv[1:], measurement)


print("-- Izhikevich network: wall s per simulated s, steps/s, network canvas frames/s, firing rate")
g = collections.defaultdict(list)
for r in rows:
    if "neurons" in r:
        g[measurement(r)].append(r)
order = {"headless": 0, "fast": 1, "guioff": 2, "paced": 3}
heading = None
for key in sorted(g, key=lambda k: (*k[:5], order[k[5]])):
    if key[:2] != heading:
        heading = key[:2]
        print(f"  {key[0]}, {key[1]} neurons")
    v = g[key]
    print(f"    {' / '.join(key[2:]):45s} n={len(v)}  rt {med([x['rt_factor'] for x in v]):9.3f}"
          f"  steps/s {med([x['iterations_per_s'] for x in v]):8.1f}  fps {med([x['frames_per_s'] for x in v]):6.1f}"
          f"  rate {med([x['rate_hz'] for x in v]):.2f} Hz  sim {v[0]['sim_ms']:.0f} ms")

print("\n-- Demo training (each demo's own settings): iterations/s, s per pass over the training data, canvas frames/s")
g = collections.defaultdict(list)
for r in rows:
    if "iterations" in r and "benchmark" in r:
        g[(r["machine"], r["benchmark"], r.get("size", ""), r["mode"])].append(r)
heading = None
for key in sorted(g, key=lambda k: (*k[:3], ["headless", "network", "dialog"].index(k[3]))):
    if key[:3] != heading:
        heading = key[:3]
        print(f"  {key[0]}, {key[1]} {key[2]}".rstrip())
    v = g[key]
    result = (f"test acc {med([x['test_acc'] for x in v]):.3f}" if "test_acc" in v[0]
              else f"train loss {med([x['train_loss'] for x in v]):.3f}")
    print(f"    {key[3]:10s} n={len(v)}  iterations/s {med([x['iterations_per_s'] for x in v]):8.1f}"
          f"  s per epoch-equivalent {med([x['epoch_equivalent_s'] for x in v]):7.3f}"
          f"  fps {med([x.get('frames_per_s', 0.0) for x in v]):6.1f}  {result}")

print("\n-- Demo inference (one example per workspace step): microseconds per example, canvas frames/s")
g = collections.defaultdict(list)
for r in rows:
    if "examples" in r and "benchmark" in r:
        g[(r["machine"], r["benchmark"], r["mode"])].append(r)
heading = None
for key in sorted(g, key=lambda k: (*k[:2], ["headless", "fast", "paced"].index(k[2]))):
    if key[:2] != heading:
        heading = key[:2]
        print(f"  {key[0]}, {key[1]}")
    v = g[key]
    print(f"    {key[2]:10s} n={len(v)}  us/example {med([x['us_per_example'] for x in v]):9.1f}"
          f"  examples/s {med([x['examples_per_s'] for x in v]):8.1f}  fps {med([x['frames_per_s'] for x in v]):6.1f}"
          f"  test acc {med([x['test_acc'] for x in v]):.3f}")

print("\n-- Demo text generation (one token per workspace step): milliseconds per token, tokens/s, canvas frames/s")
g = collections.defaultdict(list)
for r in rows:
    if "tokens" in r and "benchmark" in r:
        g[(r["machine"], r["size"], r["mode"])].append(r)
heading = None
for key in sorted(g, key=lambda k: (*k[:2], ["headless", "fast", "paced"].index(k[2]))):
    if key[:2] != heading:
        heading = key[:2]
        print(f"  {key[0]}, tiny LM {key[1]}")
    v = g[key]
    print(f"    {key[2]:10s} n={len(v)}  ms/token {med([x['ms_per_token'] for x in v]):8.3f}"
          f"  tokens/s {med([x['tokens_per_s'] for x in v]):8.1f}  fps {med([x['frames_per_s'] for x in v]):6.1f}")

if errors:
    print(f"\n{len(errors)} errors")
    for r in errors:
        print("  ", {k: r.get(k) for k in ["machine", "simulator", "mode", "variant", "representation"]}, r["error"][-200:])
