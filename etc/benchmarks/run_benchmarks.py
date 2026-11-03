"""Runs the Simbrain 4 paper benchmark suites and appends one JSON line per run to an output file.

Suites: headline (1000-neuron Izhikevich network across simulators), sweep (neurons x connection density, weights
scaled by 100 / in-degree so firing rates stay comparable), threads (Simbrain 4 worker scaling), training (MLP, CNN,
tiny language model vs PyTorch), inference (MLP and CNN test-set inference vs PyTorch).

Usage:
  run_benchmarks.py --machine NAME --out results.jsonl --suites headline,sweep,threads,training \
    --java21 PATH --s4cp CLASSPATH_FILE --s4repo DIR --s3dir DIR --python PATH --bench DIR --data DIR \
    --heap 24g [--s4-only]
"""
import argparse, os, subprocess
from _common import DT, TINY_LM_CONFIGS, record_meta, run_and_record, sweep_scale, tinylm_ids_path

p = argparse.ArgumentParser()
for a in ["machine", "out", "suites", "java21", "s4cp", "s4repo", "s3dir", "python", "bench", "data", "heap"]:
    p.add_argument("--" + a, required=True)
p.add_argument("--timeout", type=int, default=900)
# Only the Simbrain 4 runs, for refreshing its rows after code changes; summarize.py lets them supersede older rows
p.add_argument("--s4-only", action="store_true")
args = p.parse_args()

s4cp = open(args.s4cp).read().strip()
cores = os.cpu_count()


def run(cmd, extra, cwd=None):
    # A Simbrain 4 run is one on Simbrain 4's classpath; Simbrain 3 runs on the same JDK, so the executable won't tell
    if args.s4_only and s4cp not in cmd:
        return
    run_and_record(cmd, extra, args.out, args.machine, args.timeout, cwd=cwd)


def s4(main, *rest, headless=False, workers=None):
    """A Simbrain 4 harness; workers limits the network update pool by the JVM's processor count."""
    jvm = [args.java21, f"-Xmx{args.heap}"]
    if headless:
        jvm.append("-Djava.awt.headless=true")
    if workers is not None:
        jvm.append(f"-XX:ActiveProcessorCount={workers + 1}")
    return jvm + ["-cp", s4cp, "org.simbrain.benchmarks." + main, *map(str, rest)]


def s3(main, *rest):
    """A Simbrain 3 harness, run from its worktree. Same JDK as Simbrain 4: a newer JIT alone made Simbrain 3 about 10%
    faster."""
    return [args.java21, f"-Xmx{args.heap}", "-cp", "bin:lib/*", "org.simbrain.benchmarks." + main, *map(str, rest)]


def python(script, *rest):
    return [args.python, os.path.join(args.bench, script), *map(str, rest)]


def s4_izh(variant, n, p_, sim, warm, seed, scale, workers=None):
    return s4("IzhikevichBenchmarkKt", variant, n, p_, DT, sim, warm, seed, scale, workers=workers)


def s3_izh(mode, n, p_, sim, warm, seed, scale):
    return s3("IzhikevichBenchmark", mode, n, p_, DT, sim, warm, seed, scale)


def brian(target, n, p_, sim, warm, seed, scale):
    return python("izh_brian2.py", target, n, p_, DT, sim, warm, seed, scale)


def headline():
    n, p_, sim, warm = 1000, 0.1, 10000, 1000
    # Brian2's runtime mode compiles Cython on first use; one discarded run fills its cache
    run(brian("runtime", n, p_, 200, 0, 0, 1.0), {"suite": "warmup"})
    for rep in range(5):
        base = {"suite": "headline", "repeat": rep}
        run(s3_izh("serial", n, p_, sim, warm, rep + 1, 1.0), base, cwd=args.s3dir)
        run(s3_izh("concurrent", n, p_, sim, warm, rep + 1, 1.0), base, cwd=args.s3dir)
        run(s4_izh("objects", n, p_, sim, warm, rep + 1, 1.0), base)
        run(s4_izh("objects", n, p_, sim, warm, rep + 1, 1.0, workers=1), base)
        run(s4_izh("arrays", n, p_, sim, warm, rep + 1, 1.0), base)
        run(brian("runtime", n, p_, sim, warm, rep + 1, 1.0), base)
        run(brian("cpp_standalone", n, p_, sim, warm, rep + 1, 1.0), base)


def sweep():
    sim, warm = 2000, 500
    run(brian("runtime", 1000, 0.1, 200, 0, 0, 1.0), {"suite": "warmup"})
    for n in [500, 1000, 2000, 4000, 8000]:
        for p_ in [0.01, 0.05, 0.1]:
            scale = sweep_scale(n, p_)
            for rep in range(3):
                base = {"suite": "sweep", "repeat": rep}
                run(s3_izh("concurrent", n, p_, sim, warm, rep + 1, scale), base, cwd=args.s3dir)
                run(s4_izh("objects", n, p_, sim, warm, rep + 1, scale), base)
                run(s4_izh("arrays", n, p_, sim, warm, rep + 1, scale), base)
                run(brian("runtime", n, p_, sim, warm, rep + 1, scale), base)
                run(brian("cpp_standalone", n, p_, sim, warm, rep + 1, scale), base)


def threads():
    counts = sorted(c for c in {1, 2, 4, 8, 16, cores - 1, cores} if 1 <= c <= cores)
    for n, p_ in [(1000, 0.1), (4000, 0.05)]:
        scale = sweep_scale(n, p_)
        for workers in counts:
            for rep in range(3):
                run(s4_izh("objects", n, p_, 2000, 500, rep + 1, scale, workers=workers),
                    {"suite": "threads", "repeat": rep})


def training():
    text = os.path.join(args.s4repo, "simulations", "texts", "casual_texting.txt")
    results_dir = os.path.dirname(os.path.abspath(args.out))
    for rep in range(3):
        base = {"suite": "training", "repeat": rep}
        run(s4("MnistBenchmarkKt", args.data, 6, rep + 1), base)
        run(s4("CnnBenchmarkKt", 6, rep + 1, headless=True), base)
        # Simbrain computes in double precision, so PyTorch runs in both
        for threads_ in [1, cores]:
            for dtype in ["float32", "float64"]:
                run(python("mnist_torch.py", args.data, 6, rep + 1, threads_, dtype), base)
                run(python("cnn_torch.py", args.data, 6, rep + 1, threads_, dtype), base)
        for cfg in TINY_LM_CONFIGS:
            ids = tinylm_ids_path(results_dir, cfg)
            run(s4("TinyLmBenchmarkKt", text, ids, 4, *cfg), base, cwd=args.s4repo)
            for threads_ in [1, cores]:
                run(python("tinylm_torch.py", ids, 4, threads_, 1, *cfg), base)


def inference():
    for rep in range(3):
        base = {"suite": "inference", "repeat": rep}
        for flavor in ["mlp", "cnn"]:
            run(s4("MnistInferenceBenchmarkKt", flavor, args.data, 2, 10, rep + 1, headless=True), base)
            for threads_ in [1, cores]:
                for dtype in ["float32", "float64"]:
                    run(python("mnist_infer_torch.py", flavor, args.data, 2, 10, rep + 1, threads_, dtype), base)


def arrays():
    # Simbrain 4 neuron arrays only: the headline network and every sweep size, after the step-responder fast path
    for rep in range(5):
        run(s4_izh("arrays", 1000, 0.1, 10000, 1000, rep + 1, 1.0), {"suite": "headline", "repeat": rep})
    for n in [500, 1000, 2000, 4000, 8000]:
        for p_ in [0.01, 0.05, 0.1]:
            scale = sweep_scale(n, p_)
            for rep in range(3):
                run(s4_izh("arrays", n, p_, 2000, 500, rep + 1, scale), {"suite": "sweep", "repeat": rep})


def smoke():
    base = {"suite": "smoke"}
    run(s3_izh("concurrent", 200, 0.1, 100, 50, 1, 5.0), base, cwd=args.s3dir)
    run(s4_izh("objects", 200, 0.1, 100, 50, 1, 5.0, workers=2), base)
    run(s4_izh("arrays", 200, 0.1, 100, 50, 1, 5.0), base)
    run(brian("runtime", 200, 0.1, 100, 50, 1, 5.0), base)
    run(brian("cpp_standalone", 200, 0.1, 100, 50, 1, 5.0), base)
    run(s4("MnistBenchmarkKt", args.data, 1, 1), base)
    run(python("cnn_torch.py", args.data, 1, 1, 1, "float32"), base)


record_meta(args.out, args.machine, s3dir=args.s3dir,
            java21=subprocess.run([args.java21, "-version"], capture_output=True, text=True).stderr.splitlines()[0])
for suite in args.suites.split(","):
    {"smoke": smoke, "arrays": arrays, "headline": headline, "sweep": sweep, "threads": threads, "training": training,
     "inference": inference}[suite]()
