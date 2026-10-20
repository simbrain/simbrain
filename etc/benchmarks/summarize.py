"""Summarizes benchmark results (medians over repeats) from one or more run_benchmarks.py output files."""
import collections, json, statistics as st, sys

# Later files supersede earlier ones for the same (machine, suite, simulator), so a rerun of one simulator replaces
# its old rows without discarding the raw data
by_file = [[json.loads(l) for l in open(f)] for f in sys.argv[1:]]
latest = {}
for index, file_rows in enumerate(by_file):
    for r in file_rows:
        latest[(r["machine"], r.get("suite"), r.get("simulator") or r.get("framework"))] = index
rows = [r for index, file_rows in enumerate(by_file) for r in file_rows
        if latest[(r["machine"], r.get("suite"), r.get("simulator") or r.get("framework"))] == index]


def med(values):
    return st.median(values) if values else float("nan")


def steady(epochs):
    # first epoch includes JIT warm-up (Simbrain) or allocator warm-up (PyTorch)
    return med(epochs[1:] if len(epochs) > 1 else epochs)


for machine in sorted({r["machine"] for r in rows}):
    print(f"\n=== {machine}")
    meta = next(r for r in rows if r["machine"] == machine and r.get("suite") == "meta")
    print(meta["platform"], meta["cores"], "logical cores |", meta["java21"], "|", meta["java25"])

    print("\n-- headline (1000 neurons, p=0.1, 10 s simulated): wall s per simulated s")
    g = collections.defaultdict(list)
    for r in rows:
        if r["machine"] == machine and r.get("suite") == "headline":
            g[(r["simulator"], r.get("workers"))].append(r)
    for (sim, workers), v in sorted(g.items(), key=lambda kv: med([x["rt_factor"] for x in kv[1]])):
        label = sim + (f" ({workers} workers)" if sim == "simbrain-objects" else "")
        print(f"  {label:36s} {med([x['rt_factor'] for x in v]):9.4f}   rate {med([x['rate_hz'] for x in v]):.2f} Hz"
              f"   build {med([x['build_s'] for x in v]):.2f} s")

    print("\n-- threads (Simbrain 4 objects): wall s per simulated s by workers")
    g = collections.defaultdict(list)
    for r in rows:
        if r["machine"] == machine and r.get("suite") == "threads":
            g[(r["neurons"], r["p"], r["workers"])].append(r["rt_factor"])
    for net in sorted({k[:2] for k in g}):
        base = med(g[(*net, 1)])
        cells = [f"{w}:{med(v):.3f} ({base / med(v):.1f}x)" for (n, p, w), v in sorted(g.items()) if (n, p) == net]
        print(f"  {net[0]} neurons p={net[1]}: " + "  ".join(cells))

    print("\n-- sweep: wall s per simulated s (median of 3), firing rate in brackets")
    sims = ["simbrain3-concurrent", "simbrain-objects", "simbrain-arrays", "brian2-runtime", "brian2-cpp_standalone"]
    g = collections.defaultdict(list)
    for r in rows:
        if r["machine"] == machine and r.get("suite") == "sweep":
            g[(r["neurons"], r["p"], r["simulator"])].append(r)
    print("  " + f"{'neurons':>7} {'p':>5} {'synapses':>9} " + " ".join(f"{s[:22]:>24}" for s in sims))
    for n, p in sorted({k[:2] for k in g}):
        syn = med([x["synapses"] for s in sims for x in g.get((n, p, s), [])])
        cells = []
        for s in sims:
            v = g.get((n, p, s), [])
            cells.append(f"{med([x['rt_factor'] for x in v]):9.4f} [{med([x['rate_hz'] for x in v]):5.2f}]" if v else f"{'-':>24}")
        print(f"  {n:>7} {p:>5} {int(syn):>9} " + " ".join(f"{c:>24}" for c in cells))

    print("\n-- training: steady-state s per epoch (median over repeats), final accuracy or loss")
    g = collections.defaultdict(list)
    for r in rows:
        if r["machine"] == machine and r.get("suite") == "training":
            key = (r["framework"], r.get("arch") or str(r.get("config")))
            g[key].append(r)
    for (fw, arch), v in sorted(g.items(), key=lambda kv: (kv[0][1], kv[0][0])):
        quality = (f"acc {med([x['test_acc'] for x in v]):.3f}" if "test_acc" in v[0]
                   else f"loss {med([x['final_train_loss'] for x in v]):.3f}")
        print(f"  {arch:22s} {fw:36s} {med([steady(x['epoch_s']) for x in v]):8.3f} s   {quality}")

    print("\n-- inference: test set of 1000, median over repeats of the per-pass median; batch 1 unless noted")
    g = collections.defaultdict(list)
    for r in rows:
        if r["machine"] == machine and r.get("suite") == "inference" and "flavor" in r:
            g[(r["flavor"], r["framework"], r["mode"])].append(r)
    for flavor in ["mlp", "cnn"]:
        for (f, fw, mode), v in sorted((kv for kv in g.items() if kv[0][0] == flavor), key=lambda kv: kv[0][1:]):
            print(f"  {flavor:4s} {fw:36s} {mode:8s} {med([x['us_per_example'] for x in v]):8.2f} us/example"
                  f"  {med([x['examples_per_s'] for x in v]):9.0f} examples/s  acc {med([x['test_acc'] for x in v]):.3f}")
