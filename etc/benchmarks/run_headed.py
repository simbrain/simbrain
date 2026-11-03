"""Runs the GUI-open ("headed") benchmarks of HEADED_SPEC.md on a machine with a live display and appends one JSON
line per run to an output file. Run it under `kde-inhibit --power --screenSaver` (or equivalent) so the display stays
on: a locked or powered-off display lets the compositor throttle drawing.

Usage:
  run_headed.py --machine NAME --out results.jsonl --java21 PATH --s4cp CLASSPATH_FILE --s3dir DIR \
    [--s4repo DIR] [--repeats 3] [--suites s4,training,inference,s3] [--heap 8g]

Suites: s4 and s3 (the 1000-neuron network in each version), training (the MLP, CNN and tiny language model demos
training), inference (the MNIST demos classifying their test set), generation (the tiny language model generating
text), large (Simbrain 4 at 8000 neurons with and without the GUI, display decoupled; needs a larger heap for free
neurons, e.g. --heap 24g), large_paced (Simbrain 4 drawing every step) and large_s3 (Simbrain 3), each of the large
suites at the network sizes given by --sizes (default 8000).
"""
import argparse
from _common import DT, TINY_LM_CONFIGS, record_meta, run_and_record, sweep_scale

p = argparse.ArgumentParser()
for a in ["machine", "out", "java21", "s4cp", "s3dir"]:
    p.add_argument("--" + a, required=True)
p.add_argument("--s4repo", default=".", help="Simbrain 4 checkout; the tiny language model reads its text from there")
p.add_argument("--repeats", type=int, default=3)
p.add_argument("--suites", default="s4,training,inference,s3")
p.add_argument("--heap", default="8g")
p.add_argument("--sizes", default="8000", help="network sizes for the large suites, comma separated")
p.add_argument("--timeout", type=int, default=600)
args = p.parse_args()

s4cp = open(args.s4cp).read().strip()
OPENS = []
for pkg in ["java.base/java.util", "java.desktop/java.awt", "java.desktop/java.awt.geom", "java.base/java.util.concurrent",
            "java.base/java.util.concurrent.atomic", "java.base/java.lang"]:
    OPENS += ["--add-opens", pkg + "=ALL-UNNAMED"]

# The 1000-neuron demo network
DEMO_N, DEMO_P = 1000, 0.1
# Paced free-neuron runs draw every step and manage tens of steps per second, so they run a short window
PACED_OBJECTS = (150, 25)
LONG = (2000, 500)


def run(cmd, extra, cwd=None):
    # The last line is the harness's result
    run_and_record(cmd, extra, args.out, args.machine, args.timeout, cwd=cwd, last_line_only=True)


def s4(main, *rest, headless=False):
    """A Simbrain 4 harness; headless runs without the desktop."""
    extra = ["-Djava.awt.headless=true"] if headless else []
    return [args.java21, *OPENS, *extra, f"-Xmx{args.heap}", "-cp", s4cp, "org.simbrain.benchmarks." + main,
            *map(str, rest)]


def s4_izh(mode, variant, representation, n, p_, sim, warm, seed, scale=1.0):
    return s4("IzhikevichHeadedBenchmarkKt", mode, variant, representation, n, p_, DT, sim, warm, seed, scale,
              headless=mode == "headless")


def s3_izh(mode, variant, n, p_, sim, warm, seed, scale=1.0):
    """The Simbrain 3 harness, run from its worktree."""
    return [args.java21, f"-Xmx{args.heap}", "-cp", "bin:lib/*", "org.simbrain.benchmarks.IzhikevichHeadedBenchmark",
            *map(str, [mode, variant, n, p_, DT, sim, warm, seed, scale])]


def s4_suite():
    for rep in range(args.repeats):
        base = {"suite": "headed", "repeat": rep}
        for representation in ["objects", "arrays"]:
            for variant in ["network", "raster"]:
                for mode in ["headless", "paced", "fast"]:
                    sim, warm = PACED_OBJECTS if (mode, representation) == ("paced", "objects") else LONG
                    run(s4_izh(mode, variant, representation, DEMO_N, DEMO_P, sim, warm, rep + 1), base)


def training_suite():
    # Each demo as a user trains it: without the GUI, with its network window open, and with its training dialog too.
    # The tiny language model at the demo's size and at the larger size of the headless comparison.
    demo_lm, large_lm = (",".join(cfg) for cfg in TINY_LM_CONFIGS)
    demos = [("mlp", 2000, 200), ("cnn", 2000, 200), ("tinylm", 100, 10, demo_lm), ("tinylm", 10, 2, large_lm)]
    for rep in range(args.repeats):
        base = {"suite": "headed", "repeat": rep}
        for demo in demos:
            for mode in ["headless", "network", "dialog"]:
                run(s4("TrainingHeadedBenchmarkKt", demo[0], mode, *demo[1:], headless=mode == "headless"), base,
                    cwd=args.s4repo)


def inference_suite():
    # The test set one example per workspace step; drawing every example gets a shorter run
    for rep in range(args.repeats):
        base = {"suite": "headed", "repeat": rep}
        for demo in ["mlp", "cnn"]:
            for mode, (examples, warm) in [("headless", (10000, 5000)), ("fast", (10000, 5000)), ("paced", (1000, 200))]:
                run(s4("InferenceHeadedBenchmarkKt", demo, mode, examples, warm, headless=mode == "headless"), base)


def s3_suite():
    for rep in range(args.repeats):
        base = {"suite": "headed", "repeat": rep}
        for variant in ["network", "raster"]:
            for mode, (sim, warm) in [("headless", LONG), ("paced", PACED_OBJECTS), ("guioff", LONG)]:
                if mode == "headless" and variant == "raster":
                    continue
                # guioff races a toolbar repaint against the paused event queue, so it gets extra repeats
                for _ in range(2 if mode == "guioff" else 1):
                    run(s3_izh(mode, variant, DEMO_N, DEMO_P, sim, warm, rep + 1), base, cwd=args.s3dir)


# Larger networks are the size sweep's at p = 0.1, weights scaled by 100 / in-degree like the sweep
LARGE_P = 0.1
# Drawing every step of a free-neuron network larger than 1000 neurons manages a few steps per second
PACED_LARGE = (50, 10)


def paced_window(n):
    return PACED_OBJECTS if int(n) <= 1000 else PACED_LARGE


def generation_suite():
    # The tiny language model generating one token per workspace step, at the demo's size and the larger one
    demo_lm, large_lm = (",".join(cfg) for cfg in TINY_LM_CONFIGS)
    sizes = [(demo_lm, 20, (5000, 500), (1000, 200)), (large_lm, 3, (1000, 100), (300, 100))]
    for rep in range(args.repeats):
        base = {"suite": "headed", "repeat": rep}
        for size, train, long, paced in sizes:
            for mode in ["headless", "fast", "paced"]:
                tokens, warm = paced if mode == "paced" else long
                run(s4("GenerationHeadedBenchmarkKt", mode, tokens, warm, train, size, headless=mode == "headless"),
                    base, cwd=args.s4repo)


def large_suite():
    # Without and with the GUI, display decoupled. Free neurons simulate 1 s at 8000 neurons (6.4 million synapses),
    # where a simulated second takes about a minute.
    for rep in range(args.repeats):
        base = {"suite": "headed", "repeat": rep}
        for n in args.sizes.split(","):
            objects = (1000, 250) if int(n) >= 8000 else LONG
            for representation, (sim, warm) in [("objects", objects), ("arrays", LONG)]:
                for mode in ["headless", "fast"]:
                    run(s4_izh(mode, "network", representation, n, LARGE_P, sim, warm, rep + 1,
                               sweep_scale(n, LARGE_P)), base)


def large_paced_suite():
    # Simbrain 4 drawing every step at the --sizes
    for rep in range(args.repeats):
        base = {"suite": "headed", "repeat": rep}
        for n in args.sizes.split(","):
            for representation, (sim, warm) in [("objects", paced_window(n)), ("arrays", LONG)]:
                run(s4_izh("paced", "network", representation, n, LARGE_P, sim, warm, rep + 1,
                           sweep_scale(n, LARGE_P)), base)


def large_s3_suite():
    # Simbrain 3 without the GUI, drawing every step, and GUI off, at the --sizes
    for rep in range(args.repeats):
        base = {"suite": "headed", "repeat": rep}
        for n in args.sizes.split(","):
            for mode, (sim, warm) in [("headless", LONG), ("paced", paced_window(n)), ("guioff", LONG)]:
                for _ in range(2 if mode == "guioff" else 1):
                    run(s3_izh(mode, "network", n, LARGE_P, sim, warm, rep + 1, sweep_scale(n, LARGE_P)), base,
                        cwd=args.s3dir)


record_meta(args.out, args.machine, s3dir=args.s3dir)
for suite in args.suites.split(","):
    {"s4": s4_suite, "training": training_suite, "inference": inference_suite, "s3": s3_suite,
     "generation": generation_suite, "large": large_suite, "large_paced": large_paced_suite, "large_s3": large_s3_suite}[suite]()
