"""Runs the GUI-open ("headed") benchmarks of HEADED_SPEC.md on a machine with a live display and appends one JSON
line per run to an output file. Run it under `kde-inhibit --power --screenSaver` (or equivalent) so the display stays
on: a locked or powered-off display lets the compositor throttle drawing.

Usage:
  run_headed.py --machine NAME --out results.jsonl --java21 PATH --java25 PATH --s4cp CLASSPATH_FILE --s3dir DIR \
    [--repeats 3] [--suites s4,mnist,s3] [--heap 8g]

Suites: s4 and s3 (the 1000-neuron network in each version), mnist (the MNIST demo's training), large (Simbrain 4 at
8000 neurons with and without the GUI, display decoupled; needs a larger heap for free neurons, e.g. --heap 24g).
"""
import argparse, json, os, subprocess, time

p = argparse.ArgumentParser()
for a in ["machine", "out", "java21", "java25", "s4cp", "s3dir"]:
    p.add_argument("--" + a, required=True)
p.add_argument("--repeats", type=int, default=3)
p.add_argument("--suites", default="s4,mnist,s3")
p.add_argument("--heap", default="8g")
p.add_argument("--timeout", type=int, default=600)
args = p.parse_args()

s4cp = open(args.s4cp).read().strip()
OPENS = []
for pkg in ["java.base/java.util", "java.desktop/java.awt", "java.desktop/java.awt.geom", "java.base/java.util.concurrent",
            "java.base/java.util.concurrent.atomic", "java.base/java.lang"]:
    OPENS += ["--add-opens", pkg + "=ALL-UNNAMED"]

# Paced free-neuron runs draw every step and manage tens of steps per second, so they run a short window
PACED_OBJECTS = ("150", "25")
LONG = ("2000", "500")


def run(cmd, extra, cwd=None):
    started = time.time()
    try:
        out = subprocess.run(cmd, cwd=cwd, capture_output=True, text=True, timeout=args.timeout, stdin=subprocess.DEVNULL)
        lines = [l for l in out.stdout.splitlines() if l.startswith("{")]
        result = json.loads(lines[-1]) if lines else {"error": (out.stdout + out.stderr)[-800:]}
    except subprocess.TimeoutExpired:
        result = {"error": f"timeout after {args.timeout} s"}
    result.update(extra)
    result.update({"machine": args.machine, "wall_total_s": round(time.time() - started, 2),
                   "timestamp": time.strftime("%Y-%m-%dT%H:%M:%S"), "loadavg": os.getloadavg()[0]})
    with open(args.out, "a") as f:
        f.write(json.dumps(result) + "\n")
    print(json.dumps(result), flush=True)


def s4(main, *rest):
    return [args.java21, *OPENS, f"-Xmx{args.heap}", "-cp", s4cp, main, *rest]


def s4_suite():
    for rep in range(args.repeats):
        base = {"suite": "headed", "repeat": rep}
        for representation in ["objects", "arrays"]:
            for variant in ["network", "raster"]:
                for mode in ["headless", "paced", "fast"]:
                    sim, warm = PACED_OBJECTS if (mode, representation) == ("paced", "objects") else LONG
                    extra = ["-Djava.awt.headless=true"] if mode == "headless" else []
                    run([args.java21, *OPENS, *extra, f"-Xmx{args.heap}", "-cp", s4cp,
                         "org.simbrain.benchmarks.IzhikevichHeadedBenchmarkKt", mode, variant, representation, sim, warm],
                        base)


def mnist_suite():
    for rep in range(args.repeats):
        base = {"suite": "headed", "repeat": rep}
        for mode in ["headless", "network", "dialog"]:
            extra = ["-Djava.awt.headless=true"] if mode == "headless" else []
            run([args.java21, *OPENS, *extra, f"-Xmx{args.heap}", "-cp", s4cp, "org.simbrain.benchmarks.MnistHeadedBenchmarkKt",
                 mode, "2000", "200"], base)


def s3_suite():
    for rep in range(args.repeats):
        base = {"suite": "headed", "repeat": rep}
        seed = str(rep + 1)
        for variant in ["network", "raster"]:
            for mode, (sim, warm) in [("headless", LONG), ("paced", PACED_OBJECTS), ("guioff", LONG)]:
                if mode == "headless" and variant == "raster":
                    continue
                # guioff races a toolbar repaint against the paused event queue, so it gets extra repeats
                for _ in range(2 if mode == "guioff" else 1):
                    run([args.java25, f"-Xmx{args.heap}", "-cp", "bin:lib/*", "org.simbrain.benchmarks.IzhikevichHeadedBenchmark",
                         mode, variant, seed, sim, warm], base, cwd=args.s3dir)


def large_suite():
    # The size sweep's largest network (6.4 million synapses), weights scaled by 100 / in-degree like the sweep
    n, p_ = "8000", "0.1"
    scale = str(round(100.0 / ((int(n) - 1) * float(p_)), 6))
    for rep in range(args.repeats):
        base = {"suite": "headed", "repeat": rep}
        for representation, (sim, warm) in [("objects", ("1000", "250")), ("arrays", LONG)]:
            for mode in ["headless", "fast"]:
                extra = ["-Djava.awt.headless=true"] if mode == "headless" else []
                run([args.java21, *OPENS, *extra, f"-Xmx{args.heap}", "-cp", s4cp,
                     "org.simbrain.benchmarks.IzhikevichHeadedBenchmarkKt", mode, "network", representation, sim, warm,
                     n, p_, scale], base)


for suite in args.suites.split(","):
    {"s4": s4_suite, "mnist": mnist_suite, "s3": s3_suite, "large": large_suite}[suite]()
