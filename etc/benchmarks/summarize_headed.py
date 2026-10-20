"""Summarizes run_headed.py output: medians over repeats per machine, network size, simulator, representation, variant
and mode."""
import collections, json, statistics as st, sys

rows = [json.loads(l) for f in sys.argv[1:] for l in open(f)]
errors = [r for r in rows if "error" in r]
rows = [r for r in rows if "error" not in r]


def med(values):
    return st.median(values) if values else float("nan")


print("-- Izhikevich network: wall s per simulated s, steps/s, network canvas frames/s, firing rate")
g = collections.defaultdict(list)
for r in rows:
    if "neurons" in r:
        g[(r["machine"], r["neurons"], r["simulator"], r.get("representation", "objects"), r["variant"], r["mode"])].append(r)
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

print("\n-- Tiny MNIST demo training (batches of 35, test pass every 10 iterations)")
g = collections.defaultdict(list)
for r in rows:
    if r.get("benchmark") == "tiny-mnist-demo":
        g[(r["machine"], r["mode"])].append(r)
for machine in sorted({k[0] for k in g}):
    print(f"  {machine}")
    for mode in ["headless", "network", "dialog"]:
        v = g.get((machine, mode), [])
        if v:
            print(f"    {mode:10s} n={len(v)}  iterations/s {med([x['iterations_per_s'] for x in v]):7.1f}"
                  f"  s per epoch-equivalent {med([x['epoch_equivalent_s'] for x in v]):6.3f}"
                  f"  test acc {med([x['test_acc'] for x in v]):.3f}")

if errors:
    print(f"\n{len(errors)} errors")
    for r in errors:
        print("  ", {k: r.get(k) for k in ["machine", "simulator", "mode", "variant", "representation"]}, r["error"][-200:])
