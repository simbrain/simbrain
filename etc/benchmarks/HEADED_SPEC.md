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

## Representation (the GUI-relevant part)

- ONE neuron group holding all 1000 neurons (S3 `NeuronGroup`; S4 `NeuronCollection` of free neurons), grid layout,
  32 columns, 20 px spacing in S4 terms (S3: whatever its GridLayout gives at comparable spacing).
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
- Count frames: install a RepaintManager subclass that counts `paintDirtyRegions` calls that had dirty regions.
- Print one JSON line: simulator ("simbrain3-gui" / "simbrain-gui"), variant ("network" | "raster"), mode, neurons,
  synapses, dt_ms, sim_ms, build_s, warmup_s, run_s, rt_factor (run_s per simulated s), iterations_per_s,
  frames, frames_per_s, rate_hz. Then exit.
