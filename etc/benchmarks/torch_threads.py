"""Runs every PyTorch comparison script at several thread counts, to find PyTorch's best thread count on small models
(more threads is slower past a point). Appends each script's JSON line, tagged with suite "torch_threads", to --out.

Usage: torch_threads.py --machine NAME --out results.jsonl --python PATH --bench DIR --data DIR --threads 1,2,4,8 [--repeats 3]
"""
import argparse, os
from _common import TINY_LM_CONFIGS, record_meta, run_and_record, tinylm_ids_path

p = argparse.ArgumentParser()
for a in ["machine", "out", "python", "bench", "data", "threads"]:
    p.add_argument("--" + a, required=True)
p.add_argument("--repeats", type=int, default=3)
p.add_argument("--timeout", type=int, default=1200)
args = p.parse_args()
results_dir = os.path.dirname(os.path.abspath(args.out))


def run(script, *rest, extra):
    # One hung run is recorded and the sweep goes on, as in run_benchmarks.py
    run_and_record([args.python, os.path.join(args.bench, script), *map(str, rest)], {**extra, "suite": "torch_threads"},
                   args.out, args.machine, args.timeout)


record_meta(args.out, args.machine)
for rep in range(args.repeats):
    for t in [int(x) for x in args.threads.split(",")]:
        base = {"repeat": rep, "threads": t}
        for dtype in ["float32", "float64"]:
            run("mnist_torch.py", args.data, 4, rep + 1, t, dtype, extra={**base, "model": "mlp", "task": "train"})
            run("cnn_torch.py", args.data, 4, rep + 1, t, dtype, extra={**base, "model": "cnn", "task": "train"})
            for flavor in ["mlp", "cnn"]:
                run("mnist_infer_torch.py", flavor, args.data, 1, 5, rep + 1, t, dtype,
                    extra={**base, "model": flavor, "task": "infer"})
        for cfg in TINY_LM_CONFIGS:
            run("tinylm_torch.py", tinylm_ids_path(results_dir, cfg), 4, t, 1, *cfg,
                extra={**base, "model": "tinylm-" + cfg[1], "task": "train"})
