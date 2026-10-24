"""Runs every PyTorch comparison script at several thread counts, to find PyTorch's best thread count on small models
(more threads is slower past a point). Appends each script's JSON line, tagged with suite "torch_threads", to --out.

Usage: torch_threads.py --machine NAME --out results.jsonl --python PATH --bench DIR --data DIR --threads 1,2,4,8 [--repeats 3]
"""
import argparse, json, os, subprocess, time

p = argparse.ArgumentParser()
for a in ["machine", "out", "python", "bench", "data", "threads"]:
    p.add_argument("--" + a, required=True)
p.add_argument("--repeats", type=int, default=3)
args = p.parse_args()
results_dir = os.path.dirname(os.path.abspath(args.out))
tiny_configs = [("24", "20", "4", "30", "1"), ("64", "128", "4", "512", "2")]


def run(cmd, extra):
    out = subprocess.run(cmd, capture_output=True, text=True, timeout=1200, stdin=subprocess.DEVNULL)
    lines = [l for l in out.stdout.splitlines() if l.startswith("{")]
    rows = [json.loads(l) for l in lines] or [{"error": (out.stdout + out.stderr)[-800:]}]
    with open(args.out, "a") as f:
        for r in rows:
            r.update(extra)
            r.update({"suite": "torch_threads", "machine": args.machine, "timestamp": time.strftime("%Y-%m-%dT%H:%M:%S")})
            f.write(json.dumps(r) + "\n")


script = lambda name: os.path.join(args.bench, name)
for rep in range(args.repeats):
    for t in [int(x) for x in args.threads.split(",")]:
        base = {"repeat": rep, "threads": t}
        for dtype in ["float32", "float64"]:
            run([args.python, script("mnist_torch.py"), args.data, "4", str(rep + 1), str(t), dtype], {**base, "model": "mlp", "task": "train"})
            run([args.python, script("cnn_torch.py"), args.data, "4", str(rep + 1), str(t), dtype], {**base, "model": "cnn", "task": "train"})
            for flavor in ["mlp", "cnn"]:
                run([args.python, script("mnist_infer_torch.py"), flavor, args.data, "1", "5", str(rep + 1), str(t), dtype],
                    {**base, "model": flavor, "task": "infer"})
        for cfg in tiny_configs:
            ids = os.path.join(results_dir, f"tinylm_ids_{'_'.join(cfg)}.txt")
            run([args.python, script("tinylm_torch.py"), ids, "4", str(t), "1", *cfg],
                {**base, "model": "tinylm-" + cfg[1], "task": "train"})
print("ALL_DONE")
