# Headed (GUI-open) benchmark spec

Shared definition for the Simbrain 3 and Simbrain 4 GUI benchmarks, so both measure the same thing. The paper's Linux
runs used a 3840x2160 display at 120 Hz under Xwayland with `GDK_SCALE=2` for both JDKs, so windows are a realistic size
on a 4K screen; the macOS runs used the laptop's built-in display at its default scaling.

Simbrain 3 caveat: the harness runs on commit e274a943b of the `Simbrain3` branch (v3.07+15, 2025-04-22), newer than the
2016 paper's 3.0. The paper reports no
GUI-open numbers, so these are new measurements of late Simbrain 3, not a reproduction of the paper.

## Network (identical to the headless IzhikevichBenchmark "objects" variant)

- 1000 Izhikevich (2003) neurons, first 80% excitatory: a=0.02, b=0.2, c=-65+15r^2, d=8-6r^2, Gaussian noise
  sigma 5; inhibitory: a=0.02+0.08r, b=0.25-0.05r, c=-65, d=2, noise sigma 2. iBg 0, threshold 30, v0 -65.
- Random connections, p=0.1, no self-connections; weight 0.5*U(0,1) from excitatory sources, -U(0,1) from
  inhibitory sources (weight scale 1). Step spike responders lasting one time step. Synapses frozen/static.
- dt = 0.5 ms. Firing rate should come out ~1.35 Hz (check it: same dynamics in every mode).
- Both harnesses take `mode variant [representation] neurons p dtMs simMs warmupMs seed [weightScale]`, the numeric
  arguments in the order of the headless harnesses. Repeat r uses seed r + 1, so each repeat is a different network
  and a rerun times the same ones. Simbrain 4's headed and headless harnesses build the same network from a seed;
  Simbrain 3 draws from Java's generator, so its networks differ from Simbrain 4's for the same seed.
- Larger networks: the size sweep's, at p=0.1 with every weight scaled by 100 / in-degree so firing rates stay
  comparable; grid columns ceil(sqrt(neurons)).
  Drawing every step of free neurons runs 50 simulated ms after 10 above 1000 neurons, and free neurons simulate
  1000 ms after 250 at 8000 neurons.

## Representation (the GUI-relevant part)

- ONE neuron group holding all 1000 neurons (S3 `NeuronGroup`; S4 `NeuronCollection` of free neurons), grid layout,
  32 columns, 25 px apart in S4 (the demo's spacing) and 20 px in S3.
- ONE recurrent synapse group holding all ~100k synapses (S3 `SynapseGroup`; S4 `SynapseGroup`), synapses hidden
  (drawn as the group's arrow).
- Simbrain 3 update: ConcurrentBufferedUpdate (its multithreaded mode). Simbrain 4: default update (parallel).
- Optional raster plot coupled to the group's spikes ("raster" variant), placed beside the network window.

## Windows

- Desktop frame 1600 x 1000 (logical px), at (0, 0).
- Network window at (10, 10), 800 x 800, network zoomed to fit.
- Raster window (raster variant only) at (820, 10), 700 x 500.

## Modes

- `headless`: no desktop (S4: no SimbrainDesktop; S3: no SimbrainDesktop/InterceptingEventQueue), same build.
- `paced`: GUI open, every iteration drawn before the next starts (S3's normal behaviour via
  InterceptingEventQueue; S4 `workspace.runAsFastAsPossible = false`, the default).
- `fast` (S4 only): GUI open, `runAsFastAsPossible = true`; views refresh at frame rate.
- `guioff` (S3 only): GUI open but the network panel's "GUI on" toggle off (S3's documented way to speed up).

## Procedure and output

- Build, show, let the GUI settle (wait for the event queue to drain).
- Warm-up 500 simulated ms, then time 2000 simulated ms (4000 iterations), iterating through the workspace (not the
  network directly), so couplings and the plot update as in normal use.
- Count frames: an invisible node on the network canvas's camera, covering the whole view, counts the canvas's paints.
- Print one JSON line: simulator ("simbrain3-gui" / "simbrain-gui"), variant ("network" | "raster"), mode, neurons,
  p, weight_scale, seed, synapses, dt_ms, sim_ms, warmup_ms, build_s, warmup_s, run_s, rt_factor (run_s per simulated
  s), iterations_per_s, frames, frames_per_s, rate_hz. Then exit.

## Machine learning demos (Simbrain 4 only)

Each run opens the registered demo itself, with its own windows, layout and training settings, so the numbers are what a
user sees. The headless mode is the same demo without a desktop and is the baseline for the GUI's cost; the tuned
headless comparisons with PyTorch are separate (MnistBenchmark, CnnBenchmark, TinyLmBenchmark, MnistInferenceBenchmark).

- Training (TrainingHeadedBenchmark): the Feed-Forward MNIST demo (batches of 35), the CNN MNIST demo (batches of 32)
  and the Tiny Language Model demo (24,20,4,30,1 and 64,128,4,512,2, trained on `casual_texting.txt` like
  TinyLmBenchmark; one iteration is one pass over the text). Each demo's trainer tests every 10 iterations, as in the
  demo. Modes: `headless`, `network` (the network window open) and `dialog` (the demo's training dialog open too, as
  its menu opens it, with training started through the dialog's own trainer). Timed after a warm-up: 2000 iterations
  (MNIST demos), 100 and 10 (tiny language model).
- Inference (InferenceHeadedBenchmark): the two MNIST demos, briefly trained, classify their test set one example per
  workspace step, the example clamped on the input layer, as the input table's "Apply inputs" does. Modes: `headless`,
  `fast` (display decoupled) and `paced` (every example drawn). Timed: 10000 examples after 5000 (headless, fast), 1000
  after 200 (paced).
- Generation (GenerationHeadedBenchmark): the Tiny Language Model demo, briefly trained, generates text after a prompt
  typed into its text world (the first 8 words of the training text), one token per workspace step as Play does; timing
  starts once the context window is full. Modes: `headless`, `fast` (display decoupled, the demo's own setting) and
  `paced` (every token drawn). Timed: 5000 tokens after 500 at 24,20,4,30,1 and 1000 after 100 at 64,128,4,512,2
  (paced: 1000 after 200, 300 after 100).
- Output: one JSON line with the demo (`benchmark`), mode, iterations, examples or tokens, run_s, iterations_per_s and
  epoch_equivalent_s (training), us_per_example (inference) or ms_per_token (generation), frames and frames_per_s of
  the network canvas, and the test accuracy or training loss.
