# Simbrain 4 performance benchmarks

The benchmarks behind the performance comparison in the Simbrain 4 paper: Simbrain 4 against Simbrain 3 and Brian2 on
a spiking network, against PyTorch on the two MNIST demos and the tiny language model, and with the GUI open against
Simbrain 3.

## Requirements

- macOS or Linux (the Simbrain 3 classpath uses `:` separators)
- git, with this repository's history (the Simbrain 3 runs use the `Simbrain3` branch)
- JDK 21, for building and running both Simbrain 4 and Simbrain 3
- [uv](https://docs.astral.sh/uv/), which installs Python 3.12, PyTorch and Brian2 from `uv.lock`
- A C++ compiler (Xcode command line tools, or gcc/clang) for Brian2's compiled modes
- A display, only for `bench headed`

## Setup

From the repository root:

```sh
etc/benchmarks/bench setup --java21 /path/to/jdk-21
```

This puts everything under `build/benchmarks/`:

1. **Simbrain 4:** compiles this checkout with Gradle, harnesses included, and writes the test runtime classpath to
   `s4cp.txt`. The Simbrain 4 harnesses live in `src/test/kotlin/org/simbrain/benchmarks/`.
2. **Simbrain 3:** checks out commit `e274a943b` of the `Simbrain3` branch as a git worktree in `simbrain3/`,
   fetching the branch if needed. It copies the two harnesses from `simbrain3/` here into the worktree's
   `src/org/simbrain/benchmarks/`, then compiles. Simbrain 3's own sources are not changed.
3. **Data:** downloads Tiny MNIST into `data/`, checks the files against the ones the published results used, and
   copies them into Simbrain's download cache, which the CNN and demo harnesses read.

Rerunning setup rebuilds both Simbrains and keeps the data. `./gradlew clean` deletes `build/`, worktree included;
run `git worktree prune` afterwards, then setup again.

## Running

```sh
etc/benchmarks/bench run --machine my-laptop                 # headless suites
etc/benchmarks/bench headed --machine my-laptop              # GUI open; needs a display
etc/benchmarks/bench torch-threads --machine my-laptop       # PyTorch at several thread counts; after `run`
etc/benchmarks/bench summarize                               # medians of everything in build/benchmarks/results
```

`--machine` is recorded in every result. Each command appends to a dated file in `build/benchmarks/results/`; pass
`--out` to choose the file. Each file starts with a meta row recording the platform, the command line, the Simbrain 4
commit (and whether tracked files had uncommitted changes) and the Simbrain 3 commit, and each result records its seed
and any `-Dbench.*` diagnostic switch it ran with (see the harness headers; none is set by these commands).

| Command | Suite | What it times | Time on a 16-core desktop / 10-core laptop |
|---|---|---|---|
| `run` | `headline` | 1000-neuron Izhikevich network, 10 s simulated: Simbrain 3 (serial, concurrent), Simbrain 4 (objects on all workers and on one, arrays), Brian2 (runtime, C++ standalone) | 13 / 20 min |
| | `threads` | Simbrain 4 objects at 1, 2, 4, 8 and 16 workers, one per core but one (the default) and one per core, 1000 and 4000 neurons | 32 / 40 min |
| | `training` | MLP and CNN on Tiny MNIST and the tiny language model, Simbrain against PyTorch (1 thread and all, float32 and float64) | 4 / 4 min |
| | `inference` | MLP and CNN test set one example at a time; PyTorch also as one batch | 1 / 2 min |
| | `sweep` | 500 to 8000 neurons at connection probability 0.01, 0.05 and 0.1, every simulator but Simbrain 3 serial | 33 / 75 min |
| `headed` | `s4`, `s3` | the 1000-neuron network with the GUI open (no GUI, display decoupled, every step drawn; with and without a raster plot) | 25 / 20 min |
| | `training` | the MLP, CNN and tiny language model demos training as a user runs them: no GUI, network window open, training dialog open too | 15 / 25 min |
| | `inference` | the MLP and CNN demos classifying their test set one example per step: no GUI, display decoupled, every example drawn | 3 / 4 min |
| | `generation` | the tiny language model demo generating text one token per step, at both sizes: no GUI, display decoupled, every token drawn | 5 / 5 min |
| | `large` | Simbrain 4 at 8000 neurons and 6.4 million synapses, without and with the GUI (display decoupled); `--suites large --heap 24g` (12g on a 16 GB laptop); `--sizes 2000,4000,8000` adds the sweep's other sizes at p = 0.1 | 15 / 35 min at 8000 |
| | `large_paced`, `large_s3` | Simbrain 4 drawing every step, and Simbrain 3 (no GUI, every step drawn, GUI off), at the `--sizes` | |
| `torch-threads` | | every PyTorch comparison at 1, 2, 4, 8, 16 and all threads | 30 / 30 min |

`bench run --suites smoke` is a quick check that every simulator starts. `--heap` sets the Java heap; the default,
10g, ran the 8000-neuron sweep on a 16 GB laptop.

### Getting stable numbers

- Close other work. Benchmarks share the machine with everything else running on it.
- Keep laptops on power and awake: `caffeinate -i` on macOS, `systemd-inhibit` on Linux.
- For `headed`, keep the screen on and unlocked for the whole run (macOS: `caffeinate -d`; KDE:
  `kde-inhibit --power --screenSaver`), because a locked or sleeping display throttles drawing. The published Linux
  runs used a 4K display with `GDK_SCALE=2`.

## Files

| File | Role |
|---|---|
| `bench`, `bench.py` | setup and entry point; `bench` runs `bench.py` in this folder's uv environment |
| `_common.py` | what the runners and summarizers share: running a harness and recording its rows, the meta row, the suites' shared parameters, reading results with later files superseding earlier ones |
| `pyproject.toml`, `uv.lock` | Python environment: Python 3.12, PyTorch 2.14.1 (CPU), Brian2 2.10.1, NumPy 2.5.3 |
| `run_benchmarks.py`, `run_headed.py`, `torch_threads.py` | suite runners; each run is one process, appending one JSON line |
| `summarize.py`, `summarize_headed.py`, `summarize_torch_threads.py` | medians over repeats; later files supersede earlier ones for the same measurement (machine, suite and simulator for the headless suites; machine, network or demo, and mode for the headed ones; machine, model, task, precision and thread count for PyTorch's threads). `bench summarize` leaves out files named `*.discarded.jsonl` |
| `izh_brian2.py` | the Izhikevich network in Brian2 |
| `mnist_torch.py`, `cnn_torch.py`, `tinylm_torch.py`, `mnist_infer_torch.py` | the PyTorch replicas of the MLP, the CNN, the tiny language model and test-set inference |
| `torch_models.py` | the data loading, MLP, CNN and training epoch the Tiny MNIST replicas share |
| `simbrain3/*.java` | Simbrain 3 harnesses (headless network; network with the GUI open) |
| `cp.init.gradle` | Gradle init script that prints Simbrain 4's test classpath |
| `HEADED_SPEC.md` | what the GUI-open runs measure, shared by the Simbrain 3 and 4 harnesses |

The Simbrain 4 harnesses are in `src/test/kotlin/org/simbrain/benchmarks/`: `IzhikevichBenchmark`, `MnistBenchmark`,
`CnnBenchmark`, `TinyLmBenchmark` and `MnistInferenceBenchmark` without the GUI, and `IzhikevichHeadedBenchmark`,
`TrainingHeadedBenchmark`, `InferenceHeadedBenchmark` and `GenerationHeadedBenchmark` with it (sharing `HeadedSupport`).
Each takes its parameters on the command line (see its header) and prints one JSON line, so it can also be run on its
own with the classpath from `build/benchmarks/s4cp.txt`. The four Izhikevich harnesses (both Simbrains, with and
without the GUI) and `izh_brian2.py` take their numeric arguments in one order: neurons, connection probability, dt,
simulated ms, warm-up ms, seed, weight scale. The ML harnesses share their networks through `BenchmarkModels.kt` and
their output through `BenchmarkOutput.kt`.

## Notes on the comparison

- Every simulator runs the same Izhikevich (2003) network: forward Euler at dt = 0.5 ms, Gaussian noise current, a
  spike delivering a one-step current pulse. Each run records its mean firing rate (about 1.35 Hz), which should agree
  across simulators. Simbrain 4's neuron arrays use the excitatory parameters for every neuron, since an array has a
  single update rule. Every simulator starts at v = -65 mV and u = b * v; Simbrain 3's rule has no setter for u, so its
  harnesses set it by reflection.
- Each run times only the simulation after an untimed warm-up in the same process: JIT compilation for the Simbrains,
  first-run setup (code object loading) for Brian2. In Brian2's C++ standalone mode the binary's own timing of the last
  run is used, so code generation and compilation are excluded.
- Both Simbrains run on the same JDK, so neither gets a newer JIT. Result files from before 2026-10-22 ran Simbrain 3
  on Java 25, which made it about 10% faster; rerun `bench setup` after updating to rebuild Simbrain 3 with JDK 21.
- Simbrain computes in double precision, so PyTorch's float64 runs are the like-for-like comparison.
- PyTorch inference times the forward pass plus the output softmax, which the Simbrain networks compute in their output
  layer; inputs and labels are prepared before the timed loop, as Simbrain's are.
- Simbrain runs with its defaults: OpenBLAS limited to 4 threads for matrix-matrix products, and a network-update
  pool of one worker per core minus one. Brian2 runs single-threaded, its default.
- The PyTorch runs pin their thread count with `torch.set_num_threads`. For these small models, PyTorch is often fastest
  at 1 to 8 threads rather than all of them, which is what `torch-threads` measures.
